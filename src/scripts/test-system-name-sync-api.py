#!/usr/bin/env python3
"""显式授权后运行的系统名称同步回归测试，仅创建并清理本次唯一前缀的引用聚合。
使用正常登录生成的会话，不创建账号，不打印令牌或数据库凭据。
受控回滚用例只锁定本次组织，并在独立事务将其 version 加一制造真实乐观锁冲突。
"""
import argparse
import concurrent.futures
import copy
import json
from pathlib import Path
import re
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--session-file', required=True, type=Path)
parser.add_argument('--report', required=True, type=Path)
args = parser.parse_args()
config = json.loads((ROOT / 'src/aspire/DedsiArche.AppHost/appsettings.json').read_text())
base = f"http://localhost:{config['Ports']['Backend']}{config['Backend']['ContextPath']}/api"
token = json.loads(args.session_file.read_text())['accessToken']
prefix = 'system_sync_' + uuid.uuid4().hex[:12]
owned = {kind: [] for kind in ('systems', 'permissions', 'positions', 'dictionaries', 'organizations')}
lock_name = prefix + '_rollback'
results = []
http_count = 0
http_count_lock = threading.Lock()


def http(method, path, body=None, authenticated=True):
    """请求只输出状态摘要，绝不将认证头或完整响应写入报告。"""
    global http_count
    headers = {'Content-Type': 'application/json'}
    if authenticated:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base + path, method=method, headers=headers,
                                     data=None if body is None else json.dumps(body).encode())
    with http_count_lock:
        http_count += 1
    try:
        response = urllib.request.urlopen(request, timeout=35)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        payload = response.read()
        # 非 JSON 服务错误不回显原始正文，避免包含环境信息。
        try:
            return response.status, json.loads(payload) if payload else None
        except json.JSONDecodeError:
            return response.status, None


def request(method, path, status=200, body=None, authenticated=True):
    actual, value = http(method, path, body, authenticated)
    assert actual == status, f'{method} {path}: expected {status}, actual {actual}'
    return value


def passed(name):
    results.append({'name': name, 'passed': True})
    print('PASS:', name, flush=True)


def psql_command():
    return ['docker', 'exec', '-i', 'dedsiarche-aspire-postgres', 'psql', '-X', '-At',
            '-v', 'ON_ERROR_STOP=1', '-U', config['Parameters']['postgres-user'],
            '-d', config['PostgreSQL']['Database']]


def sql(query):
    """仅供迁移元数据、专用前缀记录和本次锁会话核对，错误正文不输出。"""
    try:
        process = subprocess.run(psql_command(), input=query, text=True, capture_output=True,
                                 timeout=15, check=False)
    except subprocess.TimeoutExpired:
        raise AssertionError('Database check timed out; connection detail suppressed') from None
    assert process.returncode == 0, 'Database check failed; database detail suppressed'
    return process.stdout.strip()


def checked_id(value):
    assert re.fullmatch(r'[0-7][0-9A-HJKMNP-TV-Z]{25}', value), 'Unexpected fixture ID'
    return value


def create(kind, body):
    value = request('POST', '/' + kind, 201, body)
    owned[kind].append(checked_id(value['id']))
    return value



def snapshot():
    """只读取本次创建的聚合，保留完整字段用于逐项回滚核对。"""
    return {kind: [request('GET', '/' + kind + '/' + identifier) for identifier in identifiers]
            for kind, identifiers in owned.items()}


def canonical(value):
    value = copy.deepcopy(value)
    for field in ('permissions', 'items'):
        if field in value:
            value[field].sort(key=lambda item: item['id'])
    return value


def assert_renamed(before, after, name):
    """版本恰好加一，除名称和更新时间外全部字段（含关联 ID）必须保持。"""
    for kind in before:
        assert len(before[kind]) == len(after[kind])
        for old, current in zip(before[kind], after[kind]):
            expected = copy.deepcopy(old)
            expected['version'] += 1
            expected['updatedAt'] = current['updatedAt']
            if kind == 'systems':
                expected['name'] = name
            else:
                expected['systemName'] = name
            if kind == 'positions':
                for child in expected['permissions']:
                    child['systemName'] = name
            assert canonical(current) == canonical(expected), kind + ': unexpected rename fields or version'
            assert current['updatedAt'] >= old['updatedAt'], kind + ': timestamp went backwards'


def rollback_at_last_organization(before):
    """在监听器最后组织 UPDATE 处注入版本冲突，前面所有领域已 flush 也必须回滚。"""
    target = max(before['organizations'], key=lambda value: value['id'])
    target_id = checked_id(target['id'])
    system = before['systems'][0]
    locker = subprocess.Popen(psql_command(), stdin=subprocess.PIPE, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL, text=True)
    executor = concurrent.futures.ThreadPoolExecutor(max_workers=1)
    try:
        locker.stdin.write(
            f"SET application_name='{lock_name}'; SET lock_timeout='5s'; "
            "SET statement_timeout='15s'; SET idle_in_transaction_session_timeout='20s'; BEGIN; "
            f"SELECT id FROM sys_organizations WHERE id='{target_id}' AND system_code='{prefix}' FOR UPDATE;\n")
        locker.stdin.flush()
        locker_pid = None
        for _ in range(60):
            value = sql(f"SELECT pid FROM pg_stat_activity WHERE application_name='{lock_name}' "
                        "AND state='idle in transaction';")
            if value:
                assert value.isdigit(), 'Unexpected controlled lock session'
                locker_pid = int(value)
                break
            assert locker.poll() is None, 'Controlled lock session exited'
            time.sleep(.05)
        assert locker_pid is not None, 'Organization lock was not acquired'
        future = executor.submit(http, 'PATCH', '/systems/' + system['id'],
                                 {'version': system['version'], 'name': '不得提交的名称'})
        blocked = False
        for _ in range(80):
            value = sql("SELECT count(*) FROM pg_stat_activity "
                        f"WHERE {locker_pid} = ANY(pg_blocking_pids(pid)) "
                        "AND wait_event_type='Lock' AND lower(query) LIKE '%update%sys_organizations%';")
            if value != '0':
                blocked = True
                break
            if future.done():
                break
            time.sleep(.05)
        assert blocked, 'Rename did not block at the controlled organization UPDATE'
        locker.stdin.write(
            f"UPDATE sys_organizations SET version=version+1 WHERE id='{target_id}' AND system_code='{prefix}'; "
            "COMMIT;\n\\q\n")
        locker.stdin.flush()
        locker.stdin.close()
        assert locker.wait(timeout=10) == 0, 'Fixture version injection failed'
        status, problem = future.result(timeout=15)
        assert status == 409, f'Expected true optimistic conflict, actual {status}'
        assert problem and problem.get('detail') == '数据已变化，请重新读取后提交', 'Expected database optimistic-lock conflict'
        expected = copy.deepcopy(before)
        next(value for value in expected['organizations'] if value['id'] == target_id)['version'] += 1
        after = snapshot()
        for kind in expected:
            assert [canonical(value) for value in after[kind]] == [canonical(value) for value in expected[kind]], \
                kind + ': aggregate escaped rollback'
        passed('Real organization optimistic conflict rolls back system, permissions, positions, dictionaries and organizations')
    finally:
        if locker.poll() is None:
            try:
                locker.stdin.write('ROLLBACK;\n\\q\n')
                locker.stdin.flush()
                locker.stdin.close()
            except (BrokenPipeError, ValueError):
                pass
            try:
                locker.wait(timeout=5)
            except subprocess.TimeoutExpired:
                locker.terminate()
                locker.wait(timeout=5)
        executor.shutdown(wait=True, cancel_futures=True)


failure = None
try:
    assert sql("SELECT success FROM flyway_schema_history WHERE version='14';") == 't'
    system = create('systems', {'code': prefix, 'name': '系统名称同步测试'})
    permissions = [create('permissions', {'systemCode': prefix, 'code': prefix + '_p' + str(index),
                                          'name': '权限' + str(index)}) for index in range(2)]
    position = create('positions', {'systemCode': prefix, 'code': prefix + '_position', 'name': '测试岗位',
                                    'permissionIds': [value['id'] for value in permissions]})
    for index in range(2):
        create('dictionaries', {'systemCode': prefix, 'code': prefix + '_d' + str(index),
            'name': '字典' + str(index), 'enabled': index == 0,
            'items': [{'code': 'first', 'name': '首项', 'sortOrder': 0, 'isDefault': True, 'enabled': True},
                      {'code': 'disabled', 'name': '停用项', 'sortOrder': 1, 'isDefault': False, 'enabled': False}]})
    parent = create('organizations', {'systemCode': prefix, 'code': prefix + '_root', 'name': '测试父组织'})
    create('organizations', {'systemCode': prefix, 'code': prefix + '_child', 'name': '测试子组织',
                            'parentId': parent['id'], 'enabled': False})
    before = snapshot()
    assert len(before['positions'][0]['permissions']) == 2
    assert all(len(value['items']) == 2 for value in before['dictionaries'])
    passed('Dedicated fixtures include complete children and disabled dictionary/organization')
    renamed = request('PATCH', '/systems/' + system['id'], body={'version': system['version'], 'name': '同步后的名称'})
    after = snapshot()
    assert_renamed(before, after, renamed['name'])
    passed('System rename updates every reference exactly once and preserves all other fields and child identities')
    same = request('PATCH', '/systems/' + system['id'], body={'version': renamed['version'], 'name': renamed['name']})
    unchanged = snapshot()
    for kind in owned:
        if kind != 'systems':
            assert [canonical(value) for value in unchanged[kind]] == [canonical(value) for value in after[kind]], \
                kind + ': same system name changed a reference'
    assert unchanged['systems'][0] == same
    passed('Same system name does not update any reference version or timestamp')
    rollback_at_last_organization(unchanged)
except Exception as error:
    failure = ('Subprocess timed out; connection detail suppressed'
               if isinstance(error, subprocess.TimeoutExpired) else str(error) or type(error).__name__)
    results.append({'name': 'failure', 'passed': False, 'detail': failure})
    print('FAIL:', failure, flush=True)
finally:
    cleanup_errors = []
    # 恢复创建响应中断时的本次标识，仅接纳本次唯一系统编码与业务编码前缀。
    for kind in owned:
        try:
            body = {'keyword': prefix, 'page': 1, 'size': 100}
            if kind != 'systems':
                body['systemCode'] = prefix
            page = request('POST', '/' + kind + '/page', body=body)
            for value in page['items']:
                if (value.get('code', '').startswith(prefix)
                        and (kind == 'systems' and value['code'] == prefix or value.get('systemCode') == prefix)):
                    identifier = checked_id(value['id'])
                    if identifier not in owned[kind]:
                        owned[kind].append(identifier)
        except Exception:
            cleanup_errors.append({'kind': kind, 'stage': 'recover_fixture_ids'})
    # 先按当前父关系删除组织叶子，避免依赖创建顺序；从不扩大到其他系统节点。
    pending = set(owned['organizations'])
    for _ in range(len(pending) + 1):
        if not pending:
            break
        nodes = {}
        for identifier in list(pending):
            try:
                status, value = http('GET', '/organizations/' + identifier)
                if status == 404:
                    pending.remove(identifier)
                elif status == 200 and value.get('systemCode') == prefix:
                    nodes[identifier] = value
            except Exception:
                pass
        parents = {value['parentId'] for value in nodes.values()}
        for identifier, value in nodes.items():
            if identifier not in parents:
                try:
                    request('DELETE', f'/organizations/{identifier}?version={value["version"]}', 204)
                    pending.remove(identifier)
                except Exception:
                    pass
    cleanup_errors.extend({'kind': 'organizations', 'id': value} for value in pending)
    for kind in ('dictionaries', 'positions', 'permissions', 'systems'):
        for identifier in reversed(owned[kind]):
            try:
                status, value = http('GET', '/' + kind + '/' + identifier)
                if status == 404:
                    continue
                assert status == 200
                assert (value['code'] == prefix if kind == 'systems' else value['systemCode'] == prefix)
                request('DELETE', f'/{kind}/{identifier}?version={value["version"]}', 204)
            except Exception:
                cleanup_errors.append({'kind': kind, 'id': identifier})
    remaining = {}
    try:
        for kind in ('systems', 'permissions', 'positions', 'dictionaries', 'organizations'):
            table = {'systems': 'sys_systems', 'permissions': 'sys_permissions', 'positions': 'sys_positions',
                     'dictionaries': 'sys_dictionaries', 'organizations': 'sys_organizations'}[kind]
            column = 'code' if kind == 'systems' else 'system_code'
            remaining[kind] = int(sql(f"SELECT count(*) FROM {table} WHERE {column}='{prefix}';"))
        for kind, table, column in [('positions', 'sys_position_permissions', 'position_id'),
                                    ('dictionaries', 'sys_dictionary_items', 'dictionary_id')]:
            identifiers = ','.join("'" + checked_id(value) + "'" for value in owned[kind])
            remaining[table] = int(sql(f"SELECT count(*) FROM {table} WHERE {column} IN ({identifiers});")) if identifiers else 0
        remaining['lockSessions'] = int(sql(f"SELECT count(*) FROM pg_stat_activity WHERE application_name='{lock_name}';"))
    except Exception:
        cleanup_errors.append({'stage': 'verify_fixture_cleanup'})
    report = {'prefix': prefix, 'results': results, 'httpRequests': http_count, 'cleanupErrors': cleanup_errors,
              'remaining': remaining, 'passed': failure is None and not cleanup_errors and len(remaining) == 8
              and all(value == 0 for value in remaining.values())}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('Cleanup remaining:', json.dumps(remaining), flush=True)
    print('Cleanup:', 'complete' if not cleanup_errors and len(remaining) == 8 and not any(remaining.values()) else 'INCOMPLETE', flush=True)
if not report['passed']:
    raise SystemExit(1)
