param([Parameter(Mandatory=$true)][ValidatePattern('^[a-z][a-z0-9]*$')][string]$Name)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$rootPomPath=Join-Path $root 'pom.xml'
[xml]$pom=Get-Content -Raw $rootPomPath
$ns='http://maven.apache.org/POM/4.0.0'
$prefix=$pom.project.artifactId
$group=$pom.project.groupId
$version=$pom.project.version
$module="$prefix-$Name"
$modulePath=Join-Path $root $module
if(Test-Path -LiteralPath $modulePath) { throw "模块已存在: $module" }
$app=Get-ChildItem (Join-Path $root "$prefix-host/src/main/java") -Recurse -Filter '*Application.java' | Select-Object -First 1
$match=[regex]::Match((Get-Content -Raw $app.FullName),'package\s+([\w.]+);')
if(!$match.Success) { throw '无法确定根包' }
$basePackage=$match.Groups[1].Value
$hostPomPath=Join-Path $root "$prefix-host/pom.xml"
[xml]$hostPom=Get-Content -Raw $hostPomPath
# 所有检查完成后才写入。不同业务模块互不引用。
New-Item -ItemType Directory -Path $modulePath | Out-Null
$modulePom=@"
<project xmlns="$ns"><modelVersion>4.0.0</modelVersion>
<parent><groupId>$group</groupId><artifactId>$prefix</artifactId><version>$version</version></parent>
<artifactId>$module</artifactId><dependencies>
<dependency><groupId>$group</groupId><artifactId>$prefix-common</artifactId><version>$version</version></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
</dependencies></project>
"@
[IO.File]::WriteAllText((Join-Path $modulePath 'pom.xml'),$modulePom)
foreach($layer in @('api','domain','infrastructure')) {
    $dir=Join-Path $modulePath ("src/main/java/"+$basePackage.Replace('.','/')+"/$Name/$layer")
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    [IO.File]::WriteAllText((Join-Path $dir 'package-info.java'),"/** 在此层下按领域建立子包。 */`npackage $basePackage.$Name.$layer;`n")
}
$modules=$pom.DocumentElement.SelectSingleNode("*[local-name()='modules']")
$node=$pom.CreateElement('module',$ns); $node.InnerText=$module; [void]$modules.AppendChild($node)
$dependencies=$hostPom.DocumentElement.SelectSingleNode("*[local-name()='dependencies']")
$dependency=$hostPom.CreateElement('dependency',$ns)
foreach($pair in @(@('groupId',$group),@('artifactId',$module),@('version',$version))) {
    $child=$hostPom.CreateElement($pair[0],$ns); $child.InnerText=$pair[1]; [void]$dependency.AppendChild($child)
}
[void]$dependencies.AppendChild($dependency)
$pom.Save($rootPomPath); $hostPom.Save($hostPomPath)
Write-Host "已添加 $module；在各层下建立领域目录即可。新增迁移目录时需登记 Host 的 spring.flyway.locations。"
