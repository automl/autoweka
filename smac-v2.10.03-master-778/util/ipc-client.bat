@echo off
set SMACMEM=1024
IF NOT "%SMAC_MEMORY%"=="" (set SMACMEM=%SMAC_MEMORY%)
set DIR=%~dp0
IF EXIST "%DIR%\lib\" GOTO USE_LIB
set DIR=%DIR%\..\
:USE_LIB

set EXEC=ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc.reversetcpclient.IPCTAEClient
set jarconcat=
SETLOCAL ENABLEDELAYEDEXPANSION
for /F "delims=" %%a IN ('dir /b /s "%DIR%\*.jar"') do set jarconcat=%%a;!jarconcat!
for /F "delims=" %%a IN ('dir /b /s "%DIR%\lib\*.jar"') do set jarconcat=%%a;!jarconcat!
echo Starting ipc-client with %SMACMEM% MB of RAM
java -Xmx%SMACMEM%m -cp "%DIR%conf\;%DIR%patches\;%jarconcat%%DIR%patches\ " ca.ubc.cs.beta.aeatk.ant.execscript.Launcher %EXEC% %*
