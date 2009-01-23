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

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A WindowsMachine is the instance of the abstract machine class, which runs 
 * the operating system Windows.
 * This class only contains the operating system specific functions.
 */
public class WindowsMachine extends Machine {
    /**
     * The constructor. 
     * Starts by initialising the parent abstract class, then sets the 
     * operating system dependent variables.
     * 
     * @param e The XML root element.
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
     * @param dbFile The name of the database file.
     * @param resetDir Whether the temporary directory should be reset.
     */
    public WindowsMachine(Element e, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
                File logProp, File securityPolicy, File dbFile,
                boolean resetDir) {
        super(e, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy, dbFile, resetDir);
        // set operating system
        OS = "windows";
        scriptExtension = ".bat";
    }

    /**
     * Creates the operation system specific installation script for 
     * this machine.
     * 
     * @return Operation system specific part of the installscript
     */
    @Override
    protected String osInstallScript() {
        StringBuilder res = new StringBuilder("");
        // echo copying null.zip to: kb-test-bar-011.bitarkiv.kb.dk
        res.append("echo copying ");
        res.append(netarchiveSuiteFileName);
        res.append(" to: ");
        res.append(name);
        res.append("\n");
        // scp null.zip dev@kb-test-bar-011.bitarkiv.kb.dk:
        res.append("scp ");
        res.append(netarchiveSuiteFileName);
        res.append(" ");
        res.append(machineUserLogin());
        res.append(":");
        res.append("\n");
        // echo unzipping null.zip at: kb-test-bar-011.bitarkiv.kb.dk
        res.append("echo unzipping ");
        res.append(netarchiveSuiteFileName);
        res.append(" at: ");
        res.append(name);
        res.append("\n");
        // ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d TEST 
        // -o null.zip
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" cmd /c unzip.exe -q -d ");
        res.append(getEnvironmentName());
        res.append(" -o ");
        res.append(netarchiveSuiteFileName);
        res.append("\n");
        // create other directories.
        res.append(osInstallScriptCreateDir());
        // echo preparing for copying of settings and scripts
        res.append("echo preparing for copying of settings and scripts");
        res.append("\n");
        // ssh machine: "if [ -e conf/jmxremote.password ]; 
        // then chmod +x conf/jmxremote.password; fi; "
        // if [ $(ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk 
        // cmd /c if exist JOLF\\conf\\security.policy echo 1 ) ]; then 
        // echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls 
        // JOLF\\conf\\security.policy 
        // /P BITARKIV\\ba-test:F; fi
        res.append("if [ $(ssh ");
        res.append(machineUserLogin());
        res.append(" cmd /c if exist ");
        res.append(changeToScriptPath(getLocalConfDirPath()));
        res.append("jmxremote.password");
        res.append(" echo 1");
        res.append(" ) ]; then ");
        res.append("echo Y | ssh ");
        res.append(machineUserLogin());
        res.append(" cmd /c cacls ");
        res.append(changeToScriptPath(getLocalConfDirPath()));
        res.append("jmxremote.password /P BITARKIV\\\\");
        res.append(machineParameters.getMachineUserName().getText());
        res.append(":F");
        res.append("; fi;");
        res.append("\n");
        // echo copying settings and scripts
        res.append("echo copying settings and scripts");
        res.append("\n");
        // scp -r kb-test-bar-011.bitarkiv.kb.dk/* 
        // dev@kb-test-bar-011.bitarkiv.kb.dk:
        // ""c:\\Documents and Settings\\dev\\TEST\\conf\\""
        res.append("scp -r ");
        res.append(name);
        res.append("/* ");
        res.append(machineUserLogin());
        res.append(":");
        res.append(changeToScriptPath(getLocalConfDirPath()));
        res.append("\n");
        // APPLY DATABASE
        res.append(osInstallDatabase());
        // pw.println("echo make password files readonly");
        res.append("echo make password files readonly");
        res.append("\n");
        // echo Y | ssh dev@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls 
        // ""c:\\Documents and Settings\\dev\\TEST\\conf\\jmxremote.password"" 
        // /P BITARKIV\\dev:R
        res.append("echo Y | ssh ");
        res.append(machineUserLogin());
        res.append(" cmd /c cacls ");
        res.append(changeToScriptPath(getLocalConfDirPath()));
        res.append("jmxremote.password /P BITARKIV\\\\");
        res.append(machineParameters.getMachineUserName().getText());
        res.append(":R");
        res.append("\n");
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
        StringBuilder res = new StringBuilder("");
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" \"cmd /c  ");
        res.append(getConfDirPath());
        res.append("killall");
        res.append(scriptExtension);
        res.append(" \" ");
        res.append("\n");
        return res.toString();
    }

    /**
     * Creates the operation system specific starting script for this machine.
     * 
     * @return Operation system specific part of the startscript.
     */
    @Override
    protected String osStartScript() {
        StringBuilder res = new StringBuilder("");
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" \"cmd /c  ");
        res.append(getConfDirPath());
        res.append("startall");
        res.append(scriptExtension);
        res.append(" \" ");
        res.append("\n");
        return res.toString();
    }

    /** 
     * The operation system specific path to the installation directory.
     *  
     * @return Install path.
     */
    @Override
    protected String getInstallDirPath() {
        return machineParameters.getInstallDirValue() + "\\" 
                + getEnvironmentName();
    }

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + "\\conf\\";
    }
    
    /**
     * Creates the local path to the conf dir.
     * 
     * @return The path to the conf dir for ssh.
     */
    protected String getLocalConfDirPath() {
        return getEnvironmentName() + "\\conf\\";
    }

    /**
     * This function creates the script to kill all applications on this 
     * machine.
     * The scripts calls all the kill script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createOSLocalKillAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the kill all script file
        File killAllScript = new File(directory, "killall" + scriptExtension);
        try {
            // Initialise script
            PrintWriter killPrinter = new PrintWriter(killAllScript);
            try {
                killPrinter.println("echo Killing all applications at: " 
                        + name);
                killPrinter.println("cd \"" + getConfDirPath() + "\"");
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "kill_"
                            + app.getIdentification() + scriptExtension;
                    killPrinter.print(
                            Constants.OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE);
                    killPrinter.print(appScript);
                    killPrinter.println("\"");
                    killPrinter.println();
                }
            } finally {
                // close script
                killPrinter.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create the local kill all script: " + e);
            throw new IOFailure("Problems creating local kill all script: "
                    + e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating local kill all script: "
                    + e);
        }
    }

    /**
     * This function creates the script to start all applications on this 
     * machine.
     * The scripts calls all the start script for each application. 
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createOSLocalStartAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the start all script file
        File startAllScript = new File(directory, "startall"
                + scriptExtension);
        try {
            // Initialise script
            PrintWriter startPrinter = new PrintWriter(startAllScript);
            try {
                startPrinter.println("echo Starting all applications at: " 
                        + name);
                startPrinter.println("cd \"" + getConfDirPath() + "\"");
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "start_"
                            + app.getIdentification() + scriptExtension;
                    startPrinter.print(
                            Constants.OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE);
                    startPrinter.print(appScript);
                    startPrinter.println("\"");
                    startPrinter.println();
                }
            } finally {
                // close script
                startPrinter.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create the local start all script: " + e);
            throw new IOFailure("Problems creating local start all script: "
                    + e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating local start all script: "
                    + e);
        }
    }

    /**
     * Creates the kill scripts for all the applications.
     * Two script files are created: kill_app.bat and kill_ps_app.bat. 
     * 
     * kill_ps_app.bat kills the process of the application.
     * kill_app.bat runs kill_ps_app.bat if the application is running.
     * 
     * The kill_app.bat should have the following structure:
     * 
     * - ECHO Killing application : app
     * - CD "path"
     * - IF EXIST kill_ps_app.txt GOTO KILL
     * - GOTO NOKILL
     * - 
     * - :KILL
     * - cmdrun kill_ps_app.bat
     * - DEL kill_ps_app.txt
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
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createApplicationKillScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // go through all applications and create their kill script
        try {
            for(Application app : applications) {
                File appKillScript = new File(directory, 
                        "kill_" + app.getIdentification() + scriptExtension);
                try {
                    // make print writer for writing to file
                    PrintWriter appPrint = new PrintWriter(appKillScript);
                    try {
                        // initiate variables
                        String id = app.getIdentification();
                        String killPsName = "kill_ps_" + id + scriptExtension;
                        // get the content for the kill script of 
                        // this application
                        // #echo kill windows application
                        appPrint.println("ECHO Killing windows application: "
                                + id);
                        // cd "path"
                        appPrint.println("CD \""
                                + app.installPathWindows() + "\\conf\"");
                        // if exist run_app.txt GOTO KILL
                        appPrint.println("IF EXIST " + killPsName
                                + " GOTO KILL");
                        // GOTO NOKILL
                        appPrint.println("GOTO NOKILL");
                        // 
                        appPrint.println();
                        // :KILL
                        appPrint.println(":KILL");
                        // cmdrun kill_ps_app.bat
                        appPrint.println(Constants.
                                OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE
                                + "\"kill_ps_" + id
                                + ".bat" + "\"");
                        // del run_app.txt
                        appPrint.println("DEL " + killPsName);
                        // GOTO DONE
                        appPrint.println("GOTO DONE");
                        //
                        appPrint.println();
                        // :NOKILL
                        appPrint.println(":NOKILL");
                        // echo Cannot kill application. Already running.
                        appPrint.println("ECHO Cannot kill application."
                                + " Is not running.");
                        //
                        appPrint.println();
                        // :DONE
                        appPrint.println(":DONE");
                    } finally {
                        // close files
                        appPrint.close();
                    }
                } catch (IOException e) {
                    log.trace("Cannot create the kill script for application: "
                            + app.getIdentification() + ", at machine: " 
                            + name);
                    throw new IOFailure(
                            "Problems creating local kill all script: " + e);
                } 
            }
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating kill script for "
                        + "applications: " + e);
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
     * @see windowsStartBatScript, windowsStartVbsScript.
     */
    @Override
    protected void createApplicationStartScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        try {
            // go through all applications and create their start script
            for(Application app : applications) {
                windowsStartBatScript(app, directory);
                windowsStartVbsScript(app, directory);
            }
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating start script "
                        + "for applications: " + e);
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
        StringBuilder res = new StringBuilder("");
        // get all the classpaths (change from '\' to '\\')
        for(Element cp : app.getMachineParameters().getClassPaths()) {
            // insert the path to the install directory
            res.append(getInstallDirPath().replaceAll("[\\\\]", "\\\\\\\\"));
            res.append("\\\\");
            // Then insert the class path. 
            res.append(cp.getText().replaceAll("[/]", "\\\\\\\\"));
            res.append(";");
        }
        return res.toString();
    }

    /**
     * Changes a string into correct formatted style.
     * The '.vbs' script needs '\\' instead of '\', which is quite annoying 
     * when using regular expressions, since a final '\' in regular expressions
     *  is '\\\\', thus '\\' = '\\\\\\\\' (8).
     *   
     * @param path The directory path to change to appropriate format.
     * @return The formatted path.
     */
    private String changeToScriptPath(String path) {
        ArgumentNotValid.checkNotNull(path, "String path");
        String res = path;
        res = res.replaceAll("\\\\", "\\\\\\\\");
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
     * - if exist ".\conf\kill_ps_app.txt" goto NOSTART
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
     */
    protected void windowsStartBatScript(Application app, File directory) {
        File appStartScript = new File(directory, 
                "start_" + app.getIdentification() + scriptExtension);
        try {
            // make print writer for writing to file
            PrintWriter appPrint = new PrintWriter(appStartScript);
            try {
                // initiate variables
                String id = app.getIdentification();
                String killPsName = "kill_ps_" + id + scriptExtension;

                // #echo start windows app
                appPrint.println("#echo START WINDOWS APPLICATION: "
                        + app.getIdentification());
                // cd "path"
                appPrint.println("cd \""
                        + app.installPathWindows() + "\"");
                // if exist .\conf\run_app.txt GOTO NOSTART
                appPrint.println("if Exist .\\conf\\" + killPsName
                        + " GOTO NOSTART");
                // GOTO START
                appPrint.println("GOTO START");
                // 
                appPrint.println();
                // :START
                appPrint.println(":START");
                // cscript .\conf\start_app.vbs
                appPrint.println("cscript .\\conf\\"
                        + "start_" + id + ".vbs");
                // GOTO DONE
                appPrint.println("GOTO DONE");
                // 
                appPrint.println();
                // :NOSTART
                appPrint.println(":NOSTART");
                // echo Cannot start. Application already running.
                appPrint.println("echo Cannot start. "
                        + "Application already running.");
                // 
                appPrint.println();
                // :DONE
                appPrint.println(":DONE");
            } finally {
                // close file
                appPrint.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create the start script for application: "
                    + app.getIdentification() + ", at machine: "
                    + name);
            throw new IOFailure("Problems creating local "
                        + "start all script: " + e);
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
     */
    protected void windowsStartVbsScript(Application app, File directory) {
        File appStartSupportScript = new File(directory,
                "start_" + app.getIdentification() + ".vbs");
        try {
            // make print writer for writing to file
            PrintWriter vbsPrint = new PrintWriter(
                    appStartSupportScript);
            try {
                // initiate variables
                String id = app.getIdentification();
                String killPsName = "kill_ps_" + id + scriptExtension;

                // Set WshShell = CreateObject("WScript.Shell")
                vbsPrint.println("Set WshShell= "
                        + "CreateObject(\"WScript.Shell\")");
                // Set oExec = WshShell.exec( "JAVA" )
                vbsPrint.println(
                        "Set oExec = WshShell.exec( \""
                        + "java " + app.getMachineParameters()
                        .writeJavaOptions()
                        + " -classpath \"\""
                        + osGetClassPath(app) + "\"\""
                        + " -Ddk.netarkivet.settings.file=\"\""
                        + changeToScriptPath(getConfDirPath())
                        + "settings_" + id
                        + ".xml\"\""
                        + " -Dorg.apache.commons.logging.Log="
                        + "org.apache.commons.logging.impl.Jdk14Logger"
                        + " -Djava.util.logging.config.file=\"\""
                        + changeToScriptPath(getConfDirPath()) + "log_"
                        + id + ".prop\"\""
                        + " -Djava.security.manager"
                        + " -Djava.security.policy=\"\""
                        + changeToScriptPath(getConfDirPath())
                        + "security.policy\"\" "
                        + app.getTotalName() + "\")");
                // Set fso = CreateObject("Scripting.FileSystemObject")
                vbsPrint.println("set fso= "
                + "CreateObject(\"Scripting.FileSystemObject\")");
                // set f = fso.OpenTextFile(".\conf\kill_ps_app.bat", 2, True)
                vbsPrint.println("set f=fso.OpenTextFile(\".\\conf\\"
                        + killPsName + "\",2,True)");
                // f.WriteLine "taskkill /F /PID " & oExec.ProcessID
                vbsPrint.println("f.WriteLine \"taskkill /F /PID \""
                                + " & oExec.ProcessID");
                // f.close
                vbsPrint.println("f.close");
                // set tf = fso.OpenTextFile(".\conf\run_app.txt", 2, True)
            } finally {
                // close file
                vbsPrint.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create the start script for application: "
                    + app.getIdentification() + ", at machine: "
                    + name);
            throw new IOFailure("Problems creating local "
                        + "start all script: " + e);
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
        StringBuilder res = new StringBuilder("echo Database not implemented "
                + "for windows.");
        res.append("\n");
        return res.toString();
    }

    /**
     * Creates the specified directories in the it-config.
     * 
     * @return The script for creating the directories.
     */
    @Override
    protected String osInstallScriptCreateDir() {
        StringBuilder res = new StringBuilder("");

        res.append("echo Creating directories.");
        res.append("\n");

        // send script to machine
        res.append("scp ");
        res.append(getMakeDirectoryName());
        res.append(" ");
        res.append(machineUserLogin());
        res.append(":");
        res.append("\n");

        // run script
        res.append("ssh ");
        res.append(" ");
        res.append(machineUserLogin());
        res.append(" cmd /c ");
        res.append(getMakeDirectoryName());
        res.append("\n");

        // delete script
        res.append("ssh ");
        res.append(" ");
        res.append(machineUserLogin());
        res.append(" cmd /c del ");
        res.append(getMakeDirectoryName());
        res.append("\n");

        return res.toString();
    }

    /**
     * This functions makes the script for creating the new directories.
     * 
     * Linux creates directories directly through ssh.
     * Windows creates an install
     * 
     * @param dir The name of the directory to create.
     * @param clean Whether the directory should be cleaned\reset.
     * @return The lines of code for creating the directories.
     * @see createInstallDirScript.
     */
    @Override
    protected String scriptCreateDir(String dir, boolean clean) {
        StringBuilder res = new StringBuilder();

        if(clean) {
            res.append("IF EXIST ");
            res.append(dir);
            res.append(" RD ");
            res.append(dir);
            res.append("\n");
        }
        res.append("IF NOT EXIST ");
        res.append(dir);
        res.append(" MD ");
        res.append(dir);
        
        res.append("\n");

        return res.toString();
    }
    
    /**
     * Creates the script for creating the application specified directories.
     * 
     * @return The script for creating the application specified directories.
     */
    @Override
    protected String getAppDirectories() {
        StringBuilder res = new StringBuilder("");
        String[] dirs;

        for(Application app : applications) {
            // get archive.fileDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_FILE_DIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(scriptCreateDir(dir, false));
                }
            }

            // get harvester.harvesting.serverDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_HARVEST_SERVER_DIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(scriptCreateDir(dir, false));
                }
            }
            
            // get the viewerproxy.baseDir directories.
            dirs = app.getSettingsValues(
                    Constants.SETTINGS_VIEWERPROXY_BASEDIR_LEAF);
            if(dirs != null && dirs.length > 0) {
                for(String dir : dirs) {
                    res.append(scriptCreateDir(dir, false));
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
        return "dir_" + name + scriptExtension;
    }
    
    /**
     * Function to create the script which installs the new directories.
     * This is only used for windows machines!
     * 
     * @param directory The directory to put the file
     */
    @Override
    protected void createInstallDirScript(File directory) {
           File dirScript = new File(directory, 
                   getMakeDirectoryName());
                try {
                    // make print writer for writing to file
                    PrintWriter dirPrint = new PrintWriter(dirScript);
                    try {
                        // go to correct directory
                        dirPrint.print("cd ");
                        dirPrint.print(getInstallDirPath());
                        dirPrint.print("\n");

                        // go through all directories.
                        String dir;

                        // get archive.bitpresevation.baseDir directory.
                        dir = settings.getLeafValue(
                                Constants.SETTINGS_ARCHIVE_BP_BASEDIR_LEAF);
                        if(dir != null && !dir.equalsIgnoreCase("") 
                                && !dir.equalsIgnoreCase(".")) {
                            dirPrint.print(scriptCreateDir(dir, false));
                        }

                        // get archive.arcrepository.baseDir directory.
                        dir = settings.getLeafValue(
                                Constants.SETTINGS_ARCHIVE_ARC_BASEDIR_LEAF);
                        if(dir != null && !dir.equalsIgnoreCase("")
                                && !dir.equalsIgnoreCase(".")) {
                            dirPrint.print(scriptCreateDir(dir, false));
                        }

                        dirPrint.print(getAppDirectories());

                        // get tempDir directory.
                        dir = settings.getLeafValue(
                                Constants.SETTINGS_TEMP_DIR_LEAF);
                        if(dir != null && !dir.equalsIgnoreCase("")
                                && !dir.equalsIgnoreCase(".")) {
                            dirPrint.print(scriptCreateDir(dir, resetTempDir));
                        }
                    } finally {
                        // close file
                        dirPrint.close();
                    }
                } catch (IOException e) {
                    log.trace("createInstallDirScript: " + e);
                    throw new IOFailure("Problems creating local "
                                + "start all script: " + e);
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
        path += "\\" + Constants.SECURITY_FILE_DIR_ATTACHMENT + "\\";
        return path.replace("\\", "${/}");
    }
}
