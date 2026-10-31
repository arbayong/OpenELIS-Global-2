# Windows offline installer — AegisDiagnostics OpenELIS

Builds a single `.exe` installer that deploys OpenELIS Global (with the
`org.openelisglobal.billing` module and license system) on an offline Windows
machine: WAR on Tomcat, private JRE 21, local PostgreSQL.

## Prerequisites (build machine, internet required)

1. **Java 21 JDK** for the WAR build (never Java 25):
   `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`
   `mvn clean package -DskipTests` → `target/OpenELIS-Global.war`
2. **Inno Setup 6** (`iscc`).
3. Stage dependencies next to `aegis-openelis.iss`:
   ```
   packaging/windows/
     aegis-openelis.iss
     bin/            (start.bat, stop.bat)
     launcher/       (OpenElisLauncher.cs + build-launcher.bat)
     config/         (application.properties template)
     deps/
       tomcat/       Apache Tomcat 10.x, unzipped
       jre21/        Temurin JRE 21 (full JRE, or a jlink image including java.sql, jdk.crypto.ec)
       postgresql/   (optional) PostgreSQL Windows binaries; otherwise install the
                     official PostgreSQL MSI on the target before running setup
   ```

4. Build the launcher `.exe` (plan §14.7). It starts Tomcat on the private
   JRE, so the customer never depends on the system `java`:

   ```
   cd packaging\windows\launcher
   build-launcher.bat        :: compiles OpenElisLauncher.cs with the framework csc.exe
   ```

   The `.exe` needs no runtime dependency beyond the .NET Framework that ships
   with Windows. If you skip this step the installer falls back to `start.bat`.

## Build

```
cd packaging\windows
iscc aegis-openelis.iss
```

Output: `dist/AegisOpenELIS-Setup-3.3.0.exe`.

## Install layout (target machine)

```
C:\Program Files\AegisDiagnostics\OpenELIS\
  bin/        start.bat, stop.bat
  jre/        bundled JRE 21 (app never uses system java)
  tomcat/     webapps/openelis.war
  postgresql/ data + service "AegisPostgreSQL"
  config/     application.properties, license file
  logs/
  data/nhis-exports/
```

## First run / licensing

1. Run the installer, then `bin\start.bat`. Open `http://localhost:8080/openelis`.
2. No license → 14-day trial starts. After expiry the app locks except the
   license screen, which shows the machine fingerprint.
3. Send the fingerprint to the vendor; paste the signed license key on the
   license screen to activate (offline, no phone-home).
