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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SimpleXml;
import dk.netarkivet.common.utils.StringUtils;

/**
 * Loads the IT configuration from an xml description and creates a list of host
 * definitions.
 *
 * This is split up in steps that should always be carried out in order: 1)
 * Create class to parse file 2) Set environment name 3) Calculate default
 * settings (from it-conf parsing) 4) Load default settings (from settings file
 * template) 5) Write settings 6) Write install scripts
 *
 * Please note that the parameters used for calling methods for all of this must
 * be consistent. Serious work ought to be done here!
 *
 */
public class ItConfiguration {
    /**
     * Logger for this class.
     */
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * During parsing; generate Host objects, and add them to this list.
     */
    private List<Host> hostlist = new ArrayList<Host>();

    /**
     * Name of our environment; the name we know this installation as; e.g. PROD
     * or TEST1. Set when first parsed.
     */
    private String environmentName;

    /**
     * Name of the toplevel install dir for Unix machines. Set when first
     * parsed.
     */
    private String installdirUnix;

    /**
     * Name of the toplevel install dir for windows machines. Set when first
     * parsed.
     */
    private String installdirWindows;

    /**
     * Name of the logging properties template file. Set by method
     * "calculateDefaultSettings".
     */
    private String defaultLogProperties;

    /** The default contents of the JMX passwords file.
     */
    private String initialJmxPasswordFileContents;

    /** The password to access JMX monitor role.
     */
    private String jmxMonitorRolePassword;

    /** The list of known locations. This gets built during XML parsing.
     */
    List<String> locations = new ArrayList<String>();

    /** The text in the default JMX password file that will be replaced in
     * loadDefaultSettings() with the actual password for the monitor role as
     * read from it_conf.xml.
     */
    private static final String JMX_MONITOR_ROLE_PASSWORD_PLACEHOLDER =
            "JMX_MONITOR_ROLE_PASSWORD_PLACEHOLDER";

    /** monitorSettingsContents. (jmx information inserted into
     * monitor_settings.xml on admin server).
     * This add-on is produced in the constructor, and inserted later on
     * in the writeSettings() method.
     * */
    private String monitorSettingsContent;

    /** How long to wait for large index generation jobs to time out */
    private long largeIndexTimeout;

    /**
     * Loads an xml file. During parsing, the instance variables above are set
     * to currect values, using a customized SAX parser.
     * The JMX-add-on string is produced here as well.
     *
     * @param f the file to load
     */
    public ItConfiguration(File f) {
        ArgumentNotValid.checkNotNull(f, "f");

        if (!f.exists()) {
            log.warn("XML file '" + f.getAbsolutePath()
                        + "' does not exist");
            throw new IOFailure("XML file '" + f.getAbsolutePath()
                                + "' does not exist");
        }

        // Setup and run specialised SAX parser
        try {
            log.debug("Start parsing '" + f.getAbsolutePath() + "'");
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            DefaultHandler handler = new ItConfSAXHandler();
            parser.parse(f, handler);

            // parse file again to retrieve JMX information
            StringBuilder result = new StringBuilder();
            DeploymentBuilder builder = new JmxHostsDeploymentBuilder(result);
            ItConfigParser itconfigHandler = new ItConfigParser(builder);
            parser.parse(f, itconfigHandler);
            builder.done(); // output from builder is now written to result
            monitorSettingsContent = result.toString();
        } catch (Exception e) {
            log.warn("Could not parse XML file '" + f.getAbsolutePath()
                        + "': " + e);
            throw new IOFailure("Could not parse XML file: '"
                                + f.getAbsolutePath() + "'", e);
        }
    }

    /**
     * Set "environment name", the name under which we recognise this
     * installation. This will also append envname to the end of the
     * installation path.
     *
     * @param envname The name of the environment (e.g. TEST1 or PROD)
     */
    public void setEnvironment(String envname) {
        environmentName = envname;
        for (Host host : hostlist) {
            host.setInstallDir(host.getInstallDir() + "/" + envname);
            if (host.getOS().equals(Host.OS.WINDOWS)) {
                host.setInstallDirWindows(host.getInstallDirWindows()
                                          + "\\" + envname);
            }
        }
    }

    /**
     * Determine the jms-broker and the default logging properties. For
     * test deployments the file: log.prop located in the
     * same directory as the settings xml file are used.
     *
     * Results are determined from the parsed host values.
     *
     * Effect: The following instance variables are set:
     *
     * defaultLogProperties initialJmxPasswordFileContents,
     *
     * @param dir the directory to search for the logging templates
     */
    public void calculateDefaultSettings(File dir) {
        String fnLogProp = "log.prop";
        String fnJmxPasswordProp = "jmxremote.password";

        File logSettingsFile = new File(dir, fnLogProp);
        File jmxPasswordsFile = new File(dir, fnJmxPasswordProp);
        try {
            defaultLogProperties = FileUtils.readFile(logSettingsFile);
        } catch (IOException e) {
            throw new IOFailure("Could not read logging properties", e);
        }
        try {
            initialJmxPasswordFileContents =
                FileUtils.readFile(jmxPasswordsFile);
        } catch (IOException e) {
            throw new IOFailure("Could not read JMX password file", e);
        }
    }

    /** Retrieves the host that hosts a specific service as near to a given
     * host as possible.  Best is on the same host, next best is the same
     * location, other locations are a last-ditch effort.  If there's more
     * than one service on one location, an arbitrary one is picked.
     *
     * @param service The type of service to find a host for.
     * @param targetHost The host that needs to make use of this service.  This
     * determines how far away a service host is.
     * @return The host that hosts the given service, or null if none could be
     * found.
     */
    private Host getClosestService(Host.Type service, Host targetHost) {
        Host server = null;
        String location = targetHost.getLocation();
        for (Host h : hostlist) {
            if (h.isType(service)) {
                // always use the service located on the host
                if (h.getName().equals(targetHost.getName())) {
                    return h;
                }
                if (server == null) {
                    server = h;
                } else {
                    if (h.getLocation().equals(location)) {
                        server = h;
                    }
                }
            }
        }
        return server;
    }

    /**
     * For each host apply default host settings based on location and global
     * settings (it_conf-settings) (jms-broker, ftp-servers, access-servers).
     *
     * This should always be called AFTER setEnvironment and
     * calculateDefaultSettings.
     *
     * Effect: After this call, all hosts have set their settings.xml values
     * and their defaultLogProperties value.
     *
     * Furthermore the settings.xml has been updated with correct jms
     * class, correct servers for ftp, mail, access and jms, the environment
     * name, the harvester priority, the GUI WAR-files, and the currect BA dirs
     * for the bit archives.
     *
     * @param f               the settings.xml file to use as default template
     * @param environmentName (e.g., TESTk, DEV, PROD)
     */
    public void loadDefaultSettings(File f, String environmentName) {
        for (Host host : hostlist) {
            SimpleXml xml = new SimpleXml(f);
            host.setSettingsXml(xml);

            host.setLogProperties(defaultLogProperties);
            host.setJmxPasswordFileContents(initialJmxPasswordFileContents
                    .replace(JMX_MONITOR_ROLE_PASSWORD_PLACEHOLDER,
                             jmxMonitorRolePassword));

            Host ftpServer = getClosestService(Host.Type.ftp, host);
            Host mailServer = getClosestService(Host.Type.mail, host);
            Host jmsServer = getClosestService(Host.Type.jms, host);

            host.overrideSettingsXmlDefaults(environmentName, ftpServer,
                                             mailServer, jmsServer);
        }
    }

    /**
     * For each host; write all the settings files, start scripts, stop scripts.
     *
     * This should always be called AFTER setEnvironment,
     * calculateDefaultSettings and loadDefaultSettings.
     *
     * This method overrides various settings based on the host type.
     *
     * The following applications get extra alternatively named settings files
     * with specialised settings:
     *
     * settings_bamonitor_<location>: overrides port number to 8081 and up
     *
     * settings_harvester_<<port>>: overrides port number, harvesterdir,
     *   isRunningFile to use port number found in it-conf.
     *
     * settings_access_<<port>>: overrides port number to port number found in
     *   it-conf
     *
     * settings_indexserver: overrides port number to 9999 to avoid clashing
     *   with viewer proxies. is only written for the indexserver host.
     *
     * On each host, start/kill scripts are written to each application.
     * Also, start/kill scripts are written to start/kill all applications on
     * this host.
     *
     * @param dir The directory to create the settings file in
     */
    public void writeSettings(File dir) {
        for (Host host : hostlist) {
            if (!host.isServiceHost()) {
                File subDir = new File(dir, host.getName());
                FileUtils.createDir(subDir);
                File settingsfile = new File(subDir, "settings.xml");
                if (settingsfile.exists()) {
                    settingsfile.delete();
                }
                host.getSettingsXml().save(settingsfile);

                host.writeJMXPassword(subDir);

                String ext = ".sh";
                if (host.isType(Host.Type.bitarchive)) {
                    host.writeStartBitarchiveApps(subDir);
                }
                if (host.getOS() == Host.OS.WINDOWS) {
                    ext = ".bat";
                }

                if (host.isType(Host.Type.admin)) {
                    // write monitorSettingsContents to
                    // <admin-host-dir>/monitor_settings.xml'
                    File monitorSettingsFile
                            = new File(subDir, "monitor_settings.xml");
                    if (monitorSettingsFile.exists()) {
                        monitorSettingsFile.delete();
                    }
                    List<String> monitorContentsAsList = new ArrayList<String>();
                    monitorContentsAsList.add(monitorSettingsContent);
                    FileUtils.writeCollectionToFile(monitorSettingsFile, monitorContentsAsList);

                    // create settings files for the two monitors
                    // TODO: Make bamons ports not be hardcoded
                    int locationPort = 8081;
                    for (String l : locations) {
                        host.overrideSetting(Settings.ENVIRONMENT_THIS_LOCATION,
                                             l.toUpperCase());
                        host.overrideSetting(Settings.HTTP_PORT_NUMBER,
                                             Integer.toString(locationPort));
                        host.getSettingsXml().save(
                                new File(subDir, "settings_bamonitor_" + l + ".xml"));
                        locationPort++;
                    }

                    host.writeStartAdminApps(subDir, locations);
                }

                if (host.isType(Host.Type.harvesters)) {
                    List<String> ports = host.getProperties().get("highpriport");
                    if (ports != null) {
                        for (String port : ports) {
                            // create settings files for each harvester to start
                            host.overrideSetting(Settings.HTTP_PORT_NUMBER, port);
                            host.overrideSetting(
                                    Settings.HARVEST_CONTROLLER_SERVERDIR,
                                    host.getInstallDir() + "/harvester_" + port);
                            host.overrideSetting(
                                    Settings.HARVEST_CONTROLLER_ISRUNNING_FILE,
                                    "./hcsRunning" + port + ".tmp");
                            host.overrideSetting(Settings.HARVEST_CONTROLLER_PRIORITY,
                            "HIGHPRIORITY");
                            host.getSettingsXml().save(new File(
                                    subDir, "settings_harvester_" + port + ".xml"));
                            host.writeStartHarvesterApps(new File(
                                    subDir, "start_harvester_" + port + ".sh"),
                                    "conf/settings_harvester_" + port + ".xml");
                        }
                    }
                    ports = host.getProperties().get("lowpriport");
                    if (ports != null) {
                        for (String port : ports) {
                            // create settings files for each harvester to start
                            host.overrideSetting(Settings.HTTP_PORT_NUMBER, port);
                            host.overrideSetting(
                                    Settings.HARVEST_CONTROLLER_SERVERDIR,
                                    host.getInstallDir() + "/harvester_" + port);
                            host.overrideSetting(
                                    Settings.HARVEST_CONTROLLER_ISRUNNING_FILE,
                                    "./hcsRunning" + port + ".tmp");
                            host.overrideSetting(Settings.HARVEST_CONTROLLER_PRIORITY,
                            "LOWPRIORITY");
                            /*
                             * set indexRequestTimeout to a larger value for big
                             * jobs.
                             */
                            if (largeIndexTimeout != 0) {
                                host.overrideSetting(Settings.INDEXREQUEST_TIMEOUT,
                                        Long.toString(largeIndexTimeout));
                            }
                            host.getSettingsXml().save(new File(
                                    subDir, "settings_harvester_" + port + ".xml"));
                            host.writeStartHarvesterApps(new File(
                                    subDir, "start_harvester_" + port + ".sh"),
                                    "conf/settings_harvester_" + port + ".xml");
                        }
                    }
                }

                if (host.isType(Host.Type.access)) {
                    List<String> ports = host.getProperties().get("port");
                    final String viewerproxyBasedirPrefix =
                        host.getSettingsXml().getString(Settings.VIEWERPROXY_DIR);
                    for (String port : ports) {
                        // create settings files for each viewerproxy to start
                        host.overrideSetting(Settings.HTTP_PORT_NUMBER, port);
                        // append "_PORT" (e.g _8077) to original value of viewerproxy.baseDir
                        // This solves bug 828
                        host.overrideSetting(Settings.VIEWERPROXY_DIR,
                                viewerproxyBasedirPrefix + "_" + port);
                        String vpSettings = "settings_viewerproxy_" + port
                                            + ".xml";
                        host.getSettingsXml().save
                                (new File(subDir, vpSettings));
                        host.writeStartAccessApps(new File
                                (subDir,"start_viewerproxy_" + port + ".sh"),
                                "conf/" + vpSettings);
                    }
                }

                for (Application app : host.applications) {
                    if (app.getType().equals(Application.Type.indexserver)) {
                        // create settings for starting index server
                        // It doesn't use the HTTP port at all.
                        host.overrideSetting(Settings.HTTP_PORT_NUMBER, "9999");
                    }
                    String appSettings = "settings_" + app.getType() + ".xml";
                    host.getSettingsXml().save(
                            new File(subDir, appSettings));
                    host.writeStartIndexApp(new File(
                            subDir, "start_" + app.getType() + ".sh"),
                                            "conf/" + appSettings);
                }

                try {
                    PrintWriter pwStartAll = new PrintWriter(new FileWriter(
                            new File(subDir, "startall" + ext)));
                    pwStartAll.println(host.getStartAll(ext));
                    pwStartAll.close();

                    PrintWriter pwKillAll = new PrintWriter(new FileWriter(
                            new File(subDir, "killall" + ext)));
                    pwKillAll.println(host.getStartAll(ext).replaceAll("start",
                                                                       "kill"));
                    pwKillAll.close();

                } catch (IOException e) {
                    throw new IOFailure("Could not write to:" + subDir, e);
                }
            }

        }
    }

    /**
     * Create all the install and start scripts for starting and killing all
     * applications in the system by SSH'ing to each host and calling kill.
     *
     * @param f               The file to write the install script to
     * @param startfile       The file to write the start_all script to
     * @param killFile        The file to write the kill_all script to
     * @param installLocation The installation site (e.g., KB or SB)
     */
    public void writeInstallAllSSH(File f, File startfile, File killFile,
                                   String installLocation) {
        try {
            PrintWriter pw = null;
            PrintWriter pwStart = null;
            PrintWriter pwKill = null;
            try {
                pw = new PrintWriter(new FileWriter(f));
                pwStart = new PrintWriter(new FileWriter(startfile));
                pwKill = new PrintWriter(new FileWriter(killFile));

                pwStart.println("#!/bin/bash");
                pwKill.println("#!/bin/bash");
                pw.println("#!/bin/bash");

                pw.println("if [ $# -ne 3 ]; then");
                pw.print("    echo usage ");
                pw.print(f.getName());
                pw.println(" [zip-file] [netarchive-user] [bitarchive-user]");
                pw.println("    exit");
                pw.println("fi");

                pwStart.println("if [ $# -ne 2 ]; then");
                pwStart.print("    echo usage ");
                pwStart.print(startfile.getName());
                pwStart.println(" [netarchive-user] [bitarchive-user]");
                pwStart.println("    exit");
                pwStart.println("fi");

                pwKill.println("if [ $# -ne 2 ]; then");
                pwKill.print("    echo usage ");
                pwKill.print(killFile.getName());
                pwKill.println(" [netarchive-user] [bitarchive-user]");
                pwKill.println("    exit");
                pwKill.println("fi");

                for (Host host : hostlist) {
                    if (!host.getLocation().equals(installLocation)) {
                        continue;
                    }

                    if (!host.isServiceHost()) {
                        String user;
                        String startScriptUser;
                        String dir = host.getInstallDir();
                        dir = dir.substring(0,
                                            dir.indexOf(environmentName)
                                            - 1);
                        String unzip = "unzip -q -o " + dir + "/$1 -d " + dir
                                       + "/" + environmentName;
                        String confDir = host.getInstallDir() + "/conf/";
                        boolean isWindows = host.getOS().equals(Host.OS.WINDOWS);
                        String ext = ".sh";

                        if (isWindows) {
                            user = "$3";
                            startScriptUser = "$2";
                            dir = ""; // "\"" + host.getInstallDirWindows() +
                            // "\"";
                            unzip = "cmd /c unzip.exe -d " + environmentName
                                    + " -o $1";
                            confDir = environmentName + "\\\\conf\\\\";
                            ext = ".bat";
                        } else if (host.isType(Host.Type.bitarchive)) {
                            user = "$3"; // sb-ba-user
                            startScriptUser = "$2";
                        } else {
                            user = "$2"; // sb-user
                            startScriptUser = "$1";
                        }

                        String destination = user + "@" + host.getName();
                        String startScriptDestination = startScriptUser + "@"
                                                       + host.getName();
                        pw.println("echo INSTALLING TO:" + host.getName());

                        pw.println("echo copying $1 to:" + host.getName());
                        pw.println("scp $1 " + destination + ":" + dir);

                        pw.println("echo unzipping $1 at:" + host.getName());
                        pw.println("ssh " + destination + " " + unzip);

                        pw.println("echo copying settings and scripts");
                        pw.println("scp -r " + host.getName() + "/* "
                                   + destination + ":" + confDir);
                        pw.println("echo make scripts executable");
                        if (!isWindows) {
                            pw.println("ssh  " + destination + " \"chmod +x "
                                       + confDir + "*.sh \"");
                        }

                        pw.println("echo make password files readonly");
                        if (isWindows) {
                            pw.println("echo Y | ssh " + destination
                                    + " cmd /c cacls " + confDir
                                    + "jmxremote.password /P BITARKIV\\\\"
                                    + user + ":R");
                        } else {
                            pw.println("ssh " + destination + " \"chmod 400 "
                                    + confDir + "/jmxremote.password\"");
                        }
                        pw.println(
                                "echo --------------------------------------------");

                        // Update StartAll script
                        pwStart.println(
                                "echo --------------------------------------------");
                        pwKill.println(
                                "echo --------------------------------------------");

                        pwStart.println("echo starting at:"
                                        + startScriptDestination);
                        pwKill.println("echo kill at " + startScriptDestination);
                        if (ext.equals(".sh")) {
                            pwStart.println("ssh " + startScriptDestination
                                            + " \". /etc/profile; "
                                            + host.getInstallDir()
                                            + "/conf/startall"
                                            + ext + "; sleep 5; cat "
                                            + host.getInstallDir()
                                            + "/*.log\"");
                            pwKill.println("ssh " + startScriptDestination
                                           + " \". /etc/profile; "
                                           + host.getInstallDir()
                                           + "/conf/killall"
                                           + ext + "\"");
                        } else {
                            pwStart.println("ssh " + startScriptDestination
                                            + " \"cmd /c  " + environmentName
                                            + "\\conf\\startall" + ext
                                            + " \" ");
                            pwKill.println("ssh " + startScriptDestination
                                           + " \"cmd /c  " + environmentName
                                           + "\\conf\\killall" + ext + " \" ");
                        }

                        pwStart.println(
                                "echo --------------------------------------------");
                        pwKill.println(
                                "echo --------------------------------------------");
                    }

                }
            } finally {
                if (pw != null) {
                    pw.close();
                }
                if (pwStart != null) {
                    pwStart.close();
                }
                if (pwKill != null) {
                    pwKill.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Could not write install ssh script to:" + f,
                                e);
        }
    }

    /**
     * Human readable list of all hosts.
     *
     * @return A string containing concatenation of all host's toString
     *         methods.
     */
    public String toString() {
        //Note no separator: hosts end with newline already.
        return "It configuration:\n" + StringUtils.conjoin(hostlist, "");
    }

    /**
     * Respond to the SAX events and build the parse result.
     *
     * The result of this parser is the installDirUnix and installDirWindows
     * environment variable initialised, and the list of hosts initalised with
     * type and location, but as yet without anything else.
     *
     */
    private class ItConfSAXHandler extends DefaultHandler {
        /**
         * Buffer used to collect character values.
         */
        private StringBuffer sb = new StringBuffer();

        /** Which application we're inside, if any. */
        private Application currentApplication;

        /**
         * During parsing; always set to the current location or null
         * outside elements.
         */
        private String currentLocation;

        /**
         * During parsing; always set to the current hosttype
         * (admin,access,bitarchive,harvester,ftp,jms,...), or null outside
         * elements.
         */
        private Host.Type currentHostType;

        /**
         * During parsing always set to the current host, or null outside
         * elements.
         */
        private Host currentHost;

        /** A regular expression that states the letters allowable in a
         * location name.  They must be restricted in order to ensure they
         * can be part of a filename.
         */
        private static final String LEGAL_LOCATION_NAME_REGEXP
                = "^[a-zA-Z0-9_]+$";

        /**
         * Called on stating to read. Just logs.
         *
         * @see org.xml.sax.helpers.DefaultHandler#startDocument()
         */
        public void startDocument() throws SAXException {
            log.trace("Document start");
        }

        /**
         * Called on start of elements. Remembers which location and host we are
         * inside.
         *
         * On location element: remember current location and store as one of
         * the allowed locations.
         *
         * On application element: remember the application, and check that the
         * host given in the 'host' attribute exists.
         *
         * On any element while parsing, which is not location or one of the
         * install dir elements, and we do not have a current host type:
         * Assume it is a hosttype, and remember the type.
         *
         * On host element: initialise host (type is remembered from above)
         *
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String uri, String name, String qname,
                                 Attributes atts) throws SAXException {
            sb = new StringBuffer(); // clears linefeeds etc.

            // check state
            qname = qname.toLowerCase();

            if (qname.equals("location")) {
                currentLocation = atts.getValue("name");
                if (!currentLocation.matches(LEGAL_LOCATION_NAME_REGEXP)) {
                    throw new ArgumentNotValid("Illegal location name '"
                    + currentLocation
                    + "', must contain only letters, numbers or underscore");
                }
                locations.add(currentLocation);
                return;
            }

            if (currentLocation != null) {
                // past root.location

                if (qname.equals("installdir")) {
                    return;
                }
                if (qname.equals("installdirwindows")) {
                    return;
                }

                if (qname.equals("host")) {
                    String hostname = atts.getValue("name");
                    String os = atts.getValue("os");
                    if (hostname == null) {
                        throw new ArgumentNotValid("Illegal host without name");
                    }
                    currentHost = getHostByName(hostname);
                    if (currentHost == null) {
                        currentHost = new Host(hostname, currentLocation,
                                               currentHostType);
                        if (os != null) {
                            currentHost.setOS(os);
                        }
                        hostlist.add(currentHost);
                    } else {
                        if (os != null && !currentHost.getOS().equals(os)) {
                            throw new IllegalState("Host '" + name + "' cannot "
                                                   + "have two different OSs: '"
                                                   + currentHost.getOS()
                                                   + "' and '" + os + "'");
                        }
                        currentHost.addType(currentHostType);
                    }
                    return;
                }

                if (qname.equals("application")) {
                    String applicationType = atts.getValue("type");
                    String hostName = atts.getValue("host");
                    Application app = new Application(applicationType);
                    Host owningHost = getHostByName(hostName);
                    if (owningHost == null) {
                        throw new UnknownID("Can't find host " + hostName);
                    }
                    owningHost.addApplication(app);
                    currentApplication = app;
                    return;
                }

                // host name
                if (currentHostType == null) {
                    currentHostType = Host.Type.valueOf(qname);
                    return;
                }
            }
        }

        private Host getHostByName(String hostName) {
            for (Host host : hostlist) {
                if (host.getName().equals(hostName)) {
                    return host;
                }
            }
            return null;
        }

        /*
         * Called at end of parsing element.
         *
         * Inside host elements:
         * Used to remember what the current host name/type is, and all other
         * properties in a list.
         *
         * At the end of reading a host element, add
         * host with read values to list of hosts.
         *
         * At the end of reading install dir elements: remember the directories.
         *
         * Clears remembered string buffer.
         *
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        public void endElement(String uri, String name, String qname)
                throws SAXException {

            // the string value of the element we have just ended
            String currentValue = sb.toString();

            sb = new StringBuffer(); // clears old content

            // process element value
            if (qname.equals("root")) {
                return;
            }

            if (qname.equals("installtype")) {
                return;
            }

            if (qname.equals("jmxMonitorRolePassword")) {
                jmxMonitorRolePassword = currentValue;
                return;
            }

            if (qname.equals("largeIndexTimeout")) {
                try {
                    largeIndexTimeout = Long.parseLong(currentValue);
                } catch (NumberFormatException e) {
                    log.warn("Large index timeout value not a number",
                            e);
                }
                return;
            }

            if (qname.equals("location")) {
                for (Host host : hostlist) {
                    if (host.getLocation().equals(currentLocation)) {
                        host.setInstallDir(installdirUnix);
                        if (host.getOS().equals(Host.OS.WINDOWS)) {
                            host.setInstallDirWindows(installdirWindows);
                        }
                    }
                }
                currentLocation = null;
                return;
            }

            if (qname.equals("installdir")) {
                installdirUnix = currentValue;
                return;
            }

            if (qname.equals("installdirwindows")) {
                installdirWindows = currentValue;
                return;
            }

            if (currentHostType != null
                && qname.equals(currentHostType.toString())) {
                currentHostType = null;
                return;
            }

            if (qname.equals("host")) {
                currentHost = null;
                return;
            }

            if (qname.equals("application")) {
                currentApplication = null;
                return;
            }

            if (currentHost != null) {
                currentHost.addProperty(qname, currentValue);
                return;
            }

            if (currentApplication != null) {
                currentApplication.addProperty(qname, currentValue);
                return;
            }
        }

        /**
         * Called when meeting character input.
         *
         * Remembers characters in a string buffer for use when needing to add
         * hosts.
         *
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            sb.append(ch, start, length);
        }
    }

}
