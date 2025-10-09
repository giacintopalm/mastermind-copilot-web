@echo off
echo Starting Mastermind deployment to Azure...
powershell.exe -ExecutionPolicy Bypass -File "%~dp0deploy-to-azure.ps1"
echo.
echo If deployment was successful, your app should be available at:
echo https://nice-sand-04c84f41e.1.azurestaticapps.net
echo.
pause