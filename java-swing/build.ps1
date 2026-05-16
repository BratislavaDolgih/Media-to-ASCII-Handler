$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcDir = Join-Path $scriptDir "src"
$buildDir = Join-Path $scriptDir "build"
$classesDir = Join-Path $buildDir "classes"
$packageDir = Join-Path $buildDir "package"
$distDir = Join-Path $scriptDir "dist"
$jarPath = Join-Path $distDir "AsciiPhotoSwingApp.jar"
$latestJarPath = Join-Path $distDir "AsciiPhotoSwingApp_latest.jar"
$forkDir = Split-Path -Parent $scriptDir

New-Item -ItemType Directory -Force -Path $classesDir, $packageDir, $distDir | Out-Null

Get-ChildItem -Path $classesDir -Recurse -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem -Path $packageDir -Recurse -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force
New-Item -ItemType Directory -Force -Path (Join-Path $packageDir "META-INF") | Out-Null

$javaSources = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if (-not $javaSources) {
    throw "No Java source files found in $srcDir"
}

javac -encoding UTF-8 -d $classesDir $javaSources

Copy-Item -Path (Join-Path $classesDir "*") -Destination $packageDir -Recurse -Force
Set-Content -Path (Join-Path $packageDir "META-INF\MANIFEST.MF") -Encoding ASCII -Value @(
    "Manifest-Version: 1.0"
    "Main-Class: AsciiPhotoSwingApp"
    ""
)

$targetJarPath = $jarPath
if (Test-Path $jarPath) {
    try {
        Remove-Item -Path $jarPath -Force
    }
    catch {
        Write-Warning "Main JAR is locked; building latest JAR instead."
        $targetJarPath = $latestJarPath
    }
}

$zipPath = [System.IO.Path]::ChangeExtension($targetJarPath, ".zip")
if (Test-Path $targetJarPath) {
    Remove-Item -Path $targetJarPath -Force
}
if (Test-Path $zipPath) {
    Remove-Item -Path $zipPath -Force
}

Compress-Archive -Path (Join-Path $packageDir "*") -DestinationPath $zipPath -Force
Rename-Item -Path $zipPath -NewName (Split-Path $targetJarPath -Leaf)
Copy-Item -Path (Join-Path $forkDir "ascii_media_tools.py") -Destination (Join-Path $distDir "ascii_media_tools.py") -Force
Copy-Item -Path (Join-Path $forkDir "requirements.txt") -Destination (Join-Path $distDir "requirements.txt") -Force

Write-Host "Built: $targetJarPath"
