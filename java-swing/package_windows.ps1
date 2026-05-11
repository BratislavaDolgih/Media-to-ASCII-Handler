$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$distDir = Join-Path $scriptDir "dist"
$jarPath = Join-Path $distDir "AsciiPhotoSwingApp.jar"
$packageOut = Join-Path $scriptDir "package"

if (-not (Test-Path $jarPath)) {
    & (Join-Path $scriptDir "build.ps1")
}

$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackage) {
    throw "jpackage not found. Install a full JDK with jpackage and add its bin folder to PATH to create a bundled app."
}

New-Item -ItemType Directory -Force -Path $packageOut | Out-Null

& $jpackage.Source `
    --type app-image `
    --name "ASCII Photo Fork" `
    --input $distDir `
    --main-jar "AsciiPhotoSwingApp.jar" `
    --main-class "AsciiPhotoSwingApp" `
    --dest $packageOut

Write-Host "Packaged app image in: $packageOut"
