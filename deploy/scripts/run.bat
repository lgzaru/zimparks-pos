@echo off
setlocal

:: --- CONFIGURATION ---
set APP_NAME=zimparks-pos
set JAR_FILE=zimparks-pos.jar
set CONFIG_FILE=config/application.properties
set LOG_DIR=logs
set JAVA_OPTS=-Xmx512m -Xms256m

:: --- CHECK FOR JAVA ---
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH. Please install JDK 17 or later.
    pause
    exit /b 1
)

:: --- PREPARE LOGS ---
if not exist %LOG_DIR% mkdir %LOG_DIR%

:: --- RUN APPLICATION ---
echo [INFO] Starting %APP_NAME%...
echo [INFO] Using configuration: %CONFIG_FILE%
echo [INFO] Logs will be written to %LOG_DIR%/%APP_NAME%.log

:: Use external application.properties if it exists, otherwise use internal
if exist %CONFIG_FILE% (
    set RUN_CMD=java %JAVA_OPTS% -Dspring.config.location=file:%CONFIG_FILE% -jar %JAR_FILE%
) else (
    echo [WARNING] %CONFIG_FILE% not found. Using default internal settings.
    set RUN_CMD=java %JAVA_OPTS% -jar %JAR_FILE%
)

:: Start the app and redirect output to log file AND console
%RUN_CMD% > %LOG_DIR%/%APP_NAME%.log 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] Application crashed with exit code %errorlevel%.
    pause
)

endlocal
