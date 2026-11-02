<#
.SYNOPSIS
通过真实 HTTP 请求验证用户认证、资料持久化及基础设施健康状态。
#>
param(
    [string]$BaseUrl = 'http://localhost:11518/dedsi-arche-api'
)
$ErrorActionPreference = 'Stop'
<#
.SYNOPSIS
发送 JSON HTTP 请求，并按需附加 Bearer 访问令牌。
#>
function Request {
    param(
        [string]$method,
        [string]$path,
        $body,
        $token = ''
    )

    $headers = @{
    }
    if ($token) {
        $headers.Authorization = "Bearer $token"
    }
    $args = @{
        Method  = $method
        Uri     = "$BaseUrl$path"
        Headers = $headers
    }
    if ($null -ne $body) {
        $args.ContentType = 'application/json'
        $args.Body = ($body | ConvertTo-Json -Compress)
    }
    Invoke-RestMethod @args
}
# 显式提供管理员凭据，通过受保护的管理接口创建本次测试账号。
$adminName = $env:E2E_ADMIN_USERNAME
$adminPassword = $env:E2E_ADMIN_PASSWORD
if ([string]::IsNullOrWhiteSpace($adminName) -or [string]::IsNullOrWhiteSpace($adminPassword)) {
    throw '请设置 E2E_ADMIN_USERNAME 和 E2E_ADMIN_PASSWORD'
}
$admin = Request POST '/api/users/login' @{
    username = $adminName
    password = $adminPassword
}
try {
    if (!$admin.user.canManageUsers) {
        throw '提供的账号没有用户管理权限'
    }
    # 为本次测试创建唯一账号，避免与已有数据冲突。
    $username = 'smoke_' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $password = [Guid]::NewGuid().ToString('N') + '!'
    $user = Request POST '/api/users' @{
        username    = $username
        displayName = 'Smoke User'
        password    = $password
    } $admin.accessToken
    $login = Request POST '/api/users/login' @{
        username = $username
        password = $password
    }
    # 更新后重新读取，验证事务已提交到数据库。
    $null = Request PATCH '/api/users/me' @{
        displayName = 'Changed'
    } $login.accessToken
    $me = Request GET '/api/users/me' $null $login.accessToken
    if ($me.displayName -ne 'Changed') {
        throw 'Hibernate 脏检查持久化验证失败'
    }
    # 刷新令牌轮换后注销，并检查已撤销令牌不能再次使用。
    $rotated = Request POST '/api/users/refresh' @{
        refreshToken = $login.refreshToken
    }
    $null = Request POST '/api/users/logout' @{
        refreshToken = $rotated.refreshToken
    }
    $rejected = $false
    try {
        $null = Request POST '/api/users/refresh' @{
            refreshToken = $rotated.refreshToken
        }
    }
    catch {
        if ([int]$_.Exception.Response.StatusCode -eq 401) {
            $rejected = $true
        }
        else {
            throw
        }
    }
    if (!$rejected) {
        throw '注销后刷新令牌仍有效'
    }
    # 检查服务及数据库、Redis 等依赖是否可用。
    $health = Request GET '/actuator/health' $null
    if ($health.status -ne 'UP') {
        throw '数据库或 Redis 健康检查失败'
    }
    Write-Host "PASS: 管理员新增用户、登录、JWT、用户更新、刷新、注销、PostgreSQL/Redis 健康检查。测试用户：$username"
}
finally {
    $null = Request POST '/api/users/logout' @{
        refreshToken = $admin.refreshToken
    }
}
