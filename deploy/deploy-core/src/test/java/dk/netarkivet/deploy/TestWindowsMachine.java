/*
 * #%L
 * Netarchivesuite - deploy
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.deploy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.IOFailure;

public class TestWindowsMachine extends WindowsMachine {

    public TestWindowsMachine(Element root, XmlStructure parentSettings, Parameters param,
            String netarchiveSuiteSource, File logProp, File slf4JConfig, File securityPolicy, File dbFile,
            File arcdbFile, boolean resetDir, File externalJarFolder) {
        super(root, parentSettings, param, netarchiveSuiteSource, slf4JConfig, securityPolicy, dbFile,
                arcdbFile, resetDir, externalJarFolder, null, null);
    }

    /**
     * This function creates the VBscript to start the application. It calls a command for executing the java
     * application, then it writes the way to kill the process in the kill_ps_app.bat and finally it creates the
     * run-file.
     * <p>
     * It should have the following content: - set WshShell = CreateObject("WScript.Shell") - set oExec = WshShell.exec(
     * JAVA ) - set fso = CreateObject("Scripting.FileSystemObject") - set f =
     * fso.OpenTextFile(".\conf\kill_ps_app.bat", 2, True) - f.WriteLine "taskkill /F /PID " & oExec.ProcessID - f.close
     * - 'Create a new start-log for the application - CreateObject("Scripting.FileSystemObject").OpenTextFile("
     * start_APP.log", 2, True).close - Do While oExec.Status = 0 - WScript.Sleep 1000 - Do While
     * oExec.StdOut.AtEndOfStream <> True - Set outFile = CreateObject("Scripting.FileSystemObject")
     * .OpenTextFile("start_APP.log", 8, True) - outFile.WriteLine oExec.StdOut.ReadLine - outFile.close - Loop - Do
     * While oExec.StdErr.AtEndOfStream <> True - Set outFile = CreateObject("Scripting.FileSystemObject")
     * .OpenTextFile("start_APP.log", 8, True) - outFile.WriteLine oExec.StdErr.ReadLine - outFile.close - Loop - Loop
     * <p>
     * where: JAVA = the command for starting the java application (very long). app = the name of the application.
     *
     * @param app The application to start.
     * @param directory The directory where the script should be placed.
     * @throws IOFailure If an error occurred during the creation of the windows vb script.
     */
    protected void windowsStartVbsScript(Application app, File directory) throws IOFailure {
        File appStartSupportScript = new File(directory, Constants.SCRIPT_NAME_LOCAL_START + app.getIdentification()
                + Constants.EXTENSION_VBS_FILES);
        try {
            // make print writer for writing to file
            PrintWriter vbsPrint = new PrintWriter(appStartSupportScript, getTargetEncoding());
            try {
                // initiate variables
                String id = app.getIdentification();
                String killPsName = Constants.SCRIPT_KILL_PS + id + scriptExtension;
                String tmpRunPsName = Constants.FILE_TEMPORARY_RUN_WINDOWS_NAME + id;
                String startLogName = Constants.SCRIPT_NAME_LOCAL_START + id + Constants.EXTENSION_LOG_FILES;

                // Set WshShell = CreateObject("WScript.Shell")
                vbsPrint.println(ScriptConstants.VB_CREATE_SHELL_OBJ);
                // Set oExec = WshShell.exec( "JAVA" )
                vbsPrint.println(ScriptConstants.VB_CREATE_EXECUTE + ScriptConstants.JAVA + Constants.SPACE
                        + app.getMachineParameters().writeJavaOptions() + Constants.SPACE
                        + Constants.DASH
                        + ScriptConstants.CLASSPATH
                        + Constants.SPACE
                        + Constants.QUOTE_MARK
                        + Constants.QUOTE_MARK
                        + osGetClassPath(app)
                        + Constants.QUOTE_MARK
                        + Constants.QUOTE_MARK
                        + Constants.SPACE
                        + Constants.DASH
                        + ScriptConstants.OPTION_SETTINGS_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath())
                        + Constants.SETTINGS_PREFIX
                        + id
                        + Constants.EXTENSION_XML_FILES
                        + Constants.QUOTE_MARK
                        + Constants.QUOTE_MARK

                        // TODO check to see if inheritedSlf4jConfigFile is not null
                        + Constants.SPACE + Constants.DASH + ScriptConstants.OPTION_LOGBACK_CONFIG_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath()) + Constants.LOGBACK_PREFIX + id
                        + Constants.EXTENSION_XML_FILES + Constants.QUOTE_MARK + Constants.QUOTE_MARK

                        + Constants.SPACE + Constants.DASH + ScriptConstants.OPTION_SECURITY_MANAGER + Constants.SPACE
                        + Constants.DASH + ScriptConstants.OPTION_SECURITY_POLICY_WIN
                        + ScriptConstants.doubleBackslashes(getConfDirPath()) + Constants.SECURITY_POLICY_FILE_NAME
                        + Constants.QUOTE_MARK + Constants.QUOTE_MARK + Constants.SPACE + app.getTotalName()
                        + Constants.QUOTE_MARK + Constants.BRACKET_END);
                // Set fso = CreateObject("Scripting.FileSystemObject")
                vbsPrint.println(ScriptConstants.VB_CREATE_FSO);
                // set f = fso.OpenTextFile(".\conf\kill_ps_app.bat", 2, True)
                vbsPrint.println(ScriptConstants.VB_WRITE_F_PREFIX + killPsName + ScriptConstants.VB_WRITE_F_SURFIX);
                // f.WriteLine "taskkill /F /PID " & oExec.ProcessID
                vbsPrint.println(ScriptConstants.VB_WRITE_F_KILL);
                // f.close
                vbsPrint.println(ScriptConstants.VB_WRITE_F_CLOSE);
                // set tf = fso.OpenTextFile(".\conf\run_app.txt", 2, True)
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_PREFIX + tmpRunPsName + ScriptConstants.VB_WRITE_TF_SURFIX);
                // tf.WriteLine running
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_CONTENT);
                // f.close
                vbsPrint.println(ScriptConstants.VB_WRITE_TF_CLOSE);

                // 'Create a new start-log for the application
                vbsPrint.println(ScriptConstants.VB_COMMENT_NEW_START_LOG);
                // CreateObject("Scripting.FileSystemObject").OpenTextFile(
                // "start_APP.log", 2, True).close
                vbsPrint.println(ScriptConstants.VB_OPEN_WRITE_FILE_PREFIX + startLogName
                        + ScriptConstants.VB_OPEN_WRITE_FILE_SUFFIX_2 + ScriptConstants.VB_CLOSE);
                // Do While oExec.Status = 0
                vbsPrint.println(ScriptConstants.VB_DO_WHILE_OEXEC_STATUS_0);
                // WScript.Sleep 1000
                vbsPrint.println(ScriptConstants.MULTI_SPACE_2 + ScriptConstants.VB_WSCRIPT_SLEEP_1000);
                // Do While oExec.StdOut.AtEndOfStream <> True
                vbsPrint.println(ScriptConstants.MULTI_SPACE_2 + ScriptConstants.VB_DO_WHILE
                        + ScriptConstants.VB_OEXEC_STD_OUT + ScriptConstants.VB_AT_END_OF_STREAM_FALSE);
                // Set outFile = CreateObject("Scripting.FileSystemObject")
                // .OpenTextFile("start_APP.log", 8, True)
                vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_SET_OUTFILE
                        + ScriptConstants.VB_OPEN_WRITE_FILE_PREFIX + startLogName
                        + ScriptConstants.VB_OPEN_WRITE_FILE_SUFFIX_8);
                // outFile.WriteLine oExec.StdOut.ReadLine
                vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_OUTFILE_WRITELINE
                        + ScriptConstants.VB_OEXEC_STD_OUT + ScriptConstants.VB_READ_LINE);
                // outFile.close
                vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_OUTFILE_CLOSE);
                // Loop
                vbsPrint.println(ScriptConstants.MULTI_SPACE_2 + ScriptConstants.VB_LOOP);
                // Removing the bit that writes to stderr
                /*
                 * // Do While oExec.StdErr.AtEndOfStream <> True vbsPrint.println(ScriptConstants.MULTI_SPACE_2 +
                 * ScriptConstants.VB_DO_WHILE + ScriptConstants.VB_OEXEC_STD_ERR +
                 * ScriptConstants.VB_AT_END_OF_STREAM_FALSE); // Set outFile =
                 * CreateObject("Scripting.FileSystemObject") // .OpenTextFile("start_APP.log", 8, True)
                 * vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_SET_OUTFILE +
                 * ScriptConstants.VB_OPEN_WRITE_FILE_PREFIX + startLogName +
                 * ScriptConstants.VB_OPEN_WRITE_FILE_SUFFIX_8); // outFile.WriteLine oExec.StdErr.ReadLine
                 * vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_OUTFILE_WRITELINE +
                 * ScriptConstants.VB_OEXEC_STD_ERR + ScriptConstants.VB_READ_LINE); // outFile.close
                 * vbsPrint.println(ScriptConstants.MULTI_SPACE_4 + ScriptConstants.VB_OUTFILE_CLOSE); // Loop
                 * vbsPrint.println(ScriptConstants.MULTI_SPACE_2 + ScriptConstants.VB_LOOP);
                 */
                // Loop
                vbsPrint.println(ScriptConstants.VB_LOOP);
            } finally {
                // close file
                vbsPrint.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create the start script for application: " + app.getIdentification()
                    + ", at machine: '" + hostname + "'";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

}
