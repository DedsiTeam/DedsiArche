<#
.SYNOPSIS
加载本地环境变量，构建并启动 Host 应用。
#>
$ErrorActionPreference = 'Stop'
if (!(Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
    throw '请先安装 Maven 并将 bin 目录加入 PATH，然后重新打开终端。'
}
# 源仓库脚本位于 src/scripts；模板生成项目仍使用根目录下的 scripts。
$parentDirectory = Split-Path $PSScriptRoot -Parent
$root = if (Test-Path -LiteralPath (Join-Path $parentDirectory 'pom.xml')) {
    $parentDirectory
} else {
    Join-Path $parentDirectory 'java'
}
if (!(Test-Path -LiteralPath (Join-Path $root 'pom.xml'))) {
    throw '找不到后端 pom.xml，请确认脚本目录与后端目录的位置。'
}
if (!(Test-Path (Join-Path $root '.env'))) {
    throw '请先执行同目录下的 init-local.ps1'
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
    & mvn.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw '构建失败'
    }
    $jar = Get-ChildItem '*-host/target/*-host-*.jar' | Select-Object -First 1
    & java -jar $jar.FullName
}
finally {
    Pop-Location
}
