<#
.SYNOPSIS
使用已安装的 Maven Archetype 创建项目，拒绝覆盖已有目录。
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9-]*$')]
    [string]$Name,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$')]
    [string]$Package,
    [string]$GroupId = $Package,
    [string]$OutputDirectory = (Get-Location).Path,
    [string]$TemplateVersion = '1.0.0'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$output = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath (Join-Path $output $Name)) {
    throw '目标项目目录已存在，不覆盖'
}
[void][IO.Directory]::CreateDirectory($output)
# 使用已安装的本地模板坐标，禁用交互以便脚本重复执行。
& (Join-Path $root 'mvnw.cmd') -B -ntp org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate `
    '-DarchetypeGroupId=com.dedsi' '-DarchetypeArtifactId=dedsi-arche' "-DarchetypeVersion=$TemplateVersion" `
    "-DgroupId=$GroupId" "-DartifactId=$Name" "-Dpackage=$Package" '-Dversion=0.0.1-SNAPSHOT' `
    '-DinteractiveMode=false' "-DoutputDirectory=$output"
if ($LASTEXITCODE -ne 0) {
    throw '创建项目失败，请先安装模板'
}
