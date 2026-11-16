@echo off
setlocal

set "FRONTEND_DIR=%~dp0"
for %%I in ("%FRONTEND_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "STATIC_DIR=%PROJECT_ROOT%\backend\src\main\resources\static"

pushd "%FRONTEND_DIR%"
if errorlevel 1 exit /b 1

call npm run build
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
if not "%BUILD_EXIT_CODE%"=="0" goto build_failed
if not exist "dist\" goto missing_dist

popd
robocopy "%FRONTEND_DIR%dist" "%STATIC_DIR%" /MIR
set "COPY_EXIT_CODE=%ERRORLEVEL%"
if %COPY_EXIT_CODE% GEQ 8 goto copy_failed

echo Frontend build copied to "%STATIC_DIR%".
exit /b 0

:build_failed
popd
echo Frontend build failed with exit code %BUILD_EXIT_CODE%.
exit /b %BUILD_EXIT_CODE%

:missing_dist
popd
echo Build output not found: "%FRONTEND_DIR%dist"
exit /b 1

:copy_failed
echo Failed to copy build output with robocopy exit code %COPY_EXIT_CODE%.
exit /b %COPY_EXIT_CODE%
