#!/usr/bin/env python3
"""显式授权后运行的岗位 API 测试。仅操作唯一前缀的数据，退出时清理本次记录。
会话 JSON 来自正常登录，不包含在仓库内；测试不创建用户、不修改认证配置。
"""
import argparse
import concurrent.futures
import json
from pathlib import Path
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--session-file', required=True, type=Path)
parser.add_argument('--report', required=True, type=Path)
args = parser.parse_args()
config = json.loads((ROOT / 'src/aspire/DedsiArche.AppHost/appsettings.json').read_text())
base = f"http://localhost:{config['Ports']['Backend']}{config['Backend']['ContextPath']}"
token = json.loads(args.session_file.read_text())['accessToken']
prefix = 'position_api_' + uuid.uuid4().hex[:12]
owned = {'positions': [], 'permissions': [], 'systems': []}
results = []


def http(method, path, body=None, authenticated=True):
    headers = {'Content-Type': 'application/json'}
    if authenticated:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(base + '/api' + path, method=method, headers=headers,
                                 data=None if body is None else json.dumps(body).encode())
    try:
        response = urllib.request.urlopen(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        data = response.read()
        return response.status, json.loads(data) if data else None


def request(method, path, status=200, body=None, authenticated=True):
    actual, data = http(method, path, body, authenticated)
    assert actual == status, f'{method} {path}: expected {status}, actual {actual}'
    return data


def passed(name):
    results.append({'name': name, 'passed': True})
    print('PASS:', name, flush=True)


def sql(query):
    proc = subprocess.run(['docker', 'exec', '-i', 'dedsiarche-aspire-postgres',
                           'psql', '-X', '-At', '-v', 'ON_ERROR_STOP=1',
                           '-U', config['Parameters']['postgres-user'],
                           '-d', config['PostgreSQL']['Database']],
                          input=query, text=True, capture_output=True, check=False)
    assert proc.returncode == 0, 'Database inspection failed; database error suppressed'
    return proc.stdout.strip()


def create(kind, body):
    value = request('POST', '/' + kind, 201, body)
    owned[kind].append(value['id'])
    return value


def detail(position):
    return request('GET', '/positions/' + position['id'])


def update(position, **body):
    return request('PATCH', '/positions/' + position['id'], body={'version': position['version'], **body})


def permission_ids(position):
    return {item['permissionId'] for item in position['permissions']}


def canonical(position):
    return {**position, 'permissions': sorted(position['permissions'], key=lambda item: item['id'])}


failure = None
try:
    assert sql("SELECT success FROM flyway_schema_history WHERE version='11';") == 't'
    assert sql("SELECT count(*) FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid "
               "WHERE c.relname IN ('sys_positions','sys_position_permissions') "
               "AND a.attnum>0 AND NOT a.attisdropped AND col_description(c.oid,a.attnum) IS NOT NULL;") == '18'
    passed('V11 migration and all 18 column comments')
    request('POST', '/positions/page', 401, {}, authenticated=False)
    passed('Unauthenticated access rejected')
    system = create('systems', {'code': prefix, 'name': '岗位整存整取测试系统'})
    perms = [create('permissions', {'systemCode': prefix, 'code': 'test:' + str(i),
                                    'name': '测试权限' + str(i)}) for i in range(3)]
    p = create('positions', {'systemCode': prefix, 'code': prefix, 'name': '整体创建', 'description': '专用测试',
                             'permissionIds': [perms[0]['id'], perms[1]['id']]})
    fetched = detail(p)
    assert canonical(fetched) == canonical(p)
    assert len(fetched['permissions']) == 2
    assert fetched['systemCode'] == prefix and fetched['systemName'] == system['name']
    for item in fetched['permissions']:
        source = next(x for x in perms if x['id'] == item['permissionId'])
        assert item['positionId'] == p['id'] and len(item['id']) == 26
        assert (item['systemCode'], item['systemName'], item['permissionCode'], item['permissionName']) == (
            prefix, system['name'], source['code'], source['name'])
    assert sql(f"SELECT count(*) FROM sys_position_permissions WHERE position_id='{p['id']}';") == '2'
    passed('Whole create, complete read, seven child fields and database rows')
    old = p
    kept = next(x['id'] for x in p['permissions'] if x['permissionId'] == perms[1]['id'])
    p = update(p, name='整体修改', permissionIds=[perms[1]['id'], perms[2]['id']])
    assert p['version'] > old['version'] and p['name'] == '整体修改'
    assert permission_ids(p) == {perms[1]['id'], perms[2]['id']}
    assert next(x['id'] for x in p['permissions'] if x['permissionId'] == perms[1]['id']) == kept
    assert canonical(detail(p)) == canonical(p)
    assert sql(f"SELECT count(*) FROM sys_position_permissions WHERE position_id='{p['id']}' AND permission_id='{perms[0]['id']}';") == '0'
    passed('Whole update retains child identity, inserts and removes children atomically')
    for body in [{'name': '不得保存', 'permissionIds': [perms[0]['id'], perms[0]['id']]},
                 {'name': '不得保存', 'permissionIds': ['00000000000000000000000000']},
                 {'name': ' ', 'permissionIds': []}, {'sortOrder': -1},
                 {'permissionIds': [None]}, {'permissionIds': ['bad-id']}]:
        request('PATCH', '/positions/' + p['id'], 400, {'version': p['version'], **body})
        assert canonical(detail(p)) == canonical(p)
    request('PATCH', '/positions/' + p['id'], 409,
            {'version': old['version'], 'name': '旧版本覆盖', 'permissionIds': []})
    assert canonical(detail(p)) == canonical(p)
    passed('Invalid and stale aggregate updates leave all fields and children unchanged')
    request('POST', '/positions', 409, {'systemCode': prefix, 'code': prefix, 'name': '重复', 'permissionIds': []})
    request('POST', '/positions', 400, {'systemCode': prefix, 'code': prefix + '_invalid', 'name': '非法',
                                      'permissionIds': [perms[0]['id'], perms[0]['id']]})
    assert request('POST', '/positions/page', body={'keyword': prefix + '_invalid'})['total'] == 0
    passed('Duplicate code and invalid aggregate creation rejected without residue')
    page = request('POST', '/positions/page', body={'keyword': prefix, 'page': 1, 'size': 10})
    assert page['total'] == 1 and page['items'][0]['id'] == p['id']
    assert 'permissions' not in page['items'][0]
    passed('Pagination contract and aggregate detail separation')
    p = update(p, description='')
    assert p['description'] is None and len(p['permissions']) == 2
    p = update(p, permissionIds=None, name=None)
    assert len(p['permissions']) == 2 and p['name'] == '整体修改'
    p = update(p, permissionIds=[])
    assert p['permissions'] == [] and detail(p)['permissions'] == []
    p = update(p, permissionIds=[perms[0]['id']])
    before = p
    perms[0] = request('PUT', '/permissions/' + perms[0]['id'], body={
        'version': perms[0]['version'], 'name': '刷新后的权限', 'enabled': True,
        'sortOrder': 0, 'description': None})
    p = detail(p)
    assert p['permissions'][0]['permissionName'] == perms[0]['name']
    assert p['permissions'][0]['id'] == before['permissions'][0]['id']
    assert p['version'] > before['version']
    assert p['permissions'][0]['systemName'] == before['permissions'][0]['systemName']
    request('PATCH', '/positions/' + p['id'], 409,
            {'version': before['version'], 'name': '过期编辑'})
    unchanged = p
    perms[0] = request('PUT', '/permissions/' + perms[0]['id'], body={
        'version': perms[0]['version'], 'name': perms[0]['name'], 'enabled': False,
        'sortOrder': 1, 'description': '仅修改其他字段'})
    assert canonical(detail(p)) == canonical(unchanged)
    p = update(p, permissionIds=[perms[0]['id']])
    assert p['permissions'][0]['id'] == before['permissions'][0]['id']
    passed('Null preserves, empty clears, synchronous rename advances root version; same name does not')
    request('DELETE', f"/permissions/{perms[0]['id']}?version={perms[0]['version']}", 409)
    assert canonical(detail(p)) == canonical(p)
    passed('Referenced permission deletion rejected without altering aggregate')
    # 同版本并发提交不同整聚合，胜者内容必须完整保留，败者不得残留子实体。
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for iteration in range(10):
            gate = threading.Barrier(2)
            def compete(index):
                body = {'version': p['version'], 'name': f'race-{index}',
                        'permissionIds': [perms[index]['id']]}
                gate.wait()
                return http('PATCH', '/positions/' + p['id'], body)
            responses = list(pool.map(compete, [0, 1]))
            assert sorted(x[0] for x in responses) == [200, 409], str([x[0] for x in responses])
            winner = next(x[1] for x in responses if x[0] == 200)
            p = detail(p)
            assert canonical(p) == canonical(winner)
            index = int(p['name'][-1])
            assert permission_ids(p) == {perms[index]['id']}
            assert sql(f"SELECT count(*) FROM sys_position_permissions WHERE position_id='{p['id']}';") == '1'
    passed('10 concurrent whole-update races: one winner, one 409, no mixed or orphaned children')
    # 只锁定本次岗位，确保两个请求读到同一版本后在数据库写入处竞争。
    lock_name = prefix + '_lock'
    lock_sql = (f"SET application_name='{lock_name}'; BEGIN; "
                f"SELECT id FROM sys_positions WHERE id='{p['id']}' FOR UPDATE; "
                "SELECT pg_sleep(3); COMMIT;")
    locker = subprocess.Popen(['docker', 'exec', '-i', 'dedsiarche-aspire-postgres',
                               'psql', '-X', '-At', '-v', 'ON_ERROR_STOP=1',
                               '-U', config['Parameters']['postgres-user'],
                               '-d', config['PostgreSQL']['Database']],
                              stdin=subprocess.PIPE, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL, text=True)
    locker.stdin.write(lock_sql)
    locker.stdin.close()
    try:
        for _ in range(30):
            if sql(f"SELECT count(*) FROM pg_stat_activity WHERE application_name='{lock_name}' AND wait_event='PgSleep';") == '1':
                break
            time.sleep(.05)
        else:
            raise AssertionError('Controlled row lock was not acquired')
        gate = threading.Barrier(2)
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            responses = list(pool.map(compete, [0, 1]))
        assert sorted(x[0] for x in responses) == [200, 409]
        loser = next(x[1] for x in responses if x[0] == 409)
        assert loser.get('detail') == '数据已变化，请重新读取后提交', 'Expected database optimistic lock conflict'
        winner = next(x[1] for x in responses if x[0] == 200)
        p = detail(p)
        assert canonical(p) == canonical(winner)
        assert sql(f"SELECT count(*) FROM sys_position_permissions WHERE position_id='{p['id']}';") == '1'
        passed('Controlled database optimistic lock failure rolls back the entire losing aggregate')
    finally:
        assert locker.wait(timeout=10) == 0, 'Controlled row lock session failed'
    # 同时读取和交替更新：每个响应的岗位名称与权限必须属于同一版本。
    def writer():
        value = p
        for i in range(30):
            index = i % 2
            value = update(value, name=f'race-{index}', permissionIds=[perms[index]['id']])
        return value
    def reader():
        for _ in range(60):
            value = detail(p)
            assert permission_ids(value) == {perms[int(value['name'][-1])]['id']}
        return 60
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        w = pool.submit(writer)
        r = pool.submit(reader)
        p = w.result()
        assert r.result() == 60
    passed('60 concurrent full reads during 30 updates retain consistent root and children')
    request('DELETE', f"/positions/{p['id']}?version={old['version']}", 409)
    request('DELETE', f"/positions/{p['id']}?version={p['version']}", 204)
    request('GET', '/positions/' + p['id'], 404)
    assert sql(f"SELECT count(*) FROM sys_position_permissions WHERE position_id='{p['id']}';") == '0'
    for permission in perms:
        request('GET', '/permissions/' + permission['id'])
    passed('Whole deletion cascades children while retaining independent permission definitions')
    empty = create('positions', {'systemCode': prefix, 'code': prefix + '_empty', 'name': '空权限岗位'})
    assert empty['permissions'] == [] and detail(empty)['permissions'] == []
    passed('Empty aggregate create and full read')
    before = empty
    system = request('PATCH', '/systems/' + system['id'], body={
        'version': system['version'], 'name': '事件同步后的系统'})
    empty = detail(empty)
    assert empty['systemName'] == system['name'] and empty['version'] > before['version']
    for index, permission in enumerate(perms):
        perms[index] = request('GET', '/permissions/' + permission['id'])
        assert perms[index]['systemName'] == system['name']
    # 带子实体的岗位同时引用该系统时，根与子项名称共同更新。
    empty = update(empty, permissionIds=[perms[0]['id'], perms[1]['id']])
    before = empty
    system = request('PATCH', '/systems/' + system['id'], body={
        'version': system['version'], 'name': '根子实体共同同步'})
    empty = detail(empty)
    assert empty['systemName'] == system['name']
    assert all(item['systemName'] == system['name'] for item in empty['permissions'])
    assert empty['version'] == before['version'] + 1
    assert {item['id'] for item in empty['permissions']} == {item['id'] for item in before['permissions']}
    request('PATCH', '/positions/' + empty['id'], 409, {'version': before['version'], 'name': '旧版本'})
    before = empty
    system = request('PATCH', '/systems/' + system['id'], body={
        'version': system['version'], 'name': system['name'], 'description': '同名更新'})
    assert canonical(detail(empty)) == canonical(before)
    request('PATCH', '/positions/' + empty['id'], 400,
            {'version': empty['version'], 'systemCode': 'different-system'})
    request('PATCH', '/positions/' + empty['id'], 400,
            {'version': empty['version'], 'systemCode': ' '})
    passed('System rename synchronizes permission, position root and all children; same name is idempotent')

except Exception as error:
    failure = str(error)
    results.append({'name': 'Test failure', 'passed': False, 'detail': failure})
    print('FAIL:', failure, flush=True)
finally:
    cleanup_errors = []
    for kind in ['positions', 'permissions', 'systems']:
        for identifier in reversed(owned[kind]):
            try:
                status, value = http('GET', f'/{kind}/{identifier}')
                if status == 404:
                    continue
                assert status == 200
                request('DELETE', f'/{kind}/{identifier}?version={value["version"]}', 204)
            except Exception:
                cleanup_errors.append({'kind': kind, 'id': identifier})
    # 只核对本次专用编码，绝不清理其他数据。
    residue = sql(f"SELECT (SELECT count(*) FROM sys_positions WHERE code LIKE '{prefix}%') + "
                  f"(SELECT count(*) FROM sys_permissions WHERE system_code='{prefix}') + "
                  f"(SELECT count(*) FROM sys_systems WHERE code='{prefix}');")
    report = {'prefix': prefix, 'results': results, 'cleanupErrors': cleanup_errors,
              'remainingTestRecords': int(residue), 'passed': failure is None and not cleanup_errors and residue == '0'}
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('Cleanup:', 'complete' if not cleanup_errors and residue == '0' else 'INCOMPLETE', flush=True)
if not report['passed']:
    raise SystemExit(1)
