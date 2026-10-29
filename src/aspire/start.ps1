<#
.SYNOPSIS
构建并通过 Aspire 启动 Java、React（Bun）、PostgreSQL、Redis；使用 C:\Dockers 中已迁移的数据。
#>
$ErrorActionPreference = 'Stop'
$aspireRoot = $PSScriptRoot
$javaRoot = [IO.Path]::GetFullPath((Join-Path $aspireRoot '../java'))
$reactRoot = [IO.Path]::GetFullPath((Join-Path $aspireRoot '../react'))
$project = Join-Path $aspireRoot 'DedsiArche.AppHost/DedsiArche.AppHost.csproj'
$jar = Join-Path $javaRoot 'dedsi-arche-host/target/dedsi-arche-host-1.0.0-SNAPSHOT.jar'
foreach ($command in @('docker', 'dotnet', 'mvn.cmd', 'java', 'bun')) {
    if (!(Get-Command $command -ErrorAction SilentlyContinue)) { throw "缺少命令：$command" }
}
$configPath = Join-Path $aspireRoot 'DedsiArche.AppHost/appsettings.json'
if (!(Test-Path -LiteralPath $configPath)) {
    throw '缺少 AppHost/appsettings.json，请根据 appsettings.example.json 配置现有数据库凭据。'
}
$config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($name in @('Postgres', 'Redis', 'Backend', 'Frontend', 'Dashboard', 'Otlp', 'ResourceService')) {
    $port = [int]$config.Ports.$name
    if ($port -lt 1 -or $port -gt 65535) { throw "请配置有效端口 Ports:$name" }
}
$backendPort = [int]$config.Ports.Backend
$frontendPort = [int]$config.Ports.Frontend
# 已迁移的数据文件必须存在，避免误用空目录。
foreach ($dataFile in @(
    (Join-Path $config.PostgreSQL.DataDirectory $config.PostgreSQL.DataFile),
    (Join-Path $config.Redis.DataDirectory $config.Redis.DataFile)
)) {
    if (!(Test-Path -LiteralPath $dataFile -PathType Leaf)) {
        throw "缺少现有数据文件：$dataFile。请恢复迁移数据后再启动。"
    }
}
& docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw '请先启动 Docker Desktop。' }
foreach ($port in @($config.Ports.Dashboard, $config.Ports.Otlp, $config.Ports.ResourceService)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "端口 $port 已占用，请先关闭已运行的 Aspire。"
    }
}

# 只接管本仓库通过 java -jar 启动的后端，不停止其他 Java 应用或 IDE。
$backendProcesses = @(Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {
    $_.CommandLine -like '*-jar*' -and $_.CommandLine.Contains($jar)
})
$listeners = @(Get-NetTCPConnection -LocalPort $backendPort -State Listen -ErrorAction SilentlyContinue)
foreach ($listener in $listeners) {
    if ($listener.OwningProcess -notin $backendProcesses.ProcessId) {
        throw "$backendPort 被其他进程占用（可能为 IDEA 启动的后端），请先手动停止该进程。"
    }
}

# 仅接管当前仓库的 Vite 进程；未知端口占用者保持不动。
$frontendProcesses = @()
foreach ($ownerId in @(Get-NetTCPConnection -LocalPort $frontendPort -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerId"
    $vitePath = Join-Path $reactRoot 'node_modules/vite/bin/vite.js'
    if (!$process.CommandLine -or !$process.CommandLine.Replace('/', '\').Contains($vitePath.Replace('/', '\'))) {
        throw "$frontendPort 被其他进程占用，请先手动释放端口。"
    }
    $frontendProcesses += $process
}
Push-Location $reactRoot
try {
    & bun install --frozen-lockfile
    if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败，现有服务未停止。' }
} finally { Pop-Location }

Push-Location $aspireRoot
try {
    & dotnet build $project
    if ($LASTEXITCODE -ne 0) { throw 'AppHost 构建失败，现有服务未停止。' }
} finally { Pop-Location }

foreach ($backend in $backendProcesses) {
    Stop-Process -Id $backend.ProcessId -ErrorAction SilentlyContinue
    Wait-Process -Id $backend.ProcessId -Timeout 30 -ErrorAction SilentlyContinue
}
# Windows 下先释放正在运行的 JAR，再重新打包；依赖容器此时仍在运行。
Push-Location $javaRoot
try {
    & mvn.cmd package '-Dmaven.test.skip=true'
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败，数据库容器未停止；修复构建后重新执行此脚本。' }
} finally { Pop-Location }
# 正常停止会落盘；不执行 down -v、容器删除或卷删除。
foreach ($frontend in $frontendProcesses) {
    Stop-Process -Id $frontend.ProcessId -ErrorAction SilentlyContinue
    Wait-Process -Id $frontend.ProcessId -Timeout 30 -ErrorAction SilentlyContinue
}
Push-Location $aspireRoot
try {
    Write-Host "正在启动 Aspire。Dashboard http://localhost:$($config.Ports.Dashboard)；前端 http://localhost:$frontendPort；后端 http://localhost:$backendPort$($config.Backend.ContextPath)"
    & dotnet run --no-build --project $project --launch-profile http
    if ($LASTEXITCODE -ne 0) { throw 'Aspire 启动失败。C:\Dockers 中的数据保留，请查看启动日志。' }
} finally { Pop-Location }
