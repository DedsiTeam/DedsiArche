$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$path = Join-Path $root '.env'
if (Test-Path -LiteralPath $path) { Write-Host '.env 已存在，保留现有密钥。'; exit 0 }
function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes); $rng.Dispose()
    return [Convert]::ToBase64String($bytes)
}
@("DB_PASSWORD=$(New-Secret)", "REDIS_PASSWORD=$(New-Secret)", "JWT_SECRET=$(New-Secret)") | Set-Content -LiteralPath $path -Encoding utf8
Write-Host '已生成 .env；不要提交此文件。'
