$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Users\caojie\.jdks\jdk-21.0.12.1+1"
$env:Path = "$env:JAVA_HOME\bin;C:\dev\apache-maven-3.9.9\bin;$env:Path"

$BackendDir = Join-Path $PSScriptRoot "backend"
Set-Location $BackendDir

$JarFile = Get-ChildItem -Path (Join-Path $BackendDir "target") -Filter "datastation-lite-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*.original" } |
    Select-Object -First 1

if ($JarFile) {
    Write-Host "Starting DataStation Lite backend from $($JarFile.FullName) on port 8091..." -ForegroundColor Cyan
    & java -jar $JarFile.FullName
} else {
    Write-Host "Packaged JAR not found, starting via Maven Spring Boot plugin on port 8091..." -ForegroundColor Cyan
    & mvn spring-boot:run
}
