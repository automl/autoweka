@echo off
set SMACMEM=1024
IF NOT "%SMAC_MEMORY%"=="" (set SMACMEM=%SMAC_MEMORY%)
set DIR=%~dp0
IF EXIST "%DIR%\lib\" GOTO USE_LIB
set DIR=%DIR%\..\
:USE_LIB

set EXEC=ca.ubc.cs.beta.smac.executors.SMACExecutor
java -Xmx%SMACMEM%m -cp "%DIR%conf\;%DIR%patches\;%DIR%\*;%DIR%\lib\*;%DIR%patches\ " ca.ubc.cs.beta.aeatk.ant.execscript.Launcher %EXEC% %*
