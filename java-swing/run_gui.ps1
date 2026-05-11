$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runDir = Join-Path $scriptDir "dist"

$jarCandidates = @(
    Join-Path $runDir "AsciiPhotoSwingApp.jar"
    Join-Path $runDir "AsciiPhotoSwingApp_latest.jar"
    Join-Path $runDir "AsciiPhotoSwingApp_fixed.jar"
) | Where-Object { Test-Path $_ } | Get-Item | Sort-Object LastWriteTime -Descending

if (-not $jarCandidates) {
    & (Join-Path $scriptDir "build.ps1")
    $jarCandidates = @(
        Join-Path $runDir "AsciiPhotoSwingApp.jar"
        Join-Path $runDir "AsciiPhotoSwingApp_latest.jar"
        Join-Path $runDir "AsciiPhotoSwingApp_fixed.jar"
    ) | Where-Object { Test-Path $_ } | Get-Item | Sort-Object LastWriteTime -Descending
}

$jarPath = $jarCandidates[0].FullName
Set-Location $runDir
java -jar $jarPath
