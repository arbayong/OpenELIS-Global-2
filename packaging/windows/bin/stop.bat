@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JRE_HOME=%APP_HOME%\jre"
set "CATALINA_HOME=%APP_HOME%\tomcat"
set "JAVA_HOME=%JRE_HOME%"
set "CATALINA_BASE=%APP_HOME%\tomcat"
echo Stopping AegisDiagnostics OpenELIS...
call "%CATALINA_HOME%\bin\shutdown.bat"
