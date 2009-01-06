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
 * A WindowsMachine is the instance of the abstract machine class, which runs 
 * the operating system Windows.
 * This class only contains the operating system specific functions.
 */
public class WindowsMachine extends Machine {
    public WindowsMachine(Element e, XmlStructure parentSettings, 
            Parameters param, String netarchiveSuiteSource,
                File logProp, File securityPolicy) {
        super(e, parentSettings, param, netarchiveSuiteSource,
                logProp, securityPolicy);
        // set operating system
        OS = "windows";
        scriptExtension = ".bat";
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
        res += " cmd /c unzip.exe -q -d ";
        res += getEnvironmentName();
        res += " -o ";
        res += netarchiveSuiteFileName;
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
        res += ":\"\"";
        res += changeToScriptPath(getConfDirPath());
        res += "\"\"";
        res += "\n";
        // pw.println("echo make scripts executable");
        res += "echo make scripts executable";
        res += "\n";
        // if (!isWindows) {
        // 		pw.println("ssh  " + destination + " \"chmod +x "
        //					+ confDir + "*.sh \"");
        // }
        res += "";
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
        res += "echo Y | ssh ";
        res += machineUserLogin();
        res += " cmd /c cacls \"\"";
        res += changeToScriptPath(getConfDirPath());
        res += "jmxremote.password\"\" /P BITARKIV\\\\";
        res += machineParameters.machineUserName.getText();
        res += ":R";
        res += "\n";
        // end
        return res;
    }

    @Override
    protected String osKillScript() {
        String res = "";
        res += "ssh ";
        res += machineUserLogin();
        res += " \"cmd /c  ";
        res += getConfDirPath();
        res += "killall";
        res += scriptExtension;
        res += " \" ";
        return res + "\n";
    }

    @Override
    protected String osStartScript() {
        String res = "";
        res += "ssh ";
        res += machineUserLogin();
        res += " \"cmd /c  ";
        res += getConfDirPath();
        res += "startall";
        res += scriptExtension;
        res += " \" ";
        return res + "\n";
    }

    @Override
    protected String getInstallDirPath() {
        return machineParameters.installDir.getText() + "\\" 
                + getEnvironmentName();
    }

    @Override
    protected String getConfDirPath() {
        return getInstallDirPath() + "\\conf\\";
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
                killPrinter.println("cd \"" + getConfDirPath() + "\"");
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "kill_" + 
                    app.getIdentification() + scriptExtension;
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
                startPrinter.println("cd \"" + getConfDirPath() + "\"");
                // insert path to kill script for all applications
                for(Application app : applications) {
                    // make name of file
                    String appScript = "start_" + 
                    app.getIdentification() + scriptExtension;
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
                        // get the content for the kill script of 
                        // this application
                        appPrint.println("echo KILL WINDOWS APPLICATION: ");
                    } finally {
                        // close file
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
            System.out.println("Error in creating kill script for " +
                        "applications: " + e);
        }
    }

    @Override
    protected void createApplicationStartScripts(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        try {
            // go through all applications and create their start script
            for(Application app : applications) {
                File appStartScript = new File(directory, 
                        "start_" + app.getIdentification() + scriptExtension);
                File appStartSupportScript = new File(directory,
                        "start_" + app.getIdentification() + ".vbs");
                try {
                    // make print writer for writing to file
                    PrintWriter appPrint = new PrintWriter(appStartScript);
                    PrintWriter vbsPrint = new PrintWriter(
                            appStartSupportScript);
                    try {
                        // get the content for the start script of 
                        // this application
                        appPrint.println("#echo START WINDOWS APPLICATION: " +
                                app.getIdentification());
                        appPrint.println("cd \"" +
                                app.installPathWindows() + "\"");
                        appPrint.println("cscript .\\conf\\" + 
                                "start_" + app.getIdentification() + ".vbs");
                        vbsPrint.println("Set WshShell= " +
                        "CreateObject(\"WScript.Shell\")");
                        vbsPrint.println(
                                "Set oExec = WshShell.exec( \"" +
                                "java " + app.getMachineParameters()
                                .writeJavaOptions() + 
                                " -classpath \"\"" + 
                                osGetClassPath(app) + "\"\"" + 
                                " -Ddk.netarkivet.settings.file=\"\"" +
                                changeToScriptPath(getConfDirPath()) + 
                                "settings_" + app.getIdentification() + 
                                ".xml\"\"" +
                                " -Dorg.apache.commons.logging.Log="+
                                "org.apache.commons.logging.impl.Jdk14Logger" +
                                " -Djava.util.logging.config.file=\"\"" +
                                changeToScriptPath(getConfDirPath()) + "log_" 
                                + app.getIdentification() + ".prop\"\"" +
                                " -Djava.security.manager" +
                                " -Djava.security.policy=\"\"" +
                                changeToScriptPath(getConfDirPath()) + 
                                "security.policy\"\" " + 
                                app.getTotalName() + "\")" );
                        vbsPrint.println("set fso= " +
                        "CreateObject(\"Scripting.FileSystemObject\")");
                        // open the kill batch file for this application
                        vbsPrint.println("set f=fso.OpenTextFile(\".\\conf\\" +
                                "kill_" + app.getIdentification() +
                        ".bat\",2,True)");
                        // write the process in the kill batch file for 
                        // this application
                        vbsPrint.println("f.WriteLine \"taskkill /F /PID \"" +
                                        " & oExec.ProcessID");
                        // close the kill batch file
                        vbsPrint.println("f.close");
                    } finally {
                        // close files
                        appPrint.close();
                        vbsPrint.close();
                    }
                } catch (IOException e) {
                    log.trace("Cannot create the start script for application: "
                            + app.getIdentification() + ", at machine: " + 
                            name);
                    throw new IOFailure("Problems creating local " +
                                "start all script: " + e);
                } 
            }
        } catch(Exception e) {
            // ERROR
            log.trace("Unknown error: " + e);
            System.out.println("Error in creating start script " +
                        "for applications: " + e);
        }
    }

    @Override
    protected String osGetClassPath(Application app) {
        ArgumentNotValid.checkNotNull(app, "Application app");
        String res = "";
        // get all the classpaths (change from '\' to '\\')
        for(Element cp : app.getMachineParameters().classPaths) {
            // insert the path to the install directory
            res += getInstallDirPath().replaceAll("[\\\\]", "\\\\\\\\");
            res += "\\\\";
            // Then insert the class path. 
            res += cp.getText().replaceAll("[/]", "\\\\\\\\");
            res += ";";
        }
        return res;
    }

    /** 
     * The '.vbs' script needs '\\' instead of '\', which is quite annoying 
     * when using regular expressions, since a final '\' in regular expressions
     *  is '\\\\', thus '\\' = '\\\\\\\\' (8)
     *   
     * @param path The directory path to change to appropriate format
     * @return The formatted path
     */
    private String changeToScriptPath(String path) {
        ArgumentNotValid.checkNotNull(path, "String path");
        String res = path;
        res = res.replaceAll("\\\\", "\\\\\\\\");
        return res;
    }
}
