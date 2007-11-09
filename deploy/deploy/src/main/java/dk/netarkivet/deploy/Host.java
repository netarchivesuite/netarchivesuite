/* $Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.deploy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.archive.crawler.Heritrix;

import dk.netarkivet.archive.arcrepository.ArcRepositoryApplication;
import dk.netarkivet.archive.bitarchive.BitarchiveApplication;
import dk.netarkivet.archive.bitarchive.BitarchiveMonitorApplication;
import dk.netarkivet.archive.indexserver.IndexServerApplication;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.HTTPRemoteFile;
import dk.netarkivet.common.distribute.HTTPSRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SimpleXml;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.harvesting.HarvestControllerApplication;
import dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook;
import dk.netarkivet.harvester.sidekick.SideKick;
import dk.netarkivet.harvester.webinterface.HarvestDefinitionApplication;
import dk.netarkivet.monitor.jmx.HostForwarding;
import dk.netarkivet.viewerproxy.ViewerProxyApplication;

/**
 * The host class represents the machines to install the netarkiv software onto,
 * *and* the ftp-servers and the jms-broker (ftp and jms should probably be
 * reprensented by a different class).
 *
 */
public class Host {
    /**
     * Location must be kb or sb.
     */
    private final String location;

    /** Enumeration of the allowable types of hosts (including service hosts)
     */
    public static enum Type {
        admin, bitarchive, harvesters, access, ftp, jms, mail, indexserver
    };

    /**
     * Host type: admin, bitarchive, harvester, ftp, jms, access.
     */
    private EnumSet<Type> types;

    /**
     * The host name.
     */
    private final String name;

    /**
     * Directory where the installation is placed on Unix machines.
     */
    private String installDir;
    /**
     * Directory where the installation is placed on windows machines.
     */
    private String installDirWindows;

    /**
     * The settings xml.
     */
    private SimpleXml settingsXml;

    /**
     * Additional properties defined for the host.
     */
    private Map<String, List<String>> properties =
            new HashMap<String, List<String>>();

    /**
     * Contents of the default log.prop to use.
     */
    private String logProperties;

    /** Contents of the JMX password file.
     */
    private String jmxPasswordFileContents;

    /**
     * List of start scripts, used to generate startAll.
     */
    private List<String> appsToStart = new ArrayList<String>();

    /** The extension of our webpage packages. */
    private static final String WAR_EXTENSION = ".war";

    /** Enumeration of the known types of operating systems.
     * These are only needed to generate different types of scripts for
     * Windows, since it doesn't have a normal Unix-style shell nor Unix-style
     * dir separators.
     */
    public static enum OS {
        UNIX, WINDOWS
    };

    /** The OS that this host uses.  If nothing else is said, assume Unix.
     */
    private OS os = OS.UNIX; // Default value

    /** Set of applications defined for this host.
     * Each of these will have start/stop scripts generated as well as a
     * separate settings.xml file.
     *
     * Currently, this only contains the applications that are specified
     * separately using the application tag in it_conf.xml.
     */
    Set<Application> applications = new HashSet<Application>();

    /**
     * Create new host instance.
     *
     * @param name The name of the host
     * @param location The host location
     * @param type     the host type (admin, bitarchive, harvesters, ...)
     */
    public Host(String name, String location, Type type) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        ArgumentNotValid.checkNotNullOrEmpty(location, "String location");
        ArgumentNotValid.checkNotNull(type, "Host.Type type");
        this.name = name;
        this.location = location;
        types = EnumSet.of(type);
    }

    /**
     * The host location.
     *
     * @return The host location.
     */
    public String getLocation() {
        return location;
    }

    /** Set which OS this host uses.
     *
     * @param value Must be one of the ones defined in the OS enumeration
     * (though in arbitrary case).
     */
    public void setOS(String value) {
        os = OS.valueOf(value.toUpperCase());
    }

    /** Get which operating system this host uses.
     *
     * @return One of the operating systems defined in the OS enumeration.
     */
    public OS getOS() {
        return os;
    }

    /**
     * Add another type to the host.
     */
    public void addType(Type type) {
        types.add(type);
    }

    public boolean isType(Type type) {
        return types.contains(type);
    }

    /**
     * Get the host name.
     *
     * @return The host name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the installation directory.
     *
     * @return The installation directory.
     */
    public String getInstallDir() {
        return installDir;
    }

    /**
     * Set the root of the installation directory.
     *
     * @param theInstallDir the directory to install the application to.
     */
    public void setInstallDir(String theInstallDir) {
        installDir = theInstallDir;
    }

    /**
     * Get the installation directory for Windows machines.
     *
     * @return The installation directory for Windows machines.
     */
    public String getInstallDirWindows() {
        return installDirWindows;
    }

    /**
     * Set the root of the installation directory for windows machines.
     *
     * @param theDir the windows directory to install to.
     */
    public void setInstallDirWindows(String theDir) {
        installDirWindows = theDir;
    }

    /** Add an application that this host should start.
     *
     * @param app An application that this host should take care of installing
     * and starting.
     */
    public void addApplication(Application app) {
        ArgumentNotValid.checkNotNull(app, "Application app");
        applications.add(app);
        appsToStart.add(app.getType().toString());
    }

    /**
     * Get the default settings.xml for this host.
     *
     * @return default settings.xml
     */
    public SimpleXml getSettingsXml() {
        return settingsXml;
    }

    /**
     * Define the settings xml used by the host.
     *
     * @param theXml
     */
    public void setSettingsXml(SimpleXml theXml) {
        settingsXml = theXml;
    }

    /**
     * Override the setting.xml with host specific settings.
     *
     * Will also add the location of the WAR files specified.
     *
     * Will also read bitarchive directories from properties, and write these to
     * settings.
     *
     * @param environmentName the environmentname the host should use.
     * @param ftpHost         the host that provides FTP services.
     * @param mailHost        the host that provides mail services.
     * @param jmsHost       the host that provides JMS services.
     */
    public void overrideSettingsXmlDefaults(String environmentName,
                                            Host ftpHost,
                                            Host mailHost, Host jmsHost) {

        if (jmsHost != null) {
            overrideSetting(Settings.JMS_BROKER_HOST, jmsHost.getName());
            overrideSetting(Settings.JMS_BROKER_CLASS,
                    jmsHost.getProperty(Constants.JMS_CLASS_PROPERTY));

            overrideSetting(Settings.ENVIRONMENT_NAME, environmentName);
        }

        if (ftpHost != null) {
            overrideSetting(Settings.FTP_SERVER_NAME, ftpHost.getName());
            overrideSetting(Settings.FTP_USER_NAME,
                    ftpHost.getProperty(Constants.FTP_USER_PROPERTY));
            overrideSetting(Settings.FTP_USER_PASSWORD,
                    ftpHost.getProperty(Constants.FTP_PASSWORD_PROPERTY));
        }

        overrideSetting(Settings.ENVIRONMENT_THIS_LOCATION,
                location.toUpperCase());

        if (mailHost != null) {
            overrideSetting(Settings.MAIL_SERVER, mailHost.getName());
            overrideSetting(Settings.MAIL_RECEIVER,
                    mailHost.getProperty(Constants.MAIL_RECEIVER_PROPERTY));
            overrideSetting(Settings.MAIL_SENDER,
                    mailHost.getProperty(Constants.MAIL_SENDER_PROPERTY));
        }

        List<String> webapplications
                = settingsXml.getList(Settings.SITESECTION_WEBAPPLICATION);
        List<String> warFiles = new ArrayList<String>(webapplications.size());
        for (String webapplication : webapplications) {
            if (webapplication.toLowerCase().endsWith(WAR_EXTENSION)) {
                warFiles.add(webapplication);
            } else {
                warFiles.add(webapplication + WAR_EXTENSION);
            }
        }
        settingsXml.update(Settings.SITESECTION_WEBAPPLICATION,
                warFiles.toArray(new String[0]));

        if (properties.get(Constants.BITARCHIVE_FILEDIR_PROPERTY) != null) {
            settingsXml.update(Settings.BITARCHIVE_SERVER_FILEDIR,
                    properties.get(Constants.BITARCHIVE_FILEDIR_PROPERTY)
                            .toArray(new String[0]));
        }
    }

    /** Override a value in settings.xml. This will remove all settings for
     * this key.
     *
     * @param key The key, as taken from the definitions in Settings
     * @param value The new value to set it to.
     */
    void overrideSetting(String key, String value) {
        settingsXml.update(key, value);
    }

   /** Append a suffix to a value in settings.xml.
     *
     * @param key The key, as taken from the definitions in Settings
     * @param suffix The suffix to append to the current value.
     */
    void appendToSetting(String key, String suffix) {
       String currentValue = settingsXml.getString(key);
       settingsXml.update(key, currentValue + suffix);
    }

    /**
     * Get list of additional properties.
     *
     * @return The list of aditional properties in a map from String to list of
     *         Strings.
     */
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    /**
     * Add a property value to the host properties map.
     *
     * @param key   the property name.
     * @param value the property value.
     */
    public void addProperty(String key, String value) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        ArgumentNotValid.checkNotNullOrEmpty(value, "value");
        List<String> valueList;
        if (properties.containsKey(key)) {
            valueList = properties.get(key);
        } else {
            valueList = new LinkedList<String>();
            properties.put(key, valueList);
        }
        valueList.add(value);
    }

    /**
     * Sets the default logging properties filename.
     *
     * @param logProperties
     */
    public void setLogProperties(String logProperties) {
        this.logProperties = logProperties;
    }

    /** Set the full contents of the JMX password file.
     *
     * @param passwordFileContents File contents that should have real passwords
     * substituted for placeholders.
     */
    public void setJmxPasswordFileContents(String passwordFileContents) {
        jmxPasswordFileContents = passwordFileContents;
    }

    /**
     * Create the start scripts for applications running on the admin machine.
     *
     * @param dir directory to create the scripts in
     * @param locations List of the locations (bitarchives) that there should
     * be monitors for.
     */
    public void writeStartAdminApps(File dir, List<String> locations) {
        File res = new File(dir, "start_harvestdefinition.sh");
        writeStandardStart(res,
                           HarvestDefinitionApplication.class.getName());

        res = new File(dir, "start_arcrepository.sh");
        writeStandardStart(res,
                           ArcRepositoryApplication.class.getName());

        File logdir = new File(installDir + "/conf/");
        for (String l : locations) {
            String settingsfn = installDir + "/conf/settings_bamonitor_"
                                + l + ".xml";
            res = new File(dir, "start_bitarchive_monitor_" + l + ".sh");
            writeStart(res,
                       BitarchiveMonitorApplication.class.getName(),
                       settingsfn, logdir, logProperties, "");
        }
    }

    /**
     * Create the start scripts for the bitarchive application.
     *
     * @param dir the directory to store the script in
     */
    public void writeStartBitarchiveApps(File dir) {
        File res = new File(dir, "start_bitarchive.sh");
        if (getOS().equals(OS.WINDOWS)) {
            writeStartBat(dir, BitarchiveApplication.class.getName());
        } else if (installDir != null) {
            writeStandardStart(res,
                    BitarchiveApplication.class.getName());
        } else {
            throw new IllegalState("Bit archive " + this + " in dir " + dir
                    + " has neither Windows nor Linux installDir");
        }
    }

    /**
     * Create the start scripts for the harvester applications. Expected to be
     * called once for each harvester instance.
     *
     * @param res   The file to write the harvester script to. Note that this
     *              filename, with "harvester" replaced by "sidekick" will also
     *              be used to write the sidekick start script.
     * @param setfn the name of the settings file to use.
     */
    public void writeStartHarvesterApps(File res, String setfn) {
        File logdir = new File(installDir + "/conf/");
        String settingsfn = installDir + "/" + setfn;

        writeStart(res,
                   HarvestControllerApplication.class.getName(),
                   settingsfn, logdir, logProperties,
                   getHeritrixUiPortArgs() + " " + getHeritrixJmxPortArgs());


        String sidekick = SideKick.class.getName() + " "
                          + HarvestControllerServerMonitorHook.class.getName()
                          + " ./conf/" + res.getName() + " ";

        File dir = res.getParentFile();
        String fn = res.getName().replaceAll("harvester", "sidekick");
        writeStart(new File(dir, fn), sidekick, settingsfn,
                   logdir, logProperties, "");

    }

    /**
     * Create the start script for the access applications. Expected to be
     * called once for each viewerproxy instance.
     *
     * @param res The file to write the script to
     * @param setfn the name of the settings file to use
     */
    public void writeStartAccessApps(File res, String setfn) {
        File logdir = new File(installDir + "/conf/");
        String settingsfn = installDir + "/" + setfn;

        writeStart(res, ViewerProxyApplication.class.getName(),
                   settingsfn, logdir, logProperties, "");
    }

    /**
     * Create the start script for the index application.
     *
     * @param res   The file to write the script to.
     * @param setfn the name of the settings file to use.
     */
    public void writeStartIndexApp(File res, String setfn) {
        File logdir = new File(installDir + "/conf/");
        String settingsfn = installDir + "/" + setfn;

        writeStart(res, IndexServerApplication.class.getName(),
                   settingsfn, logdir, logProperties, "");
    }

    /**
     * Write a standard linux start script.
     *
     * @param res     The file to write the script to
     * @param appName the name of the class to start
     */
    private void writeStandardStart(File res, String appName) {
        String settingsfn = installDir + "/conf/settings.xml";
        File logdir = new File(installDir + "/conf/");
        writeStart(res, appName, settingsfn, logdir, logProperties, "");
    }

    /**
     * Write a properties file for logging .
     *
     * @param appName           the application to create the file for.
     * @param dir               the directory where the file should be created.
     * @param logpropDir        the install directory of the properties file.
     * @param loggingProperties the content of the file.
     * @return the install path to the logging properties file.
     */
    private String writeLogPropertiesFile(String appName, File dir,
                                          File logpropDir,
                                          String loggingProperties) {
        // Create and write the logging properties file
        String[] temp1 = appName.split(" ");
        String[] temp2 = temp1[0].split("\\.");
        String appId = temp2[temp2.length - 1].toLowerCase();
        String fnLogProp = "log_" + appId + ".prop";
        String appLogProperties = loggingProperties.replaceAll("APPID", appId);
        File logpropFile = new File(logpropDir, fnLogProp);
        FileUtils.writeBinaryFile(new File(dir, fnLogProp),
                                  appLogProperties.getBytes());
        return logpropFile.getPath();
    }

    /**
     * Write a linux start script.
     *
     * @param res               The file to write the script to
     * @param appName           the class name to start
     * @param settingsfn        the settings file to use
     * @param logpropDir        the directory to store the logging properties
*                          file in
     * @param loggingProperties the content of the logging-properties file
     * @param extraOptions      Any extra options to the java process.  These
     *                          options are inserted <em>before</em> the
     *                          name of the Java class, so they cannot be used
     *                          to pass command-line options to the application.
     */
    private void writeStart(File res, String appName, String settingsfn,
                            File logpropDir, String loggingProperties,
                            String extraOptions) {

        String logfn = writeLogPropertiesFile(appName, res.getParentFile(),
                                              logpropDir, loggingProperties);
        PrintWriter pw = null;
        PrintWriter pw2 = null;
        try {
            try {
                pw = new PrintWriter(new FileWriter(res));
                String JVMARGS = "-Xmx1536m";
                String SETTINGSFILE = "-D" + Settings
                        .SETTINGS_FILE_NAME_PROPERTY
                                      + "=" + settingsfn;
                String LOGFILE = "-Dorg.apache.commons.logging.Log="
                                 + "org.apache.commons.logging.impl.Jdk14Logger "
                                 + "-Djava.util.logging.config.file=" + logfn;
                //TODO: The JMX and httptransfer settings should really be
                // merged into settings.xml. These settings override JMX
                // settings from there
                File JMXPasswordFile = new File(installDir + "/conf/",
                        Constants.JMX_PASSWORD_FILENAME);
                String JMXArgs = getJMXPortParameter()
                        + " -Dsettings.common.jmx.passwordFile="
                        + JMXPasswordFile.getAbsolutePath();
                String httpFileTransferArgs = getHttpFileTransferArgs();
                String OPTIONS = extraOptions + " " + SETTINGSFILE + " "
                                 + LOGFILE + " " + JMXArgs
                                 + httpFileTransferArgs;
                pw.println("#!/bin/bash");
                pw.println("export CLASSPATH=" +
                        StringUtils.surjoin(getJarFiles(appName),
                                installDir + "/lib/", ":") +"$CLASSPATH;");
                pw.println("cd " + installDir);
                pw.println("java " + JVMARGS + " " + OPTIONS + " " + appName
                           + " < /dev/null > " + res.getName() + ".log 2>&1 &");
                pw.close();

                //appName must now be stripped to not include any arguments -
                // specially for sidekick-application.
                String killAppName = res.getName().replaceAll("start", "kill");
                String[] temp = appName.split(" ");
                appName = temp[0];

                pw2 = new PrintWriter(new FileWriter(
                        new File(res.getParentFile(), killAppName)));
                pw2.println("#!/bin/bash");
                pw2.println("PIDS=$(ps -wwfe | grep " + appName
                            + " | grep -v grep | grep " + settingsfn
                            + " | awk \"{print \\$2}\")");
                pw2.println("if [ -n \"$PIDS\" ] ; then");
                pw2.println("    kill -9 $PIDS");
                pw2.println("fi");

                if (HarvestControllerApplication.class.getName().equals(appName)) {
                    // Write script code to kill Heritrix
                    pw2.println("PIDS=$(ps -wwfe | grep " + Heritrix.class.getName()
                                + " | grep -v grep | grep " + settingsfn
                                + " | awk \"{print \\$2}\")");
                    pw2.println("if [ -n \"$PIDS\" ] ; then");
                    pw2.println("    kill -9 $PIDS");
                    pw2.println("fi");
                }

                pw2.close();
                appsToStart.add(res.getName());
            } finally {
                if (pw != null) {
                    pw.close();
                }
                if (pw2 != null) {
                    pw2.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Could not create:" + res + " for:" + appName, e);
        }

    }


    /**
     * Write a standard windows start bat file.
     *
     * @param dir     the directory to write the file to
     * @param appName the java class to start
     */
    private void writeStartBat(File dir, String appName) {
        String settingsfn = installDirWindows + "\\conf\\settings.xml";
        writeStartBat(dir, appName, settingsfn, new File(installDirWindows
                                                         + "\\conf\\"),
                      logProperties);
    }

    /**
     * Write a windows start bat file.
     *
     * @param dir               The directory to write the bat file to
     * @param appName           the java class to start
     * @param settingsfn        the settings file to use
     * @param logpropDir        the directory to place the log.prop file
     * @param loggingProperties the contents of the log.prop file
     */
    private void writeStartBat(File dir, String appName, String settingsfn,
                               File logpropDir, String loggingProperties) {
        String logfn = writeLogPropertiesFile(appName, dir, logpropDir,
                                              loggingProperties).replace("/",
                                                                         "");

        try {
            File vbs = new File(dir, "start_helper.vbs");
            File batHelper = new File(dir, "start_helper.bat");
            File res = new File(dir, "start_bitarchive.bat");


            PrintWriter pwRes = null;
            PrintWriter pwBat = null;
            PrintWriter pwVbs = null;
            try {
                pwRes = new PrintWriter(new FileWriter(res));
                pwBat = new PrintWriter(new FileWriter(batHelper));
                pwVbs = new PrintWriter(new FileWriter(vbs));

                String SETTINGSFILE = "-D"
                                      + Settings.SETTINGS_FILE_NAME_PROPERTY
                                      + "=\"" + settingsfn + "\"";
                String LOGFILE = "-Dorg.apache.commons.logging.Log="
                                 + "org.apache.commons.logging.impl.Jdk14Logger "
                                 + "-Djava.util.logging.config.file=" + "\""
                                 + logfn + "\"";
                //TODO: The JMX and httptransfer settings should really be
                // merged into settings.xml. These settings override JMX
                // settings from there
                String JMXPasswordFile = installDirWindows + "\\conf\\"
                        + Constants.JMX_PASSWORD_FILENAME;
                String JMXArgs = getJMXPortParameter()
                        + " -Dsettings.common.jmx.passwordFile=\""
                        + JMXPasswordFile + "\"";
                String httpTransferPortArgs = getHttpFileTransferArgs();
                String OPTIONS = SETTINGSFILE + " " + LOGFILE + " " + JMXArgs
                        + httpTransferPortArgs;
                pwBat.println("cd " + "\"" + installDirWindows + "\"");
                pwBat.println("set CLASSPATH=" +
                        StringUtils.surjoin(getJarFiles(appName),
                                installDirWindows + "\\lib\\", ";"));
                String JVMARGS = "-Xmx1150m -classpath \"" +
                        StringUtils.surjoin(getJarFiles(appName),
                                installDirWindows + "\\lib\\", ";") + "\"";
                String cmdline = "java " + JVMARGS + " " + OPTIONS + " "
                                 + appName;
                cmdline = cmdline.replaceAll("\\\\", "\\\\\\\\");
                cmdline = cmdline.replaceAll("\"", "\"\"");
                pwVbs.println("Set WshShell= CreateObject(\"WScript.Shell\")");
                pwVbs.println("Set oExec = WshShell.exec( " + "\""
                              + cmdline + "\")");
                pwVbs.println(
                        "Set fso= CreateObject(\"Scripting.FileSystemObject\")"
                );
                pwVbs.println(
                        "set f=fso.OpenTextFile("
                        + "\".\\conf\\kill_bitarchive.bat\",2,True)");

                pwVbs.println(
                        "f.WriteLine \"taskkill /F /PID \" & oExec.ProcessID ");
                pwVbs.println("f.close");

                pwRes.println("cd " + "\"" + installDirWindows + "\"");
                pwRes.println("cscript " + ".\\conf\\" + vbs.getName());
            } finally {
                if (pwBat != null) {
                    pwBat.close();
                }
                if (pwVbs != null) {
                    pwVbs.close();
                }
                if (pwRes != null) {
                    pwRes.close();
                }
            }
            appsToStart.add(res.getName());
        } catch (IOException e) {
            throw new IOFailure("Could not create batfile for:" + appName, e);
        }

    }


    /**
     * Get a string that invokes all start*.bat files created for this host.
     *
     * @return The above mentioned String
     */
    private String getStartAllBat() {
        String res = "cd \"" + installDirWindows + "\\conf\\\"\n";
        res = res
              + "\"C:\\Program Files\\Bitvise WinSSHD\\bvRun\" -brj -new -cmd=\"";
        for (String appName : appsToStart) {
            if (appName.endsWith(".bat")) {
                res = res + appName + "\" \n";
            }
        }
        return res;
    }

    /**
     * Get string that invokes all start*.ext scripts created on this host.
     *
     * @param ext The extention the start script must have (".bat" or ".sh")
     * @return the above mentioned string
     */
    public String getStartAll(String ext) {
        if (ext.equals(".bat")) {
            return getStartAllBat();
        }

        String res = "#!/bin/bash\n";
        res = res + "cd " + installDir + "/conf\n";
        for (String appName : appsToStart) {
            if (appName.endsWith(ext)) {
                res += "if [ -e ./" + appName + " ]; then\n";
                res += "    ./" + appName + " \n";
                res += "fi\n";
            }
        }
        return res;
    }

    /** For a given application, return the jar files it requires.  This also
     * handles strings with more than one application, giving the jar files
     * for the <b>first</b> application.
     *
     * @param appName The name of an application.
     * @return A list of the full name of the required dk.netarkivet jar files,
     *  e.g., [dk.netarkivet.archive.jar].  Note that dk.netarkivet.common.jar
     * is automatically included through the jar file's manifest.
     */
    public List<String> getJarFiles(String appName) {
        ArgumentNotValid.checkNotNullOrEmpty(appName, "appName");
        if (appName.startsWith(HarvestControllerApplication.class.getName())
                || appName.startsWith(HarvestDefinitionApplication.class.getName())
                || appName.startsWith(SideKick.class.getName())
                || appName.startsWith(HarvestControllerServerMonitorHook.class.getName())) {
            return Arrays.asList("dk.netarkivet.harvester.jar",
                                 "dk.netarkivet.archive.jar",
                                 "dk.netarkivet.viewerproxy.jar",
                                 "dk.netarkivet.monitor.jar");
        }
        if (appName.startsWith(BitarchiveApplication.class.getName())
                || appName.startsWith(ArcRepositoryApplication.class.getName())
                || appName.startsWith(BitarchiveMonitorApplication.class.getName())
                || appName.startsWith(IndexServerApplication.class.getName())) {
            return  Arrays.asList("dk.netarkivet.archive.jar",
                                  "dk.netarkivet.viewerproxy.jar",
                                  "dk.netarkivet.monitor.jar");
        }
        if (appName.startsWith(ViewerProxyApplication.class.getName())) {
            return  Arrays.asList("dk.netarkivet.viewerproxy.jar",
                                  "dk.netarkivet.archive.jar",
                                  "dk.netarkivet.monitor.jar");
        }
        throw new ArgumentNotValid("Don't know the jar file for '"
                + appName + "'");
    }

    /** Get a fresh JMX port that no other application on this host uses.
     * Each call to this method gobbles one item from the it-conf list of
     * JMX ports for this host. This will also set the RMI port to a number
     * 100 higher.
     *
     * @return A string that can be added to the command line for starting Java
     * in order to specify the JMX port.
     * @throws IllegalState If no more ports are specified in the it_conf.xml
     * file.
     */
    private String getJMXPortParameter() {
        List<String> available = getProperties().get(Constants.JMXPORT_PROPERTY);
        if (available == null || available.isEmpty()) {
            throw new IllegalState("No more JMX ports for " + this);
        }
        int myPort = Integer.valueOf(available.remove(0));
        return "-Dsettings.common.jmx.port=" + myPort
               + " -Dsettings.common.jmx.rmiPort="
               + (myPort + HostForwarding.JMX_RMI_INCREMENT);
    }

    /** Get a fresh http file transfer port that no other application on this
     * host uses.
     * Each call to this method gobbles one item from the it-conf list of
     * http file transfer ports for this host.
     *
     * @return A string that can be added to the command line for starting Java
     * in order to specify the http transfer port.
     * @throws IllegalState If no more ports are specified in the it_conf.xml
     * file.
     */
    private String getHttpFileTransferArgs() {
        String remoteFileClass = settingsXml
                .getString(Settings.REMOTE_FILE_CLASS);
        if (remoteFileClass.equals(HTTPRemoteFile.class.getName())
            || remoteFileClass.equals(HTTPSRemoteFile.class.getName())) {
            List<String> available = getProperties().get(
                    Constants.HTTPFILETRANSFERPORT_PROPERTY);
            if (available == null || available.isEmpty()) {
                throw new IllegalState("No more HTTP file transfer ports for "
                                       + this);
            }
            int myPort = Integer.valueOf(available.remove(0));
            return " -Dsettings.common.remoteFile.port=" + myPort;
        } else {
            return "";
        }
    }

    /** Get a fresh Heritrix GUI port that no other application on this
     * host uses.
     * Each call to this method gobbles one item from the it-conf list of
     * Heritrix GUI ports for this host.
     *
     * @return A string that can be added to the command line for starting Java
     * in order to specify the Heritrix GUI port.
     * @throws IllegalState If no more ports are specified in the it_conf.xml
     * file.
     */
    private String getHeritrixUiPortArgs() {
        List<String> available = getProperties().get(
                Constants.HERITRIX_GUI_PORT_PROPERTY);
        if (available == null || available.isEmpty()) {
            throw new IllegalState("No more Heritrix GUI ports for "
                                   + this);
        }
        int myPort = Integer.valueOf(available.remove(0));
        return " -Dsettings.harvester.harvesting.heritrix.guiPort="
               + myPort;
    }

    /** Get a fresh Heritrix JMX port that no other application on this
     * host uses.
     * Each call to this method gobbles one item from the it-conf list of
     * Heritrix JMX ports for this host.
     *
     * @return A string that can be added to the command line for starting Java
     * in order to specify the Heritrix JMX port.
     * @throws IllegalState If no more ports are specified in the it_conf.xml
     * file.
     */
    private String getHeritrixJmxPortArgs() {
        List<String> available = getProperties().get(
                Constants.HERITRIX_JMX_PORT_PROPERTY);
        if (available == null || available.isEmpty()) {
            throw new IllegalState("No more Heritrix JMX ports for "
                                   + this);
        }
        int myPort = Integer.valueOf(available.remove(0));
        return " -Dsettings.harvester.harvesting.heritrix.jmxPort="
               + myPort;
    }

    /**
     * A string representation of this host.
     * @return a string representation of this object.
     */
    public String toString() {
        String res = "Host:" + name + ";\n"
                     + "at:" + location + ";\n"
                     + "is:" + types + ";\n";
        for (Map.Entry<String, List<String>> p : properties.entrySet()) {
            for (String value : p.getValue()) {
                res = res + "property:" + p.getKey() + "=" + value + "\n";
            }
        }
        return res;

    }

    /** Update the conf/jmxremote.password file to have the password specified
     * in it_conf.xml.
     *
     * @param confDir The directory that the password file should be written to.
     */
    public void writeJMXPassword(File confDir) {
        ArgumentNotValid.checkNotNull(confDir, "File confDir");
        ArgumentNotValid.checkTrue(confDir.isDirectory(),
                "conf dir must exist");
        File passwordFile = new File(confDir, Constants.JMX_PASSWORD_FILENAME);
        FileUtils.writeBinaryFile(passwordFile, jmxPasswordFileContents.getBytes());
    }

    /** Get a single-valued property out of this host.
     *
     * @param key The name of the property.
     * @return The value of the named property for this host.
     * @throws UnknownID if no property is associated with this key
     * @throws IOFailure if there is not exactly one property for this key
     * on this host.
     */
    String getProperty(String key) {
        List<String> properties = getProperties().get(key);
        if (properties == null) {
        	throw new UnknownID("No property is associated with key '"
        			+ key + "'.");
        }

        if (properties.size() != 1) {
            throw new IOFailure("Error in it_conf file: Property '" + key
                    + "' should have one value, but has " + properties);
        } else {
            return properties.get(0);
        }
    }

    /** True if this host object is merely a pseudo-host indicating the
     * location of a service like FTP, JMS or SMTP.  These "hosts" should not
     * have settings, install scripts etc. generated, as that is handled
     * elsewhere.  Note that these services are never NetarchiveSuite
     * applications, but things that should be started in other ways that by
     * deploy-generated scripts.
     *
     * @return True if this host is a service-host only and thus should not
     * generate separate install/settings/start/kill files.
     */
    boolean isServiceHost() {
        EnumSet<Type> nonServiceTypes = types.clone();
        nonServiceTypes.removeAll(EnumSet.of(Type.ftp, Type.jms, Type.mail));
        return nonServiceTypes.isEmpty();
    }
}
