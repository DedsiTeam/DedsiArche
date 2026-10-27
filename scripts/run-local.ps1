<#
.SYNOPSIS
加载本地环境变量，构建并启动 Host 应用。
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (!(Test-Path (Join-Path $root '.env'))) {
    throw '请先执行 scripts/init-local.ps1'
}
# 仅将环境文件中的变量加载到当前进程及其子进程。
Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
# 确保构建在项目根目录执行，退出时恢复调用者目录。
Push-Location $root
try {
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw '构建失败'
    }
    $jar = Get-ChildItem '*-host/target/*-host-*.jar' | Select-Object -First 1
    & java -jar $jar.FullName
}
finally {
    Pop-Location
}
