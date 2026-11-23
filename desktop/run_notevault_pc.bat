@echo off
title NoteVault PC Companion
cd /d "%~dp0"
echo ========================================================
echo   Starting NoteVault PC Companion...
echo ========================================================
python server.py
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Failed to start with python. Trying py...
    py server.py
)
pause
