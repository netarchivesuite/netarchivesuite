/* $Id: Deploy.java 470 2008-08-20 16:08:30Z svc $
 * $Revision: 470 $
 * $Date: 2008-08-20 18:08:30 +0200 (Wed, 20 Aug 2008) $
 * $Author: svc $
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
     */
    public LinuxMachine(Element e, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
            File logProp, File securityPolicy) {
        super(e, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy);
        // set operating system
        OS = "linux";
        scriptExtension = ".sh";
    }

    @Override
    protected String osInstallScript() {
        String res = "";
        // pw.println("echo copying $1 to:" + host.getName());
        res += "echo copying ";
        res += netarchiveSuiteFileName;
        res += " to:";
        res += name;
        res += "\n";
        // pw.println("scp $1 " + destination + ":" + dir);
        res += "scp ";
        res += netarchiveSuiteFileName;
        res += " ";
        res += machineUserLogin();
        res += ":";
        res += machineParameters.installDir.getText();
        res += "\n";
        // pw.println("echo unzipping $1 at:" + host.getName());
        res += "echo unzipping ";
        res += netarchiveSuiteFileName;
        res += " at:";
        res += name;
        res += "\n";
        // pw.println("ssh " + destination + " " + unzip);
        res += "ssh ";
        res += machineUserLogin();
        res += " unzip -q -o ";
        res += machineParameters.installDir.getText();
        res += "/";
        res += netarchiveSuiteFileName;
        res += " -d ";
        res += getInstallDirPath();
        res += "\n";
        // pw.println("echo copying settings and scripts");
        res += "echo copying settings and scripts";
        res += "\n";
        // pw.println("scp -r " + host.getName() + "/* "
        // 			+ destination + ":" + confDir);
        res += "scp -r ";
        res += name;
        res += "/* ";
        res += machineUserLogin();
        res += ":";
        res += getConfDirPath();
        res += "\n";
        // pw.println("echo make scripts executable");
        res += "echo make scripts executable";
        res += "\n";
        // if (!isWindows) {
        // 		pw.println("ssh  " + destination + " \"chmod +x "
        //					+ confDir + "*.sh \"");
        // }
        res += "ssh ";
        res += machineUserLogin();
        res += " \"chmod +x ";
        res += getConfDirPath();
        res += "*.sh \"";
        res += "\n";
        // pw.println("echo make password files readonly");
        res += "echo make password files readonly";
        res += "\n";
        // 	if (isWindows) {
        //		pw.println("echo Y | ssh " + destination
        //		            + " cmd /c cacls " + confDir
        //		            + "jmxremote.password /P BITARKIV\\\\"
        //		            + user + ":R");
        //	} else {
        //		pw.println("ssh " + destination + " \"chmod 400 "
        //		            + confDir + "/jmxremote.password\"");
        //	}
        res += "ssh ";
        res += machineUserLogin();
        res += " \"chmod 400 ";
        res += getConfDirPath();
        res += "jmxremote.password\"";
        res += "\n";
        // 
        return res;
    }

    @Override
    protected String osKillScript() {
        String res = "";
        res += "ssh ";
        res += machineUserLogin();
        res += " \". /etc/profile; ";
        res += getConfDirPath();
        res += "killall";
        res += scriptExtension;
        res += "\";";
        return res + "\n";
    }

    @Override
    protected String osStartScript() {
        String res = "";
        res += "ssh ";
        res += machineUserLogin();
        res += " \". /etc/profile; ";
        res += getConfDirPath();
        res += "startall";
        res += scriptExtension;
        res += "; sleep 5; cat ";
        res += getInstallDirPath();
        res += "/*.log\"";
        return res + "\n";
    }

    @Override
    protected String getInstallDirPath() {
        return machineParameters.installDir.getText() + "/" 
                + getEnvironmentName();
    }

    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + "/conf/";
    }

    @Override
    protected void createOSLocalKillAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the kill all script file
        File KillAllScript = new File(directory, "killall" + scriptExtension);
        try {
            // Initialise script
            PrintWriter killPrinter = new PrintWriter(KillAllScript);
            try {
                killPrinter.println("echo Killing all applications at: " 
                        + name);
                killPrinter.println("#!/bin/bash");
                killPrinter.println("cd " + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "./kill_" + 
                            app.getIdentification() + scriptExtension;
                    // check if file exists
                    killPrinter.println("if [ -e " +
                                appScript + "]; then ");
                    killPrinter.println("      " + appScript);
                    killPrinter.println("fi");
                }
            } finally {
                // close script
                killPrinter.close();
            }
        } catch (IOException e) {
            log.trace("Cannot create local kill all script.");
            throw new IOFailure("Problems creating local kill all script: " + 
                    e);
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in create local kill all script: "
                    + e);
        }
    }

    @Override
    protected void createOSLocalStartAllScript(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // create the start all script file
        File StartAllScript = new File(directory, "startall" + 
                scriptExtension);
        try {
            // Initialise script
            PrintWriter startPrinter = new PrintWriter(StartAllScript);
        try {
                startPrinter.println("echo Starting all applications at: " 
                        + name);
                startPrinter.println("#!/bin/bash");
                startPrinter.println("cd " + getConfDirPath());
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "./start_" + 
                    app.getIdentification() + scriptExtension;
                    // check if file exists
                    startPrinter.println("if [ -e " +
                            appScript + " ]; then ");
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
                    appPrint.println("PIDS=$(ps -wwfe | grep " +
                            app.getTotalName() + " | grep -v grep | grep " +
                            getConfDirPath() + "settings_" + 
                            app.getIdentification() + ".xml" +
                    " | awk \"{print \\$2}\")");
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
                throw new IOFailure("Problems creating application kill " +
                        "script: " + e);
            } catch(Exception e) {
                // ERROR
                log.trace("Unknown error: " + e);
                System.out.println("Error in creating application kill script: "
                        + e);
            }
        }
    }

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
                    appPrint.println("echo START LINUX APPLICATION: "+
                            app.getIdentification());
                            appPrint.println("#!/bin/bash");
                    // apply class path
                    appPrint.println("export CLASSPATH=" +
                            osGetClassPath(app) +
                    "$CLASSPATH;");
                    // move to directory
                    appPrint.println("cd "
                            + app.installPathLinux());
                    // Run the java program
                    appPrint.println(
                            "java " +
                            app.getMachineParameters().writeJavaOptions() +
                            " -Ddk.netarkivet.settings.file=" +
                            getConfDirPath() + "settings_" +
                            app.getIdentification() + ".xml" +
                            " -Dorg.apache.commons.logging.Log="+
                            "org.apache.commons.logging.impl.Jdk14Logger" +
                            " -Djava.util.logging.config.file=" +
                            getConfDirPath() + "log_" + 
                            app.getIdentification() + ".prop" +
                            " -Djava.security.manager" +
                            " -Djava.security.policy=" +
                            getConfDirPath() + "security.policy " +
                            app.getTotalName() + " < /dev/null > " +
                            "start_" + app.getIdentification() + ".sh.log" +
                            " 2>&1 &" );
                } finally {
                    // close file
                    appPrint.close();
                }
            } catch (IOException e) {
                log.trace("Cannot create application kill script.");
                throw new IOFailure("Problems creating application start" +
                        "script: " + e);
            } catch(Exception e) {
                // ERROR
                log.trace("Unknown error: " + e);
                System.out.println("Error in creating application start" +
                        "script: " + e);
            }
        }
    }

    @Override
    protected String osGetClassPath(Application app) {
        ArgumentNotValid.checkNotNull(app, "Application app");
        String res = "";
        // get all the classpaths
        for(Element cp : app.getMachineParameters().classPaths) {
            res += getInstallDirPath() + "/" + cp.getText() + ":";
        }
        return res;
    }
}
