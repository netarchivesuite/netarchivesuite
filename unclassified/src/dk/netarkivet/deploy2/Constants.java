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

/**
 * Class containing the constant variables.
 */
public class Constants {
    // Setting specific
    /** Path to the Settings branch.*/
    static final String SETTINGS_BRANCH = "settings";
    /** Path to the common branch within the settings branch.*/
    static final String COMMON_BRANCH = "common";
    /** Path to the environment name branch in the common branch.*/
    static final String ENVIRONMENT_NAME_BRANCH = "environmentName";
    /** The total path to the environment name from an entity branch.*/
    static final String[] ENVIRONMENT_NAME_TOTAL_PATH_LEAF = 
        {SETTINGS_BRANCH, COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};
    /** The path to the environment name from the settings branch.*/
    static final String[] ENVIRONMENT_NAME_SETTING_PATH_LEAF = 
        {COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};
    /** Path to the environment name branch in the common branch.*/
    static final String DATABASE_BRANCH = "database";
    /** Path to the environment name branch in the common branch.*/
    static final String DATABASE_URL_BRANCH = "url";
    /** The path to the database directory from the settings branch.*/
    static final String[] DATABASE_URL_SETTING_LEAF_PATH = 
        {COMMON_BRANCH, DATABASE_BRANCH, DATABASE_URL_BRANCH};

    // parameter specific
    /** The path to the class path branches.*/
    static final String CLASS_PATH_BRANCH = "deployClassPath";
    /** The path to the java option branches.*/
    static final String JAVA_OPTIONS_BRANCH = "deployJavaOpt";
    /** The path to the optional installation directory.*/
    static final String PARAMETER_INSTALL_DIR_BRANCH = "deployInstallDir";
    /** The path to the machine user name.*/
    static final String PARAMETER_MACHINE_USER_NAME_BRANCH = 
        "deployMachineUserName";
    /** The path to the directory for the database.*/
    static final String PARAMETER_DATABASE_DIR_BRANCH = "deployDatabaseDir";

    // traversing the XML tree
    /** The path to physical locations in from the global scope.*/
    static final String PHYSICAL_LOCATION_BRANCH = "thisPhysicalLocation";
    /** The path to machines from a physical location.*/
    static final String MACHINE_BRANCH = "deployMachine";
    /** The path to applications from a machine.*/
    static final String APPLICATION_BRANCH = "applicationName";

    // physical location specific
    /** The path to name in a physical location instance.*/
    static final String PHYSICAL_LOCATION_NAME_ATTRIBUTES = "name";

    // machine specific
    /** The path to name in a machine instance.*/
    static final String MACHINE_NAME_ATTRIBUTE = "name";
    /** The path to the operating system variable.*/
    static final String MACHINE_OPERATING_SYSTEM_ATTRIBUTE = "os";

    // application specific
    /** The path to name in a application instance.*/
    static final String APPLICATION_NAME_ATTRIBUTE = "name";
    /** The path to the instance id for the application.*/
    static final String APPLICATION_INSTANCE_ID_BRANCH = 
        "applicationInstanceId";

    // operating system specific
    /** The operating system attribute for windows.*/
    static final String OPERATING_SYSTEM_WINDOWS_ATTRIBUTE = "windows";
    /** The call for running a batch script in windows.*/
    static final String OPERATING_SYSTEM_WINDOWS_RUN_BATCH_FILE = 
        "\"C:\\Program Files\\Bitvise WinSSHD\\bvRun\" -brj -new -cmd=\"";

    // jmx remote password specific
    /** The path from settings to the monitor branch.*/
    static final String JMX_PASSWORD_MONITOR_BRANCH = "monitor";
    /** The path from monitor to the jmxUsername leaf.*/
    static final String JMX_PASSWORD_NAME_BRANCH = "jmxUsername";
    /** The path from monitor to the jmxPassword leaf.*/
    static final String JMX_PASSWORD_PASSWORD_BRANCH = "jmxPassword";
    /** The name of the JMX remote password file.*/
    static final String JMX_FILE_NAME = "jmxremote.password";
    
    // database constants
    /** The directory for the database in the unpacked NetarchiveSuite.*/
    static final String DATABASE_BASE_DIR = "harvestdefinitionbasedir/";
    /** The name of the database in the directory above.*/
    static final String DATABASE_BASE_FILE = "fullhddb.jar";
    /** The path to the base database (the two above combined).*/
    static final String DATABASE_BASE_PATH = 
        DATABASE_BASE_DIR + DATABASE_BASE_FILE;
    /** The message to write when database is trying to overwrite a
      * non-empty directory· */
    static final String DATABASE_ERROR_PROMPT_DIR_NOT_EMPTY = 
        "The database directory is not empty as required.";

    // other constants
    /** Number of '-' repeat in scripts. */
    static final int SCRIPT_DASH_NUM_REPEAT = 44;
    /** The minimum number of arguments required.*/
    static final int ARGUMENTS_REQUIRED = 4;

    // argument parameters as constants.
    /** For initiating a argument. */
    public static final String ARG_INIT_ARG = "-";
    /** For giving the configuration file as argument. */
    public static final String ARG_CONFIG_FILE = "C";
    /** For giving the NetarchiveSuite package file as argument. */
    public static final String ARG_NETARCHIVE_SUITE_FILE = "Z";
    /** For giving the security file as argument. */
    public static final String ARG_SECURITY_FILE = "S";
    /** For giving the log property file as argument. */
    public static final String ARG_LOG_PROPERTY_FILE = "L";
    /** For giving the optional output directory as argument. */
    public static final String ARG_OUTPUT_DIRECTORY = "O";
    /** For giving the optional database file as argument. */
    public static final String ARG_DATABASE_FILE = "D";
    
    // messages
    /** The error message for error in parsing the arguments.*/
    public static final String MSG_ERROR_PARSE_ARGUMENTS = 
        "WARNING: wrong arguments given.\n";
    /** The error message when too many arguments are given.*/
    public static final String MSG_ERROR_TOO_MANY_ARGUMENTS = 
        "Too many arguments given.\n";
    /** The error message when not enough arguments are given.*/
    public static final String MSG_ERROR_NOT_ENOUGH_ARGUMENTS = 
        "Not enough arguments given.\n";
    /** The error message when no it-config file is given.*/
    public static final String MSG_ERROR_NO_CONFIG_FILE_ARG = 
        "No config file argument: -C (Must end with '.xml').\n";
    /** The error message when no NetarchiveSuite file is given.*/
    public static final String MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG = 
        "No NetarchiveSuite file argument: -Z (Must end with '.zip').\n";
    /** The error message when no security file is given.*/
    public static final String MSG_ERROR_NO_SECURITY_FILE_ARG = 
        "No security file argument: -S (Must end with '.policy').\n";
    /** The error message when no log property file is given.*/
    public static final String MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG = 
        "No log property file argument: -L (Must end with '.prop').\n";
    /** The error message when config file does not exist.*/
    public static final String MSG_ERROR_NO_CONFIG_FILE_FOUND = 
        "Reference to non-existing config file (-C argument).";
    /** The error message when NetarchiveSuite file does not exist.*/
    public static final String MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND = 
        "Reference to non-existing NetarchiveSuite file (-Z argument).";
    /** The error message when security file does not exist.*/
    public static final String MSG_ERROR_NO_SECURITY_FILE_FOUND = 
        "Reference to non-existing security file (-S argument).";
    /** The error message when log property file does not exist.*/
    public static final String MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND = 
        "Reference to non-existing log property file (-L argument).";
    /** The error message when database file does not exist.*/
    public static final String MSG_ERROR_NO_DATABASE_FILE_FOUND = 
        "Reference to non-existing database file (-D argument).";
    /** The error message for wrong it-config file extension.*/
    public static final String MSG_ERROR_CONFIG_EXTENSION = 
        "Config file must be '.xml'!.\n";
    /** The error message for wrong NetarchiveSuite file extension.*/
    public static final String MSG_ERROR_NETARCHIVESUITE_EXTENSION = 
        "NetarchiveSuite file must be '.zip'.\n";
    /** The error message for wrong security file extension.*/
    public static final String MSG_ERROR_SECURITY_EXTENSION = 
        "Security policy file must be '.policy'.\n";
    /** The error message for wrong log property file extension.*/
    public static final String MSG_ERROR_LOG_PROPERTY_EXTENSION = 
        "Log property file must be '.prop'.\n";
    /** The error message for wrong database extension.*/
    public static final String MSG_ERROR_DATABASE_EXTENSION = 
        "Database file must have extension '.jar' or '.zip'";
}
