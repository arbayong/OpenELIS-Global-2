@echo off
setlocal
REM Build the Aegis OpenELIS launcher (plan §14.7). Run on the Windows build
REM machine; the .NET Framework csc.exe that ships with Windows is enough and
REM adds no runtime dependency to the installed app.
set OUT=%~dp0OpenELIS.exe
"%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe" -nologo -optimize+ -target:winexe -out:"%OUT%" "%~dp0OpenElisLauncher.cs"
if errorlevel 1 (
    echo Launcher build failed.
    exit /b 1
)
echo Built %OUT%
