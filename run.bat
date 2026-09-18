@echo off
title QuickBite Food Ordering & Delivery Management System
echo ========================================================
echo QuickBite - JavaFX Food Ordering & Delivery Management
echo ========================================================
echo.

set M2_REPO=%USERPROFILE%\.m2\repository

for /r "%M2_REPO%\org\openjfx\javafx-base" %%f in (javafx-base-*-win.jar) do set JFX_BASE=%%f
for /r "%M2_REPO%\org\openjfx\javafx-controls" %%f in (javafx-controls-*-win.jar) do set JFX_CONTROLS=%%f
for /r "%M2_REPO%\org\openjfx\javafx-graphics" %%f in (javafx-graphics-*-win.jar) do set JFX_GRAPHICS=%%f
for /r "%M2_REPO%\org\xerial\sqlite-jdbc" %%f in (sqlite-jdbc-*.jar) do set SQLITE_JAR=%%f

set CP=target\classes;%JFX_BASE%;%JFX_CONTROLS%;%JFX_GRAPHICS%;%SQLITE_JAR%
set MODULE_PATH=%JFX_BASE%;%JFX_CONTROLS%;%JFX_GRAPHICS%

echo Compiling Java source files...
if not exist "target\classes" mkdir "target\classes"
if not exist "target\classes\css" mkdir "target\classes\css"
copy /y "src\main\resources\css\style.css" "target\classes\css\style.css" >nul

javac -cp "%CP%" -d "target\classes" src\main\java\com\quickbite\*.java src\main\java\com\quickbite\config\*.java src\main\java\com\quickbite\model\*.java src\main\java\com\quickbite\dao\*.java src\main\java\com\quickbite\service\*.java src\main\java\com\quickbite\concurrency\*.java src\main\java\com\quickbite\api\*.java src\main\java\com\quickbite\controller\*.java src\main\java\com\quickbite\util\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Starting QuickBite GUI via AppLauncher...
java -cp "%CP%" --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.graphics,javafx.base --enable-native-access=ALL-UNNAMED com.quickbite.AppLauncher
pause
