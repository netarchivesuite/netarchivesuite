/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.deploy;

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
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Machine defines an abstract representation of a physical computer
 * at a physical location.
 * The actual instances are depending on the operation system:
 * LinuxMachine and WindowsMachine.
 * All non-OS specific methods are implemented this machine class.
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
    protected String operatingSystem;
    /** The extension on the scipt files (specified by operating system).*/
    protected String scriptExtension;
    /** The name of the NetarchiveSuite.zip file.*/
    protected String netarchiveSuiteFileName;
    /** The inherited log.prop file.*/
    protected File inheritedLogPropFile;
    /** The inherited security.policy file.*/
    protected File inheritedSecurityPolicyFile;
    /** The inherited database file name.*/
    protected File databaseFile;
    /** The directory for this machine.*/
    protected File machineDirectory;
    /** Whether the temp dir should be cleaned.*/
    protected boolean resetTempDir;

    /**
     * A machine is referring to an actual computer at a physical location, 
     * which can have independent applications from the other machines at the 
     * same location.
     * 
     * @param subTreeRoot The root of this instance in the XML document.
     * @param parentSettings The setting inherited by the parent.
     * @param param The machine parameters inherited by the parent.
     * @param netarchiveSuiteSource The name of the NetarchiveSuite 
     * package file.
     * @param logProp The logging property file.
     * @param securityPolicy The security policy file.
     * @param dbFileName The name of the database file.
     * @param resetDir Whether the temporary directory should be reset.
     */
    public Machine(Element subTreeRoot, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
            File logProp, File securityPolicy, File dbFileName, 
            boolean resetDir) {
        ArgumentNotValid.checkNotNull(subTreeRoot, "Element e");
        ArgumentNotValid.checkNotNull(parentSettings,
                "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        ArgumentNotValid.checkNotNull(netarchiveSuiteSource,
                "String netarchiveSuiteSource");
        ArgumentNotValid.checkNotNull(logProp, "File logProp");
        ArgumentNotValid.checkNotNull(securityPolicy, "File securityPolicy");

        settings = new XmlStructure(parentSettings.getRoot());
        machineRoot = subTreeRoot;
        machineParameters = new Parameters(param);
        netarchiveSuiteFileName = netarchiveSuiteSource;
        inheritedLogPropFile = logProp;
        inheritedSecurityPolicyFile = securityPolicy;
        databaseFile = dbFileName;
        resetTempDir = resetDir;

        // retrieve the specific settings for this instance 
        Element tmpSet = machineRoot.element(
                Constants.COMPLETE_SETTINGS_BRANCH);
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
            throw new IllegalState("A Machine instance has no name!");
        }
    }

    /**
     * Extracts the XML for the applications from the root, 
     * creates the applications and puts them into the list.
     */
    @SuppressWarnings("unchecked")
    private void extractApplications() {
        applications = new ArrayList<Application>();
        List<Element> le = machineRoot.elements(
                Constants.DEPLOY_APPLICATION_NAME);
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
        ArgumentNotValid.checkNotNull(parentDirectory, "File parentDirectory");

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
        // create restart script
        createRestartScript(machineDirectory);
        // copy the security policy file
        createSecurityPolicyFile(machineDirectory);
        // create the log property files
        createLogPropertyFiles(machineDirectory);
        // create the jmx remote files
        createJmxRemotePasswordFile(machineDirectory);
        createJmxRemoteAccessFile(machineDirectory);
        // create the installCreateDir script
        createInstallDirScript(parentDirectory);

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
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.writeKillMachineHeader(machineUserLogin()));
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
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.writeInstallMachineHeader(
                machineUserLogin()));
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
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.writeStartMachineHeader(
                machineUserLogin()));
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
        File secPolFile = new File(directory, 
                Constants.SECURITY_POLICY_FILE_NAME);
        try {
            // init writer
            PrintWriter secPrinter = new PrintWriter(secPolFile);
            try {
                // read the inherited security policy file.
                String prop = FileUtils.readFile(inheritedSecurityPolicyFile);

                // change the jmx monitor role (if defined in settings)
                String monitorRole = settings.getLeafValue(
                        Constants.SETTINGS_MONITOR_JMX_NAME_LEAF);
                if(monitorRole != null) {
                    prop = prop.replace(
                            Constants.SECURITY_JMX_PRINCIPAL_NAME_TAG, 
                            monitorRole);
                }

                // Change the common temp dir (if defined in settings)
                String ctd = settings.getLeafValue(
                        Constants.SETTINGS_TEMPDIR_LEAF);
                if(ctd != null) {
                    prop = prop.replace(Constants.SECURITY_COMMON_TEMP_DIR_TAG, 
                            ctd);
                }

                // write to file.
                secPrinter.write(prop);

                // initialise list of directories to add
                List<String> dirs = new ArrayList<String>();

                // get all directories to add and put them into the list
                for(Application app : applications) {
                    // get archive.fileDir directory.
                    String[] tmpDirs = app.getSettingsValues(
                            Constants.SETTINGS_BITARCHIVE_BASEFILEDIR_LEAF);
                    if(tmpDirs != null && tmpDirs.length > 0) {
                        for(String st : tmpDirs) {
                            dirs.add(st);
                        }
                    }
                }

                // append file directories
                if(!dirs.isEmpty()) {
                    secPrinter.write("grant {" + "\n");
                    for(String dir : dirs) {
                        secPrinter.write(ScriptConstants
                                .writeSecurityPolicyDirPermission(
                                        changeFileDirPathForSecurity(dir)));
                    }
                    secPrinter.write("};");
                }
            } finally {
                secPrinter.close();
            }
        } catch (IOException e) {
            String errMsg = "Cannot create security policy file."; 
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Creates a the log property file for every application.
     * This is done by taking the inherited log file and changing 
     * "APPID" in the file into the identification of the application.
     * 
     * @param directory The local directory for this machine
     */
    protected void createLogPropertyFiles(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make log property file for every application
        for(Application app : applications) {
            // make file
            File logProp = new File(directory, 
                    Constants.LOG_PROP_APPLICATION_PREFIX 
                    + app.getIdentification() 
                    + Constants.LOG_PROP_APPLICATION_SUFFIX);
            try {
                // init writer
                PrintWriter logPrinter = new PrintWriter(logProp);
                
                try {
                    // read the inherited log property file.
                    String prop = FileUtils.readFile(inheritedLogPropFile);

                    // append stuff!
                    prop = prop.replace(
                            Constants.LOG_PROPERTY_APPLICATION_ID_TAG, 
                            app.getIdentification());

                    // write to file.
                    logPrinter.write(prop);
                } finally {
                        logPrinter.close();
                }
            } catch (IOException e) {
                String errMsg = "Cannot create log property file.";
                log.warn(errMsg, e);
                throw new IOFailure(errMsg, e);
            }
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
        File jmxFile = new File(directory, Constants.JMX_PASSWORD_FILE_NAME);
        try {
            // init writer
            PrintWriter jw = new PrintWriter(jmxFile);
            try {
                // Write the header of the jmxremote.password file.
                jw.print(ScriptConstants.JMXREMOTE_PASSWORD_HEADER);

                // Get the username and password for monitor and heritrix.
                StringBuilder logins = new StringBuilder();

                // Append the jmx logins for monitor and heritrix. 
                logins.append(getMonitorLogin());
                logins.append(getHeritrixLogin());
                
                jw.print(logins.toString());
            } finally {
                jw.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create jmxremote.password";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * Creates the jmxremote.password file, based on the settings.
     * 
     * @param directory The local directory for this machine 
     */
    protected void createJmxRemoteAccessFile(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make file
        File jmxFile = new File(directory, Constants.JMX_ACCESS_FILE_NAME);
        try {
            // init writer
            PrintWriter jw = new PrintWriter(jmxFile);
            try {
                // Write the header of the jmxremote.password file.
                jw.print(ScriptConstants.JMXREMOTE_ACCESS_HEADER);

                // Get the username and password for monitor and heritrix.
                StringBuilder logins = new StringBuilder();

                // Append the jmx logins for monitor and heritrix. 
                logins.append(getMonitorUsername());
                logins.append(getHeritrixUsername());
                
                jw.print(logins.toString());
            } finally {
                jw.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create jmxremote.access file. ";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }
    
    /**
     * For finding the jmxUsernames and jmxPasswords under the monitor branch
     * in the settings. 
     * Goes through all applications, which all must have the same 
     * username and the same passwords.  
     *  
     * @return The string to add to the jmxremote.password file.
     * @throws IllegalState If there is a different amount of usernames 
     * and passwords, or if two application has different values for their
     * username or passwords (applications without values are ignored). 
     */
    protected String getMonitorLogin() throws IllegalState {
        StringBuilder res = new StringBuilder();
        // initialise list of usernames and passwords to add
        List<String> usernames = new ArrayList<String>();
        List<String> passwords = new ArrayList<String>();
        String[] tmpVals;

        // get values from applications and put them into the lists
        for(Application app : applications) {
            // get monitor.jmxUsername
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_MONITOR_JMX_NAME_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    usernames.add(st);
                }
            }
            // get monitor.jmxPassword
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_MONITOR_JMX_PASSWORD_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    passwords.add(st);
                }
            }
        }

        // if different amount of usernames and passwords => DIE
        if(usernames.size() != passwords.size()) {
            String msg = "Different amount of usernames and passwords "
                    + "in monitor under applications on machine: '" 
                    + name + "'"; 
            log.warn(msg);
            throw new IllegalState(msg);
        }

        // warn if no usernames for monitor.
        if(usernames.size() == 0) {
            log.warn("No usernames or passwords for monitor on machine: '"
                    + name + "'");
        }
        
        // check if the usernames and passwords are the same.
        for(int i = 1; i < usernames.size(); i++) {
            if(!usernames.get(0).equals(usernames.get(i))
                    || !passwords.get(0)
                    .equals(passwords.get(i))) {
                String msg = "Different usernames or passwords "
                    + "under monitor on the same machine: '" + name + "'";
                log.warn(msg);
                throw new IllegalState(msg);
            }
        }
        
        // make the resulting string
        if(usernames.size() > 0) {
            res.append(usernames.get(0));
            res.append(Constants.SPACE);
            res.append(passwords.get(0));
            res.append(Constants.NEWLINE);
        }
        return res.toString();
    }
    
    /**
     * For retrieving the monitor username for the jmxremote.access file.
     * This will have the rights 'readonly'.
     * 
     * @return The string for the jmxremote.access file for allowing the 
     * monitor user to readonly.
     */
    protected String getMonitorUsername(){
        StringBuilder res = new StringBuilder();
        // initialise list of usernames and passwords to add
        List<String> usernames = new ArrayList<String>();
        String[] tmpVals;

        // get values from applications and put them into the lists
        for(Application app : applications) {
            // get monitor.jmxUsername
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_MONITOR_JMX_NAME_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    usernames.add(st);
                }
            }
        }
        
        // check if the usernames and passwords are the same.
        for(int i = 1; i < usernames.size(); i++) {
            if(!usernames.get(0).equals(usernames.get(i))) {
                String msg = "Different usernames "
                    + "for the monitor on the same machine: '" + name + "'";
                log.warn(msg);
                throw new IllegalState(msg);
            }
        }
        
        // make the resulting string
        if(usernames.size() > 0) {
            res.append(usernames.get(0));
            res.append(Constants.SPACE);
            res.append(ScriptConstants.JMXREMOTE_MONITOR_PRIVILEGES);
            res.append(Constants.NEWLINE);
        }
        return res.toString();
    }

    /**
     * For finding the jmxUsernames and jmxPasswords under the 
     * harvest.harvesting.heritrix branch under in the settings. 
     * Goes through all applications, which all must have the same 
     * username and the same passwords.  
     *  
     * @return The string to add to the jmxremote.password file.
     * @throws IllegalState If there is a different amount of usernames 
     * and passwords, or if two application has different values for their
     * username or passwords (applications without values are ignored). 
     */
    protected String getHeritrixLogin() throws IllegalState { 
        StringBuilder res = new StringBuilder();
        // initialise list of usernames and passwords to add
        List<String> usernames = new ArrayList<String>();
        List<String> passwords = new ArrayList<String>();
        String[] tmpVals;

        // get values from applications and put them into the lists
        for(Application app : applications) {
            // get heritrix.jmxUsername
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_HERITRIX_JMX_USERNAME_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    usernames.add(st);
                }
            }
            // get heritrix.jmxPassword
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_HERITRIX_JMX_PASSWORD_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    passwords.add(st);
                }
            }
        }

        // if different amount of usernames and passwords. DIE
        if(usernames.size() != passwords.size()) {
            String msg = "Different amount of usernames and passwords "
                    + "in heritrix under applications on machine: '"
                    + name + "'";
            log.warn(msg);
            throw new IllegalState(msg);
        }

        // if no usernames, and thus no passwords, finish!
        if(usernames.size() == 0) {
            return "";
        }
        
        // check if the usernames and passwords are the same.
        for(int i = 1; i < usernames.size(); i++) {
            if(!usernames.get(0).equals(usernames.get(i))
                    || !passwords.get(0)
                    .equals(passwords.get(i))) {
                String msg = "Different usernames or passwords "
                    + "under heritrix on machine: '" + name + "'";
                log.warn(msg);
                throw new IllegalState(msg);
            }
        }

        // make the resulting string
        if(usernames.size() > 0) {
            res.append(usernames.get(0));
            res.append(Constants.SPACE);
            res.append(passwords.get(0));
            res.append(Constants.NEWLINE);
        }
        return res.toString();
    }

    /**
     * For retrieving the Heritrix username for the jmxremote.access file.
     * This will have the rights 'readwrite'.
     * 
     * @return The string for the jmxremote.access file for allowing the 
     * heritrix user to readonly.
     */
    protected String getHeritrixUsername(){
        StringBuilder res = new StringBuilder();
        // initialise list of usernames and passwords to add
        List<String> usernames = new ArrayList<String>();
        String[] tmpVals;

        // get values from applications and put them into the lists
        for(Application app : applications) {
            // get heritrix.jmxUsername
            tmpVals = app.getSettingsValues(
                    Constants.SETTINGS_HERITRIX_JMX_USERNAME_LEAF);
            if(tmpVals != null && tmpVals.length > 0) {
                for(String st : tmpVals) {
                    usernames.add(st);
                }
            }
        }

        // check if the usernames and passwords are the same.
        for(int i = 1; i < usernames.size(); i++) {
            if(!usernames.get(0).equals(usernames.get(i))) {
                String msg = "Different usernames "
                    + "for Heritrix on the same machine: '" + name + "'";
                log.warn(msg);
                throw new IllegalState(msg);
            }
        }
        
        // make the resulting string
        if(usernames.size() > 0) {
            res.append(usernames.get(0));
            res.append(Constants.SPACE);
            res.append(ScriptConstants.JMXREMOTE_HERITRIX_PRIVELEGES);
            res.append(Constants.NEWLINE);
        }
        return res.toString();
    }

    /**
     * The string for accessing this machine through SSH.
     * 
     * @return The access through SSH to the machine
     */
    protected String machineUserLogin() {
        return machineParameters.getMachineUserName().getStringValue()
                + Constants.AT + name;
    }

    /**
     * For retrieving the environment name variable.
     * 
     * @return The environment name.
     */
    protected String getEnvironmentName() {
        return settings.getSubChildValue(
                Constants.SETTINGS_ENVIRONMENT_NAME_LEAF);
    }

    /**
     * Creates the kill scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    protected abstract void createApplicationKillScripts(File directory);

    /**
     * Creates the start scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    protected abstract void createApplicationStartScripts(File directory);

    /**
     * This function creates the script to start all applications on this 
     * machine.
     * The scripts calls all the start script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    protected abstract void createOSLocalStartAllScript(File directory);

    /**
     * This function creates the script to kill all applications on this 
     * machine.
     * The scripts calls all the kill script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    protected abstract void createOSLocalKillAllScript(File directory);

    /** 
     * The operation system specific path to the installation directory.
     *  
     * @return Install path.
     */
    protected abstract String getInstallDirPath();

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    protected abstract String getConfDirPath();

    /**
     * Creates the operation system specific killing script for this machine.
     * 
     * @return Operation system specific part of the killscript.
     */
    protected abstract String osKillScript();

    /**
     * Creates the operation system specific installation script for 
     * this machine.
     * 
     * @return Operation system specific part of the installscript.
     */
    protected abstract String osInstallScript();
    
    /**
     * Creates the specified directories in the deploy-configuration file.
     * 
     * @return The script for creating the directories.
     */
    protected abstract String osInstallScriptCreateDir();

    /**
     * Creates the operation system specific starting script for this machine.
     * 
     * @return Operation system specific part of the startscript.
     */
    protected abstract String osStartScript();

    /**
     * Makes all the class paths into the operation system specific syntax,
     * and puts them into a string where they are separated by the operation
     * system specific separator (':' for linux, ';' for windows).
     * 
     * @param app The application which has the class paths.
     * @return The class paths in operation system specific syntax.
     */
    protected abstract String osGetClassPath(Application app);
    
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
    protected abstract String osInstallDatabase();
    
    /**
     * This functions makes the script for creating the new directories.
     * 
     * Linux creates directories directly through ssh.
     * Windows creates an install a script file for installing the directories, 
     * which has to be sent to the machine, then executed and finally deleted. 
     * 
     * @param dir The name of the directory to create.
     * @param clean Whether the directory should be cleaned\reset.
     * @return The lines of code for creating the directories.
     * @see #createInstallDirScript(File)
     */
    protected abstract String scriptCreateDir(String dir, boolean clean);
    
    /**
     * Creates the script for creating the application specified directories.
     * 
     * @return The script for creating the application specified directories.
     */
    protected abstract String getAppDirectories();
    
    /**
     * This method does the following:
     * 
     * Retrieves the path to the jmxremote.access and jmxremote.password files.
     * 
     * Moves these files, if they are different from standard.
     * 
     * Makes the jmxremote.access and jmxremote.password files readonly.
     *  
     * @return The commands for handling the jmxremote files.
     */
    protected abstract String getJMXremoteFilesCommand();

    /**
     * Function to create the script which installs the new directories.
     * This is only used for windows machines!
     * 
     * @param dir The directory to put the file
     */
    protected abstract void createInstallDirScript(File dir);
    
    /**
     * Creates a script for restating all the applications on a given machine.
     * 
     * @param dir The directory where the script will be placed.
     */
    protected abstract void createRestartScript(File dir);

    /**
     * Changes the file directory path to the format used in the security 
     * policy.
     * @param path The current path.
     * @return The formatted path.
     */
    protected abstract String changeFileDirPathForSecurity(String path);
}
