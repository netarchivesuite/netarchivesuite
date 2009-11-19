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

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StringUtils;

/**
 * This class contains constants and functions specific for creating the
 * scripts and other files for the different machines and applications. 
 */
public final class ScriptConstants {
    /**
     * Private constructor to avoid instantiation of this class.
     */
    private ScriptConstants() {}
    
    // Character constants as Strings.
    /** The newline '\n' - acquired from Constants. */
    static final String NEWLINE = Constants.NEWLINE;
    /** The directory separator for policy files.*/
    static final String SECURITY_DIR_SEPARATOR = "${/}";
    
    // Strings
    /** The header of some scripts.*/
    static final String BIN_BASH_COMMENT = "#!/bin/bash";
    /** The call for running a batch script from another batch script.*/
    static final String OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE = 
        "\"C:\\Program Files\\Bitvise WinSSHD\\bvRun\" -brj -new -cmd=";
    /** Ddk.netarkivet.settings.file=.*/
    static final String OPTION_SETTINGS = 
        "Ddk.netarkivet.settings.file=";
    /** Ddk.netarkivet.settings.file=\"\".*/
    static final String OPTION_SETTINGS_WIN = 
        OPTION_SETTINGS + "\"\"";
    /** Dorg.apache.commons.logging.Log="
                        + "org.apache.commons.logging.impl.Jdk14Logger.*/
    static final String OPTION_LOG_COMPLETE = "Dorg.apache.commons.logging.Log="
        + "org.apache.commons.logging.impl.Jdk14Logger";
    /** Djava.util.logging.config.file=.*/
    static final String OPTION_LOG_CONFIG = 
        "Djava.util.logging.config.file=";
    /** Djava.util.logging.config.file=\"\".*/
    static final String OPTION_LOG_CONFIG_WIN = 
        OPTION_LOG_CONFIG + "\"\"";
    /** Djava.security.manager.*/
    static final String OPTION_SECURITY_MANAGER = "Djava.security.manager";
    /** Djava.security.policy=.*/
    static final String OPTION_SECIRITY_POLICY = "Djava.security.policy=";
    /** Djava.security.policy=\"\".*/
    static final String OPTION_SECIRITY_POLICY_WIN = OPTION_SECIRITY_POLICY 
            + "\"\"";

    /** The message when database is trying to overwrite a non-empty dir.*/
    static final String DATABASE_ERROR_PROMPT_DIR_NOT_EMPTY = 
        "The database directory already exists. Thus database not reset.";

    /** cmd /c - Command for running programs on windows.*/
    static final String WINDOWS_COMMAND_RUN = "cmd /c";
    /** cmd /c unzip.exe -q -d - Command for unzipping on windows.*/
    static final String WINDOWS_UNZIP_COMMAND = WINDOWS_COMMAND_RUN 
            + " unzip.exe -q -d";
    /** output. -o.*/
    static final String SCRIPT_OUTPUT = "-o";
    /** directory. -d.*/
    static final String SCRIPT_DIR = "-d";
    /** repository. -r.*/
    static final String SCRIPT_REPOSITORY = "-r";
    /** unzip -q -o.*/
    static final String LINUX_UNZIP_COMMAND = "unzip -q -o";
    
    /** Linux chmod u+rwx.*/
    static final String LINUX_USER_ONLY = "chmod u+rwx";
    /** Linux chmod 700.*/
    static final String LINUX_USER_700 = "chmod 700"; 
    /** Linux chmod 400.*/
    static final String LINUX_USER_400 = "chmod 400";
    /** Linux sent output to dev/null.*/
    static final String LINUX_DEV_NULL = "< /dev/null >";
    /** 2>&1 &.*/
    static final String LINUX_ERROR_MESSAGE_TO_1 = "2>&1 &"; 
    /** /etc/profile.*/
    static final String ECT_PROFILE = "/etc/profile";
    /** The linux command for sleeping. sleep.*/
    static final String SLEEP = "sleep";
    /** sleep 5.*/
    static final String SLEEP_5 = SLEEP + " 5";
    /** *.log.*/
    static final String STAR_LOG = "*.log";
    /** '      '.*/
    static final String MULTI_SPACE = "      ";
    /** '    '.*/
    static final String MULTI_SPACE_2 = "    ";
    /** ssh.*/
    static final String SSH = "ssh";
    /** scp.*/
    static final String SCP = "scp";
    /** $PIDS.*/
    static final String PIDS = "$PIDS";
    /** "    kill -9 $PIDS".*/
    static final String KILL_9_PIDS = "    kill -9 $PIDS";
    /** "    export CLASSPATH=".*/
    static final String EXPORT_CLASSPATH = "    export CLASSPATH=";
    /** to.*/
    static final String TO = "to";
    /** if.*/
    static final String IF = "if";
    /** fi.*/
    static final String FI = "fi";
    /** at.*/
    static final String AT = "at";
    /** cd.*/
    static final String CD = "cd";
    /** cat.*/
    static final String CAT = "cat"; 
    /** exist.*/
    static final String EXIST = "exist";
    /** exit.*/
    static final String EXIT = "exit";
    /** then.*/
    static final String THEN = "then";
    /** cacls.*/
    static final String CACLS = "cacls";
    /** cscript.*/
    static final String CSCRIPT = "cscript";
    /** goto.*/
    static final String GOTO = "goto";
    /** else.*/
    static final String ELSE = "else";
    /** else rm -r.*/
    static final String ELSE_REMOVE = "else rm -r";
    /** del.*/
    static final String DEL = "del";
    /** cd ~.*/
    static final String LINUX_HOME_DIR = "cd ~";
    /** if [ -e.*/
    static final String LINUX_IF_EXIST = "if [ -e";
    /** if [ -d.*/
    static final String LINUX_IF_DIR_EXIST = "if [ -d";
    /** if [ ! -d.*/
    static final String LINUX_IF_NOT_DIR_EXIST = "if [ ! -d";
    /** if [ -n.*/
    static final String LINUX_IF_N_EXIST = "if [ -n";
    /** ]; then.*/
    static final String LINUX_THEN = "]; then";
    /** ] ; then.*/
    static final String LINUX_N_THEN = "] ; then";
    /** java.*/
    static final String JAVA = "java";
    /** rd. (windows for remove dir).*/
    static final String RD = "rd";
    /** not.*/
    static final String NOT = "not";
    /** md. (windows for makedir).*/
    static final String MD = "md";
    /** mkdir. (linux for makedir).*/
    static final String MKDIR = "mkdir";
    /** mv -f. (Linux force move of file).*/
    static final String LINUX_FORCE_MOVE = "mv -f";
    /** move /Y. (force move on windows).*/
    static final String WINDOWS_FORCE_MOVE = "move /Y";
    /** classpath.*/
    static final String CLASSPATH = "classpath";
    /** $CLASSPATH.*/
    static final String VALUE_OF_CLASSPATH = "$CLASSPATH";
    /** label KILL.*/
    static final String LABEL_KILL = "KILL";
    /** label NOKILL.*/
    static final String LABEL_NOKILL = "NOKILL";
    /** label DONE.*/
    static final String LABEL_DONE = "DONE";
    /** label START.*/
    static final String LABEL_START = "START";
    /** label NOSTART.*/
    static final String LABEL_NOSTART = "NOSTART";
    /** /P - slash p. */
    static final String SLASH_P = "/P";
    /** :F - colon f.*/
    static final String COLON_F = ":F";
    /** :R - colon r.*/
    static final String COLON_R = ":R";
    /** -r - dash r.*/
    static final String DASH_R = "-r";
    /** BITARKIV\\\\ - prefix for windows user rights.*/
    static final String BITARKIV_BACKSLASH_BACKSLASH = "BITARKIV\\\\";
    /** readonly - for the monitorRole.*/
    static final String JMXREMOTE_MONITOR_PRIVILEGES = "readonly";
    /** readonly - for the controlRole.*/
    static final String JMXREMOTE_HERITRIX_PRIVELEGES = "readwrite";
    
    
    // echos
    /** echo.*/
    static final String ECHO = "echo";
    /** echo copying.*/
    static final String ECHO_COPYING = "echo copying";
    /** echo unzipping.*/
    static final String ECHO_UNZIPPING = "echo unzipping";
    /** echo preparing for copying of settings and scripts.*/
    static final String ECHO_PREPARING_FOR_COPY = 
        "echo preparing for copying of settings and scripts";
    /** echo 1.*/
    static final String ECHO_ONE = "echo 1";
    /** echo Y.*/
    static final String ECHO_Y = "echo Y"; 
    /** echo copying settings and scripts.*/
    static final String ECHO_COPY_SETTINGS_AND_SCRIPTS =
        "echo copying settings and scripts";
    /** echo make password files readonly.*/
    static final String ECHO_MAKE_PASSWORD_FILES =
        "echo make password and access files readonly";
    /** echo Killing all applications on.*/
    static final String ECHO_KILL_ALL_APPS = "echo Killing all applications on";
    /** echo Starting all applications on.*/
    static final String ECHO_START_ALL_APPS = 
        "echo Starting all applications on";
    /** ECHO Killing windows application.*/
    static final String ECHO_KILL_WINDOWS_APPLICATION = 
        "ECHO Killing windows application";
    /** echo Killing linux application.*/
    static final String ECHO_KILL_LINUX_APPLICATION = 
        "echo Killing linux application";
    /** ECHO Cannot kill application. Is not running.*/
    static final String ECHO_CANNOT_KILL_APP = 
        "ECHO Cannot kill application. Is not running.";
    /** echo Cannot start. Application already running.*/
    static final String ECHO_CANNOT_START_APP = 
        "echo Cannot start. Application already running.";
    /** echo Database not implemented for windows.*/
    static final String ECHO_WINDOWS_DATABASE = "echo Database not implemented "
        + "for windows.";
    /** echo Creating directories.*/
    static final String ECHO_CREATING_DIRECTORIES = 
        "echo Creating directories.";
    /** echo make scripts executable.*/
    static final String ECHO_MAKE_EXECUTABLE = "echo make scripts executable";
    /** echo Starting linux application.*/
    static final String ECHO_START_LINUX_APP = 
        "echo Starting linux application";
    /** "    echo Application already running.".*/
    static final String ECHO_APP_ALREADY_RUNNING = 
        "    echo Application already running.";
    /** echo Copying database.*/
    static final String ECHO_COPYING_DATABASE = "echo Copying database";
    /** echo Unzipping database.*/
    static final String ECHO_UNZIPPING_DATABASE = "echo Unzipping database";
    
    // VB script
    /** Set WshShell= CreateObject(\"WScript.Shell\").*/
    static final String VB_CREATE_SHELL_OBJ = 
        "Set WshShell= CreateObject(\"WScript.Shell\")";
    /** Set oExec = WshShell.exec( \".*/
    static final String VB_CREATE_EXECUTE = "Set oExec = WshShell.exec( \"";
    /** "set fso= CreateObject(\"Scripting.FileSystemObject\")".*/
    static final String VB_CREATE_FSO = "set fso= "
        + "CreateObject(\"Scripting.FileSystemObject\")";
    /** "set f=fso.OpenTextFile(\".\\conf\\".*/
    static final String VB_WRITE_F_PREFIX = 
        "set f=fso.OpenTextFile(\".\\conf\\";
    /** "\",2,True)".*/
    static final String VB_WRITE_F_SURFIX = "\",2,True)";
    /** "f.WriteLine \"taskkill /F /PID \" & oExec.ProcessID".*/
    static final String VB_WRITE_F_KILL = "f.WriteLine \"taskkill /F /PID \""
        + " & oExec.ProcessID";
    /** f.close.*/
    static final String VB_WRITE_F_CLOSE = "f.close";
    /** "set f=fso.OpenTextFile(\".\\conf\\".*/
    static final String VB_WRITE_TF_PREFIX = 
        "set tf=fso.OpenTextFile(\".\\conf\\";
    /** "\",8,True)".*/
    static final String VB_WRITE_TF_SURFIX = "\",8,True)";
    /** "tf.WriteLine \"taskkill /F /PID \" & oExec.ProcessID".*/
    static final String VB_WRITE_TF_CONTENT = "tf.WriteLine \"running process: "
        + "\" & oExec.ProcessID";
    /** f.close.*/
    static final String VB_WRITE_TF_CLOSE = "tf.close";
    /** WScript.Sleep.*/
    static final String VB_WRITE_WAIT = "WScript.Sleep";
    
    // integers
    /** Number of '-' repeat for the writeDashLine function.*/
    static final int SCRIPT_DASH_NUM_REPEAT = 44;

    // functions
    /**
     * Function for creating dash lines in scripts.
     * 
     * @return A line of dashes.
     */
    public static String writeDashLine() {
        return "echo " + StringUtils.repeat("-", SCRIPT_DASH_NUM_REPEAT);
    }

    /**
     * The header for the kill all script for the machine.
     * 
     * @param login The login to the machine (username@machinename)
     * @return The echo header for killing a machine.
     */
    public static String writeKillMachineHeader(String login) {
        ArgumentNotValid.checkNotNull(login, "String login");
        return "echo KILLING MACHINE: " + login + NEWLINE;
    }
    /**
     * The header for the start all script for the machine.
     * 
     * @param login The login to the machine (username@machinename)
     * @return The echo header for killing a machine.
     */
    public static String writeStartMachineHeader(String login) {
        ArgumentNotValid.checkNotNull(login, "String login");
        return "echo STARTING MACHINE: " + login + NEWLINE;
    }
    /**
     * The header for the install all script for the machine.
     * 
     * @param login The login to the machine (username@machinename)
     * @return The echo header for killing a machine.
     */
    public static String writeInstallMachineHeader(String login) {
        ArgumentNotValid.checkNotNull(login, "String login");
        return "echo INSTALLING TO MACHINE: " + login + NEWLINE;
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
    public static String doubleBackslashes(String path) {
        ArgumentNotValid.checkNotNull(path, "String path");
        return path.replaceAll("[\\\\]", "\\\\\\\\");
    }
    
    /**
     * Changes a string into correct formatted style.
     * The '.vbs' script needs '\\' instead of '\', which is quite annoying 
     * when using regular expressions, since a final '\' in regular expressions
     *  is '/', thus '\\' = '\\\\\\\\' (8).
     *   
     * @param path The directory path to change to appropriate format.
     * @return The formatted path.
     */
    public static String replaceWindowsDirSeparators(String path) {
        ArgumentNotValid.checkNotNull(path, "String path");
        return path.replaceAll("[/]", "\\\\\\\\");
    }
    
    /**
     * For giving readonly permission to a directory in the security policy.
     *  
     * @param dir The path to the directory. This has to be formatted to have  
     * the correct directory separator: '${/}', instead of '/' or '\\' for 
     * Windows and Linux respectively.
     * @return The permission string.
     */
    public static String writeSecurityPolicyDirPermission(String dir) {
        return "  permission java.io.FilePermission \"" + dir + "-\", \"read\""
               + ";" + "\n";
    }
    
    /**
     * Creates the script for extracting the processes of a specific 
     * application, depending on the name of the application and the settings
     * file. 
     * 
     * @param totalName The total name of the application.
     * @param path The path to the directory of the settings file (conf-dir).
     * @param id The identification of the application (name + instanceId).
     * @return The script for getting the list of running application.
     */
    public static String getLinuxPIDS(String totalName, String path, 
            String id) {
        return "PIDS=$(ps -wwfe | grep " + totalName + " | grep -v grep | grep "
        + path + "settings_" + id + ".xml" + " | awk \"{print \\$2}\")";
    }

    // Headers
    /** The header for the jxmremote.password file.*/
    public static final String JMXREMOTE_PASSWORD_HEADER =
        "##############################################################"
        + NEWLINE
        + "#        Password File for Remote JMX Monitoring"
        + NEWLINE
        + "##############################################################"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# Password file for Remote JMX API access to monitoring.  This"
        + NEWLINE
        + "# file defines the different roles and their passwords.  The access"
        + NEWLINE
        + "# control file (jmxremote.access by default) defines the allowed"
        + NEWLINE
        + "# access for each role.  To be functional, a role must have an entry"
        + NEWLINE
        + "# in both the password and the access files."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# Default location of this file is "
                + "$JRE/lib/management/jmxremote.password"
        + NEWLINE
        + "# You can specify an alternate location by specifying a property in"
        + NEWLINE
        + "# the management config file "
                + "$JRE/lib/management/management.properties"
        + NEWLINE
        + "# or by specifying a system property (See that file for details)."
        + NEWLINE
        + NEWLINE
        + NEWLINE
        + "##############################################################"
        + NEWLINE
        + "#    File permissions of the jmxremote.password file"
        + NEWLINE
        + "##############################################################"
        + NEWLINE
        + "#      Since there are cleartext passwords stored in this file,"
        + NEWLINE
        + "#      this file must be readable by ONLY the owner,"
        + NEWLINE
        + "#      otherwise the program will exit with an error."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# The file format for password and access files "
                + "is syntactically the same"
        + NEWLINE
        + "# as the Properties file format.  The syntax is "
                + "described in the Javadoc"
        + NEWLINE
        + "# for java.util.Properties.load."
        + NEWLINE
        + "# Typical password file has multiple  lines, "
                        + "where each line is blank,"
        + NEWLINE                        
        + "# a comment (like this one), or a password entry."
        + NEWLINE
        + "#"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# A password entry consists of a role name and an associated"
        + NEWLINE
        + "# password. "
                + " The role name is any string that does not itself contain"
        + NEWLINE
        + "# spaces or tabs.  The password is again any string that does not"
        + NEWLINE
        + "# contain spaces or tabs. "
                + " Note that passwords appear in the clear in"
        + NEWLINE
        + "# this file, so it is a good idea not to use valuable passwords."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# A given role should have at most one entry in this file. "
                + " If a role"
        + NEWLINE
        + "# has no entry"
        + NEWLINE
        + "# If multiple entries are found for the same role name, "
                + "then the last one"
        + NEWLINE
        + "# is used."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# In a typical installation, this file can be read by anybody on the"
        + NEWLINE
        + "# local machine, and possibly by people on other machines."
        + NEWLINE
        + "# For # security, you should either restrict the"
                + " access to this file,"
        + NEWLINE
        + "# or specify another, less accessible file in "
                + "the management config file"
        + NEWLINE
        + "# as described above."
        + NEWLINE
        + "#"
        + NEWLINE;
    /** The header for the jmxremote.access file.*/
    public static final String JMXREMOTE_ACCESS_HEADER = 
        "#################################################################"
        + "#####"
        + NEWLINE
        + "#Default Access Control File for Remote JMX(TM) Monitoring"
        + NEWLINE
        + "################################################################"
        + "######"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# Access control file for Remote JMX API access to monitoring."
        + NEWLINE
        + "# This file defines the allowed access for different roles.  The"
        + NEWLINE
        + "# password file (jmxremote.password by default) defines the "
        + "roles and their"
        + NEWLINE
        + "# passwords.  To be functional, a role must have an entry in"
        + NEWLINE
        + "# both the password and the access files."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# Default location of this file is "
        + "$JRE/lib/management/jmxremote.access"
        + NEWLINE
        + "# You can specify an alternate location by specifying a property in"
        + NEWLINE
        + "# the management config file "
        + "$JRE/lib/management/management.properties"
        + NEWLINE
        + "# (See that file for details)"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# The file format for password and access files is syntactically "
        + "the same"
        + NEWLINE
        + "# as the Properties file format.  The syntax is described in "
        + "the Javadoc"
        + NEWLINE
        + "# for java.util.Properties.load."
        + NEWLINE
        + "# Typical access file has multiple  lines, where each line is blank,"
        + NEWLINE
        + "# a comment (like this one), or an access control entry."
        + NEWLINE
        + "#"
        + NEWLINE
        + "# An access control entry consists of a role name, and an"
        + NEWLINE
        + "# associated access level.  The role name is any string that "
        + "does not"
        + NEWLINE
        + "# itself contain spaces or tabs.  It corresponds to an entry in the"
        + NEWLINE
        + "# password file (jmxremote.password).  The access level is one "
        + "of the"
        + NEWLINE
        + "# following:"
        + NEWLINE
        + "#       \"readonly\" grants access to read attributes of MBeans."
        + NEWLINE
        + "#                   For monitoring, this means that a remote "
        + "client in this"
        + NEWLINE
        + "#                   role can read measurements but cannot perform "
        + "any action"
        + NEWLINE
        + "#                   that changes the environment of the "
        + "running program."
        + NEWLINE
        + "#       \"readwrite\" grants access to read and write attributes "
        + "of MBeans,"
        + NEWLINE
        + "#                   to invoke operations on them, and to create "
        + "or remove them."
        + NEWLINE
        + "#                   This access should be granted to only "
        + "trusted clients,"
        + NEWLINE
        + "#                   since they can potentially interfere with "
        + "the smooth"
        + NEWLINE
        + "#                   operation of a running program"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# A given role should have at most one entry in this file.  "
        + "If a role"
        + NEWLINE
        + "# has no entry, it has no access."
        + NEWLINE
        + "# If multiple entries are found for the same role name, "
        + "then the last"
        + NEWLINE
        + "# access entry is used."
        + NEWLINE
        + "#"
        + NEWLINE
        + "#"
        + NEWLINE
        + "# Default access control entries:"
        + NEWLINE
        + "# o The \"monitorRole\" role has readonly access."
        + NEWLINE
        + "# o The \"controlRole\" role has readwrite access."
        + NEWLINE
        + ""
        + NEWLINE;
}
