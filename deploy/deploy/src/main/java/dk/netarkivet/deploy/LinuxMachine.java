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
package dk.netarkivet.deploy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.archive.crawler.Heritrix;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A LinuxMachine is the instance of the abstract machine class, which runs 
 * the operating system Linux or another Unix dependent operation system.
 * This class only contains the operating system specific functions.
 */
public class LinuxMachine extends Machine {
    /**
     * The constructor. 
     * Starts by initialising the parent abstract class, then sets the 
     * operating system dependent variables.
     * 
     * @param subTreeRoot The XML root element.
     * @param parentSettings The Settings to be inherited from the 
     * PhysicalLocation, where this machine is placed.
     * @param param The machine parameters to be inherited from the 
     * PhysicalLocation.
     * @param netarchiveSuiteSource The name of the NetarchiveSuite package 
     * file. Must end with '.zip'.
     * @param logProp The logging property file, to be copied into 
     * machine directory.
     * @param securityPolicy The security policy file, to be copied into
     * machine directory.
     * @param dbFile The name of the database file.
     * @param resetDir Whether the temporary directory should be reset.
     */
    public LinuxMachine(Element subTreeRoot, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
            File logProp, File securityPolicy, File dbFile,
            boolean resetDir) {
        super(subTreeRoot, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy, dbFile, resetDir);
        // set operating system
        OS = Constants.OPERATING_SYSTEM_LINUX_ATTRIBUTE;
        scriptExtension = Constants.SCRIPT_EXTENSION_LINUX;
    }

    /**
     * Creates the operation system specific installation script for 
     * this machine.
     * 
     * @return Operation system specific part of the installscript
     */
    @Override
    protected String osInstallScript() {
        StringBuilder res = new StringBuilder();
        // echo copying null.zip to:kb-test-adm-001.kb.dk
        res.append(ScriptConstants.ECHO_COPYING + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE + ScriptConstants.TO + Constants.COLON);
        res.append(name);
        res.append(Constants.NEWLINE);
        // scp null.zip dev@kb-test-adm-001.kb.dk:/home/dev
        res.append(ScriptConstants.SCP + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.COLON);
        res.append(machineParameters.getInstallDirValue());
        res.append(Constants.NEWLINE);
        // echo unzipping null.zip at:kb-test-adm-001.kb.dk
        res.append(ScriptConstants.ECHO_UNZIPPING + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE + ScriptConstants.AT + Constants.COLON);
        res.append(name);
        res.append(Constants.NEWLINE);
        // ssh dev@kb-test-adm-001.kb.dk unzip -q -o /home/dev/null.zip -d 
        // /home/dev/TEST
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.LINUX_UNZIP_COMMAND
                + Constants.SPACE);
        res.append(machineParameters.getInstallDirValue());
        res.append(Constants.SLASH);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE + ScriptConstants.SCRIPT_DIR 
                + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.NEWLINE);
        // create other directories.
        res.append(osInstallScriptCreateDir());
        // echo preparing for copying of settings and scripts
        res.append(ScriptConstants.ECHO_PREPARING_FOR_COPY);
        res.append(Constants.NEWLINE);
        // For overriding jmxremote.password give user all rights.
        // ssh machine: "if [ -e conf/jmxremote.password ]; 
        // then chmod u+rwx conf/jmxremote.password; fi; "
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK + Constants.SPACE
                + ScriptConstants.LINUX_HOME_DIR + Constants.SEMICOLON 
                + Constants.SPACE + ScriptConstants.LINUX_IF_EXIST
                + Constants.SPACE);
        res.append(getConfDirPath());
        res.append(Constants.JMX_PASSWORD_FILE_NAME);
        res.append(Constants.SPACE + ScriptConstants.LINUX_THEN 
                + Constants.SPACE + ScriptConstants.LINUX_USER_ONLY 
                + Constants.SPACE);
        res.append(getConfDirPath());
        res.append(Constants.JMX_PASSWORD_FILE_NAME + Constants.SEMICOLON 
                + Constants.SPACE + ScriptConstants.FI + Constants.SEMICOLON
                + Constants.SPACE + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);
        // echo copying settings and scripts
        res.append(ScriptConstants.ECHO_COPY_SETTINGS_AND_SCRIPTS);
        res.append(Constants.NEWLINE);
        // scp -r kb-test-adm-001.kb.dk/* 
        // dev@kb-test-adm-001.kb.dk:/home/dev/TEST/conf/
        res.append(ScriptConstants.SCP + Constants.SPACE
                + ScriptConstants.SCRIPT_REPOSITORY + Constants.SPACE);
        res.append(name);
        res.append(Constants.SLASH + Constants.STAR + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.COLON);
        res.append(getConfDirPath());
        res.append(Constants.NEWLINE);
        // APPLY DATABASE!
        res.append(osInstallDatabase());
        // echo make scripts executable
        res.append(ScriptConstants.ECHO_MAKE_EXECUTABLE);
        res.append(Constants.NEWLINE);
        // Allow only user to be able to deal with these files 
        // (go=-rwx,u=+rwx) = 700.
        // ssh dev@kb-test-adm-001.kb.dk "chmod 700 /home/dev/TEST/conf/*.sh "
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK 
                + ScriptConstants.LINUX_USER_700 + Constants.SPACE); 
        res.append(getConfDirPath());
        res.append(Constants.STAR + Constants.SCRIPT_EXTENSION_LINUX 
                + Constants.SPACE + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);
        // HANDLE JMXREMOTE PASSWORD AND ACCESS FILE.
        res.append(getJMXremoteFilesCommand());
        // END OF SCRIPT 
        return res.toString();
    }

    /**
     * Creates the operation system specific killing script for this machine.
     * 
     * @return Operation system specific part of the killscript.
     */
    @Override
    protected String osKillScript() {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK + Constants.DOT
                + Constants.SPACE + ScriptConstants.ECT_PROFILE 
                + Constants.SEMICOLON + Constants.SPACE);
        res.append(getConfDirPath());
        res.append(Constants.SCRIPT_NAME_KILL_ALL);
        res.append(scriptExtension);
        res.append(Constants.QUOTE_MARK + Constants.SEMICOLON);
        res.append(Constants.NEWLINE);
        return res.toString();
    }

    /**
     * Creates the operation system specific starting script for this machine.
     * 
     * pseudo code:
     * - ssh maclogin ". /etc/profile; conf/startall.sh; sleep 5; 
     * cat install/*.log"
     * 
     * where:
     * maclogin = login for machine (username@machine).
     * conf = path to /conf directory.
     * install = path to install directory.
     * 
     * @return Operation system specific part of the startscript.
     */
    @Override
    protected String osStartScript() {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK + Constants.DOT
                + Constants.SPACE + ScriptConstants.ECT_PROFILE 
                + Constants.SEMICOLON + Constants.SPACE);
        res.append(getConfDirPath());
        res.append(Constants.SCRIPT_NAME_START_ALL);
        res.append(scriptExtension);
        res.append(Constants.SEMICOLON + Constants.SPACE 
                + ScriptConstants.SLEEP_5 + Constants.SEMICOLON 
                + Constants.SPACE + ScriptConstants.CAT + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.SLASH + ScriptConstants.STAR_LOG 
                + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);
        return res.toString();
    }

    /** 
     * The operation system specific path to the installation directory.
     *  
     * @return Install path.
     */
    @Override
    protected String getInstallDirPath() {
        return machineParameters.getInstallDirValue() + Constants.SLASH 
                + getEnvironmentName();
    }

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + Constants.CONF_DIR_LINUX;
    }

    /**
     * This function creates the script to kill all applications on this 
     * machine.
     * The scripts calls all the kill script for each application. 
     * 
     * pseudo code:
     * - echo Killing all applications at machine: mac
     * - if [ -e ./kill_app.sh ]
     * -    ./kill_app.sh
     * - fi
     * - ...
     * 
     * where:
     * mac = machine name.
     * app = application name.
     * ... = the same for other applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createOSLocalKillAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the kill all script file
        File killAllScript = new File(directory, 
                Constants.SCRIPT_NAME_KILL_ALL + scriptExtension);
        try {
            // Initialise script
            PrintWriter killPrinter = new PrintWriter(killAllScript);
            try {
                killPrinter.println(ScriptConstants.ECHO_KILL_ALL_APPS
                        + Constants.COLON + Constants.SPACE 
                        + Constants.APOSTROPHE + name + Constants.APOSTROPHE);
                killPrinter.println(ScriptConstants.BIN_BASH_COMMENT);
                killPrinter.println(ScriptConstants.CD + Constants.SPACE
                        + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // Constructing filename
                    String appScript = Constants.DOT + Constants.SLASH
                            + Constants.SCRIPT_LOCAL_KILL_ALL
                            + app.getIdentification() + scriptExtension;
                    // check if file exists
                    killPrinter.println(ScriptConstants.LINUX_IF_EXIST
                            + Constants.SPACE + appScript + Constants.SPACE
                            + ScriptConstants.LINUX_THEN + Constants.SPACE);
                    killPrinter.println(ScriptConstants.MULTI_SPACE
                            + appScript);
                    killPrinter.println(ScriptConstants.FI);
                }
            } finally {
                // close script
                killPrinter.close();
            }
        } catch (IOException e) {
            String msg = "Problems creating local kill all script: " + e;
            log.trace(msg);
            throw new IOFailure(msg);
        } catch(Exception e) {
            String msg = "Error in create local kill all script: " + e;
            log.trace(msg);
            System.out.println(msg);
        }
    }

    /**
     * This function creates the script to start all applications on this 
     * machine.
     * The scripts calls all the start script for each application. 
     * 
     * pseudo code:
     * - echo Starting all applications at machine: mac
     * - if [ -e ./start_app.sh ]
     * -    ./start_app.sh
     * - fi
     * - ...
     * 
     * where:
     * mac = machine name.
     * app = application name.
     * ... = the same for other applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createOSLocalStartAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the start all script file
        File startAllScript = new File(directory, 
                Constants.SCRIPT_NAME_START_ALL + scriptExtension);
        try {
            // Initialise script
            PrintWriter startPrinter = new PrintWriter(startAllScript);
        try {
                startPrinter.println(ScriptConstants.ECHO_START_ALL_APPS
                        + Constants.COLON + Constants.SPACE 
                        + Constants.APOSTROPHE + name + Constants.APOSTROPHE);
                startPrinter.println(ScriptConstants.BIN_BASH_COMMENT);
                startPrinter.println(ScriptConstants.CD + Constants.SPACE
                        + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = Constants.DOT + Constants.SLASH
                            + Constants.SCRIPT_LOCAL_START_ALL
                            + app.getIdentification() + scriptExtension;
                    // check if file exists
                    startPrinter.println(ScriptConstants.LINUX_IF_EXIST
                            + Constants.SPACE + appScript + Constants.SPACE
                            + ScriptConstants.LINUX_THEN + Constants.SPACE);
                    startPrinter.println(ScriptConstants.MULTI_SPACE
                            + appScript);
                    startPrinter.println(ScriptConstants.FI);
                }
            } finally {
                // close script
                startPrinter.close();
            }
        } catch (IOException e) {
            String msg = "Problems creating local start all script: " + e;
            log.trace(msg);
            throw new IOFailure(msg);
        } catch(Exception e) {
            String msg = "Error in create local start all script: " + e;
            log.trace(msg);
            System.out.println(msg);
        }
    }

    /**
     * Creates the kill scripts for all the applications.
     * 
     * The script starts by finding all running processes of the application.
     * If it finds any processes, it kills them. 
     * 
     * The kill_app.sh should have the following structure:
     * 
     * - echo Killing linux application.
     * - #!/bin/bash
     * - PIDS = $(ps -wwfe | grep fullapp | grep -v grep | grep 
     * path\settings_app.xml | awk "{print \\$2}")
     * - if [ -n "$PIDS" ]; then
     * -     kill -9 $PIDS;
     * - fi
     * 
     * Also, if a heritrix process is started, the following is added:
     * - PIDS = $(ps -wwfe | grep heritrix | grep -v grep | grep 
     * path\settings_app.xml | awk "{print \\$2}")
     * - if [ -n "$PIDS" ]; then
     * -     kill -9 $PIDS;
     * - fi
     * 
     * where:
     * path = the path to the ./conf directory.
     * fullapp = the full application name with class path.
     * app = the id of the application (name + instanceId).
     * heritrix = the heritrix class path.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createApplicationKillScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // go through all applications and create their kill script
        for(Application app : applications) {
            File appKillScript = new File(directory, 
                    Constants.SCRIPT_LOCAL_KILL_ALL + app.getIdentification() 
                    + scriptExtension);
            try {
                // make print writer for writing to file
                PrintWriter appPrint = new PrintWriter(appKillScript);
                try {
                    // echo Killing linux application.
                    appPrint.println(ScriptConstants.ECHO_KILL_LINUX_APPLICATION
                            + Constants.COLON + Constants.SPACE
                            + app.getIdentification());
                    // #!/bin/bash
                    appPrint.println(ScriptConstants.BIN_BASH_COMMENT);
                    // PIDS = $(ps -wwfe | grep fullapp | grep -v grep | grep 
                    // path\settings_app.xml | awk "{print \\$2}")
                    appPrint.println(ScriptConstants.getLinuxPIDS(
                            app.getTotalName(), getConfDirPath(), 
                            app.getIdentification()));
                    // if [ -n "$PIDS" ]; then
                    appPrint.println(ScriptConstants.LINUX_IF_N_EXIST 
                            + Constants.SPACE + Constants.QUOTE_MARK
                            + ScriptConstants.PIDS + Constants.QUOTE_MARK
                            + Constants.SPACE + ScriptConstants.LINUX_N_THEN);
                    //     kill -9 $PIDS;
                    appPrint.println(ScriptConstants.KILL_9_PIDS 
                            + Constants.SEMICOLON);
                    // fi
                    appPrint.println(ScriptConstants.FI);
                    
                    // If the application contains a heritrix instance,
                    // then make script for killing the heritrix process.
                    String[] heritrixJmxPort = app.getSettingsValues(
                            Constants.SETTINGS_HARVEST_HETRIX_JMX_PORT);
                    if(heritrixJmxPort != null) {
                        // log if more than one jmx port defined for heritrix.
                        if(heritrixJmxPort.length > 1) {
                            log.trace(heritrixJmxPort.length 
                                    + " number of jmx-ports for a heritrix "
                                    + "harvester.");
                        }

                        // - PIDS = $(ps -wwfe | grep heritrix | grep -v grep 
                        // | grep path\settings_app.xml | awk "{print \\$2}")
                        appPrint.println(ScriptConstants.getLinuxPIDS(
                                Heritrix.class.getName(), getConfDirPath(), 
                                app.getIdentification()));
                        // - if [ -n "$PIDS" ]; then
                        appPrint.println(ScriptConstants.LINUX_IF_N_EXIST
                                + Constants.SPACE + Constants.QUOTE_MARK 
                                + ScriptConstants.PIDS + Constants.QUOTE_MARK
                                + Constants.SPACE 
                                + ScriptConstants.LINUX_N_THEN);
                        // -     kill -9 $PIDS;
                        appPrint.println(ScriptConstants.KILL_9_PIDS);
                        // - fi
                        appPrint.println(ScriptConstants.FI);
                    }
                } finally {
                    // close file
                    appPrint.close();
                }
            } catch (IOException e) {
                String msg = "Problems creating application kill script: " + e;
                log.trace(msg);
                throw new IOFailure(msg);
            } catch(Exception e) {
                String msg = "Error in creating application kill script: " + e;
                log.trace(msg);
                System.out.println(msg);
            }
        }
    }

    /**
     * Creates the start scripts for all the applications.
     * 
     * The application should only be started, if it is not running already.
     * The script starts by finding all running processes of the application.
     * If any processes are found, a new application should not be started.
     * Otherwise start the application.
     * 
     * The start_app.sh should have the following structure:
     * 
     * - echo Starting linux application: app
     * - cd path
     * - #!/bin/bash
     * - PIDS = $(ps -wwfe | grep fullapp | grep -v grep | grep 
     * path\settings_app.xml | awk "{print \\$2}")
     * - if [ -n "$PIDS" ]; then
     * -     echo Application already running.
     * - else
     * -     export CLASSPATH = cp:$CLASSPATH;
     * -     JAVA
     * - fi
     * 
     * where:
     * path = the path to the install directory.
     * fullapp = the full name application with java path.
     * app = the name of the application.
     * cp = the classpaths for the application.
     * JAVA = the command to run the java application.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createApplicationStartScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // go through all applications and create their start script
        for(Application app : applications) {
            File appStartScript = new File(directory, 
                    Constants.SCRIPT_LOCAL_START_ALL + app.getIdentification() 
                    + scriptExtension);
            try {
                // make print writer for writing to file
                PrintWriter appPrint = new PrintWriter(appStartScript);
                try {
                    // #!/bin/bash
                    appPrint.println(ScriptConstants.ECHO_START_LINUX_APP
                            + Constants.COLON + Constants.SPACE
                            + app.getIdentification());
                    // cd path
                    appPrint.println(ScriptConstants.CD + Constants.SPACE
                            + app.installPathLinux());
                    // PIDS = $(ps -wwfe | grep fullapp | grep -v grep | grep 
                    //    path\settings_app.xml | awk "{print \\$2}")
                    appPrint.println(ScriptConstants.getLinuxPIDS(
                            app.getTotalName(), getConfDirPath(), 
                            app.getIdentification()));
                    // if [ -n "$PIDS" ]; then
                    appPrint.println(ScriptConstants.LINUX_IF_N_EXIST 
                            + Constants.SPACE + Constants.QUOTE_MARK
                            + ScriptConstants.PIDS + Constants.QUOTE_MARK
                            + Constants.SPACE + ScriptConstants.LINUX_N_THEN);
                    //     echo Application already running.
                    appPrint.println(ScriptConstants.ECHO_APP_ALREADY_RUNNING);
                    // else
                    appPrint.println(ScriptConstants.ELSE);
                    //     export CLASSPATH = cp;
                    appPrint.println(ScriptConstants.EXPORT_CLASSPATH
                            + osGetClassPath(app)
                            + ScriptConstants.VALUE_OF_CLASSPATH 
                            + Constants.SEMICOLON);
                    //     JAVA
                    appPrint.println(ScriptConstants.MULTI_SPACE_2
                            + ScriptConstants.JAVA + Constants.SPACE
                            + app.getMachineParameters().writeJavaOptions()
                            + Constants.SPACE + Constants.DASH 
                            + ScriptConstants.OPTION_SETTINGS 
                            + getConfDirPath() + Constants.PREFIX_SETTINGS
                            + app.getIdentification() 
                            + Constants.EXTENSION_XML_FILES + Constants.SPACE
                            + Constants.DASH 
                            + ScriptConstants.OPTION_LOG_COMPLETE
                            + Constants.SPACE + Constants.DASH 
                            + ScriptConstants.OPTION_LOG_CONFIG
                            + getConfDirPath() + Constants.LOG_PREFIX
                            + app.getIdentification() 
                            + Constants.EXTENSION_LOG_PROPERTY_FILES
                            + Constants.SPACE + Constants.DASH 
                            + ScriptConstants.OPTION_SECURITY_MANAGER
                            + Constants.SPACE + Constants.DASH 
                            + ScriptConstants.OPTION_SECIRITY_POLICY
                            + getConfDirPath() 
                            + Constants.SECURITY_POLICY_FILE_NAME 
                            + Constants.SPACE + app.getTotalName()
                            + Constants.SPACE + ScriptConstants.LINUX_DEV_NULL
                            + Constants.SPACE + Constants.SCRIPT_LOCAL_START_ALL
                            + app.getIdentification() 
                            + Constants.EXTENSION_LOG_FILES
                            + Constants.SPACE 
                            + ScriptConstants.LINUX_ERROR_MESSAGE_TO_1);
                    // fi
                    appPrint.println(ScriptConstants.FI);
                } finally {
                    // close file
                    appPrint.close();
                }
            } catch (IOException e) {
                String msg = "Problems creating application start script: " + e;
                log.trace(msg);
                throw new IOFailure(msg);
            } catch(Exception e) {
                String msg = "Error in creating application start script: " + e;
                log.trace(msg);
                System.out.println(msg);
            }
        }
    }

    /**
     * Makes all the class paths into the operation system specific syntax,
     * and puts them into a string where they are separated by the operation
     * system specific separator (':' for linux, ';' for windows).
     * 
     * @param app The application which has the class paths.
     * @return The class paths in operation system specific syntax.
     */
    @Override
    protected String osGetClassPath(Application app) {
        ArgumentNotValid.checkNotNull(app, "Application app");
        StringBuilder res = new StringBuilder();
        // get all the classpaths
        for(Element cp : app.getMachineParameters().getClassPaths()) {
            res.append(getInstallDirPath() + Constants.SLASH + cp.getText() 
                    + Constants.COLON);
        }
        return res.toString();
    }

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
    @Override
    protected String osInstallDatabase() {
        StringBuilder res = new StringBuilder();

        String databaseDir = machineParameters.getDatabaseDirValue();
        // Do not install if no proper database directory.
        if(databaseDir == null || databaseDir.isEmpty()) {
            return Constants.EMPTY;
        }

        // copy to final destination if database argument.
        if(databaseFile != null) {
            // echo Copying database
            res.append(ScriptConstants.ECHO_COPYING_DATABASE);
            res.append(Constants.NEWLINE);
            // scp database.jar user@machine:dbDir/db
            res.append(ScriptConstants.SCP + Constants.SPACE);
            res.append(databaseFile.getPath());
            res.append(Constants.SPACE);
            res.append(machineUserLogin());
            res.append(Constants.COLON);
            res.append(getInstallDirPath());
            res.append(Constants.SLASH);
            res.append(Constants.DATABASE_BASE_PATH);
            res.append(Constants.NEWLINE);
        }
        // unzip database.
        res.append(ScriptConstants.ECHO_UNZIPPING_DATABASE);
        res.append(Constants.NEWLINE);
        // ssh user@machine "
        // cd dir; if [ -d databaseDir ]; then echo ; 
        // else mkdir databaseDir; fi; if [ $(ls -A databaseDir) ]; 
        // then echo ERROR MESSAGE: DIR NOT EMPTY; 
        // else unzip -q -o dbDir/db -d databaseDir/.; fi; exit;
        // "
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK
                + ScriptConstants.CD + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.SEMICOLON + Constants.SPACE 
                + ScriptConstants.LINUX_IF_DIR_EXIST + Constants.SPACE);
        res.append(databaseDir);
        res.append(Constants.SPACE + ScriptConstants.LINUX_THEN 
                + Constants.SPACE + ScriptConstants.ECHO + Constants.SPACE);
        res.append(ScriptConstants.DATABASE_ERROR_PROMPT_DIR_NOT_EMPTY);
        res.append(Constants.SEMICOLON + Constants.SPACE + ScriptConstants.ELSE
                + Constants.SPACE + ScriptConstants.LINUX_UNZIP_COMMAND
                + Constants.SPACE);
        res.append(Constants.DATABASE_BASE_PATH);
        res.append(Constants.SPACE + ScriptConstants.SCRIPT_DIR 
                + Constants.SPACE);
        res.append(databaseDir);
        res.append(Constants.SEMICOLON + Constants.SPACE + ScriptConstants.FI
                + Constants.SEMICOLON + Constants.SPACE + ScriptConstants.EXIT
                + Constants.SEMICOLON + Constants.SPACE + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);

        return res.toString();
    }

    /**
     * Creates the specified directories in the deploy-configuration file.
     * 
     * Structure
     * - ssh login cd path; DIRS; CLEANDIR; exit;
     * 
     * where:
     * login = username@machine.
     * path = path to install directory.
     * DIRS = the way to create directories. 
     * CLEANDIR = the command to clean the tempDir (if chosen as optional)
     * 
     * The install creation of DIR has the following structure 
     * for directory dir:
     * if [ ! -d dir ]; then mkdir dir; fi;  
     * 
     * @return The script for creating the directories.
     */
    @Override
    protected String osInstallScriptCreateDir() {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.ECHO_CREATING_DIRECTORIES);
        res.append(Constants.NEWLINE);
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK 
                + ScriptConstants.CD + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.SEMICOLON + Constants.SPACE);
        
        // go through all directories.
        String dir;
        
        // get archive.bitpresevation.baseDir directory.
        dir = settings.getLeafValue(
                Constants.SETTINGS_ARCHIVE_BP_BASEDIR_LEAF);
        if(dir != null && !dir.isEmpty() 
                && !dir.equalsIgnoreCase(Constants.DOT)) {
            res.append(createPathToDir(dir));
            res.append(scriptCreateDir(dir, false));
        }
        
        // get archive.arcrepository.baseDir directory.
        dir = settings.getLeafValue(
                Constants.SETTINGS_ARCHIVE_ARC_BASEDIR_LEAF);
        if(dir != null && !dir.isEmpty()
                && !dir.equalsIgnoreCase(Constants.DOT)) {
            res.append(scriptCreateDir(dir, false));
        }
        
        // get tempDir directory.
        dir = settings.getLeafValue(
                Constants.SETTINGS_TEMPDIR_LEAF);
        if(dir != null && !dir.isEmpty()
                && !dir.equalsIgnoreCase(Constants.DOT)) {
            res.append(createPathToDir(dir));
            res.append(scriptCreateDir(dir, resetTempDir));
        }

        // get the application specific directories.
        res.append(getAppDirectories());
        
        res.append(ScriptConstants.EXIT + Constants.SEMICOLON + Constants.SPACE
                + Constants.QUOTE_MARK + Constants.NEWLINE);
        
        return res.toString();
    }
    
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
    @Override
    protected String scriptCreateDir(String dir, boolean clean) {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.LINUX_IF_NOT_DIR_EXIST + Constants.SPACE);
        res.append(dir);
        res.append(Constants.SPACE + ScriptConstants.LINUX_THEN
                + Constants.SPACE + ScriptConstants.MKDIR + Constants.SPACE);
        res.append(dir);
        if(clean) {
            res.append(Constants.SEMICOLON + Constants.SPACE
                    + ScriptConstants.ELSE_REMOVE + Constants.SPACE);
            res.append(dir);
            res.append(Constants.SEMICOLON + Constants.SPACE 
                    + ScriptConstants.MKDIR + Constants.SPACE);
            res.append(dir);
        }
        res.append(Constants.SEMICOLON + Constants.SPACE + ScriptConstants.FI
                + Constants.SEMICOLON + Constants.SPACE);

        return res.toString();
    }
    
    /**
     * Function for creating the directories along the path
     * until the end directory. Does not create the end directory.
     * 
     * @param dir The path to the directory.
     * @return The script for creating the directory.
     */
    protected String createPathToDir(String dir) {
        StringBuilder res = new StringBuilder();

        String[] pathDirs = dir.split(Constants.REGEX_SLASH_CHARACTER);
        String path = "";

        // only make directories along path to last directory, 
        // don't create end directory.
        for(int i = 0; i < pathDirs.length-1; i++) {
            // don't make directory of empty path.
            if(!pathDirs[i].isEmpty()) {
                path += pathDirs[i];
                res.append(scriptCreateDir(path, false));
            }
            path += Constants.SLASH;
        }

        return res.toString();
    }
    
    /**
     * Creates the script for creating the application specified directories.
     * 
     * @return The script for creating the application specified directories.
     */
    @Override
    protected String getAppDirectories() {
        StringBuilder res = new StringBuilder();
        String[] dirs;

        for(Application app : applications) {
            // get archive.fileDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_BITARCHIVE_BASEFILEDIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(createPathToDir(dir));
                    res.append(scriptCreateDir(dir, false));
                    for(String subdir : Constants.BASEFILEDIR_SUBDIRECTORIES) {
                        res.append(scriptCreateDir(
                                dir + Constants.SLASH + subdir, false));
                    }
                }
            }

            // get harvester.harvesting.serverDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_HARVEST_SERVERDIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(createPathToDir(dir));
                    res.append(scriptCreateDir(dir, false));
                }
            }
            
            // get the viewerproxy.baseDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_VIEWERPROXY_BASEDIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(createPathToDir(dir));
                    res.append(scriptCreateDir(dir, false));
                }
            }
            
            // get the common.tempDir directories. But only those, 
            // which are not the same as the machine common.tempDir.
            dirs = app.getSettingsValues(Constants.SETTINGS_TEMPDIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                String machineDir = settings.getLeafValue(
                        Constants.SETTINGS_TEMPDIR_LEAF);
                for(String dir : dirs) {
                    // Don't make machine temp dir twice.
                    if(dir != machineDir) {
                        res.append(createPathToDir(dir));
                        res.append(scriptCreateDir(dir, resetTempDir));
                    }
                }
            }
        }

        return res.toString();
    }

    /**
     * Dummy function on linux machine.
     * This is only used for windows machines!
     * 
     * @param dir The directory to put the file.
     */
    @Override
    protected void createInstallDirScript(File dir) {
        return;
    }

    /**
     * Changes the file directory path to the format used in the security 
     * policy.
     * @param path The current path.
     * @return The formatted path.
     */
    @Override
    protected String changeFileDirPathForSecurity(String path) {
        path += Constants.SLASH + Constants.SECURITY_FILE_DIR_TAG 
                + Constants.SLASH;
        return path.replace(Constants.SLASH, 
                ScriptConstants.SECURITY_DIR_SEPARATOR);
    }

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
    @Override
    protected String getJMXremoteFilesCommand() {
        String accessFilePath;
        String passwordFilePath;
        String[] options;

        // retrieve the access file path.
        options = settings.getLeafValues(Constants
                .SETTINGS_COMMON_JMX_ACCESSFILE);

        // extract the path, if any. Else set default.
        if(options == null || options.length < 0) {
            accessFilePath = Constants.JMX_ACCESS_FILE_PATH_DEFAULT;
        } else {
            accessFilePath = options[0];
            // warn if more than one access file is defined.
            if(options.length > 1) {
                log.debug(Constants.MSG_WARN_TOO_MANY_JMXREMOTE_FILE_PATHS);
            }
        }

        // retrieve the password file path.
        options = settings.getLeafValues(Constants
                .SETTINGS_COMMON_JMX_PASSWORDFILE);

        // extract the path, if any. Else set default.
        if(options == null || options.length < 0) {
            passwordFilePath = Constants.JMX_PASSWORD_FILE_PATH_DEFAULT;
        } else {
            passwordFilePath = options[0];
            // warn if more than one access file is defined.
            if(options.length > 1) {
                log.debug(Constants.MSG_WARN_TOO_MANY_JMXREMOTE_FILE_PATHS);
            }
        }

        // initialise the resulting command string.
        StringBuilder res = new StringBuilder();

        // echo make password files readonly
        res.append(ScriptConstants.ECHO_MAKE_PASSWORD_FILES);
        res.append(Constants.NEWLINE);

        // IF NOT DEFAULT PATHS, THEN MAKE SCRIPT TO MOVE THE FILES.
        if(!accessFilePath.equals(Constants.JMX_ACCESS_FILE_PATH_DEFAULT)) {
            // ssh dev@kb-test-adm-001.kb.dk "mv 
            // installpath/conf/jmxremote.access installpath/accessFilePath"
            res.append(ScriptConstants.SSH + Constants.SPACE);
            res.append(machineUserLogin());
            res.append(Constants.SPACE + Constants.QUOTE_MARK);
            res.append(ScriptConstants.MV);
            res.append(Constants.SPACE);
            res.append(getInstallDirPath());
            res.append(Constants.SLASH);
            res.append(Constants.JMX_ACCESS_FILE_PATH_DEFAULT);
            res.append(Constants.SPACE);
            res.append(getInstallDirPath());
            res.append(Constants.SLASH);
            res.append(accessFilePath);
            res.append(Constants.QUOTE_MARK);
            res.append(Constants.NEWLINE);
        }

        if(!passwordFilePath.equals(Constants.JMX_PASSWORD_FILE_PATH_DEFAULT)) {
            // ssh dev@kb-test-adm-001.kb.dk "mv 
            // installpath/conf/jmxremote.access installpath/accessFilePath"
            res.append(ScriptConstants.SSH + Constants.SPACE);
            res.append(machineUserLogin());
            res.append(Constants.SPACE + Constants.QUOTE_MARK);
            res.append(ScriptConstants.MV);
            res.append(Constants.SPACE);
            res.append(getInstallDirPath());
            res.append(Constants.SLASH);
            res.append(Constants.JMX_PASSWORD_FILE_PATH_DEFAULT);
            res.append(Constants.SPACE);
            res.append(getInstallDirPath());
            res.append(Constants.SLASH);
            res.append(passwordFilePath);
            res.append(Constants.QUOTE_MARK);
            res.append(Constants.NEWLINE);
        }

        // Allow only user to be able to only read jmxremote.password 
        // (a=-rwx,u=+r) = 400.
        // ssh dev@kb-test-adm-001.kb.dk "chmod 400 
        // /home/dev/TEST/conf/jmxremote.password"
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK
                + ScriptConstants.LINUX_USER_400 + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.SLASH);
        res.append(passwordFilePath);
        res.append(Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);
        // ssh dev@kb-test-adm-001.kb.dk "chmod 400 
        // /home/dev/TEST/conf/jmxremote.access"
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK
                + ScriptConstants.LINUX_USER_400 + Constants.SPACE);
        res.append(getInstallDirPath());
        res.append(Constants.SLASH);
        res.append(accessFilePath);
        res.append(Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);

        return res.toString();
    }
}
