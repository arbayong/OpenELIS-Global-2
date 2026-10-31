using System;
using System.Diagnostics;
using System.IO;

/*
 * Aegis OpenELIS launcher (plan §14.7).
 *
 * Starts Tomcat with the installer's private JRE instead of whatever java.exe
 * happens to be on the PATH, so the customer always runs the Java version the
 * build was made and tested with. Dependency-free: it targets the .NET
 * Framework that ships with Windows, so nothing has to be installed.
 *
 * Build with build-launcher.bat (uses the framework csc.exe).
 *
 * Behaviour:
 *   OpenELIS.exe             start Tomcat in a console window
 *   OpenELIS.exe --stop      send Tomcat a shutdown signal
 *   OpenELIS.exe --service   not supported: Tomcat is run in the foreground,
 *                            stop it from the window or the Start-menu Stop item
 */

internal static class OpenElisLauncher
{
    private const int TomcatShutdownPort = 8005;

    private static int Main(string[] args)
    {
        string mode = args.Length > 0 ? args[0].ToLowerInvariant() : "--start";
        string appHome = AppDomain.CurrentDomain.BaseDirectory;

        string jre = Path.Combine(appHome, "jre");
        string java = Path.Combine(jre, "bin", "java.exe");
        if (!File.Exists(java))
        {
            Console.Error.WriteLine(
                "Bundled JRE not found at " + java + Environment.NewLine +
                "Reinstall AegisDiagnostics OpenELIS; the app does not use the system Java.");
            return 2;
        }

        string catalinaHome = Path.Combine(appHome, "tomcat");

        if (mode == "--stop")
        {
            return StopTomcat();
        }

        return StartTomcat(java, catalinaHome, appHome);
    }

    private static int StartTomcat(string java, string catalinaHome, string appHome)
    {
        string webapps = Path.Combine(catalinaHome, "webapps");
        string war = Path.Combine(webapps, "openelis.war");
        if (!File.Exists(war))
        {
            Console.Error.WriteLine("Deployment not found at " + war);
            return 3;
        }

        /* Tomcat's own bootstrap keeps the daemon semantics (shutdown port,
           lifecycle listeners); the launcher only has to hand it the right
           Java and working directory. */
        string bootstrap = Path.Combine(catalinaHome, "bin", "bootstrap.jar");
        string classpath = bootstrap + ";" + Path.Combine(catalinaHome, "bin", "tomcat-juli.jar");

        string configDir = Path.Combine(appHome, "config");

        /* .NET Framework has no ProcessStartInfo.ArgumentList, so the command
           line is built by hand. Every path is quoted: the default install
           location is under Program Files, where spaces are guaranteed. */
        var arguments = new System.Text.StringBuilder();
        arguments.Append(Quote("-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"));
        arguments.Append(' ').Append(Quote("-Dcatalina.base=" + catalinaHome));
        arguments.Append(' ').Append(Quote("-Dcatalina.home=" + catalinaHome));
        arguments.Append(' ').Append(Quote("-Djava.io.tmpdir=" + Path.Combine(catalinaHome, "temp")));

        /* Site configuration lives outside the deployment so a WAR redeploy
           does not discard it, matching packaging/windows/config. */
        arguments.Append(' ').Append(Quote("-Dspring.config.additional-location=file:" + configDir + Path.DirectorySeparatorChar));
        arguments.Append(' ').Append(Quote("-Daegis.data.dir=" + Path.Combine(appHome, "data")));

        arguments.Append(' ').Append(Quote("-cp")).Append(' ').Append(Quote(classpath));
        arguments.Append(' ').Append(Quote("org.apache.catalina.startup.Bootstrap"));
        arguments.Append(' ').Append(Quote("start"));

        var start = new ProcessStartInfo
        {
            FileName = java,
            Arguments = arguments.ToString(),
            WorkingDirectory = catalinaHome,
            UseShellExecute = false,
        };

        Console.WriteLine("Starting AegisDiagnostics OpenELIS on http://localhost:8080/openelis");
        Console.WriteLine("Close this window or run OpenELIS.exe --stop to shut down.");

        try
        {
            using (Process tomcat = Process.Start(start))
            {
                tomcat.WaitForExit();
                return tomcat.ExitCode;
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("Failed to start Tomcat: " + ex.Message);
            return 4;
        }
    }

    /* Quote an argument so paths with spaces survive the command line. */
    private static string Quote(string value)
    {
        return "\"" + value.Replace("\"", "\\\"") + "\"";
    }

    private static int StopTomcat()

    {
        /* A shutdown command on the shutdown port is the supported way to stop
           Tomcat cleanly; the port is configured in tomcat/conf/server.xml. */
        try
        {
            using (var client = new System.Net.Sockets.TcpClient("127.0.0.1", TomcatShutdownPort))
            using (var stream = client.GetStream())
            using (var writer = new System.IO.StreamWriter(stream))
            {
                writer.Write("SHUTDOWN");
                writer.Flush();
            }
            Console.WriteLine("Shutdown signal sent to Tomcat.");
            return 0;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("Tomcat does not appear to be running: " + ex.Message);
            return 1;
        }
    }
}
