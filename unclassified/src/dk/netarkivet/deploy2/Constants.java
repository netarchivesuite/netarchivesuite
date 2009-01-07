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
    static final String[] ENVIRONMENT_NAME_TOTAL_PATH_BRANCH = 
        {SETTINGS_BRANCH, COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};
    /** The path to the environment name from the settings branch.*/
    static final String[] ENVIRONMENT_NAME_SETTING_PATH_BRANCH = 
        {COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};

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
    
    // other constants
    /** Number of '-' repeat in scripts. */
    static final int SCRIPT_DASH_NUM_REPEAT = 44;

    // Parameters as constants.
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
	"WARNING: wrong arguments given.";
    /** The error when too many arguments are given.*/
    public static final String MSG_ERROR_TOO_MANY_ARGUMENTS = 
	"Too many arguments given.";
    /** */
    public static final String MSG_ERROR_NOT_ENOUGH_ARGUMENTS = 
	"Not enough arguments given.";
    /** The error message for wrong it-config file extension.*/
    public static final String MSG_ERROR_CONFIG_EXTENSION = 
	"Config file must be '.xml'!";
    /** The error message for wrong NetarchiveSuite file extension.*/
    public static final String MSG_ERROR_NETARCHIVESUITE_EXTENSION = 
	"NetarchiveSuite file must be '.zip'";
    /** The error message for wrong security file extension.*/
    public static final String MSG_ERROR_SECURITY_EXTENSION = 
	"Security policy file must be '.policy'";
    /** The error message for wrong log property file extension.*/
    public static final String MSG_ERROR_LOG_PROPERTY_EXTENSION = 
	"Log property file must be '.prop'";
}
