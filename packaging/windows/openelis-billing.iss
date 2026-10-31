; Inno Setup script for OpenELIS Global (billing module)
; Offline Windows installer: bundles WAR, Tomcat, JRE 21, PostgreSQL.
; Build with:  iscc openelis-billing.iss
; Prerequisites staged next to this script:
;   ..\..\target\OpenELIS-Global.war
;   deps\tomcat\            (Apache Tomcat 10.x distribution, unzipped)
;   deps\jre21\             (Temurin JRE 21, jlink'd or full JRE)
;   deps\postgresql\        (PostgreSQL Windows binaries, or use the official PG installer at runtime)

#define MyAppName "OpenELIS Billing"
#define MyAppVersion "3.3.0"
#define MyAppPublisher "OpenELIS"
#define MyAppURL "https://github.com/arbayong/OpenELIS-Global-2"

[Setup]
AppId={{A7E1C4B2-3F5D-4E6A-9B8C-1D2E3F4A5B6C}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\OpenELIS\Billing
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=..\..\dist
OutputBaseFilename=OpenELIS-Billing-Setup-{#MyAppVersion}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
UninstallDisplayName={#MyAppName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
; Application WAR
Source: "..\..\target\OpenELIS-Global.war"; DestDir: "{app}\tomcat\webapps"; DestName: "openelis.war"; Flags: ignoreversion
; Tomcat
Source: "deps\tomcat\*"; DestDir: "{app}\tomcat"; Flags: ignoreversion recursesubdirs createallsubdirs
; Bundled JRE 21 (private to the app)
Source: "deps\jre21\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
; Launcher: the .exe wrapper that starts Tomcat on the private JRE (build it
; first with launcher\build-launcher.bat). The .bat files ship as the fallback
; for machines where the wrapper could not be produced.
Source: "launcher\OpenELIS.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Check: FileExists(ExpandConstant('{src}\launcher\OpenELIS.exe'))
Source: "bin\start.bat"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "bin\stop.bat"; DestDir: "{app}\bin"; Flags: ignoreversion
; Config template
Source: "config\application.properties"; DestDir: "{app}\config"; Flags: ignoreversion onlyifdoesntexist
; License docs
Source: "..\..\LICENSE_NOTES.md"; DestDir: "{app}"; Flags: ignoreversion onlyifdoesntexist; Check: FileExists(ExpandConstant('..\..\LICENSE_NOTES.md'))

[Dirs]
Name: "{app}\logs"
Name: "{app}\data"
Name: "{app}\data\nhis-exports"
Name: "{app}\config"

[Icons]
Name: "{group}\Start OpenELIS"; Filename: "{app}\bin\OpenELIS.exe"; WorkingDir: "{app}\bin"; Check: FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))
Name: "{group}\Start OpenELIS"; Filename: "{app}\bin\start.bat"; WorkingDir: "{app}\bin"; Check: not FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))
Name: "{group}\Stop OpenELIS"; Filename: "{app}\bin\OpenELIS.exe"; Parameters: "--stop"; WorkingDir: "{app}\bin"; Check: FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))
Name: "{group}\Stop OpenELIS"; Filename: "{app}\bin\stop.bat"; WorkingDir: "{app}\bin"; Check: not FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))
Name: "{group}\OpenELIS"; Filename: "http://localhost:8080/openelis"; WorkingDir: "{app}"
Name: "{group}\Uninstall {#MyAppName}"; Filename: "{uninstallexe}"

[Run]
; Initialize PostgreSQL data directory on first install if bundled
Filename: "{app}\postgresql\bin\initdb.exe"; Parameters: "-D ""{app}\postgresql\data"" -U postgres"; WorkingDir: "{app}\postgresql\bin"; Flags: runhidden; Check: FileExists(ExpandConstant('{app}\postgresql\bin\initdb.exe'))
; Register and start PostgreSQL service
Filename: "{app}\postgresql\bin\pg_ctl.exe"; Parameters: "register -N ""OpenELISPostgreSQL"" -D ""{app}\postgresql\data"""; Flags: runhidden; Check: FileExists(ExpandConstant('{app}\postgresql\bin\pg_ctl.exe'))
Filename: "{app}\postgresql\bin\pg_ctl.exe"; Parameters: "start -N ""OpenELISPostgreSQL"" -D ""{app}\postgresql\data"""; Flags: runhidden; Check: FileExists(ExpandConstant('{app}\postgresql\bin\pg_ctl.exe'))
; Start Tomcat with bundled JRE
Filename: "{app}\bin\OpenELIS.exe"; Description: "Start OpenELIS now"; Flags: nowait postinstall skipifsilent; Check: FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))
Filename: "{app}\bin\start.bat"; Description: "Start OpenELIS now"; Flags: nowait postinstall skipifsilent; Check: not FileExists(ExpandConstant('{app}\bin\OpenELIS.exe'))

[UninstallRun]
Filename: "{app}\bin\stop.bat"; Flags: runhidden
Filename: "{app}\postgresql\bin\pg_ctl.exe"; Parameters: "stop -D ""{app}\postgresql\data"""; Flags: runhidden; Check: FileExists(ExpandConstant('{app}\postgresql\bin\pg_ctl.exe'))
