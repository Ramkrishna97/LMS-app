@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"

echo Building the Render backend artifact...
call gradlew.bat clean bootJar -PskipFrontend
if errorlevel 1 goto :failed

echo Building the Netlify frontend artifact...
pushd frontend
call npm ci
if errorlevel 1 (
  popd
  goto :failed
)
call npm run build
if errorlevel 1 (
  popd
  goto :failed
)
popd

echo.
echo Deployment artifacts built successfully.
echo Backend: build\libs\learning-platform-0.0.1-SNAPSHOT.jar
echo Frontend: frontend\dist
exit /b 0

:failed
echo.
echo Deployment build failed. Review the output above.
exit /b 1
