@echo off
setlocal

:: Get the directory where the script is located
set "SCRIPT_DIR=%~dp0"
:: Change to the parent directory (the "deploy" folder)
cd /d "%SCRIPT_DIR%.."

:: --- CONFIGURATION ---
set APP_NAME=zimparks-pos
set JAR_FILE=zimparks-pos.jar
set CONFIG_FILE=config\application.properties
set LOG_DIR=logs
set JAVA_OPTS=-Xmx512m -Xms256m

:: Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH. Please install JDK 17 or later.
    pause
    exit /b 1
)

:: Prepare logs
if not exist %LOG_DIR% mkdir %LOG_DIR%

:: Run Application
echo [INFO] Starting %APP_NAME%...

if exist "%CONFIG_FILE%" (
    echo [INFO] Using configuration: %CONFIG_FILE%
    set RUN_CMD=java %JAVA_OPTS% -Dspring.config.location=file:"%CONFIG_FILE%" -jar %JAR_FILE%
) else (
    echo [WARNING] %CONFIG_FILE% not found. Using defaults.
    echo [TIP] Copy config\application.properties.example to %CONFIG_FILE% for custom settings.
    set RUN_CMD=java %JAVA_OPTS% -jar %JAR_FILE%
)

:: Start the app and redirect output to log file AND console
echo [INFO] Logs will be written to %LOG_DIR%\%APP_NAME%.log
%RUN_CMD% > %LOG_DIR%\%APP_NAME%.log 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] Application crashed with exit code %errorlevel%.
    pause
)

endlocal
