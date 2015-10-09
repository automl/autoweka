@echo off
set DIR=%~dp0\..\
set jarconcat=
SETLOCAL ENABLEDELAYEDEXPANSION
for /F "delims=" %%a IN ('dir /b /s "%DIR%\lib\swingbuilder\*.jar"') do set jarconcat=%%a;!jarconcat!
java -Xmx1024m -cp "%DIR%\autoweka-light.jar;%DIR%\lib\weka.jar;%jarconcat% " %*
