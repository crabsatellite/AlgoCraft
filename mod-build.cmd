@echo off
pwsh -NoProfile -File "%~dp0mod-build.ps1" %*
exit /b %errorlevel%
