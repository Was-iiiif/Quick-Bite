$jfxBase = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "javafx-base-26*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$jfxControls = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "javafx-controls-26*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$jfxGraphics = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "javafx-graphics-26*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$sqlite = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "sqlite-jdbc-3.53.4.0.jar" -Recurse | Select-Object -First 1).FullName
$jackson = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "jackson-databind-*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$jacksonCore = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "jackson-core-*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$jacksonAnnotations = (Get-ChildItem -Path "C:\Users\User\.m2\repository" -Filter "jackson-annotations-*.jar" -Recurse | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1).FullName
$cp = "target\classes;$jfxBase;$jfxControls;$jfxGraphics;$sqlite;$jackson;$jacksonCore;$jacksonAnnotations"
$sources = (Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse).FullName
if (Test-Path "src\main\resources") {
    Copy-Item -Path "src\main\resources\*" -Destination "target\classes\" -Recurse -Force
}
Write-Host "Compiling..."
javac -cp $cp -d "target\classes" $sources 2>&1
Write-Host "Exit code: $LASTEXITCODE"
