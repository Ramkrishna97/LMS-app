@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

powershell -NoProfile -Command "if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet) { exit 0 } else { exit 1 }" > nul 2>&1
if not errorlevel 1 (
  echo Port 8080 is already in use. Run stop.bat before starting the updated application.
  exit /b 1
)

if not exist "frontend\node_modules" (
  echo Installing frontend dependencies...
  cd frontend
  call npm install
  cd ..
)

start "Learning Platform" /D "%~dp0" cmd /k ""%~dp0gradlew.bat" bootRun"
echo Waiting for the application to start...
for /l %%N in (1,1,30) do (
  powershell -NoProfile -Command "if (Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet) { exit 0 } else { exit 1 }" > nul 2>&1
  if not errorlevel 1 goto :ready
  ping -n 2 127.0.0.1 > nul 2>&1
)
echo The application did not start on port 8080. Check the Learning Platform window.
goto :done
:ready
start "" http://localhost:8080
:done
endlocal
