# ZimParks POS Backend - Windows Server Deployment Guide

This document provides instructions for deploying the backend to a Windows Server.

## Prerequisites

1.  **JDK 17 or later**: Ensure Java 17 is installed and added to the system `PATH`.
    *   Verify by running `java -version` in Command Prompt.
2.  **PostgreSQL 14+**: Install PostgreSQL on the server (or have access to a remote instance).
    *   Ensure a database named `zimparks_pos2` (or your preferred name) is created.

## Deployment Package Structure

*   `zimparks-pos.jar`: The executable application file.
*   `config/`: Contains configuration files.
    *   `application.properties.example`: Template for configuration.
*   `scripts/`: Contains utility scripts.
    *   `run.bat`: Batch script to start the application.
*   `logs/`: Directory where application logs will be stored.

## Steps to Deploy

### 1. Prepare Configuration

1.  Navigate to the `config/` directory.
2.  Copy `application.properties.example` to `application.properties`.
3.  Open `application.properties` and update the following settings:
    *   `server.port`: The port the application will listen on (default: `8080`).
    *   `spring.datasource.url`: The JDBC URL for your PostgreSQL database (e.g., `jdbc:postgresql://localhost:5432/zimparks_pos2`).
    *   `spring.datasource.username`: Your database username.
    *   `spring.datasource.password`: Your database password.
    *   `app.jwt.secret`: A strong, random string (at least 256 bits).
    *   `app.cors.allowed-origins`: Update with your frontend's URL.

### 2. Database Setup

Ensure the database specified in `spring.datasource.url` exists. Hibernate is configured to automatically update the schema (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Run the Application

1.  Open a Command Prompt or PowerShell in the deployment directory.
2.  Navigate to the `scripts` folder and run `run.bat`.
    ```cmd
    cd scripts
    run.bat
    ```
3.  The application will start, and logs will be written to `logs/zimparks-pos.log`.

## Running as a Windows Service (Recommended)

To ensure the backend starts automatically when the server boots, it's recommended to use a tool like **WinSW** (Windows Service Wrapper) or **NSSM** (Non-Sucking Service Manager).

### Example with NSSM:

1.  Download NSSM from [nssm.cc](https://nssm.cc/download).
2.  Run `nssm install ZimParksBackend`.
3.  In the GUI:
    *   **Path**: `C:\path\to\java.exe`
    *   **Startup directory**: `C:\path\to\deployment\folder`
    *   **Arguments**: `-Dspring.config.location=file:config/application.properties -jar zimparks-pos.jar`
4.  Click "Install service".
5.  Start the service: `nssm start ZimParksBackend`.

## Verification

Access the health or documentation endpoint (if enabled) at:
`http://<server-ip>:<port>/swagger-ui/index.html`
