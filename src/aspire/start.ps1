<#
.SYNOPSIS
构建并通过 Aspire 启动 Java、React（Bun）、PostgreSQL、Redis；复用现有数据卷。
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
if (!(Test-Path -LiteralPath (Join-Path $javaRoot '.env'))) { throw '缺少 src/java/.env；请使用原数据库凭据。' }

function Invoke-Docker {
    param([string[]]$DockerArguments)
    $result = & docker @DockerArguments
    if ($LASTEXITCODE -ne 0) { throw 'Docker 操作失败，已停止启动流程。' }
    return $result
}

# 先确认卷和占用者，再进行构建；不允许 Docker 自动新建空卷替代旧数据。
$legacyContainers = @()
$mappings = @(
    @{ Volume = 'dedsiarche_postgres-data'; Old = 'dedsiarche-postgres-1'; New = 'dedsiarche-aspire-postgres' },
    @{ Volume = 'dedsiarche_redis-data'; Old = 'dedsiarche-redis-1'; New = 'dedsiarche-aspire-redis' }
)
foreach ($mapping in $mappings) {
    Invoke-Docker -DockerArguments @('volume', 'inspect', $mapping.Volume, '--format', '{{.Name}}') | Out-Null
    $holders = @(Invoke-Docker -DockerArguments @('ps', '--filter', "volume=$($mapping.Volume)", '--format', '{{.Names}}'))
    foreach ($holder in $holders) {
        if ($holder -eq $mapping.Old) { $legacyContainers += $holder }
        elseif ($holder -ne $mapping.New) { throw "数据卷 $($mapping.Volume) 被其他容器 $holder 占用，未执行切换。" }
    }
}
foreach ($port in @(11519, 11520, 11521)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "端口 $port 已占用，请先关闭已运行的 Aspire。"
    }
}

# 只接管本仓库通过 java -jar 启动的后端，不停止其他 Java 应用或 IDE。
$backendProcesses = @(Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {
    $_.CommandLine -like '*-jar*' -and $_.CommandLine.Contains($jar)
})
$listeners = @(Get-NetTCPConnection -LocalPort 11518 -State Listen -ErrorAction SilentlyContinue)
foreach ($listener in $listeners) {
    if ($listener.OwningProcess -notin $backendProcesses.ProcessId) {
        throw '11518 被其他进程占用（可能为 IDEA 启动的后端），请先手动停止该进程。'
    }
}

# 仅接管当前仓库的 Vite 进程；未知端口占用者保持不动。
$frontendProcesses = @()
foreach ($ownerId in @(Get-NetTCPConnection -LocalPort 11126 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerId"
    $vitePath = Join-Path $reactRoot 'node_modules/vite/bin/vite.js'
    if (!$process.CommandLine -or !$process.CommandLine.Replace('/', '\').Contains($vitePath.Replace('/', '\'))) {
        throw '11126 被其他进程占用，请先手动释放端口。'
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

# 首次从 Compose 切换时先保存逻辑备份。失败则不停止原服务。
if ('dedsiarche-postgres-1' -in $legacyContainers) {
    $backupDirectory = Join-Path $aspireRoot '.local/backups'
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    $backupName = 'dedsi_arche-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump'
    $containerBackup = '/tmp/' + $backupName
    Invoke-Docker -DockerArguments @('exec', 'dedsiarche-postgres-1', 'pg_dump', '-U', 'dedsi', '-d', 'dedsi_arche', '-Fc', '-f', $containerBackup) | Out-Null
    Invoke-Docker -DockerArguments @('cp', "dedsiarche-postgres-1:$containerBackup", (Join-Path $backupDirectory $backupName)) | Out-Null
    Write-Host "数据库备份已保存：$backupDirectory/$backupName"
}

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
foreach ($container in $legacyContainers) {
    Invoke-Docker -DockerArguments @('stop', '--time', '60', $container) | Out-Null
}

Push-Location $aspireRoot
try {
    Write-Host '正在启动 Aspire。Dashboard http://localhost:11519；前端 http://localhost:11126；后端 http://localhost:11518/dedsi-arche-api'
    & dotnet run --no-build --project $project --launch-profile http
    if ($LASTEXITCODE -ne 0) { throw 'Aspire 启动失败。数据卷保留；按 README 中的步骤恢复原有容器。' }
} finally { Pop-Location }
