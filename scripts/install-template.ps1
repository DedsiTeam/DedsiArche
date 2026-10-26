param([string]$TemplateVersion='1.0.0')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$build=Join-Path $root 'target/archetype'
# 仅重建本项目下固定的临时目录，避免旧文件或本地密钥进入模板。
$expected=[IO.Path]::GetFullPath((Join-Path $root 'target/archetype'))
if([IO.Path]::GetFullPath($build) -ne $expected) { throw '模板路径校验失败' }
if(Test-Path -LiteralPath $build) { Remove-Item -LiteralPath $build -Recurse -Force }
$resources=Join-Path $build 'src/main/resources/archetype-resources'
$utf8=New-Object System.Text.UTF8Encoding($false)
function Write-File([string]$path,[string]$content) {
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))
    [IO.File]::WriteAllText($path,$content,$utf8)
}
function Filter-Source([string]$source,[bool]$pom=$false) {
    $text=[IO.File]::ReadAllText($source)
    $text=$text.Replace('$','${dollar}').Replace('com.dedsi.arche','${package}').Replace('dedsi-arche','${rootArtifactId}')
    if($pom) { $text=$text.Replace('${package}','${groupId}').Replace('1.0.0-SNAPSHOT','${version}') }
    return '#set( $dollar = ''$'' )' + "`n" + $text
}
Write-File (Join-Path $resources 'pom.xml') (Filter-Source (Join-Path $root 'pom.xml') $true)
$fileSets=@'
<fileSets>
 <fileSet><directory>.mvn</directory><includes><include>**/*</include></includes></fileSet>
 <fileSet><directory>scripts</directory><includes><include>**/*.ps1</include></includes></fileSet>
 <fileSet><includes><include>.gitignore</include><include>mvnw</include><include>mvnw.cmd</include><include>README.md</include></includes></fileSet>
 <fileSet filtered="true"><includes><include>compose.yaml</include></includes></fileSet>
</fileSets>
'@
foreach($name in @('.gitignore','mvnw','mvnw.cmd')) {
    Write-File (Join-Path $resources $name) ([IO.File]::ReadAllText((Join-Path $root $name)))
}
Write-File (Join-Path $resources 'README.md') ([IO.File]::ReadAllText((Join-Path $root 'docs/GENERATED-README.md')))
Write-File (Join-Path $resources 'compose.yaml') (Filter-Source (Join-Path $root 'compose.yaml'))
foreach($name in @('init-local.ps1','run-local.ps1','add-module.ps1','smoke-test.ps1')) {
    Write-File (Join-Path $resources "scripts/$name") ([IO.File]::ReadAllText((Join-Path $root "scripts/$name")))
}
Write-File (Join-Path $resources '.mvn/wrapper/maven-wrapper.properties') ([IO.File]::ReadAllText((Join-Path $root '.mvn/wrapper/maven-wrapper.properties')))
$modules=''
foreach($suffix in @('common','system','host')) {
    $sourceModule=Join-Path $root "dedsi-arche-$suffix"
    $targetModule=Join-Path $resources "__rootArtifactId__-$suffix"
    Write-File (Join-Path $targetModule 'pom.xml') (Filter-Source (Join-Path $sourceModule 'pom.xml') $true)
    foreach($kind in @('main','test')) {
        $sourceJava=Join-Path $sourceModule "src/$kind/java/com/dedsi/arche"
        if(Test-Path $sourceJava) {
            Get-ChildItem $sourceJava -Recurse -File -Filter '*.java' | ForEach-Object {
                $relative=$_.FullName.Substring($sourceJava.Length+1)
                Write-File (Join-Path $targetModule "src/$kind/java/$relative") (Filter-Source $_.FullName)
            }
        }
        $sourceResources=Join-Path $sourceModule "src/$kind/resources"
        if(Test-Path $sourceResources) {
            Get-ChildItem $sourceResources -Recurse -File | Where-Object { $_.Extension -in @('.yml','.sql') } | ForEach-Object {
                $relative=$_.FullName.Substring($sourceResources.Length+1)
                Write-File (Join-Path $targetModule "src/$kind/resources/$relative") (Filter-Source $_.FullName)
            }
        }
    }
    $modules += '<module id="${rootArtifactId}-'+$suffix+'" dir="__rootArtifactId__-'+$suffix+'" name="${rootArtifactId}-'+$suffix+'"><fileSets>'
    foreach($kind in @('main','test')) {
        $modules += '<fileSet filtered="true" packaged="true" encoding="UTF-8"><directory>src/'+$kind+'/java</directory><includes><include>**/*.java</include></includes></fileSet>'
        $modules += '<fileSet filtered="true" encoding="UTF-8"><directory>src/'+$kind+'/resources</directory><includes><include>**/*</include></includes></fileSet>'
    }
    $modules += '</fileSets></module>'
}
$descriptor='<archetype-descriptor xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.2.0" name="DedsiArche">'+$fileSets+'<modules>'+$modules+'</modules></archetype-descriptor>'
Write-File (Join-Path $build 'src/main/resources/META-INF/maven/archetype-metadata.xml') $descriptor
$pom=@"
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
<groupId>com.dedsi</groupId><artifactId>dedsi-arche</artifactId><version>$TemplateVersion</version><packaging>maven-archetype</packaging>
<name>DedsiArche Maven Archetype</name>
<properties><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
<build><extensions><extension><groupId>org.apache.maven.archetype</groupId><artifactId>archetype-packaging</artifactId><version>3.4.1</version></extension></extensions>
<pluginManagement><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-archetype-plugin</artifactId><version>3.4.1</version></plugin></plugins></pluginManagement></build></project>
"@
Write-File (Join-Path $build 'pom.xml') $pom
Push-Location $root
try {
    & .\mvnw.cmd -B -ntp -f "$build/pom.xml" install
    if($LASTEXITCODE -ne 0) { throw '模板安装失败' }
} finally { Pop-Location }
Write-Host "模板已安装：com.dedsi:dedsi-arche:$TemplateVersion"
