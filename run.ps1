# QuickBite PowerShell Launcher Script
$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "QuickBite - JavaFX Food Ordering & Delivery Management" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$m2Repo = "$env:USERPROFILE\.m2\repository"

$jfxBase = (Get-ChildItem -Path "$m2Repo\org\openjfx\javafx-base" -Filter "javafx-base-*-win.jar" -Recurse | Select-Object -First 1).FullName
$jfxControls = (Get-ChildItem -Path "$m2Repo\org\openjfx\javafx-controls" -Filter "javafx-controls-*-win.jar" -Recurse | Select-Object -First 1).FullName
$jfxGraphics = (Get-ChildItem -Path "$m2Repo\org\openjfx\javafx-graphics" -Filter "javafx-graphics-*-win.jar" -Recurse | Select-Object -First 1).FullName
$sqliteJar = (Get-ChildItem -Path "$m2Repo" -Filter "sqlite-jdbc-*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName

$cp = "target\classes;$jfxBase;$jfxControls;$jfxGraphics;$sqliteJar"
$modulePath = "$jfxBase;$jfxControls;$jfxGraphics"

Write-Host "Compiling Java source files..." -ForegroundColor Yellow
if (Test-Path "src\main\resources") {
    Copy-Item -Path "src\main\resources\*" -Destination "target\classes\" -Recurse -Force
}

$sources = (Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse).FullName
javac -cp $cp -d "target\classes" $sources

Write-Host "Launching QuickBite GUI Application..." -ForegroundColor Green
java -cp $cp --module-path "$modulePath" --add-modules javafx.controls,javafx.graphics,javafx.base --enable-native-access=ALL-UNNAMED com.quickbite.AppLauncher
