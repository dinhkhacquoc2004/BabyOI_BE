param(
    [string]$Profile = "quoc"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$adminDir = Join-Path $root "admin-dashboard"
$adminEnv = Join-Path $adminDir ".env"
$adminEnvExample = Join-Path $adminDir ".env.example"

if (-not (Test-Path $adminEnv) -and (Test-Path $adminEnvExample)) {
    Copy-Item -Path $adminEnvExample -Destination $adminEnv
}

Write-Host "Starting BabyOI backend with profile '$Profile'..."
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$root'; .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=$Profile'"
)

Write-Host "Starting AdminJS dashboard..."
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$adminDir'; node src\index.js"
)

Write-Host ""
Write-Host "Backend: http://localhost:8085"
Write-Host "AdminJS: http://localhost:8090/admin"
