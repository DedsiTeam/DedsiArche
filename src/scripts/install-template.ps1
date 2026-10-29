<#
.SYNOPSIS
按白名单生成模板并安装到本地 Maven 仓库。
#>
param(
    [string]$TemplateVersion = '1.0.0'
)
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
$build = Join-Path $root 'target/archetype'
# 仅重建本项目下固定的临时目录，避免旧文件或本地密钥进入模板。
$expected = [IO.Path]::GetFullPath((Join-Path $root 'target/archetype'))
if ([IO.Path]::GetFullPath($build) -ne $expected) {
    throw '模板路径校验失败'
}
if (Test-Path -LiteralPath $build) {
    Remove-Item -LiteralPath $build -Recurse -Force
}
$resources = Join-Path $build 'src/main/resources/archetype-resources'
$utf8 = New-Object System.Text.UTF8Encoding($false)
<#
.SYNOPSIS
创建目标目录并以无 BOM 的 UTF-8 编码写入文件。
#>
function Write-File {
    param(
        [string]$path,
        [string]$content
    )

    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))
    [IO.File]::WriteAllText($path, $content, $utf8)
}
<#
.SYNOPSIS
将示例项目标识替换为模板变量，同时保护源文件中的美元符号。
#>
function Filter-Source {
    param(
        [string]$source,
        [bool]$pom = $false
    )

    $text = [IO.File]::ReadAllText($source)
    $text = $text.Replace('$', '${dollar}')
    $text = $text.Replace('com.dedsi.arche', '${package}')
    $text = $text.Replace('dedsi-arche', '${rootArtifactId}')
    $text = $text.Replace('name: dedsiarche', 'name: ${rootArtifactId}')
    if ($pom) {
        $text = $text.Replace('${package}', '${groupId}').Replace('1.0.0-SNAPSHOT', '${version}')
    }
    return '#set( $dollar = ''$'' )' + "`n" + $text
}
Write-File (Join-Path $resources 'pom.xml') (Filter-Source (Join-Path $root 'pom.xml') $true)
$fileSets = @'
<fileSets>
  <fileSet>
    <directory>scripts</directory>
    <includes>
      <include>**/*.ps1</include>
    </includes>
  </fileSet>
  <fileSet>
    <includes>
      <include>.gitignore</include>
      <include>README.md</include>
    </includes>
  </fileSet>
  <fileSet filtered="true">
    <includes>
    </includes>
  </fileSet>
</fileSets>
'@
foreach ($name in @('.gitignore')) {
    Write-File (Join-Path $resources $name) ([IO.File]::ReadAllText((Join-Path $root $name)))
}
Write-File (Join-Path $resources 'README.md') ([IO.File]::ReadAllText((Join-Path $root 'docs/GENERATED-README.md')))
foreach ($name in @('init-local.ps1', 'run-local.ps1', 'add-module.ps1', 'smoke-test.ps1')) {
    Write-File (Join-Path $resources "scripts/$name") ([IO.File]::ReadAllText((Join-Path $PSScriptRoot $name)))
}
$modules = ''
foreach ($suffix in @('common', 'system', 'host')) {
    $sourceModule = Join-Path $root "dedsi-arche-$suffix"
    $targetModule = Join-Path $resources "__rootArtifactId__-$suffix"
    Write-File (Join-Path $targetModule 'pom.xml') (Filter-Source (Join-Path $sourceModule 'pom.xml') $true)
    foreach ($kind in @('main', 'test')) {
        $sourceJava = Join-Path $sourceModule "src/$kind/java/com/dedsi/arche"
        if (Test-Path $sourceJava) {
            Get-ChildItem $sourceJava -Recurse -File -Filter '*.java' | ForEach-Object {
                $relative = $_.FullName.Substring($sourceJava.Length + 1)
                Write-File (Join-Path $targetModule "src/$kind/java/$relative") (Filter-Source $_.FullName)
            }
        }
        $sourceResources = Join-Path $sourceModule "src/$kind/resources"
        if (Test-Path $sourceResources) {
            Get-ChildItem $sourceResources -Recurse -File | Where-Object {
                $_.Extension -in @('.yml', '.sql')
            } | ForEach-Object {
                $relative = $_.FullName.Substring($sourceResources.Length + 1)
                Write-File (Join-Path $targetModule "src/$kind/resources/$relative") (Filter-Source $_.FullName)
            }
        }
    }
    # 为各模块登记源码和资源过滤规则，保留 Maven 模板占位符。
    $modules += '<module id="${rootArtifactId}-' + $suffix +
        '" dir="__rootArtifactId__-' + $suffix +
        '" name="${rootArtifactId}-' + $suffix + '"><fileSets>'
    foreach ($kind in @('main', 'test')) {
        $modules += '<fileSet filtered="true" packaged="true" encoding="UTF-8">' +
            '<directory>src/' + $kind + '/java</directory>' +
            '<includes><include>**/*.java</include></includes></fileSet>'
        $modules += '<fileSet filtered="true" encoding="UTF-8">' +
            '<directory>src/' + $kind + '/resources</directory>' +
            '<includes><include>**/*</include></includes></fileSet>'
    }
    $modules += '</fileSets></module>'
}
$descriptor = '<archetype-descriptor ' +
    'xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.2.0" ' +
    'name="DedsiArche">' + $fileSets + '<modules>' + $modules +
    '</modules></archetype-descriptor>'
Write-File (Join-Path $build 'src/main/resources/META-INF/maven/archetype-metadata.xml') $descriptor
$pom = @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.dedsi</groupId>
  <artifactId>dedsi-arche</artifactId>
  <version>$TemplateVersion</version>
  <packaging>maven-archetype</packaging>
  <name>DedsiArche Maven Archetype</name>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <build>
    <extensions>
      <extension>
        <groupId>org.apache.maven.archetype</groupId>
        <artifactId>archetype-packaging</artifactId>
        <version>3.4.1</version>
      </extension>
    </extensions>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-archetype-plugin</artifactId>
          <version>3.4.1</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
"@
Write-File (Join-Path $build 'pom.xml') $pom
Push-Location $root
try {
    & mvn.cmd -B -ntp -f "$build/pom.xml" install
    if ($LASTEXITCODE -ne 0) {
        throw '模板安装失败'
    }
}
finally {
    Pop-Location
}
Write-Host "模板已安装：com.dedsi:dedsi-arche:$TemplateVersion"
