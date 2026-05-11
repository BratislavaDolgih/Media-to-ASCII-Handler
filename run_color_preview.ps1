param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [ValidateSet("auto", "image", "video")]
    [string]$Type = "auto",

    [int]$Width = 120,

    [string]$Python = "python"
)

$ErrorActionPreference = "Stop"
$env:TERM = "xterm-256color"

if (Get-Variable -Name PSStyle -ErrorAction SilentlyContinue) {
    $PSStyle.OutputRendering = "Ansi"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolPath = Join-Path $scriptDir "ascii_media_tools.py"

if ($Type -eq "auto") {
    $extension = [System.IO.Path]::GetExtension($InputPath).ToLowerInvariant()
    if ($extension -in @(".jpg", ".jpeg", ".png", ".bmp", ".webp", ".tif", ".tiff")) {
        $Type = "image"
    }
    elseif ($extension -in @(".mp4", ".avi", ".mkv", ".mov", ".webm")) {
        $Type = "video"
    }
    else {
        throw "Cannot detect media type from extension. Pass -Type image or -Type video."
    }
}

if ($Type -eq "image") {
    & $Python $toolPath image $InputPath --width $Width --color --print
}
else {
    & $Python $toolPath video $InputPath --width $Width --color --preview
}
