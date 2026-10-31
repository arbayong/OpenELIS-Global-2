@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JRE_HOME=%APP_HOME%\jre"
set "CATALINA_HOME=%APP_HOME%\tomcat"
set "JAVA_HOME=%JRE_HOME%"
set "CATALINA_BASE=%APP_HOME%\tomcat"
set "LOG_DIR=%APP_HOME%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
set "CATALINA_OPTS=-Dspring.config.additional-location=file:%APP_HOME%\config\ -Dopenelis.billing.data.dir=%APP_HOME%\data"
echo Starting OpenELIS Billing with bundled JRE 21...
call "%CATALINA_HOME%\bin\catalina.bat" run
