#!/usr/bin/env python3
"""显式授权后运行的字典 API 回归测试，仅创建并清理本次唯一前缀的系统和字典。
使用正常登录生成的会话，不创建账号，不打印令牌或数据库凭据。
受控回滚用例只锁定本次字典，并在独立事务将其 version 加一制造真实乐观锁冲突。
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
prefix = 'dictionary_api_' + uuid.uuid4().hex[:12]
owned = {'dictionaries': [], 'systems': []}
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


def detail(dictionary):
    return request('GET', '/dictionaries/' + dictionary['id'])


def update(dictionary, **body):
    return request('PATCH', '/dictionaries/' + dictionary['id'],
                   body={'version': dictionary['version'], **body})


def available(code, status=200):
    params = urllib.parse.urlencode({'systemCode': prefix, 'code': code})
    return request('GET', '/dictionaries/available?' + params, status)


def item(code, name=None, sort=0, default=False, enabled=True):
    return {'code': code, 'name': name or code, 'sortOrder': sort,
            'isDefault': default, 'enabled': enabled}


def inputs(dictionary):
    return [{key: value[key] for key in ('id', 'code', 'name', 'sortOrder', 'isDefault', 'enabled')}
            for value in dictionary['items']]


def canonical(dictionary):
    return {**dictionary, 'items': sorted(dictionary['items'], key=lambda value: value['id'])}


def assert_order(dictionary):
    order = [(value['sortOrder'], value['code'], value['id']) for value in dictionary['items']]
    assert order == sorted(order), 'Dictionary items are not in stable order'


def assert_complete(dictionary):
    fetched = detail(dictionary)
    assert canonical(fetched) == canonical(dictionary), 'Complete response does not match persisted GET'
    assert_order(fetched)
    identifier = checked_id(dictionary['id'])
    stored = sql(f"SELECT coalesce(json_agg(row_to_json(i) ORDER BY i.id), '[]'::json) "
                 f"FROM sys_dictionary_items i WHERE dictionary_id='{identifier}';")
    rows = json.loads(stored)
    expected_rows = [{'id': value['id'], 'dictionary_id': identifier, 'code': value['code'],
                      'name': value['name'], 'sort_order': value['sortOrder'],
                      'is_default': value['isDefault'], 'enabled': value['enabled']}
                     for value in sorted(dictionary['items'], key=lambda value: value['id'])]
    assert rows == expected_rows, 'Persisted child fields differ from complete API response'
    for value in fetched['items']:
        assert set(value) == {'id', 'dictionaryId', 'code', 'name', 'sortOrder', 'isDefault', 'enabled'}
        assert value['dictionaryId'] == dictionary['id']
        checked_id(value['id'])
    return fetched


def controlled_system_rename_rollback(system, dictionaries):
    """只动专用 fixture：阻塞最后一个字典更新，再由持锁事务推进该 fixture 的版本。
    系统更名及前面字典已执行的 UPDATE 应全部回滚；人为版本增量独立提交且随后清理。
    """
    before = sorted([detail(value) for value in dictionaries], key=lambda value: value['id'])
    target_id = checked_id(before[-1]['id'])
    lock_name = prefix + '_rollback'
    locker = subprocess.Popen(psql_command(), stdin=subprocess.PIPE, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL, text=True)
    executor = concurrent.futures.ThreadPoolExecutor(max_workers=1)
    future = None
    try:
        # 超时仅属于本次持锁会话，防止脚本中断后长期保留锁；不改全局参数。
        locker.stdin.write(
            f"SET application_name='{lock_name}'; SET lock_timeout='5s'; "
            "SET statement_timeout='15s'; SET idle_in_transaction_session_timeout='20s'; BEGIN; "
            f"SELECT id FROM sys_dictionaries WHERE id='{target_id}' AND system_code='{prefix}' FOR UPDATE;\n")
        locker.stdin.flush()
        locker_pid = None
        for _ in range(60):
            value = sql(f"SELECT pid FROM pg_stat_activity WHERE application_name='{lock_name}' "
                        "AND state='idle in transaction';")
            if value:
                assert value.isdigit(), 'Unexpected controlled-lock session'
                locker_pid = int(value)
                break
            assert locker.poll() is None, 'Controlled lock session exited unexpectedly'
            time.sleep(.05)
        assert locker_pid is not None, 'Controlled dictionary row lock was not acquired'
        future = executor.submit(http, 'PATCH', '/systems/' + system['id'],
                                 {'version': system['version'], 'name': '不得提交的系统名称'})
        blocked = False
        for _ in range(80):
            count = sql("SELECT count(*) FROM pg_stat_activity "
                        f"WHERE {locker_pid} = ANY(pg_blocking_pids(pid)) "
                        "AND wait_event_type='Lock' AND lower(query) LIKE '%update%sys_dictionaries%';")
            if count != '0':
                blocked = True
                break
            if future.done():
                break
            time.sleep(.05)
        assert blocked, 'System rename did not reach the controlled dictionary UPDATE'
        locker.stdin.write(
            f"UPDATE sys_dictionaries SET version=version+1 WHERE id='{target_id}' AND system_code='{prefix}'; "
            "COMMIT;\n\\q\n")
        locker.stdin.flush()
        locker.stdin.close()
        assert locker.wait(timeout=10) == 0, 'Controlled fixture version transaction failed'
        status, problem = future.result(timeout=15)
        assert status == 409, f'Expected database rollback conflict, actual {status}'
        assert problem and problem.get('detail') == '数据已变化，请重新读取后提交', 'Expected actual database optimistic-lock failure'
        assert request('GET', '/systems/' + system['id']) == system, 'System rename or version escaped rollback'
        after = []
        for previous in before:
            current = detail(previous)
            expected = copy.deepcopy(previous)
            if previous['id'] == target_id:
                expected['version'] += 1  # 独立持锁事务的唯一预期改动。
            assert canonical(current) == canonical(expected), 'Dictionary change escaped system transaction rollback'
            assert_complete(current)
            after.append(current)
        passed('Database optimistic conflict rolls back system rename and every previously updated dictionary')
        return after
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
                # 仅取消本次专用锁会话，连接中断后 PostgreSQL 自动回滚该事务。
                locker.terminate()
                locker.wait(timeout=5)
        executor.shutdown(wait=True, cancel_futures=True)


failure = None
try:
    assert sql("SELECT success FROM flyway_schema_history WHERE version='13';") == 't'
    assert sql("SELECT count(*) FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid "
               "JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' "
               "AND c.relname IN ('sys_dictionaries','sys_dictionary_items') "
               "AND a.attnum>0 AND NOT a.attisdropped AND col_description(c.oid,a.attnum) IS NOT NULL;") == '17'
    passed('V13 migration and all 17 dictionary column comments are present')
    request('POST', '/dictionaries/page', 401, {}, authenticated=False)
    request('GET', '/dictionaries/available?systemCode=missing&code=missing', 401, authenticated=False)
    passed('Anonymous management and business reads are rejected')
    system = create('systems', {'code': prefix, 'name': '字典专用测试系统'})
    dictionary = create('dictionaries', {'systemCode': prefix, 'code': 'primary', 'name': '主字典',
        'description': '本次测试专用', 'items': [item('alpha', '甲', 2, True),
                                               item('beta', '乙', 1), item('hidden', '隐藏', 0, False, False)]})
    assert_complete(dictionary)
    assert dictionary['systemName'] == system['name'] and dictionary['systemCode'] == prefix
    assert [value['code'] for value in dictionary['items']] == ['hidden', 'beta', 'alpha']
    assert [value['code'] for value in available('primary')['items']] == ['beta', 'alpha']
    available('missing', 404)
    passed('Whole create/read preserves all seven child fields; available excludes disabled items')
    other = create('dictionaries', {'systemCode': prefix, 'code': 'secondary', 'name': '次字典',
                                    'items': [item('foreign', '其他字典子项')]})
    page = request('POST', '/dictionaries/page', body={'systemCode': prefix, 'page': 1, 'size': 10})
    assert page['total'] == 2 and all('items' not in value for value in page['items'])
    request('POST', '/dictionaries', 409, {'systemCode': prefix, 'code': 'primary', 'name': '重复'})
    request('POST', '/dictionaries', 400, {'systemCode': prefix, 'code': 'invalid', 'name': '非法',
            'items': [{**item('fake'), 'id': other['items'][0]['id']}]})
    assert request('POST', '/dictionaries/page', body={'systemCode': prefix, 'keyword': 'invalid'})['total'] == 0
    passed('Summary pagination, unique dictionary code and create-time item ID validation')
    before = dictionary
    replacement = inputs(dictionary)
    for value in replacement:
        value['isDefault'] = value['code'] == 'beta'
    dictionary = update(dictionary, items=replacement)
    assert dictionary['version'] == before['version'] + 1
    assert {value['id'] for value in dictionary['items']} == {value['id'] for value in before['items']}
    assert [value['code'] for value in dictionary['items'] if value['isDefault']] == ['beta']
    assert_complete(dictionary)
    passed('Existing default switches from alpha to beta atomically without transient unique conflicts')
    replacement = inputs(dictionary)
    for value in replacement:
        value['isDefault'] = False
    replacement.append(item('gamma', '新默认项', 3, True))
    dictionary = update(dictionary, items=replacement)
    assert [value['code'] for value in dictionary['items'] if value['isDefault']] == ['gamma']
    assert_complete(dictionary)
    passed('New default item inserts in the same whole-array update that clears the old default')
    before = dictionary
    replacement = [value for value in inputs(dictionary) if value['code'] != 'hidden']
    for value in replacement:
        if value['code'] == 'beta':
            value['name'] = '修改后的乙'
            value['sortOrder'] = 7
    replacement.append(item('delta', '新增丁', 1))
    dictionary = update(dictionary, name='整体变更', items=replacement)
    old_ids = {value['code']: value['id'] for value in before['items']}
    assert 'hidden' not in {value['code'] for value in dictionary['items']}
    for value in dictionary['items']:
        if value['code'] in old_ids:
            assert value['id'] == old_ids[value['code']]
    assert_complete(dictionary)
    passed('Whole update adds, removes and edits children while preserving retained identities')
    replacement = inputs(dictionary)
    for value in replacement:
        value['isDefault'] = False
    dictionary = update(dictionary, items=replacement)
    assert not any(value['isDefault'] for value in dictionary['items'])
    replacement = inputs(dictionary)
    next(value for value in replacement if value['code'] == 'delta')['isDefault'] = True
    dictionary = update(dictionary, items=replacement)
    replacement = [value for value in inputs(dictionary) if value['code'] != 'delta']
    next(value for value in replacement if value['code'] == 'gamma')['isDefault'] = True
    dictionary = update(dictionary, items=replacement)
    assert [value['code'] for value in dictionary['items'] if value['isDefault']] == ['gamma']
    assert_complete(dictionary)
    passed('No default is allowed; deleting the current default and selecting another is atomic')
    invalids = []
    values = inputs(dictionary)
    for value in values:
        value['isDefault'] = value['code'] in ('beta', 'gamma')
    invalids.append(values)
    values = inputs(dictionary)
    next(value for value in values if value['isDefault'])['enabled'] = False
    invalids.append(values)
    invalids.append(inputs(dictionary) + [item('beta', '重复编码')])
    invalids.append(inputs(dictionary) + inputs(other))
    values = inputs(dictionary)
    values[0]['code'] = 'changed-code'
    invalids.append(values)
    invalids.append(inputs(dictionary) + [None])
    for invalid_items in invalids:
        request('PATCH', '/dictionaries/' + dictionary['id'], 400,
                {'version': dictionary['version'], 'name': '不得保存', 'items': invalid_items})
        assert canonical(detail(dictionary)) == canonical(dictionary)
        assert_complete(dictionary)
    passed('Invalid default, disabled default, duplicate code, foreign ID and changed code leave entire aggregate unchanged')
    before = dictionary
    dictionary = update(dictionary, name=None, description=None, enabled=None, items=None)
    assert canonical(dictionary) == canonical(before)
    dictionary = update(dictionary, items=list(reversed(inputs(dictionary))))
    assert canonical(dictionary) == canonical(before)
    request('PATCH', '/dictionaries/' + dictionary['id'], 409,
            {'version': before['version'] - 1, 'items': None})
    passed('Null and identical whole-array updates are idempotent; even stale no-op requests are rejected')
    dictionary = update(dictionary, description='')
    assert dictionary['description'] is None
    stored_items = [{key: value for key, value in entry.items() if key != 'id'} for entry in inputs(dictionary)]
    old_item_ids = {value['id'] for value in dictionary['items']}
    dictionary = update(dictionary, items=[])
    assert not dictionary['items'] and not available('primary')['items']
    assert_complete(dictionary)
    dictionary = update(dictionary, items=stored_items)
    assert old_item_ids.isdisjoint({value['id'] for value in dictionary['items']})
    dictionary = update(dictionary, enabled=False)
    available('primary', 404)
    assert detail(dictionary)['items'] == dictionary['items']
    dictionary = update(dictionary, enabled=True)
    assert_complete(dictionary)
    passed('Empty array clears children, recreation assigns new IDs, disabled root preserves management configuration')
    before = [dictionary, other]
    system = request('PATCH', '/systems/' + system['id'], body={'version': system['version'], 'name': '同步后的系统名称'})
    after = []
    for previous in before:
        current = detail(previous)
        assert current['systemName'] == system['name']
        assert current['version'] == previous['version'] + 1
        assert current['items'] == previous['items']
        request('PATCH', '/dictionaries/' + previous['id'], 409, {'version': previous['version']})
        after.append(current)
    dictionary, other = after
    system = request('PATCH', '/systems/' + system['id'], body={
        'version': system['version'], 'name': system['name'], 'description': '同名系统更新'})
    for current in after:
        assert canonical(detail(current)) == canonical(current)
    request('DELETE', f"/systems/{system['id']}?version={system['version']}", 409)
    assert request('GET', '/systems/' + system['id']) == system
    passed('System rename updates every dictionary name and version exactly once; same name leaves dictionaries unchanged')
    # 系统锁串行化正常编辑；一胜一败仍验证完整集合不会混合，但不将预检冲突称为数据库回滚。
    gate = threading.Barrier(2)
    def compete(index):
        gate.wait()
        return http('PATCH', '/dictionaries/' + dictionary['id'],
                    {'version': dictionary['version'], 'name': '竞态' + str(index),
                     'items': [item('race' + str(index), default=True)]})
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        responses = list(executor.map(compete, [0, 1]))
    assert sorted(value[0] for value in responses) == [200, 409]
    winner = next(value[1] for value in responses if value[0] == 200)
    dictionary = detail(dictionary)
    assert canonical(dictionary) == canonical(winner)
    assert_complete(dictionary)
    passed('Concurrent whole updates have one complete winner and one rejected stale request')
    reloaded = controlled_system_rename_rollback(system, [dictionary, other])
    dictionary = next(value for value in reloaded if value['id'] == dictionary['id'])
    other = next(value for value in reloaded if value['id'] == other['id'])
    request('DELETE', f"/dictionaries/{dictionary['id']}?version={dictionary['version']}", 204)
    request('GET', '/dictionaries/' + dictionary['id'], 404)
    assert sql(f"SELECT count(*) FROM sys_dictionary_items WHERE dictionary_id='{dictionary['id']}';") == '0'
    passed('Whole dictionary deletion removes every child and preserves its system')
except Exception as error:
    failure = ('Subprocess timed out; connection detail suppressed'
               if isinstance(error, subprocess.TimeoutExpired) else str(error) or type(error).__name__)
    results.append({'name': 'Test failure', 'passed': False, 'detail': failure})
    print('FAIL:', failure, flush=True)
finally:
    cleanup_errors = []
    # 先恢复本次前缀下的 ID，处理请求超时但服务端可能已创建成功的情形。
    try:
        page = request('POST', '/dictionaries/page', body={'systemCode': prefix, 'page': 1, 'size': 100})
        for value in page['items']:
            if value['systemCode'] == prefix and value['id'] not in owned['dictionaries']:
                owned['dictionaries'].append(checked_id(value['id']))
        page = request('POST', '/systems/page', body={'keyword': prefix, 'page': 1, 'size': 100})
        for value in page['items']:
            if value['code'] == prefix and value['id'] not in owned['systems']:
                owned['systems'].append(checked_id(value['id']))
    except Exception:
        cleanup_errors.append({'stage': 'recover_fixture_ids'})
    for kind in ('dictionaries', 'systems'):
        for identifier in reversed(owned[kind]):
            try:
                status, value = http('GET', '/' + kind + '/' + identifier)
                if status == 404:
                    continue
                assert status == 200
                if kind == 'systems':
                    assert value['code'] == prefix
                else:
                    assert value['systemCode'] == prefix
                request('DELETE', f'/{kind}/{identifier}?version={value["version"]}', 204)
            except Exception:
                cleanup_errors.append({'kind': kind, 'id': identifier})
    remaining = None
    root_count = None
    child_count = None
    try:
        fixture_ids = ','.join("'" + checked_id(value) + "'" for value in owned['dictionaries'])
        child_count = int(sql(
            f"SELECT count(*) FROM sys_dictionary_items WHERE dictionary_id IN ({fixture_ids});")) if fixture_ids else 0
        root_count = int(sql(
            f"SELECT (SELECT count(*) FROM sys_dictionaries WHERE system_code='{prefix}') + "
            f"(SELECT count(*) FROM sys_systems WHERE code='{prefix}');"))
        remaining = root_count + child_count
    except Exception:
        cleanup_errors.append({'stage': 'verify_fixture_cleanup'})
    report = {'prefix': prefix, 'results': results, 'httpRequests': http_count,
              'cleanupErrors': cleanup_errors, 'remainingTestRecords': remaining, 'remainingRoots': root_count, 'remainingChildren': child_count,
              'passed': failure is None and not cleanup_errors and remaining == 0}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('Cleanup remaining roots/children:', root_count, child_count, flush=True)
    print('Cleanup:', 'complete' if remaining == 0 and not cleanup_errors else 'INCOMPLETE', flush=True)
if not report['passed']:
    raise SystemExit(1)
