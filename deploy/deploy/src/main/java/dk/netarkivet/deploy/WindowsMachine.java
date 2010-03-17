/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A WindowsMachine is the instance of the abstract machine class, which runs 
 * the operating system Windows.
 * This class only contains the operating system specific functions.
 */
public class WindowsMachine extends Machine {
    /**
     * The constructor. 
     * Starts by initializing the parent abstract class, then sets the 
     * operating system dependent variables.
     * 
     * @param root The XML root element.
     * @param parentSettings The Settings to be inherited from the 
     * PhysicalLocation, where this machine is placed.
     * @param param The machine parameters to be inherited from the 
     * PhysicalLocation.
     * @param netarchiveSuiteSource The name of the NetarchiveSuite package 
     * file. Must be '.zip'.
     * @param logProp The logging property file, to be copied into 
     * machine directory.
     * @param securityPolicy The security policy file, to be copied into
     * machine directory.
     * @param dbFile The harvest definition database file.
     * @param arcdbFile The archive database.
     * @param resetDir Whether the temporary directory should be reset.
     */
    public WindowsMachine(Element root, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
                File logProp, File securityPolicy, File dbFile,
                File arcdbFile, boolean resetDir) {
        super(root, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy, dbFile, arcdbFile, resetDir);
        // set operating system
        operatingSystem = Constants.OPERATING_SYSTEM_WINDOWS_ATTRIBUTE;
        scriptExtension = Constants.SCRIPT_EXTENSION_WINDOWS;
    }

    /**
     * Creates the operation system specific installation script for 
     * this machine.
     * 
     * Pseudo code:
     * - echo copying 'NetarchiveSuite.zip' to: 'machine'
     * - scp 'NetarchiveSuite.zip' 'login'@'machine':
     * - echo unzipping 'NetarchiveSuite.zip' at: 'machine'
     * - ssh 'login'@'machine' 'unzip' 'environmentName' -o 'NetarchiveSuite.zip
     * - $'create directories'.
     * - echo preparing for copying of settings and scripts
     * - if [ $( ssh 'login'@'machine' cmd /c if exist 
     * 'environmentName'\\conf\\jmxremote.password echo 1 ) ]; then echo Y | 
     * ssh 'login'@'machine' cmd /c cacls 
     * 'environmentName'\\conf\\jmxremote.password
     * /P BITARKIV\\'login':F; fi
     * - if [ $( ssh 'login'@'machine' cmd /c if exist 
     * 'environmentName'\\conf\\jmxremote.access echo 1 ) ]; then echo Y | ssh 
     * 'login'@'machine' cmd /c cacls 'environmentName'\\conf\\jmxremote.access
     * /P BITARKIV\\'login':F; fi
     * - echo copying settings and scripts
     * - scp -r 'machine'/* 'login'@'machine':'environmentName'\\conf\\
     * - $'apply database script'
     * - echo make password files readonly
     * * if 'jmxremote-password-path' != 'jmxremote-password-defaultpath'
     * - ssh 'login'@'machine' move /Y 'jmxremote-password-defaultpath' 
     * 'jmxremote-password-path'
     * * if 'jmxremote-access-path' != 'jmxremote-access-defaultpath'
     * - ssh 'login'@'machine' move /Y 'jmxremote-access-defaultpath' 
     * 'jmxremote-access-path'
     * - echo Y | ssh 'login'@'machine' cmd /c cacls 
     * 'environmentName'\\'jmxremote-password-path' /P BITARKIV\\'login':R
     * - echo Y | ssh 'login'@'machine' cmd /c cacls 
     * 'environmentName'\\'jmxremote-access-path' /P BITARKIV\\'login':R
     * 
     * variables:
     * 'NetarchiveSuite.zip' = The NetarchiveSuitePackage with '.zip' extension.
     * 'machine' = The machine name.
     * 'login' = The username for the machine.
     * 'unzip' = The command for unzipping.
     * 'environmentName' = the environmentName from the configuration file.
     * 
     * $'...' = call other function.
     * 
     * @return Operation system specific part of the installscript
     */
    @Override
    protected String osInstallScript() {
        StringBuilder res = new StringBuilder();
        // - echo copying 'NetarchiveSuite.zip' to: 'machine'
        res.append(ScriptConstants.ECHO_COPYING + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE + ScriptConstants.TO + Constants.COLON 
                + Constants.SPACE);
        res.append(name);
        res.append(Constants.NEWLINE);
        // - scp 'NetarchiveSuite.zip' 'login'@'machine':
        res.append(ScriptConstants.SCP + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.COLON);
        res.append(Constants.NEWLINE);
        // - echo unzipping 'NetarchiveSuite.zip' at: 'machine'
        res.append(ScriptConstants.ECHO_UNZIPPING + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.SPACE + ScriptConstants.AT + Constants.COLON 
                + Constants.SPACE);
        res.append(name);
        res.append(Constants.NEWLINE);
        // - ssh 'login'@'machine' 'unzip' 'environmentName' -o 
        // 'NetarchiveSuite.zip 
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_UNZIP_COMMAND
                + Constants.SPACE);
        res.append(getEnvironmentName());
        res.append(Constants.SPACE + ScriptConstants.SCRIPT_OUTPUT 
                + Constants.SPACE);
        res.append(netarchiveSuiteFileName);
        res.append(Constants.NEWLINE);
        // - $'create directories'.
        res.append(osInstallScriptCreateDir());
        // - echo preparing for copying of settings and scripts
        res.append(ScriptConstants.ECHO_PREPARING_FOR_COPY);
        res.append(Constants.NEWLINE);
        // - if [ $( ssh 'login'@'machine' cmd /c if exist 
        // 'environmentName'\\'jmxremote.password' echo 1 ) ]; then echo Y 
        // | ssh 'login'@'machine' cmd /c cacls 
        // 'environmentName'\\'jmxremote.password' /P BITARKIV\\'login':F; fi
        res.append(ScriptConstants.IF + Constants.SPACE 
                + Constants.SQUARE_BRACKET_BEGIN + Constants.SPACE
                + Constants.DOLLAR_SIGN + Constants.BRACKET_BEGIN 
                + Constants.SPACE + ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.IF + Constants.SPACE
                + ScriptConstants.EXIST + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(getLocalConfDirPath()));
        res.append(Constants.JMX_PASSWORD_FILE_NAME);
        res.append(Constants.SPACE + ScriptConstants.ECHO_ONE);
        res.append(Constants.SPACE + Constants.BRACKET_END 
                + Constants.SPACE + Constants.SQUARE_BRACKET_END
                + Constants.SEMICOLON + Constants.SPACE
                + ScriptConstants.THEN + Constants.SPACE);
        res.append(ScriptConstants.ECHO_Y + Constants.SPACE 
                + Constants.SEPARATOR + Constants.SPACE
                + ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.CACLS + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(getLocalConfDirPath()));
        res.append(Constants.JMX_PASSWORD_FILE_NAME + Constants.SPACE 
                + ScriptConstants.SLASH_P + Constants.SPACE 
                + ScriptConstants.BITARKIV_BACKSLASH_BACKSLASH);
        res.append(machineParameters.getMachineUserName().getText());
        res.append(ScriptConstants.COLON_F + Constants.SEMICOLON
                + Constants.SPACE + ScriptConstants.FI + Constants.SEMICOLON);
        res.append(Constants.NEWLINE);
        // - if [ $( ssh 'login'@'machine' cmd /c if exist 
        // 'environmentName'\\'jmxremote.access' echo 1 ) ]; then echo Y 
        // | ssh 'login'@'machine' cmd /c cacls 
        // 'environmentName'\\'jmxremote.access' /P BITARKIV\\'login':F; fi
        res.append(ScriptConstants.IF + Constants.SPACE 
                + Constants.SQUARE_BRACKET_BEGIN + Constants.SPACE
                + Constants.DOLLAR_SIGN + Constants.BRACKET_BEGIN
                + Constants.SPACE + ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.IF + Constants.SPACE
                + ScriptConstants.EXIST + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(getLocalConfDirPath()));
        res.append(Constants.JMX_ACCESS_FILE_NAME);
        res.append(Constants.SPACE + ScriptConstants.ECHO_ONE);
        res.append(Constants.SPACE + Constants.BRACKET_END 
                + Constants.SPACE + Constants.SQUARE_BRACKET_END
                + Constants.SEMICOLON + Constants.SPACE
                + ScriptConstants.THEN + Constants.SPACE 
                + ScriptConstants.ECHO_Y + Constants.SPACE);
        res.append(Constants.SEPARATOR + Constants.SPACE
                + ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.CACLS + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(getLocalConfDirPath()));
        res.append(Constants.JMX_ACCESS_FILE_NAME + Constants.SPACE 
                + ScriptConstants.SLASH_P + Constants.SPACE 
                + ScriptConstants.BITARKIV_BACKSLASH_BACKSLASH);
        res.append(machineParameters.getMachineUserName().getText());
        res.append(ScriptConstants.COLON_F + Constants.SEMICOLON
                + Constants.SPACE + ScriptConstants.FI + Constants.SEMICOLON);
        res.append(Constants.NEWLINE);
        // - echo copying settings and scripts
        res.append(ScriptConstants.ECHO_COPY_SETTINGS_AND_SCRIPTS);
        res.append(Constants.NEWLINE);
        // - scp -r 'machine'/* 'login'@'machine':'environmentName'\\conf\\
        res.append(ScriptConstants.SCP + Constants.SPACE 
                + ScriptConstants.DASH_R + Constants.SPACE);
        res.append(name);
        res.append(Constants.SLASH + Constants.STAR + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.COLON);
        res.append(ScriptConstants.doubleBackslashes(getLocalConfDirPath()));
        res.append(Constants.NEWLINE);
        // APPLY DATABASE!
        res.append(osInstallDatabase());
        // APPLY ARCHIVE DATABASE!
        res.append(osInstallArchiveDatabase());
        // HANDLE JMXREMOTE PASSWORD AND ACCESS FILE.
        res.append(getJMXremoteFilesCommand());
        // END OF SCRIPT
        return res.toString();
    }

    /**
     * Creates the operation system specific killing script for this machine.
     * 
     * pseudocode:
     * - ssh 'login'@'machine' cmd /c 'environmentName'\\conf\\killall.bat
     * 
     * variables:
     * 'login' = machine user name
     * 'machine' = name of the machine
     * 'environmentName' = the environmentName from configuration.
     * 
     * @return Operation system specific part of the killscript.
     */
    @Override
    protected String osKillScript() {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK 
                + ScriptConstants.WINDOWS_COMMAND_RUN + Constants.SPACE
                + Constants.SPACE);
        res.append(getLocalConfDirPath());
        res.append(Constants.SCRIPT_NAME_KILL_ALL);
        res.append(scriptExtension);
        res.append(Constants.SPACE + Constants.QUOTE_MARK + Constants.SPACE);
        res.append(Constants.NEWLINE);
        return res.toString();
    }

    /**
     * Creates the operation system specific starting script for this machine.
     * 
     * pseudocode:
     * - ssh 'login'@'machine' cmd /c 'environmentName'\\conf\\startall.bat
     * 
     * variables:
     * 'login' = machine user name
     * 'machine' = name of the machine
     * 'environmentName' = the environmentName from configuration.
     * 
     * @return Operation system specific part of the startscript.
     */
    @Override
    protected String osStartScript() {
        StringBuilder res = new StringBuilder();
        res.append(ScriptConstants.SSH + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK 
                + ScriptConstants.WINDOWS_COMMAND_RUN + Constants.SPACE
                + Constants.SPACE);
        res.append(getLocalConfDirPath());
        res.append(Constants.SCRIPT_NAME_START_ALL);
        res.append(scriptExtension);
        res.append(Constants.SPACE + Constants.QUOTE_MARK + Constants.SPACE);
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
        return machineParameters.getInstallDirValue() + Constants.BACKSLASH 
                + getEnvironmentName();
    }

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + Constants.CONF_DIR_WINDOWS;
    }
    
    /**
     * Creates the local path to the conf dir.
     * 
     * @return The path to the conf dir for ssh.
     */
    protected String getLocalConfDirPath() {
        return getEnvironmentName() + Constants.CONF_DIR_WINDOWS;
    }
    
    /**
     * Creates the local path to the installation directory.
     * 
     * @return The path to the installation directory for ssh.
     */
    protected String getLocalInstallDirPath() {
        return getEnvironmentName() + Constants.BACKSLASH;
    }

    /**
     * This function creates the script to kill all applications on this 
     * machine.
     * The scripts calls all the kill script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     * @throws IOFailure If an error occurred during the creation of the local
     * killall script.
     */
    @Override
    protected void createOSLocalKillAllScript(File directory) throws IOFailure {
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
                killPrinter.println(ScriptConstants.CD + Constants.SPACE 
                        + Constants.QUOTE_MARK + getConfDirPath() 
                        + Constants.QUOTE_MARK);
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = Constants.SCRIPT_NAME_LOCAL_KILL
                            + app.getIdentification() + scriptExtension;
                    killPrinter.print(ScriptConstants
                            .OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE);
                    killPrinter.print(Constants.QUOTE_MARK);
                    killPrinter.print(appScript);
                    killPrinter.print(Constants.QUOTE_MARK);
                    killPrinter.println();
                }
            } finally {
                // close script
                killPrinter.close();
            }
        } catch (IOException e) {
            String msg = "Problems creating local kill all script. ";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * This function creates the script to start all applications on this 
     * machine.
     * The scripts calls all the start script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     * @throws IOFailure If an error occurred during the creation of the local
     * startall script.
     */
    @Override
    protected void createOSLocalStartAllScript(File directory) 
            throws IOFailure {
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
                startPrinter.println(ScriptConstants.CD + Constants.SPACE
                        + Constants.QUOTE_MARK + getConfDirPath()
                        + Constants.QUOTE_MARK);
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = Constants.SCRIPT_NAME_LOCAL_START
                            + app.getIdentification() + scriptExtension;
                    startPrinter.print(ScriptConstants
                            .OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE);
                    startPrinter.print(Constants.QUOTE_MARK);
                    startPrinter.print(appScript);
                    startPrinter.print(Constants.QUOTE_MARK);
                    startPrinter.println();
                }
            } finally {
                // close script
                startPrinter.close();
            }
        } catch (IOException e) {
            String msg = "Problems during creation of the local start "
                    + "all script.";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Creates the kill scripts for all the applications.
     * Two script files are created: kill_app.bat and kill_ps_app.bat. 
     * 
     * kill_ps_app.bat kills the process of the application.
     * kill_app.bat runs kill_ps_app.bat if the application is running.
     * run_app tells if the application is running. It is deleted during kill.
     * 
     * The kill_app.bat should have the following structure:
     * 
     * - ECHO Killing application : app
     * - CD "path"
     * - IF EXIST run_app GOTO KILL
     * - GOTO NOKILL
     * - 
     * - :KILL
     * - cmdrun kill_ps_app.bat
     * - DEL run_app
     * - GOTO DONE
     * - 
     * - :NOKILL
     * - ECHO Cannot kill application. Already running.
     * -
     * - :DONE 
     * 
     * where:
     * app = application name.
     * path = the path to the ./conf directory.
     * cmdrun = the windows command to run other batch programs.
     * 
     * The kill_ps_app.bat is empty upon creation.
     * When the application is started, the command to kill the process of 
     * he application is written to this file as the only content.
     * It will look something like this:
     * 
     * - taskkill /F /PID id
     * 
     * where:
     * id = the process identification number of the running application. 
     * 
     * TODO kill the potential heritrix process, created by a harvester.
     * Just like on Linux/Unix. 
     * If we in the future add the possibility of running heritrix on Windows.
     * 
     * @param directory The directory for this machine (use global variable?).
     * @throws IOFailure If an error occurred during the creation of the 
     * application kill script file.
     */
    @Override
    protected void createApplicationKillScripts(File directory) 
            throws IOFailure {
        // go through all applications and create their kill script
        for(Application app : applications) {
            String id = app.getIdentification();
            String killPsName = Constants.SCRIPT_KILL_PS + id 
            + scriptExtension;
            File appKillScript = new File(directory, 
                    Constants.SCRIPT_NAME_LOCAL_KILL + id 
                    + scriptExtension);
            File appKillPsScript = new File(directory, killPsName);
            try {
                // make print writer for writing to file
                PrintWriter appPrint = new PrintWriter(appKillScript);
                try {
                    // initiate variables
                    String tmpRunApp = Constants
                    .FILE_TEMPORARY_RUN_WINDOWS_NAME + id;
                    // get the content for the kill script of 
                    // this application
                    // #echo kill windows application
                    appPrint.println(
                            ScriptConstants.ECHO_KILL_WINDOWS_APPLICATION
                            + Constants.COLON + Constants.SPACE + id);
                    // cd "path"
                    appPrint.println(ScriptConstants.CD + Constants.SPACE
                            + Constants.QUOTE_MARK
                            + app.installPathWindows() 
                            + Constants.CONF_DIR_WINDOWS
                            + Constants.QUOTE_MARK);
                    // if exist run_app.txt GOTO KILL
                    appPrint.println(ScriptConstants.IF + Constants.SPACE
                            + ScriptConstants.EXIST + Constants.SPACE
                            + tmpRunApp + Constants.SPACE 
                            + ScriptConstants.GOTO + Constants.SPACE
                            + ScriptConstants.LABEL_KILL);
                    // GOTO NOKILL
                    appPrint.println(ScriptConstants.GOTO + Constants.SPACE
                            + ScriptConstants.LABEL_NOKILL);
                    // 
                    appPrint.println();
                    // :KILL
                    appPrint.println(Constants.COLON 
                            + ScriptConstants.LABEL_KILL);
                    // cmdrun kill_ps_app.bat
                    appPrint.println(ScriptConstants.
                            OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE
                            + Constants.QUOTE_MARK + killPsName 
                            + Constants.QUOTE_MARK);
                    // del run_app.txt
                    appPrint.println(ScriptConstants.DEL + Constants.SPACE
                            + tmpRunApp);
                    // GOTO DONE
                    appPrint.println(ScriptConstants.GOTO + Constants.SPACE
                            + ScriptConstants.LABEL_DONE);
                    //
                    appPrint.println();
                    // :NOKILL
                    appPrint.println(Constants.COLON 
                            + ScriptConstants.LABEL_NOKILL);
                    // echo Cannot kill application. Already running.
                    appPrint.println(ScriptConstants.ECHO_CANNOT_KILL_APP);
                    //
                    appPrint.println();
                    // :DONE
                    appPrint.println(Constants.COLON 
                            + ScriptConstants.LABEL_DONE);
                } finally {
                    // close file
                    appPrint.close();
                }
                // Printer for making the kill process file.
                PrintWriter appPsPrint = new PrintWriter(appKillPsScript);
                try {
                    // write dummy line in kill script.
                    appPsPrint.println("ECHO Not started!");
                } finally {
                    // close file
                    appPsPrint.close();
                }
            } catch (IOException e) {
                String msg = "Cannot create the kill script for "
                    + "application: " + app.getIdentification() 
                    + ", at machine: '" + name + "'"; 
                log.trace(msg, e);
                throw new IOFailure(msg, e);
            } 
        }
    }

    /**
     * Creates the start scripts for all the applications.
     * 
     * It creates the batch and the VBscript for starting the application,
     * which are called start_app.bat and start_app.vbs respectively.
     * These files are created in each of their own function.
     * 
     * @param directory The directory for this machine (use global variable?).
     * @see #windowsStartBatScript(Application, File)
     * @see #windowsStartVbsScript(Application, File)
     */
    @Override
    protected void createApplicationStartScripts(File directory) {
        // go through all applications and create their start script
        for(Application app : applications) {
            windowsStartBatScript(app, directory);
            windowsStartVbsScript(app, directory);
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
        StringBuilder res = new StringBuilder();
        // get all the classpaths (change from '\' to '\\')
        for(Element cp : app.getMachineParameters().getClassPaths()) {
            // insert the path to the install directory
            res.append(ScriptConstants.doubleBackslashes(
                    getInstallDirPath()));
            res.append(Constants.BACKSLASH + Constants.BACKSLASH);
            // Then insert the class path. 
            res.append(ScriptConstants.replaceWindowsDirSeparators(
                    cp.getText()));
            res.append(Constants.SEMICOLON);
        }
        return res.toString();
    }

    /**
     * Creates the batch script for starting the application.
     * It checks if the application is running and starts the application
     * if it is not the case. 
     * The application is started by calling the start_app.vbs. 
     * 
     * The script should be the following:
     * 
     * - echo Starting windows application : app
     * - cd "path"
     * - if exist ".\conf\run_app.txt" goto NOSTART
     * - goto START
     * - 
     * - :START
     * - cscript .\conf\start_app.vbs
     * - goto DONE
     * - 
     * - :NOSTART
     * - echo Application already running.
     * - 
     * - :DONE
     * 
     * where:
     * app = the name of the application.
     * path = the path to the installation directory.
     * 
     * @param app The application to start.
     * @param directory The directory where the script should be placed.
     * @throws IOFailure If an error occurred during the creation of the 
     * windows start bat script.
     */
    protected void windowsStartBatScript(Application app, File directory) 
            throws IOFailure {
        File appStartScript = new File(directory, 
                Constants.SCRIPT_NAME_LOCAL_START + app.getIdentification() 
                + scriptExtension);
        try {
            // make print writer for writing to file
            PrintWriter appPrint = new PrintWriter(appStartScript);
            try {
                // initiate variables
                String id = app.getIdentification();
                String tmpRunApp = Constants.FILE_TEMPORARY_RUN_WINDOWS_NAME
                        + id;

                // cd "path"
                appPrint.println(ScriptConstants.CD + Constants.SPACE
                        + Constants.QUOTE_MARK
                        + app.installPathWindows() + Constants.QUOTE_MARK);
                // if exist .\conf\run_app.txt GOTO NOSTART
                appPrint.println(ScriptConstants.IF + Constants.SPACE
                        + ScriptConstants.EXIST + Constants.SPACE 
                        + Constants.DOT + Constants.CONF_DIR_WINDOWS + tmpRunApp
                        + Constants.SPACE + ScriptConstants.GOTO
                        + Constants.SPACE + ScriptConstants.LABEL_NOSTART);
                // GOTO START
                appPrint.println(ScriptConstants.GOTO + Constants.SPACE
                        + ScriptConstants.LABEL_START);
                // 
                appPrint.println();
                // :START
                appPrint.println(Constants.COLON
                        + ScriptConstants.LABEL_START);
                // cscript .\conf\start_app.vbs
                appPrint.println(ScriptConstants.CSCRIPT + Constants.SPACE
                        + Constants.DOT + Constants.CONF_DIR_WINDOWS
                        + Constants.SCRIPT_NAME_LOCAL_START + id 
                        + Constants.EXTENSION_VBS_FILES);
                // GOTO DONE
                appPrint.println(ScriptConstants.GOTO + Constants.SPACE
                        + ScriptConstants.LABEL_DONE);
                // 
                appPrint.println();
                // :NOSTART
                appPrint.println(Constants.COLON 
                        + ScriptConstants.LABEL_NOSTART);
                // echo Cannot start. Application already running.
                appPrint.println(ScriptConstants.ECHO_CANNOT_START_APP);
                // 
                appPrint.println();
                // :DONE
                appPrint.println(Constants.COLON + ScriptConstants.LABEL_DONE);
            } finally {
                // close file
                appPrint.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create the start script for application: "
                + app.getIdentification() + ", at machine: '" + name + "'"; 
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * This function creates the VBscript to start the application.
     * It calls a command for executing the java application, then 
     * it writes the way to kill the process in the kill_ps_app.bat
     * and finally it creates the run-file.
     * 
     * It should have the following content:
     * - set WshShell = CreateObject("WScript.Shell")
     * - set oExec = WshShell.exec( JAVA )
     * - set fso = CreateObject("Scripting.FileSystemObject")
     * - set f = fso.OpenTextFile(".\conf\kill_ps_app.bat", 2, True)
     * - f.WriteLine "taskkill /F /PID " & oExec.ProcessID
     * - f.close
     * 
     * where:
     * JAVA = the command for starting the java application (very long).
     * app = the name of the application.
     * 
     * @param app The application to start.
     * @param directory The directory where the script should be placed.
     * @throws IOFailure If an error occurred during the creation of the 
     * windows vb script.
     */
    protected void windowsStartVbsScript(Application app, File directory) 
            throws IOFailure {
        File appStartSupportScript = new File(directory,
                Constants.SCRIPT_NAME_LOCAL_START + app.getIdentification() 
                + Constants.EXTENSION_VBS_FILES);
        try {
            // make print writer for writing to file
            PrintWriter vbsPrint = new PrintWriter(
                    appStartSupportScript);
            try {
                // initiate variables
                String id = app.getIdentification();
                String killPsName = Constants.SCRIPT_KILL_PS + id 
                        + scriptExtension;
                String tmpRunPsName = Constants.FILE_TEMPORARY_RUN_WINDOWS_NAME
                        + id;

                // Set WshShell = CreateObject("WScript.Shell")
                vbsPrint.println(ScriptConstants.VB_CREATE_SHELL_OBJ);
                // Set oExec = WshShell.exec( "JAVA" )
                vbsPrint.println(
                        ScriptConstants.VB_CREATE_EXECUTE
                        + ScriptConstants.JAVA + Constants.SPACE 
                        + app.getMachineParameters().writeJavaOptions()
                        + Constants.SPACE + Constants.DASH
                        + ScriptConstants.CLASSPATH + Constants.SPACE
                        + Constants.QUOTE_MARK + Constants.QUOTE_MARK
                        + osGetClassPath(app) + Constants.QUOTE_MARK
                        + Constants.QUOTE_MARK + Constants.SPACE 
                        + Constants.DASH + ScriptConstants.OPTION_SETTINGS_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath())
                        + Constants.SETTINGS_PREFIX + id 
                        + Constants.EXTENSION_XML_FILES + Constants.QUOTE_MARK
                        + Constants.QUOTE_MARK + Constants.SPACE 
                        + Constants.DASH + ScriptConstants.OPTION_LOG_COMPLETE
                        + Constants.SPACE + Constants.DASH
                        + ScriptConstants.OPTION_LOG_CONFIG_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath()) 
                        + Constants.LOG_PREFIX + id 
                        + Constants.EXTENSION_LOG_PROPERTY_FILES
                        + Constants.QUOTE_MARK + Constants.QUOTE_MARK
                        + Constants.SPACE + Constants.DASH
                        + ScriptConstants.OPTION_SECURITY_MANAGER
                        + Constants.SPACE + Constants.DASH
                        + ScriptConstants.OPTION_SECURITY_POLICY_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath())
                        + Constants.SECURITY_POLICY_FILE_NAME
                        + Constants.QUOTE_MARK + Constants.QUOTE_MARK
                        + Constants.SPACE + app.getTotalName() 
                        + Constants.QUOTE_MARK + Constants.BRACKET_END);
                // Set fso = CreateObject("Scripting.FileSystemObject")
                vbsPrint.println(ScriptConstants.VB_CREATE_FSO);
                // set f = fso.OpenTextFile(".\conf\kill_ps_app.bat", 2, True)
                vbsPrint.println(ScriptConstants.VB_WRITE_F_PREFIX
                        + killPsName + ScriptConstants.VB_WRITE_F_SURFIX);
                // f.WriteLine "taskkill /F /PID " & oExec.ProcessID
                vbsPrint.println(ScriptConstants.VB_WRITE_F_KILL);
                // f.close
                vbsPrint.println(ScriptConstants.VB_WRITE_F_CLOSE);
                // set tf = fso.OpenTextFile(".\conf\run_app.txt", 2, True)
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_PREFIX
                        + tmpRunPsName + ScriptConstants.VB_WRITE_TF_SURFIX);
                // tf.WriteLine running
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_CONTENT);
                // f.close
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_CLOSE);
            } finally {
                // close file
                vbsPrint.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create the start script for application: "
                + app.getIdentification() + ", at machine: '" + name + "'"; 
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }


    /**
     * THIS HAS NOT BEEN IMPLEMENTED FOR WINDOWS YET - ONLY LINUX!
     * 
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
        String databaseDir = machineParameters.getDatabaseDirValue();
        // Do not install if no proper database directory.
        if(databaseDir == null || databaseDir.isEmpty()) {
            return Constants.EMPTY;
        }

        StringBuilder res = new StringBuilder(
                ScriptConstants.ECHO_WINDOWS_DATABASE);
        res.append(Constants.NEWLINE);

        return res.toString();
    }
    
    /**
     * THIS HAS NOT BEEN IMPLEMENTED FOR WINDOWS YET - ONLY LINUX!
     * 
     * Checks if a specific directory for the archive database is given 
     * in the settings, and thus if the archive database should be 
     * installed on this machine.
     * 
     * If not specific database is given (archiveDatabaseFileName = null)
     * then use the default in the NetarchiveSuite.zip package.
     * Else send the new archive database to the standard database 
     * location, and extract it to the given location.
     * 
     * @return The script for installing the archive database 
     * (if needed).
     */

    @Override
    protected String osInstallArchiveDatabase() {
        String bpDatabaseDir = 
            machineParameters.getArchiveDatabaseDirValue();
        
        // Do not install if no proper archive database directory.
        if(bpDatabaseDir == null || bpDatabaseDir.isEmpty()) {
            return Constants.EMPTY;
        }

        StringBuilder res = new StringBuilder(
                ScriptConstants.ECHO_WINDOWS_DATABASE);
        res.append(Constants.NEWLINE);

        return res.toString();
    }

    /**
     * Creates the specified directories in the deploy config file.
     * 
     * @return The script for creating the directories.
     */
    @Override
    protected String osInstallScriptCreateDir() {
        StringBuilder res = new StringBuilder();

        res.append(ScriptConstants.ECHO_CREATING_DIRECTORIES);
        res.append(Constants.NEWLINE);

        // send script to machine
        res.append(ScriptConstants.SCP + Constants.SPACE);
        res.append(getMakeDirectoryName());
        res.append(Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.COLON);
        res.append(Constants.NEWLINE);

        // run script
        res.append(ScriptConstants.SSH + Constants.SPACE + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE);
        res.append(getMakeDirectoryName());
        res.append(Constants.NEWLINE);

        // delete script
        res.append(ScriptConstants.SSH + Constants.SPACE + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.DEL + Constants.SPACE);
        res.append(getMakeDirectoryName());
        res.append(Constants.NEWLINE);

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

        if(clean) {
            res.append(ScriptConstants.IF + Constants.SPACE
                    + ScriptConstants.EXIST + Constants.SPACE);
            res.append(dir);
            res.append(Constants.SPACE + ScriptConstants.RD + Constants.SPACE);
            res.append(dir);
            res.append(Constants.NEWLINE);
        }
        res.append(ScriptConstants.IF + Constants.SPACE + ScriptConstants.NOT
                + Constants.SPACE + ScriptConstants.EXIST + Constants.SPACE);
        res.append(dir);
        res.append(Constants.SPACE + ScriptConstants.MD + Constants.SPACE);
        res.append(dir);
        
        res.append(Constants.NEWLINE);

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

        String[] pathDirs = dir.split(Constants.REGEX_BACKSLASH_CHARACTER);
        StringBuilder path = new StringBuilder();

        // only make directories along path to last directory, 
        // don't create end directory.
        for(int i = 0; i < pathDirs.length-1; i++) {
            // don't make directory of empty path.
            if(!pathDirs[i].isEmpty()) {
                path.append(pathDirs[i]);
                if(!path.substring(path.length()-1).endsWith(Constants.COLON)) {
                    res.append(scriptCreateDir(path.toString(), false));
                }
            }
            path.append(Constants.BACKSLASH);
        }

        return res.toString();
    }
    
    /**
     * Creates the script for creating the application specified directories.
     * Also creates the directories along the path to the directories.
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
                                dir + Constants.BACKSLASH + subdir, false));
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
                    if(!dir.equals(machineDir)) {
                        res.append(createPathToDir(dir));
                        res.append(scriptCreateDir(dir, resetTempDir));
                    }
                }
            }
        }

        return res.toString();
    }
   
    /**
     * Creates the name for the make dir script.
     * 
     * @return The name of the script for creating the directories.
     */
    protected String getMakeDirectoryName() {
        return Constants.WINDOWS_DIR_CREATE_PREFIX + name + scriptExtension;
    }
    
    /**
     * Function to create the script which installs the new directories.
     * This is only used for windows machines!
     * 
     * @param directory The directory to put the file.
     * @throws IOFailure If an error occurred during the creation of the 
     * install-dir script.
     */
    @Override
    protected void createInstallDirScript(File directory) throws IOFailure {
        File dirScript = new File(directory, 
                getMakeDirectoryName());
        try {
            // make print writer for writing to file
            PrintWriter dirPrint = new PrintWriter(dirScript);
            try {
                // go to correct directory
                dirPrint.print(ScriptConstants.CD + Constants.SPACE);
                dirPrint.print(getInstallDirPath());
                dirPrint.print(Constants.NEWLINE);

                // go through all directories.
                String dir;

                // get archive.bitpresevation.baseDir directory.
                dir = settings.getLeafValue(
                        Constants.SETTINGS_ARCHIVE_BP_BASEDIR_LEAF);
                if(dir != null && !dir.isEmpty() 
                        && !dir.equalsIgnoreCase(Constants.DOT)) {
                    dirPrint.print(createPathToDir(dir));
                    dirPrint.print(scriptCreateDir(dir, false));
                }

                // get archive.arcrepository.baseDir directory.
                dir = settings.getLeafValue(
                        Constants.SETTINGS_ARCHIVE_ARC_BASEDIR_LEAF);
                if(dir != null && !dir.isEmpty()
                        && !dir.equalsIgnoreCase(Constants.DOT)) {
                    dirPrint.print(createPathToDir(dir));
                    dirPrint.print(scriptCreateDir(dir, false));
                }

                dirPrint.print(getAppDirectories());

                // get tempDir directory.
                dir = settings.getLeafValue(
                        Constants.SETTINGS_TEMPDIR_LEAF);
                if(dir != null && !dir.isEmpty()
                        && !dir.equalsIgnoreCase(Constants.DOT)) {
                    dirPrint.print(createPathToDir(dir));
                    dirPrint.print(scriptCreateDir(dir, resetTempDir));
                }
            } finally {
                // close file
                dirPrint.close();
            }
        } catch (IOException e) {
            String msg = "Problems creating install directory script. ";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Changes the file directory path to the format used in the security 
     * policy.
     * @param path The current path.
     * @return The formatted path.
     */
    @Override
    protected String changeFileDirPathForSecurity(String path) {
        path += Constants.BACKSLASH + Constants.SECURITY_FILE_DIR_TAG
                + Constants.BACKSLASH;
        return path.replace(Constants.BACKSLASH, 
                ScriptConstants.SECURITY_DIR_SEPARATOR);
    }

    /**
     * This method does the following:
     * 
     * Retrieves the path to the jmxremote.access and jmxremote.password files.
     * 
     * Moves these files, if they are different from standard.
     * This has to be a force move (command 'move /Y').
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
        if(options.length == 0) {
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
        if(options.length == 0) {
            passwordFilePath = Constants.JMX_PASSWORD_FILE_PATH_DEFAULT;
        } else {
            passwordFilePath = options[0];
            // warn if more than one access file is defined.
            if(options.length > 1) {
                log.debug(Constants.MSG_WARN_TOO_MANY_JMXREMOTE_FILE_PATHS);
            }
        }

        // change the path to windows syntax.
        accessFilePath = ScriptConstants.replaceWindowsDirSeparators(
                accessFilePath);
        passwordFilePath = ScriptConstants.replaceWindowsDirSeparators(
                passwordFilePath);

        // initialise the resulting command string.
        StringBuilder res = new StringBuilder();

        // - echo make password files readonly 
        res.append(ScriptConstants.ECHO_MAKE_PASSWORD_FILES);
        res.append(Constants.NEWLINE);

        // IF NOT DEFAULT PATHS, THEN MAKE SCRIPT TO MOVE THE FILES.
        if(!accessFilePath.equals(ScriptConstants.replaceWindowsDirSeparators(
                Constants.JMX_ACCESS_FILE_PATH_DEFAULT))) {
            // ssh dev@kb-test-adm-001.kb.dk "mv 
            // installpath/conf/jmxremote.access installpath/accessFilePath"
            res.append(ScriptConstants.SSH + Constants.SPACE);
            res.append(machineUserLogin());
            res.append(Constants.SPACE + Constants.QUOTE_MARK);
            res.append(ScriptConstants.WINDOWS_COMMAND_RUN + Constants.SPACE);
            res.append(ScriptConstants.WINDOWS_FORCE_MOVE);
            res.append(Constants.SPACE);
            res.append(ScriptConstants.doubleBackslashes(
                    getLocalInstallDirPath()));
            res.append(ScriptConstants.replaceWindowsDirSeparators(
                    Constants.JMX_ACCESS_FILE_PATH_DEFAULT));
            res.append(Constants.SPACE);
            res.append(ScriptConstants.doubleBackslashes(
                    getLocalInstallDirPath()));
            res.append(accessFilePath);
            res.append(Constants.QUOTE_MARK);
            res.append(Constants.NEWLINE);
        }

        if(!passwordFilePath.equals(ScriptConstants.replaceWindowsDirSeparators(
                Constants.JMX_PASSWORD_FILE_PATH_DEFAULT))) {
            // ssh dev@kb-test-adm-001.kb.dk "mv 
            // installpath/conf/jmxremote.access installpath/accessFilePath"
            res.append(ScriptConstants.SSH + Constants.SPACE);
            res.append(machineUserLogin());
            res.append(Constants.SPACE + Constants.QUOTE_MARK);
            res.append(ScriptConstants.WINDOWS_COMMAND_RUN + Constants.SPACE);
            res.append(ScriptConstants.WINDOWS_FORCE_MOVE);
            res.append(Constants.SPACE);
            res.append(ScriptConstants.doubleBackslashes(
                    getLocalInstallDirPath()));
            res.append(ScriptConstants.replaceWindowsDirSeparators(
                    Constants.JMX_PASSWORD_FILE_PATH_DEFAULT));
            res.append(Constants.SPACE);
            res.append(ScriptConstants.doubleBackslashes(
                    getLocalInstallDirPath()));
            res.append(passwordFilePath);
            res.append(Constants.QUOTE_MARK);
            res.append(Constants.NEWLINE);
        }

        // - echo Y | ssh 'login'@'machine' cmd /c cacls 
        // 'environmentName'\\conf\\jmxremote.password /P BITARKIV\\'login':R
        res.append(ScriptConstants.ECHO_Y + Constants.SPACE 
                + Constants.SEPARATOR + Constants.SPACE + ScriptConstants.SSH
                + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK
                + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.CACLS + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(
            getLocalInstallDirPath()));
        res.append(passwordFilePath);
        res.append(Constants.SPACE 
                + ScriptConstants.SLASH_P + Constants.SPACE 
                + ScriptConstants.BITARKIV_BACKSLASH_BACKSLASH);
        res.append(machineParameters.getMachineUserName().getText());
        res.append(ScriptConstants.COLON_R + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);

        // - echo Y | ssh 'login'@'machine' cmd /c cacls 
        // 'environmentName'\\conf\\jmxremote.access /P BITARKIV\\'login':R
        res.append(ScriptConstants.ECHO_Y + Constants.SPACE 
                + Constants.SEPARATOR + Constants.SPACE + ScriptConstants.SSH
                + Constants.SPACE);
        res.append(machineUserLogin());
        res.append(Constants.SPACE + Constants.QUOTE_MARK
                + ScriptConstants.WINDOWS_COMMAND_RUN
                + Constants.SPACE + ScriptConstants.CACLS + Constants.SPACE);
        res.append(ScriptConstants.doubleBackslashes(
                getLocalInstallDirPath()));
            res.append(accessFilePath);
        res.append(Constants.SPACE 
                + ScriptConstants.SLASH_P + Constants.SPACE 
                + ScriptConstants.BITARKIV_BACKSLASH_BACKSLASH);
        res.append(machineParameters.getMachineUserName().getText());
        res.append(ScriptConstants.COLON_R + Constants.QUOTE_MARK);
        res.append(Constants.NEWLINE);

        return res.toString();
    }

    /**
     * Creates scripts for restarting all the applications on a machine.
     * This script should start by killing all the existing processes, and then
     * starting them again.
     * 
     * First the killall scripts is called, then wait for 5 seconds for the 
     * applications to be fully terminated, and finally call the startall 
     * script.
     * 
     * The createWaitScript is called through this script to create the wait 
     * script which is used by the restart script.
     * 
     * @param dir The directory where the script file will be placed.
     * @throws IOFailure If the restart script cannot be created, or if the 
     * wait script cannot be created.
     */
    @Override
    protected void createRestartScript(File dir) throws IOFailure {
        // Start by creating the wait script.
        createWaitScript(dir);
        
        try {
            // initialise the script file.
            File restartScript = new File(dir, 
                    Constants.SCRIPT_NAME_RESTART + scriptExtension);
            
            // make print writer for writing to file
            PrintWriter restartPrint = new PrintWriter(restartScript);
            try {
                restartPrint.println(ScriptConstants.CD + Constants.SPACE
                        + Constants.QUOTE_MARK + getConfDirPath()
                        + Constants.QUOTE_MARK);

                // call killall script.
                restartPrint.print(ScriptConstants.WINDOWS_COMMAND_RUN);
                restartPrint.print(Constants.SPACE);
                restartPrint.print(Constants.SCRIPT_NAME_KILL_ALL 
                        + scriptExtension);
                restartPrint.print(Constants.NEWLINE);
                
                // call wait script.
                restartPrint.print(ScriptConstants.CSCRIPT);
                restartPrint.print(Constants.SPACE);
                restartPrint.print(Constants.SCRIPT_NAME_WAIT 
                        + Constants.EXTENSION_VBS_FILES);
                restartPrint.print(Constants.NEWLINE);
                
                // call startall script.
                restartPrint.print(ScriptConstants.WINDOWS_COMMAND_RUN);
                restartPrint.print(Constants.SPACE);
                restartPrint.print(Constants.SCRIPT_NAME_START_ALL 
                        + scriptExtension);
                restartPrint.print(Constants.NEWLINE);
            } finally {
                // close file
                restartPrint.close();
            }
        } catch (IOException e) {
            // Log the error and throw an IOFailure.
            log.trace(Constants.MSG_ERROR_RESTART_FILE, e);
            throw new IOFailure(Constants.MSG_ERROR_RESTART_FILE, e);
        }
    }
    
    /**
     * Creates the script for waiting during restart.
     * 
     * @param dir The directory where the script should be placed.
     * @throws IOFailure If the method fails in creating the wait script.
     */
    protected void createWaitScript(File dir) throws IOFailure {
        try {
            // initialise the script file.
            File waitScript = new File(dir, 
                    Constants.SCRIPT_NAME_WAIT + Constants.EXTENSION_VBS_FILES);
            
            // make print writer for writing to file
            PrintWriter waitPrint = new PrintWriter(waitScript);
            try {
                // Create the wait script.
                waitPrint.print(ScriptConstants.VB_WRITE_WAIT
                        + Constants.SPACE
                        + (Constants.WAIT_TIME_DURING_RESTART 
                                * Constants.TIME_SECOND_IN_MILLISECONDS));
                waitPrint.print(Constants.NEWLINE);
            } finally {
                // close file
                waitPrint.close();
            }
        } catch (IOException e) {
            // log error and throw a IOFailure.
            log.trace(Constants.MSG_ERROR_WAIT_FILE, e);
            throw new IOFailure(Constants.MSG_ERROR_WAIT_FILE, e);
        }
    }
    
    /**
     * Creates a script for starting the archive database on a given machine.
     * This is only created if the &lt;globalArchiveDatabaseDir&gt; parameter
     * is defined on the machine level.
     * 
     * @param dir The directory where the script will be placed.
     */
    @Override
    protected void createArchiveDatabaseStartScript(File dir) {
        
        // Ignore if no archive database directory has been defined.
        String dbDir = machineParameters.getArchiveDatabaseDirValue();
        if(dbDir.isEmpty()) {
            return;
        }
    }

    /**
     * Creates a script for killing the archive database on a given machine.
     * This is only created if the &lt;globalArchiveDatabaseDir&gt; parameter
     * is defined on the machine level.
     * 
     * @param dir The directory where the script will be placed.
     */
    @Override
    protected void createArchiveDatabaseKillScript(File dir) {
        
        // Ignore if no archive database directory has been defined.
        String dbDir = machineParameters.getArchiveDatabaseDirValue();
        if(dbDir.isEmpty()) {
            return;
        }
    }
}
