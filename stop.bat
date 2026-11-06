@echo off
setlocal
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  taskkill /PID %%P /F > nul 2>&1
  echo Learning platform stopped.
  goto :done
)
echo Learning platform is not running.
:done
endlocal
