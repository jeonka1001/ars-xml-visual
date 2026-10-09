@echo off
rem arsxml2wv launcher for environments where the exe cannot be used.
rem Requires Java 1.8 or later in JAVA_HOME or PATH.
setlocal
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java"
"%JAVA%" -jar "%~dp0arsxml2wv.jar" %*
exit /b %ERRORLEVEL%
