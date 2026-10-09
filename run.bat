@echo off
echo ===================================================
echo Building and running Bank Management System
echo ===================================================

if not exist "bin" mkdir bin

javac -cp "lib\mysql-connector-j-8.3.0.jar" -d bin -sourcepath src src\App.java
if %errorlevel% neq 0 (
    echo.
    echo Compilation failed. See errors above.
    pause
    exit /b %errorlevel%
)

echo.
echo Starting Bank Management System...
java -cp "bin;lib\mysql-connector-j-8.3.0.jar" App

pause
