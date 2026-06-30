# Imports external platformer zips into app assets with normalized layout.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = 'd:\soft\app\src\main\assets\platformer'
$srcRoot = 'c:\Users\Administrator\Desktop\yishi\yishi2'

function Ensure-Dir([string]$path) {
    if (-not (Test-Path $path)) { New-Item -ItemType Directory -Path $path -Force | Out-Null }
}

function Extract-ZipEntry([System.IO.Compression.ZipArchive]$zip, [string]$entryName, [string]$destFile) {
    $entry = $zip.Entries | Where-Object { $_.FullName -replace '\\','/' -eq ($entryName -replace '\\','/') } | Select-Object -First 1
    if (-not $entry) { return $false }
    Ensure-Dir (Split-Path $destFile -Parent)
    $stream = $entry.Open()
    try {
        $fs = [System.IO.File]::Create($destFile)
        try { $stream.CopyTo($fs) } finally { $fs.Close() }
    } finally { $stream.Close() }
    return $true
}

function Import-CraftpixPack([string]$zipName, [string]$destName, [string]$tilesPattern, [string]$bgEntry, [string]$objectsPrefix) {
    $zipPath = Join-Path $srcRoot $zipName
    if (-not (Test-Path $zipPath)) { Write-Warning "Skip missing $zipName"; return }
    $dest = Join-Path $root "tilesets\$destName"
    Ensure-Dir "$dest\tiles"
    Ensure-Dir "$dest\objects"
    $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        for ($i = 1; $i -le 18; $i++) {
            $name = $tilesPattern -f $i
            $entry = $zip.Entries | Where-Object { $_.Name -eq $name -or $_.FullName -like "*/$name" } | Select-Object -First 1
            if ($entry) {
                Extract-ZipEntry $zip $entry.FullName "$dest\tiles\$i.png"
            }
        }
        $bg = $zip.Entries | Where-Object { $_.FullName -like "*$bgEntry" -and $_.Name -notlike '._*' } | Select-Object -First 1
        if ($bg) { Extract-ZipEntry $zip $bg.FullName "$dest\bg.png" }
        $zip.Entries | Where-Object {
            $_.FullName -like "$objectsPrefix*" -and $_.Name -like '*.png' -and $_.Name -notlike '._*'
        } | ForEach-Object {
            Extract-ZipEntry $zip $_.FullName "$dest\objects\$($_.Name)"
        }
    } finally { $zip.Dispose() }
    Write-Host "Imported pack $destName"
}

function Import-Recursive([string]$zipName, [string]$destSub, [string]$filter = '*.png') {
    $zipPath = Join-Path $srcRoot $zipName
    if (-not (Test-Path $zipPath)) { return }
    $dest = Join-Path $root $destSub
    $zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $zip.Entries | Where-Object {
            $_.Name -like $filter -and $_.Name -notlike '._*' -and $_.FullName -notlike '*__MACOSX*'
        } | ForEach-Object {
            $rel = $_.FullName -replace '^[^/]+/', '' -replace '/', '\'
            $out = Join-Path $dest $rel
            Extract-ZipEntry $zip $_.FullName $out | Out-Null
        }
    } finally { $zip.Dispose() }
    Write-Host "Imported $destSub from $zipName"
}

Ensure-Dir $root

# Craftpix-style tilesets (tiles 1-18 + bg + objects)
Import-CraftpixPack 'FreeTileset.zip' 'forest' 'png/Tiles/{0}.png' 'BG/BG.png' 'png/Object/'
Import-CraftpixPack 'GraveyardTileset.zip' 'graveyard' 'png/Tiles/Tile ({0}).png' 'BG.png' 'png/Objects/'

# Sci-fi: Tile (N).png
Import-CraftpixPack 'FreeSciFiPlatform.zip' 'scifi' 'png/Tiles/Tile ({0}).png' 'BGTile (1).png' 'png/Objects/'

# Grotto: copy tileset + layers
$grottoZip = Join-Path $srcRoot 'super_grotto_escape_files.zip'
if (Test-Path $grottoZip) {
    $dest = Join-Path $root 'tilesets\grotto'
    Ensure-Dir "$dest\tiles"
    Ensure-Dir "$dest\objects"
    $zip = [System.IO.Compression.ZipFile]::OpenRead($grottoZip)
    try {
        $tileset = $zip.Entries | Where-Object { $_.Name -eq 'tileset.png' } | Select-Object -First 1
        if ($tileset) { Extract-ZipEntry $zip $tileset.FullName "$dest\tiles\1.png" }
        $back = $zip.Entries | Where-Object { $_.Name -eq 'back.png' } | Select-Object -First 1
        if ($back) { Extract-ZipEntry $zip $back.FullName "$dest\bg.png" }
        $zip.Entries | Where-Object { $_.FullName -like '*/props/*.png' } | ForEach-Object {
            Extract-ZipEntry $zip $_.FullName "$dest\objects\$($_.Name)" | Out-Null
        }
        # duplicate fill tiles for autotile indices
        2..18 | ForEach-Object { Copy-Item "$dest\tiles\1.png" "$dest\tiles\$_.png" -Force -ErrorAction SilentlyContinue }
    } finally { $zip.Dispose() }
    Write-Host 'Imported grotto pack'
}

# Jungle: bg + select tiles as 1-18
$jungleZip = Join-Path $srcRoot '2D Platformer Jungle Pack (Tio Aimar).zip'
if (Test-Path $jungleZip) {
    $dest = Join-Path $root 'tilesets\jungle'
    Ensure-Dir "$dest\tiles"
    Ensure-Dir "$dest\objects"
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jungleZip)
    try {
        $bg = $zip.Entries | Where-Object { $_.Name -eq 'bg_jungle.png' } | Select-Object -First 1
        if ($bg) { Extract-ZipEntry $zip $bg.FullName "$dest\bg.png" }
        $tileIds = @(3,5,7,9,11,13,15,17,19,21,33,34,35,36,37,38,39,40)
        $i = 1
        foreach ($id in $tileIds) {
            $entry = $zip.Entries | Where-Object { $_.Name -eq "jungle_pack_$('{0:D2}' -f $id).png" } | Select-Object -First 1
            if ($entry) { Extract-ZipEntry $zip $entry.FullName "$dest\tiles\$i.png"; $i++ }
        }
        $zip.Entries | Where-Object { $_.FullName -like '*/bg_jungle_layers/*.png' } | ForEach-Object {
            Extract-ZipEntry $zip $_.FullName "$dest\objects\$($_.Name)" | Out-Null
        }
    } finally { $zip.Dispose() }
    Write-Host 'Imported jungle pack'
}

# Enemies: Pixel Adventure 2
Import-Recursive 'Pixel Adventure 2.zip' 'enemies\pixel_adventure'

# Kenney abstract enemies
Import-Recursive 'Abstract Platformer (370 assets).zip' 'enemies\kenney' 'enemy*.png'

# Characters
Import-Recursive 'TreasureHunter16x16.zip' 'characters\treasure_hunter'
Import-Recursive 'coldvalleys-demoversion-oga.zip' 'characters\coldvalleys'

# Minimal geometry-dash style
Import-Recursive 'content.zip' 'tilesets\minimal'

# Parallax mountains (optional bg)
Import-Recursive 'parallax_mountain_pack.zip' 'backgrounds\mountains'

Write-Host 'Done.'
