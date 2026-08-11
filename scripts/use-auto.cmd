@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0switch-environment.ps1" auto
if errorlevel 1 exit /b %errorlevel%
