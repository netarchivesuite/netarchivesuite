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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Machine defines an abstract representation of a physical computer
 * at a physical location.
 * The actual instances are depending on the operation system:
 * LinuxMachine and WindowsMachine.
 * All non-OS specific methods are implemented in MachineBase.
 */
public abstract class Machine {
    /** the log, for logging stuff instead of displaying them directly.*/
    protected final Log log = LogFactory.getLog(getClass().getName());
    /** The root-branch for this machine in the XML tree.*/
    protected Element machineRoot;
    /** The settings, inherited from parent and overwritten.*/
    protected XmlStructure settings;
    /** The machine parameters.*/
    protected Parameters machineParameters;
    /** The list of the application on this machine.*/
    protected List<Application> applications;
    /** The name of this machine.*/
    protected String name;
    /** The operating system on this machine: 'windows' or 'linux'.*/
    protected String OS;
    /** The extension on the scipt files (specified by operating system).*/
    protected String scriptExtension;
    /** The name of the NetarchiveSuite.zip file.*/
    protected String netarchiveSuiteFileName;
    /** The inherited log.prop file.*/
    protected File inheritedLogPropFile;
    /** The inherited security.policy file.*/
    protected File inheritedSecurityPolicyFile;
    /** The inherited database file name.*/
    protected String databaseFileName;
    /** The directory for this machine.*/
    protected File machineDirectory;

    /**
     * A machine is referring to an actual computer at a physical location, 
     * which can have independent applications from the other machines at the 
     * same location.
     * 
     * @param e The root of this instance in the XML document.
     * @param parentSettings The setting inherited by the parent.
     * @param param The machine parameters inherited by the parent.
     * @param netarchiveSuiteSource The name of the NetarchiveSuite 
     * package file.
     * @param logProp The logging property file.
     * @param securityPolicy The security policy file.
     * @param dbFileName The name of the database file.
     */
    public Machine(Element e, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
            File logProp, File securityPolicy, String dbFileName) {
        ArgumentNotValid.checkNotNull(e, "Element e");
        ArgumentNotValid.checkNotNull(parentSettings,
                "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        ArgumentNotValid.checkNotNull(netarchiveSuiteSource,
                "String netarchiveSuiteSource");
        ArgumentNotValid.checkNotNull(logProp, "File logProp");
        ArgumentNotValid.checkNotNull(securityPolicy, "File securityPolicy");

        settings = new XmlStructure(parentSettings.getRoot());
        machineRoot = e;
        machineParameters = new Parameters(param);
        netarchiveSuiteFileName = netarchiveSuiteSource;
        inheritedLogPropFile = logProp;
        inheritedSecurityPolicyFile = securityPolicy;
        databaseFileName = dbFileName;

        // retrieve the specific settings for this instance 
        Element tmpSet = machineRoot.element(Constants.SETTINGS_BRANCH);
        // Generate the specific settings by combining the general settings 
        // and the specific, (only if this instance has specific settings)
        if(tmpSet != null) {
                settings.overWrite(tmpSet);
        }

        // check if new machine parameters
        machineParameters.newParameters(machineRoot);
        // Retrieve the variables for this instance.
        extractVariables();
        // generate the machines on this instance
        extractApplications();
    }

    /**
     * Extract the local variables from the root.
     * Currently, this is the name and the operating system.
     */
    private void extractVariables() {
        // retrieve name
        Attribute at = machineRoot.attribute(
                Constants.MACHINE_NAME_ATTRIBUTE);
        if(at != null) {
            name = at.getText();
        } else {
            log.debug("Physical location has no name!");
            name = "";
        }
    }

    /**
     * Extracts the XML for the applications from the root, 
     * creates the applications and puts them into the list.
     */
    @SuppressWarnings("unchecked")
    private void extractApplications() {
        applications = new ArrayList<Application>();
        List<Element> le = machineRoot.elements(Constants.APPLICATION_BRANCH);
        for(Element e : le) {
            applications.add(new Application(e, settings, machineParameters));
        }
    }

    /**
     * Create the directory for the specific configurations of this machine
     * and call the functions for creating all the scripts in this directory.
     * 
     * @param parentDirectory The directory where to write the files.
     */
    public void write(File parentDirectory) {
        // create the directory for this machine
        machineDirectory = new File(parentDirectory, name);
        FileUtils.createDir(machineDirectory);

        //
        // create the content in the directory
        //

        // Create kill scripts
        createApplicationKillScripts(machineDirectory);
        createOSLocalKillAllScript(machineDirectory);
        // create start scripts
        createApplicationStartScripts(machineDirectory);
        createOSLocalStartAllScript(machineDirectory);
        // copy the security policy file
        createSecurityPolicyFile(machineDirectory);
        // create the log property files
        createLogPropertyFiles(machineDirectory);
        // create the jmx remote file
        createJmxRemotePasswordFile(machineDirectory);

        // write the settings for all application at this machine
        for(Application app : applications) {
            app.createSettingsFile(machineDirectory);
        }
    }

    /**
     * Make the script for killing this machine.
     * This is put into the entire kill all script for the physical location.
     * 
     * @return The script to kill this machine.
     */
    public String writeToGlobalKillScript() {
        StringBuilder res = new StringBuilder("");
        res.append("echo KILLING MACHINE: ");
        res.append(machineUserLogin());
        res.append("\n");
        // write the operating system dependent part of the kill script
        res.append(osKillScript());
        return res.toString();
    }

    /**
     * Make the script for installing this machine.
     * This is put into the entire install script for the physical location.
     * 
     * @return The script to make the installation on this machine
     */
    public String writeToGlobalInstallScript() {
        StringBuilder res = new StringBuilder("");
        res.append("echo INSTALLING TO MACHINE: ");
        res.append(machineUserLogin());
        res.append("\n");
        // write the operating system dependent part of the install script
        res.append(osInstallScript());
        return res.toString();
    }

    /**
     * Make the script for starting this machine.
     * This is put into the entire startall script for the physical location.
     * 
     * @return The script to start this machine.
     */
    public String writeToGlobalStartScript() {
        StringBuilder res = new StringBuilder("");
        res.append("echo STARTING MACHINE: ");
        res.append(machineUserLogin());
        res.append("\n");
        // write the operating system dependent part of the start script
        res.append(osStartScript());
        return res.toString();
    }

    /**
     * Copy inherited securityPolicyFile to local directory.
     * 
     * @param directory The local directory for this machine
     */
    protected void createSecurityPolicyFile(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make file
        File secPolFile = new File(directory, "security.policy");
        // copy inherited securityPolicyFile to local directory
        FileUtils.copyFile(inheritedSecurityPolicyFile, secPolFile);
    }

    /**
     * Creates a copy of the log property file for every application.
     * 
     * @param directory The local directory for this machine
     */
    protected void createLogPropertyFiles(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make log property file for every application
        for(Application app : applications) {
            // make file
            File logProp = new File(directory, 
                    "log_" + app.getIdentification() + ".prop");
            // copy inherited securityPolicyFile to local directory
            FileUtils.copyFile(inheritedLogPropFile, logProp);
        }
    }

    /**
     * Creates the jmxremote.password file, based on the settings.
     * 
     * @param directory The local directory for this machine 
     */
    protected void createJmxRemotePasswordFile(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make file
        File jmxFile = new File(directory, Constants.JMX_FILE_NAME);
        try {
            // init writer
            PrintWriter jw = new PrintWriter(jmxFile);
            try {
                // write template comment header in jmx file!
                jw.println("################################################"
                                + "##############");
                jw.println("#        Password File for Remote JMX Monitoring");
                jw.println("################################################"
                                + "##############");
                jw.println("#");
                jw.println("# Password file for Remote JMX API access to "
                                + "monitoring.  This");
                jw.println("# file defines the different roles and their "
                                + "passwords.  The access");
                jw.println("# control file (jmxremote.access by default) "
                                + "defines the allowed");
                jw.println("# access for each role.  To be functional, a "
                                + "role must have an entry");
                jw.println("# in both the password and the access files.");
                jw.println("#");
                jw.println("# Default location of this file is $JRE/lib/"
                                + "management/jmxremote.password");
                jw.println("# You can specify an alternate location by "
                                + "specifying a property in");
                jw.println("# the management config file $JRE/lib/"
                                + "management/management.properties");
                jw.println("# or by specifying a system property (See that "
                                + "file for details).");
                jw.println();
                jw.println();
                jw.println("################################################"
                                + "##############");
                jw.println("#    File permissions of the jmxremote.password "
                                + "file");
                jw.println("################################################"
                                + "##############");
                jw.println("#      Since there are cleartext passwords "
                                + "stored in this file,");
                jw.println("#      this file must be readable by ONLY the "
                                + "owner,");
                jw.println("#      otherwise the program will exit with an "
                                + "error.");
                jw.println("#");
                jw.println("# The file format for password and access files "
                                + "is syntactically the same");
                jw.println("# as the Properties file format.  The syntax is "
                                + "described in the Javadoc");
                jw.println("# for java.util.Properties.load.");
                jw.println("# Typical password file has multiple  lines, "
                                + "where each line is blank,");
                jw.println("# a comment (like this one), or a password entry.");
                jw.println("#");
                jw.println("#");
                jw.println("# A password entry consists of a role name "
                                + "and an associated");
                jw.println("# password.  The role name is any string "
                                + "that does not itself contain");
                jw.println("# spaces or tabs.  The password is again any "
                                + "string that does not");
                jw.println("# contain spaces or tabs.  Note that passwords "
                                + "appear in the clear in");
                jw.println("# this file, so it is a good idea not to use "
                                + "valuable passwords.");
                jw.println("#");
                jw.println("# A given role should have at most one entry "
                                + "in this file.  If a role");
                jw.println("# has no entry");
                jw.println("# If multiple entries are found for the same "
                                + "role name, then the last one");
                jw.println("# is used.");
                jw.println("#");
                jw.println("# In a typical installation, this file can be "
                                + "read by anybody on the");
                jw.println("# local machine, and possibly by people on other"
                                + " machines.");
                jw.println("# For # security, you should either restrict the"
                                + " access to this file,");
                jw.println("# or specify another, less accessible file in "
                                + "the management config file");
                jw.println("# as described above.");
                jw.println("#");

                // get the monitor name and password
                StringBuilder monitor = new StringBuilder("");
                monitor.append(settings.getSubChildValue(
                        Constants.JMX_PASSWORD_MONITOR_BRANCH,
                        Constants.JMX_PASSWORD_NAME_BRANCH));
                monitor.append(" ");
                monitor.append(settings.getSubChildValue(
                        Constants.JMX_PASSWORD_MONITOR_BRANCH,
                        Constants.JMX_PASSWORD_PASSWORD_BRANCH));
                jw.print(monitor.toString());
            } finally {
                jw.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create the jmxremote.password file.");
            throw new IOFailure("Problems creating jmxremote.password: "
                    + e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating jmxremote.password " + e);
        }
    }

    /**
     * The string for accessing this machine through SSH.
     * 
     * @return The access through SSH to the machine
     */
    protected String machineUserLogin() {
        return machineParameters.getMachineUserName().getStringValue()
                + "@" + name;
    }

    /**
     * For retrieving the environment name variable.
     * 
     * @return The environment name.
     */
    protected String getEnvironmentName() {
        return settings.getSubChildValue(
                Constants.ENVIRONMENT_NAME_SETTING_PATH_LEAF);
    }

    /**
     * Creates the kill scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    abstract protected void createApplicationKillScripts(File directory);

    /**
     * Creates the start scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    abstract protected void createApplicationStartScripts(File directory);

    /**
     * This function creates the script to start all applications on this 
     * machine.
     * The scripts calls all the start script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    abstract protected void createOSLocalStartAllScript(File directory);

    /**
     * This function creates the script to kill all applications on this 
     * machine.
     * The scripts calls all the kill script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    abstract protected void createOSLocalKillAllScript(File directory);

    /** 
     * The operation system specific path to the installation directory.
     *  
     * @return Install path.
     */
    abstract protected String getInstallDirPath();

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    abstract protected String getConfDirPath();

    /**
     * Creates the operation system specific killing script for this machine.
     * 
     * @return Operation system specific part of the killscript.
     */
    abstract protected String osKillScript();

    /**
     * Creates the operation system specific installation script for 
     * this machine.
     * 
     * @return Operation system specific part of the installscript
     */
    abstract protected String osInstallScript();

    /**
     * Creates the operation system specific starting script for this machine.
     * 
     * @return Operation system specific part of the startscript.
     */
    abstract protected String osStartScript();

    /**
     * Makes all the class paths into the operation system specific syntax,
     * and puts them into a string where they are separated by the operation
     * system specific separator (':' for linux, ';' for windows).
     * 
     * @param app The application which has the class paths.
     * @return The class paths in operation system specific syntax.
     */
    abstract protected String osGetClassPath(Application app);
    
    /**
     * Checks if a specific directory for the database is given in the settings,
     * and thus if the database should be installed on this machine.
     * 
     * If no specific database is given (databaseFileName = null) then use the 
     * standard database extracted from NetarchiveSuite.zip.
     * Else send the given new database to the standard database location.
     * 
     * Extract the database in the standard database location to the specified
     * database directory.
     * 
     * @return The script for installing the database (if needed).
     */
    abstract protected String osInstallDatabase();
}
