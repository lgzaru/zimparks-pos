# Manual Deployment Preparation Commands

To manually recreate the deployment package for your Windows Server, follow these steps in your project root directory.

## 1. Build the Executable JAR
Use the Maven Wrapper (`mvnw`) to clean and package the application while skipping tests to save time.
```bash
./mvnw clean package -DskipTests
```
*This creates a JAR file in the `target/` directory.*

## 2. Create the Deployment Directory Structure
Create a clean `deploy` folder with subdirectories for configuration, logs, and scripts.
```bash
mkdir -p deploy/config deploy/scripts deploy/logs
```

## 3. Copy the JAR and Configuration Template
Rename the JAR for consistency and copy the existing configuration as an example for the server.
```bash
# Copy and rename the JAR
cp target/zimparks-0.0.1-SNAPSHOT.jar deploy/zimparks-pos.jar

# Copy the properties file as a template AND as the active config
cp src/main/resources/application.properties deploy/config/application.properties
cp src/main/resources/application.properties deploy/config/application.properties.example
```

## 4. Create the Startup Script (`deploy/scripts/run.bat`)
Create a new file named `run.bat` inside `deploy/scripts/` with the following content:
```batch
@echo off
setlocal

:: Get the directory where the script is located
set "SCRIPT_DIR=%~dp0"
:: Change to the parent directory (the "deploy" folder)
cd /d "%SCRIPT_DIR%.."

set APP_NAME=zimparks-pos
set JAR_FILE=zimparks-pos.jar
set CONFIG_FILE=config\application.properties
set LOG_DIR=logs
set JAVA_OPTS=-Xmx512m -Xms256m

:: Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH.
    pause
    exit /b 1
)

:: Prepare logs
if not exist %LOG_DIR% mkdir %LOG_DIR%

:: Run Application
echo [INFO] Starting %APP_NAME%...
if exist %CONFIG_FILE% (
    echo [INFO] Using configuration: %CONFIG_FILE%
    java %JAVA_OPTS% -Dspring.config.location=file:%CONFIG_FILE% -jar %JAR_FILE%
) else (
    echo [WARNING] %CONFIG_FILE% not found. Using defaults.
    echo [TIP] Copy config\application.properties.example to %CONFIG_FILE% for custom settings.
    java %JAVA_OPTS% -jar %JAR_FILE%
)

pause
endlocal
```

## 5. Verify the Package
Your `deploy/` directory should now look like this:
```text
deploy/
├── config/
│   ├── application.properties
│   └── application.properties.example
├── logs/
├── scripts/
│   └── run.bat
└── zimparks-pos.jar
```

## 6. Server Setup (Windows)
1.  Copy the entire `deploy/` folder to your Windows Server.
2.  In `deploy/config/`, copy `application.properties.example` to `application.properties` and edit your database credentials.
3.  Run `scripts\run.bat` to start the backend.
