param(
    [string]$Profile = "quoc"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$adminDir = Join-Path $root "admin-dashboard"
$adminEnv = Join-Path $adminDir ".env"
$adminEnvExample = Join-Path $adminDir ".env.example"

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing command '$Name'. Install it and try again."
    }
}

function Assert-NodeVersion {
    Assert-Command "node"
    $versionText = (& node --version).Trim().TrimStart("v")
    $parts = $versionText.Split(".")
    $major = [int]$parts[0]
    $minor = [int]$parts[1]
    if ($major -lt 20 -or ($major -eq 20 -and $minor -lt 10)) {
        throw "AdminJS requires Node.js 20.10+. Current version is v$versionText."
    }
}

if (-not (Test-Path $adminEnv) -and (Test-Path $adminEnvExample)) {
    Copy-Item -Path $adminEnvExample -Destination $adminEnv
}

Assert-NodeVersion
$adminCommand = "pnpm"
if (-not (Get-Command "pnpm" -ErrorAction SilentlyContinue)) {
    Assert-Command "corepack"
    corepack enable
    if (-not (Get-Command "pnpm" -ErrorAction SilentlyContinue)) {
        $adminCommand = "corepack pnpm"
    }
}

Write-Host "Installing AdminJS dependencies for this OS..."
Push-Location $adminDir
try {
    Invoke-Expression "$adminCommand install --frozen-lockfile"
}
finally {
    Pop-Location
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
    "Set-Location '$adminDir'; $adminCommand start"
)

Write-Host ""
Write-Host "Backend: http://localhost:8085"
Write-Host "AdminJS: http://localhost:8090/admin"
