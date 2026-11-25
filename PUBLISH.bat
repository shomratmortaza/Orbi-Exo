@echo off
setlocal
cd /d "%~dp0"
echo ============================================
echo   ORBI - publish to GitHub (one click)
echo ============================================
where git >nul 2>nul
if errorlevel 1 (
  echo Git is not installed. Install it from https://git-scm.com/download/win then run this file again.
  pause & exit /b 1
)
if exist *.jks ( echo STOP: a .jks key file is in this folder. Move it out first. & pause & exit /b 1 )
if exist .env ( echo STOP: a .env file is in this folder. Move it out first. & pause & exit /b 1 )

if not exist .git git init
git branch -M main
git remote remove origin >nul 2>nul
git remote add origin https://github.com/shomratmortaza/Orbi-Exo.git
echo Fetching your existing repo...
git fetch origin
git rev-parse --verify origin/main >nul 2>nul
if not errorlevel 1 git reset --soft origin/main
git add -A
git commit -m "ORBI: download center, alarms, calendar, release workflow"
echo Pushing code (a browser window may ask you to sign in to GitHub)...
git push -u origin main
if errorlevel 1 ( echo PUSH FAILED. Send me the red text above. & pause & exit /b 1 )

echo.
set /p V=Type version to release, for example v1.0.0 (leave empty to skip release):
if "%V%"=="" goto done
git tag %V%
git push origin %V%
if errorlevel 1 ( echo TAG PUSH FAILED. If the tag already exists, use a new version like v1.0.1. & pause & exit /b 1 )
echo.
echo Release build started. Open https://github.com/shomratmortaza/Orbi-Exo/actions and wait for green tick.
:done
echo DONE.
pause
