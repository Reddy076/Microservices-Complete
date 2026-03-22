@echo off
SETLOCAL

SET JAVA_HOME=C:\Program Files\Java\jdk-21
SET PATH=%JAVA_HOME%\bin;%PATH%

echo =========================================
echo  Building all microservices with Maven
echo =========================================
mvn clean package -DskipTests --no-transfer-progress

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Maven build failed! Check errors above.
    pause
    exit /b 1
)

echo.
echo =========================================
echo  Build SUCCESS. Starting Docker Compose
echo =========================================
docker compose up --build -d

echo.
echo =========================================
echo  All services starting. Checking status:
echo =========================================
docker compose ps

pause
