#!/bin/bash
# macOS 入口：使用已有数据，构建并通过 Aspire 启动全部服务。
set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo '此入口适用于 macOS；Windows 请使用 src/aspire/start.ps1。' >&2
    exit 1
fi

aspire_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
java_root="$(cd "${aspire_root}/../java" && pwd)"
react_root="$(cd "${aspire_root}/../react" && pwd)"
project="${aspire_root}/DedsiArche.AppHost/DedsiArche.AppHost.csproj"
config_path="${aspire_root}/DedsiArche.AppHost/appsettings.json"

fail() {
    echo "$*" >&2
    exit 1
}

if [[ ! -f "${config_path}" ]]; then
    cp -n "${aspire_root}/DedsiArche.AppHost/appsettings.mac.example.json" "${config_path}"
    fail "已根据 Mac 模板创建 ${config_path}。请填写已有数据库、Redis 凭据和 JWT 密钥，恢复对应数据后重新运行。"
fi

# 只提取所需字段，不打印包含凭据的完整配置。
config_value() {
    /usr/bin/plutil -extract "$1" raw -o - "${config_path}"
}

for key in postgres-user postgres-password redis-password jwt-secret; do
    value="$(config_value "Parameters.${key}")"
    if [[ -z "${value}" || "${value}" == REPLACE_WITH_* ]]; then
        fail "请在 appsettings.json 中填写 Parameters:${key}。"
    fi
done
unset value

port_list=' '
for name in Postgres Redis Backend Frontend Dashboard Otlp ResourceService; do
    port="$(config_value "Ports.${name}")"
    [[ "${port}" =~ ^[1-9][0-9]{0,4}$ ]] || fail "请配置有效端口 Ports:${name}。"
    (( port <= 65535 )) || fail "请配置有效端口 Ports:${name}。"
    [[ "${port_list}" != *" ${port} "* ]] || fail "Ports:${name} 与其他资源端口重复。"
    port_list+="${port} "
done

for resource in PostgreSQL Redis; do
    directory="$(config_value "${resource}.DataDirectory")"
    relative_file="$(config_value "${resource}.DataFile")"
    [[ "${directory}" == /* ]] || fail "${resource}:DataDirectory 必须是 macOS 绝对路径，请参考 appsettings.mac.example.json。"
    [[ -n "${relative_file}" ]] || fail "请配置 ${resource}:DataFile。"
    [[ -f "${directory}/${relative_file}" ]] || fail "缺少现有数据文件：${directory}/${relative_file}。请恢复迁移数据，不会自动创建空库。"
done

for command in docker dotnet mvn java bun lsof; do
    command -v "${command}" >/dev/null 2>&1 || fail "缺少命令：${command}，请安装并加入 PATH。"
done

docker info --format '{{.ServerVersion}}' >/dev/null || fail '请先启动 Docker Desktop。'
# PostgreSQL / Redis 的 Persistent 容器可能仍在运行，不将其端口视为应用冲突。
for name in Backend Frontend Dashboard Otlp ResourceService; do
    port="$(config_value "Ports.${name}")"
    if lsof -nP -iTCP:"${port}" -sTCP:LISTEN -t >/dev/null 2>&1; then
        fail "端口 ${port}（${name}）已占用，请先手动停止对应应用，再重新运行；脚本不会结束未知进程。"
    fi
done

(
    cd "${react_root}" || exit 1
    bun install --frozen-lockfile
) || fail '前端依赖安装失败。'

(
    cd "${aspire_root}" || exit 1
    dotnet build "${project}"
) || fail 'AppHost 构建失败。'

(
    cd "${java_root}" || exit 1
    mvn package -Dmaven.test.skip=true
) || fail '后端构建失败，数据库数据保留。'

backend_port="$(config_value Ports.Backend)"
frontend_port="$(config_value Ports.Frontend)"
dashboard_port="$(config_value Ports.Dashboard)"
context_path="$(config_value Backend.ContextPath)"
printf '正在启动 Aspire。Dashboard http://localhost:%s；前端 http://localhost:%s；后端 http://localhost:%s%s\n' \
    "${dashboard_port}" "${frontend_port}" "${backend_port}" "${context_path}"
cd "${aspire_root}"
# 不删除容器、卷或数据目录；AppHost 退出后 Persistent 数据库容器可能继续运行。
exec dotnet run --no-build --project "${project}" --launch-profile http
