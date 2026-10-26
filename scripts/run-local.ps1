$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (!(Test-Path (Join-Path $root '.env'))) { throw '请先执行 scripts/init-local.ps1' }
Get-Content (Join-Path $root '.env') | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1],$matches[2],'Process') }
}
Push-Location $root
try {
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw '构建失败' }
    $jar=Get-ChildItem '*-host/target/*-host-*.jar' | Select-Object -First 1
    & java -jar $jar.FullName
} finally { Pop-Location }
