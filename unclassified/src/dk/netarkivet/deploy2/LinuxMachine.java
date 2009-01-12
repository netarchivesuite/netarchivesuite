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
     * @param dbFileName The name of the database file.
     */
    public LinuxMachine(Element e, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
            File logProp, File securityPolicy, String dbFileName) {
        super(e, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy, dbFileName);
        // set operating system
        OS = "linux";
        scriptExtension = ".sh";
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
        // echo copying null.zip to:kb-test-adm-001.kb.dk
        res.append("echo copying ");
        res.append(netarchiveSuiteFileName);
        res.append(" to:");
        res.append(name);
        res.append("\n");
        // scp null.zip dev@kb-test-adm-001.kb.dk:/home/dev
        res.append("scp ");
        res.append(netarchiveSuiteFileName);
        res.append(" ");
        res.append(machineUserLogin());
        res.append(":");
        res.append(machineParameters.getInstallDirValue());
        res.append("\n");
        // echo unzipping null.zip at:kb-test-adm-001.kb.dk
        res.append("echo unzipping ");
        res.append(netarchiveSuiteFileName);
        res.append(" at:");
        res.append(name);
        res.append("\n");
        // ssh dev@kb-test-adm-001.kb.dk unzip -q -o /home/dev/null.zip -d 
        // /home/dev/TEST
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" unzip -q -o ");
        res.append(machineParameters.getInstallDirValue());
        res.append("/");
        res.append(netarchiveSuiteFileName);
        res.append(" -d ");
        res.append(getInstallDirPath());
        res.append("\n");
        // echo copying settings and scripts
        res.append("echo copying settings and scripts");
        res.append("\n");
        // DATABASE!
        res.append(osInstallDatabase());
        // scp -r kb-test-adm-001.kb.dk/* 
        // dev@kb-test-adm-001.kb.dk:/home/dev/TEST/conf/
        res.append("scp -r ");
        res.append(name);
        res.append("/* ");
        res.append(machineUserLogin());
        res.append(":");
        res.append(getConfDirPath());
        res.append("\n");
        // echo make scripts executable
        res.append("echo make scripts executable");
        res.append("\n");
        // ssh dev@kb-test-adm-001.kb.dk "chmod +x /home/dev/TEST/conf/*.sh "
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" \"chmod +x ");
        res.append(getConfDirPath());
        res.append("*.sh \"");
        res.append("\n");
        // echo make password files readonly
        res.append("echo make password files readonly");
        res.append("\n");
        // ssh dev@kb-test-adm-001.kb.dk "chmod 400 
        // /home/dev/TEST/conf/jmxremote.password"
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" \"chmod 400 ");
        res.append(getConfDirPath());
        res.append("jmxremote.password\"");
        res.append("\n");
        // 
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
        res.append(" \". /etc/profile; ");
        res.append(getConfDirPath());
        res.append("killall");
        res.append(scriptExtension);
        res.append("\";");
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
        res.append(" \". /etc/profile; ");
        res.append(getConfDirPath());
        res.append("startall");
        res.append(scriptExtension);
        res.append("; sleep 5; cat ");
        res.append(getInstallDirPath());
        res.append("/*.log\"");
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
        return machineParameters.getInstallDirValue() + "/" 
                + getEnvironmentName();
    }

    /**
     * The operation system specific path to the conf directory.
     * 
     * @return Conf path.
     */
    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + "/conf/";
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
                killPrinter.println("#!/bin/bash");
                killPrinter.println("cd " + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "./kill_"
                            + app.getIdentification() + scriptExtension;
                    // check if file exists
                    killPrinter.println("if [ -e "
                                + appScript + "]; then ");
                    killPrinter.println("      " + appScript);
                    killPrinter.println("fi");
                }
            } finally {
                // close script
                killPrinter.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create local kill all script.");
            throw new IOFailure("Problems creating local kill all script: "
                    + e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in create local kill all script: "
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
                startPrinter.println("#!/bin/bash");
                startPrinter.println("cd " + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "./start_"
                            + app.getIdentification() + scriptExtension;
                    // check if file exists
                    startPrinter.println("if [ -e "
                            + appScript + " ]; then ");
                    startPrinter.println("      " + appScript);
                    startPrinter.println("fi");
                }
            } finally {
                // close script
                startPrinter.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create local start all script.");
            throw new IOFailure("Problems creating local start all script: "
                    + e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in create local start all script: " + e);
        }
    }

    /**
     * Creates the kill scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createApplicationKillScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // go through all applications and create their kill script
        for(Application app : applications) {
            File appKillScript = new File(directory, 
                    "kill_" + app.getIdentification() + scriptExtension);
            try {
                // make print writer for writing to file
                PrintWriter appPrint = new PrintWriter(appKillScript);
                try {
                    // get the content for the kill script of this application
                    appPrint.println("echo KILL LINUX APPLICATION: ");
                    // initialise bash
                    appPrint.println("#!/bin/bash");
                    // Get the process ID for this application
                    appPrint.println("PIDS=$(ps -wwfe | grep "
                            + app.getTotalName() + " | grep -v grep | grep "
                            + getConfDirPath() + "settings_"
                            + app.getIdentification() + ".xml"
                            + " | awk \"{print \\$2}\")");
                    // If the process ID exists, then kill the process
                    appPrint.println("if [ -n \"$PIDS\" ] ; then");
                    appPrint.println("    kill -9 $PIDS");
                    appPrint.println("fi");
                } finally {
                    // close file
                    appPrint.close();
                }
            } catch (IOException e) {
                log.trace("Cannot create application kill script.");
                throw new IOFailure("Problems creating application kill "
                        + "script: " + e);
            } catch(Exception e) {
                // ERROR
                log.trace("Unknown error: " + e);
                System.out.println("Error in creating application kill script: "
                        + e);
            }
        }
    }

    /**
     * Creates the start scripts for all the applications.
     * 
     * @param directory The directory for this machine (use global variable?).
     */
    @Override
    protected void createApplicationStartScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // go through all applications and create their start script
        for(Application app : applications) {
            File appStartScript = new File(directory, 
                    "start_" + app.getIdentification() + scriptExtension);
            try {
                // make print writer for writing to file
                PrintWriter appPrint = new PrintWriter(appStartScript);
                try {
                    // get the content for the start script of this application
                    appPrint.println("echo START LINUX APPLICATION: "
                            + app.getIdentification());
                            appPrint.println("#!/bin/bash");
                    // apply class path
                    appPrint.println("export CLASSPATH="
                            + osGetClassPath(app)
                            + "$CLASSPATH;");
                    // move to directory
                    appPrint.println("cd "
                            + app.installPathLinux());
                    // Run the java program
                    appPrint.println(
                            "java "
                            + app.getMachineParameters().writeJavaOptions()
                            + " -Ddk.netarkivet.settings.file="
                            + getConfDirPath() + "settings_"
                            + app.getIdentification() + ".xml"
                            + " -Dorg.apache.commons.logging.Log="
                            + "org.apache.commons.logging.impl.Jdk14Logger"
                            + " -Djava.util.logging.config.file="
                            + getConfDirPath() + "log_"
                            + app.getIdentification() + ".prop"
                            + " -Djava.security.manager"
                            + " -Djava.security.policy="
                            + getConfDirPath() + "security.policy "
                            + app.getTotalName() + " < /dev/null > "
                            + "start_" + app.getIdentification() + ".sh.log"
                            + " 2>&1 &");
                } finally {
                    // close file
                    appPrint.close();
                }
            } catch (IOException e) {
                log.trace("Cannot create application kill script.");
                throw new IOFailure("Problems creating application start"
                        + "script: " + e);
            } catch(Exception e) {
                // ERROR
                log.trace("Unknown error: " + e);
                System.out.println("Error in creating application start"
                        + "script: " + e);
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
        StringBuilder res = new StringBuilder("");
        // get all the classpaths
        for(Element cp : app.getMachineParameters().getClassPaths()) {
            res.append(getInstallDirPath() + "/" + cp.getText() + ":");
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
        if(databaseDir == null || databaseDir == "") {
            return "";
        }

        // copy to final destination if database argument.
        if(databaseFileName != null) {
            // echo Copying database
            res.append("echo Copying database" + "\n");
            // scp database.jar user@machine:dbDir/db
            res.append("scp ");
            res.append(databaseFileName);
            res.append(" ");
            res.append(machineUserLogin());
            res.append(":");
            res.append(getInstallDirPath());
            res.append("/");
            res.append(Constants.DATABASE_BASE_PATH);
            res.append("\n");
        }
        // unzip database.
        res.append("echo Unzipping database" + "\n");
        // ssh user@machine "
        // cd dir; if [ -d databaseDir ]; then echo ; 
        // else mkdir databaseDir; fi; if [ $(ls -A databaseDir) ]; 
        // then echo ERROR MESSAGE: DIR NOT EMPTY; 
        // else unzip -q -o dbDir/db -d databaseDir/.; fi; exit;
        // "
        res.append("ssh ");
        res.append(machineUserLogin());
        res.append(" \"cd ");
        res.append(getInstallDirPath());
        res.append("; if [ -d ");
        res.append(databaseDir);
        res.append(" ]; then echo ; else mkdir ");
        res.append(databaseDir);
        res.append("; fi; if [ $(ls -A ");
        res.append(databaseDir);
        res.append(") ]; then echo ");
        res.append(Constants.DATABASE_ERROR_PROMPT_DIR_NOT_EMPTY);
        res.append("; else unzip -q -o ");
        res.append(Constants.DATABASE_BASE_PATH);
        res.append(" -d ");
        res.append(databaseDir);
        res.append("/.; fi; exit; \"");
        res.append("\n");

//         System.out.println("Install database: ");
//        System.out.println(res);
        return res.toString();
    }
}
