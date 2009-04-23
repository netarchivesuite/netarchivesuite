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

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.EMailNotifications;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.monitor.MonitorSettings;
import dk.netarkivet.viewerproxy.ViewerProxySettings;

/**
 * Class containing the constant variables.
 * 
 * SETTINGS_ = path to branches from the settings branch.
 * COMPLETE_ = path from beyond the settings branch.
 */
public final class Constants {
    // Single character specific
    /** The empty string, "".*/
    static final String EMPTY = "";
    /** The newline character as a string.*/
    static final String NEWLINE = "\n";
    /** Quote mark.*/
    static final String QUOTE_MARK = "\"";
    /** Apostrophe.*/
    static final String APOSTROPHE = "'"; 
    /** The space character as a string.*/
    static final String SPACE = " ";
    /** The at '@' character as a string.*/
    static final String AT = "@";
    /** The underscore '_' character as a string.*/
    static final String UNDERSCORE = "_";
    /** Less than (prefix for XML).*/
    static final String LESS_THAN = "<";
    /** Greater than (suffix for XML).*/
    static final String GREATER_THAN = ">";
    /** Slash, Linux/Unix directory path separator.*/
    static final String SLASH = "/";
    /** Dot: '.'.*/
    static final String DOT = ".";
    /** Dash: '-'.*/
    static final String DASH = "-";
    /** BackSlash: '\\'.*/
    static final String BACKSLASH = "\\";
    /** Colon: ':'.*/
    static final String COLON = ":";
    /** Semicolon: ';'.*/
    static final String SEMICOLON = ";";
    /** ( - To start a standard bracket.*/
    static final String BRACKET_BEGIN = "(";
    /** ) - To end a standard bracket.*/
    static final String BRACKET_END = ")";
    /** [ - To start a square bracket.*/
    static final String SQUARE_BRACKET_BEGIN = "[";
    /** ] - To end a square bracket.*/
    static final String SQUARE_BRACKET_END = "]";
    /** $ - Dollar sign.*/
    static final String DOLLAR_SIGN = "$";
    /** | - Separator.*/
    static final String SEPARATOR = "|";
    /** * - Star.*/
    static final String STAR = "*";
    
    
    // deploy specific parameters.
    /** The path to the class path branches.*/
    static final String DEPLOY_CLASS_PATH = "deployClassPath";
    /** The path to the java option branches.*/
    static final String DEPLOY_JAVA_OPTIONS = "deployJavaOpt";
    /** The path to the optional installation directory.*/
    static final String DEPLOY_INSTALL_DIR = "deployInstallDir";
    /** The path to the machine user name.*/
    static final String DEPLOY_MACHINE_USER_NAME = "deployMachineUserName";
    /** The path to the directory for the database.*/
    static final String DEPLOY_DATABASE_DIR = "deployDatabaseDir";
    /** The path to physical locations in from the global scope.*/
    static final String DEPLOY_PHYSICAL_LOCATION = "thisPhysicalLocation";
    /** The path to machines from a physical location.*/
    static final String DEPLOY_MACHINE = "deployMachine";
    /** The path to applications from a machine.*/
    static final String DEPLOY_APPLICATION_NAME = "applicationName";

    // Attributes
    /** The path to name in a application instance.*/
    static final String APPLICATION_NAME_ATTRIBUTE = "name";
    /** The path to name in a physical location instance.*/
    static final String PHYSICAL_LOCATION_NAME_ATTRIBUTE = "name";
    /** The path to name in a machine instance.*/
    static final String MACHINE_NAME_ATTRIBUTE = "name";
    /** The path to the operating system variable.*/
    static final String MACHINE_OPERATING_SYSTEM_ATTRIBUTE = "os";

    // Attribute values
    /** The operating system attribute for windows.*/
    static final String OPERATING_SYSTEM_WINDOWS_ATTRIBUTE = "windows";
    /** The operating system attribute for linux/unix. */
    static final String OPERATING_SYSTEM_LINUX_ATTRIBUTE = "linux";

    // TAGS
    /** The attachment for the file dir in the security policy file.*/
    static final String SECURITY_FILE_DIR_TAG = "filedir";
    /** The name of the jmx principal name tag in the security file.*/
    static final String SECURITY_JMX_PRINCIPAL_NAME_TAG = "ROLE";
    /** The name of the common temp dir tag in the security policy file.*/
    static final String SECURITY_COMMON_TEMP_DIR_TAG = "TEMPDIR";
    /** The name of the application id in the log.prop file.*/
    static final String LOG_PROPERTY_APPLICATION_ID_TAG = "APPID";

    // Setting specific
    /** Path to the Settings branch.*/
    static final String COMPLETE_SETTINGS_BRANCH = 
        CommonSettings.SETTINGS;
    /** The total path to the environment name from an entity branch.*/
    static final String[] COMPLETE_ENVIRONMENT_NAME_LEAF = 
        CommonSettings.ENVIRONMENT_NAME.split("[.]");
    /** The path to the environment name from the settings branch.*/
    static final String[] SETTINGS_ENVIRONMENT_NAME_LEAF =
        CommonSettings.ENVIRONMENT_NAME
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the database directory from the settings branch.*/
    static final String[] DATABASE_URL_SETTING_LEAF_PATH = CommonSettings
        .DB_URL.replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The complete path to the port leaf from beyond settings.*/
    static final String[] COMPLETE_HTTP_PORT_LEAF = 
        CommonSettings.HTTP_PORT_NUMBER.split("[.]");
    /** The complete path to the receiver leaf from beyond settings.*/
    static final String[] SETTINGS_NOTIFICATION_RECEIVER_PATH = 
        EMailNotifications.MAIL_RECEIVER_SETTING.split("[.]");
    /** The path to the jmxPort leaf from beyond settings.*/
    static final String[] COMPLETE_JMX_PORT_PATH =
        CommonSettings.JMX_PORT.split("[.]");
    /** The path to the rmiPort leaf from beyond settings.*/
    static final String[] COMPLETE_JMX_RMIPORT_PATH =
        CommonSettings.JMX_RMI_PORT.split("[.]");
    /** The path to the heritrix guiPort from beyond settings.*/
    static final String[] COMPLETE_HARVEST_HETRIX_GUI_PORT_PATH = 
        HarvesterSettings.HERITRIX_GUI_PORT.split("[.]");
    /** The path to the heritrix jmxPort from beyond settings.*/
    static final String[] COMPLETE_HARVEST_HETRIX_JMX_PORT =
        HarvesterSettings.HERITRIX_JMX_PORT.split("[.]");
    /** The path to the heritrix jmxPort from the settings branch.*/
    static final String[] SETTINGS_HARVEST_HETRIX_JMX_PORT =
        HarvesterSettings.HERITRIX_JMX_PORT
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the tempDir leaf from settings.*/
    static final String[] SETTINGS_TEMPDIR_LEAF = 
        CommonSettings.DIR_COMMONTEMPDIR
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the fileDir branch from settings.*/
    static final String[] SETTINGS_BITARCHIVE_BASEFILEDIR_LEAF = 
        ArchiveSettings.BITARCHIVE_SERVER_FILEDIR
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the serverDir leaf from settings.*/
    static final String[] SETTINGS_HARVEST_SERVERDIR_LEAF = 
        HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the bitpreservation base dir leaf from settings.
     *  Uses the constant from ArciveSettings, with the 'settings' removed.*/
    static final String[] SETTINGS_ARCHIVE_BP_BASEDIR_LEAF = 
        ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION
            .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the arcrepository base dir leaf from settings.
     *  Uses the constant from ArciveSettings, with the 'settings' removed.*/
    static final String[] SETTINGS_ARCHIVE_ARC_BASEDIR_LEAF = 
        ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the viewer proxy base dir leaf from settings.*/
    static final String[] SETTINGS_VIEWERPROXY_BASEDIR_LEAF = 
        ViewerProxySettings.VIEWERPROXY_DIR
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path from monitor to the jmxUsername leaf.*/
    static final String[] SETTINGS_MONITOR_JMX_NAME_LEAF = 
        MonitorSettings.JMX_USERNAME_SETTING
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path from monitor to the jmxPassword leaf.*/
    static final String[] SETTINGS_MONITOR_JMX_PASSWORD_LEAF = 
        MonitorSettings.JMX_PASSWORD_SETTING
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to the application instance id leaf from settings.*/
    static final String[] SETTINGS_APPLICATION_INSTANCE_ID_LEAF = 
        CommonSettings.APPLICATION_INSTANCE_ID
        .replace(CommonSettings.SETTINGS + ".", "").split("[.]");
    /** The path to thisPhysicalLocation from settings.*/
    static final String[] COMPLETE_THIS_PHYSICAL_LOCATION_LEAF = 
    CommonSettings.THIS_PHYSICAL_LOCATION.split("[.]");
    /** The path to applicationName from beyond settings.*/
    static final String[] COMPLETE_APPLICATION_NAME_LEAF = 
    CommonSettings.APPLICATION_NAME.split("[.]");
    /** The path to the jmxUsername under heritrix from settings.*/
    static final String[] SETTINGS_HERITRIX_JMX_USERNAME_LEAF = 
        HarvesterSettings.HERITRIX_JMX_USERNAME
        .replace(COMPLETE_SETTINGS_BRANCH + ".", "").split("[.]");
    /** The path to the jmxPassword under heritrix from settings.*/
    static final String[] SETTINGS_HERITRIX_JMX_PASSWORD_LEAF = 
        HarvesterSettings.HERITRIX_JMX_PASSWORD
        .replace(COMPLETE_SETTINGS_BRANCH + ".", "").split("[.]");
    /** The path to the jmx accessFile leaf from settings.*/
    static final String[] SETTINGS_COMMON_JMX_ACCESSFILE =
        CommonSettings.JMX_ACCESS_FILE
        .replace(COMPLETE_SETTINGS_BRANCH + ".", "").split("[.]");
    /** The path to the jmx passwordFile leaf from settings.*/
    static final String[] SETTINGS_COMMON_JMX_PASSWORDFILE = 
        CommonSettings.JMX_PASSWORD_FILE
        .replace(COMPLETE_SETTINGS_BRANCH + ".", "").split("[.]");

    // File and directory names
    /** The name of the JMX remote password file.*/
    static final String JMX_PASSWORD_FILE_NAME = "jmxremote.password";
    /** The name of the JMX remote access file.*/
    static final String JMX_ACCESS_FILE_NAME = "jmxremote.access";
    /** The default path to the jmxremote.password file.*/
    static final String JMX_PASSWORD_FILE_PATH_DEFAULT = 
        "conf/" + JMX_PASSWORD_FILE_NAME;
    /** The default path to the jmxremote.access file.*/
    static final String JMX_ACCESS_FILE_PATH_DEFAULT = 
        "conf/" + JMX_ACCESS_FILE_NAME;
    /** The name of the security policy file.*/
    static final String SECURITY_POLICY_FILE_NAME = "security.policy";
    /** The prefix for the log property file for the application.*/
    static final String LOG_PROP_APPLICATION_PREFIX = "log_";
    /** The suffix for the log property file for the application.*/
    static final String LOG_PROP_APPLICATION_SUFFIX = ".prop";
    /** The directory for the database in the unpacked NetarchiveSuite.
     *  The default directory for the database file.
     */
    static final String DATABASE_BASE_DIR = "harvestdefinitionbasedir/";
    /** The name of the database in the directory above.
     *  The default name for the database file.
     */
    static final String DATABASE_BASE_FILE = "fullhddb.jar";
    /** The path to the base database (the two above combined).
     *  This is the default location for the database. 
     */
    static final String DATABASE_BASE_PATH = 
        DATABASE_BASE_DIR + DATABASE_BASE_FILE;
    /** The name of the new modified configuration file for tests.*/
    static final String TEST_CONFIG_FILE_REPLACE_ENDING = "_test.xml";
    /** The script extension for Linux/Unix.*/
    static final String SCRIPT_EXTENSION_LINUX = ".sh";
    /** The script extension for Windows.*/
    static final String SCRIPT_EXTENSION_WINDOWS = ".bat";
    /** The name of the killall script. killall.*/
    static final String SCRIPT_NAME_KILL_ALL = "killall";
    /** The name of the killall script. install.*/
    static final String SCRIPT_NAME_INSTALL_ALL = "install";
    /** The name of the killall script. startall.*/
    static final String SCRIPT_NAME_START_ALL = "startall";
    /** Prefix for the application kill script. kill_ .*/
    static final String SCRIPT_LOCAL_KILL_ALL = "kill_";
    /** Prefix for the application start script. start_ .*/
    static final String SCRIPT_LOCAL_START_ALL = "start_";
    /** Prefix for the application kill_ps script.*/
    static final String SCRIPT_KILL_PS = "kill_ps_";
    /** The prefix of the name for application specific settings files.*/
    static final String PREFIX_SETTINGS = "settings_";
    /** The extension on XML files.*/
    static final String EXTENSION_XML_FILES = ".xml";
    /** The extension on zip files.*/
    static final String EXTENSION_ZIP_FILES = ".zip";
    /** The extension on policy files.*/
    static final String EXTENSION_POLICY_FILES = ".policy";
    /** The extension on property files.*/
    static final String EXTENSION_LOG_PROPERTY_FILES = ".prop";
    /** The extension on jar files.*/
    static final String EXTENSION_JAR_FILES = ".jar";
    /** The extension on vb-script files.*/
    static final String EXTENSION_VBS_FILES = ".vbs";
    /** The extension on log files.*/
    static final String EXTENSION_LOG_FILES = ".log";
    /** The config directory path from install directory.*/
    static final String CONF_DIR_WINDOWS = "\\conf\\";
    /** The config directory path from install directory.*/
    static final String CONF_DIR_LINUX = "/conf/";
    /** Settings prefix. settings_.*/
    static final String SETTINGS_PREFIX = "settings_";
    /** Log property prefix. log_.*/
    static final String LOG_PREFIX = "log_";
    /** Windows directory creation script prefix.*/
    static final String WINDOWS_DIR_CREATE_PREFIX = "dir_";
    
    // evaluate specific constants
    /** Complete list of settings files to combine to complete settings file.*/
    static final String[] BUILD_SETTING_FILES = {
        "dk/netarkivet/archive/settings.xml",
        "dk/netarkivet/common/settings.xml",
        "dk/netarkivet/harvester/settings.xml",
        "dk/netarkivet/monitor/settings.xml",
        "dk/netarkivet/viewerproxy/settings.xml",
        "dk/netarkivet/archive/arcrepository/distribute/"
            + "JMSArcRepositoryClientSettings.xml",
        "dk/netarkivet/archive/indexserver/distribute/"
            + "IndexRequestClientSettings.xml",
        "dk/netarkivet/common/utils/EMailNotificationsSettings.xml",
        "dk/netarkivet/common/distribute/FTPRemoteFileSettings.xml",
        "dk/netarkivet/common/distribute/HTTPRemoteFileSettings.xml",
        "dk/netarkivet/common/distribute/HTTPSRemoteFileSettings.xml",
        "dk/netarkivet/common/distribute/JMSConnectionSunMQSettings.xml"
        };
    /** The path to the complete settings file.*/
    public static final String BUILD_COMPLETE_SETTINGS_FILE_PATH = 
        "dk/netarkivet/deploy/complete_settings.xml";
    /** The name of the temporary run-file for windows.*/
    public static final String FILE_TEMPORARY_RUN_WINDOWS_NAME = "running_";
    
    // other constants
    /** The minimum number of arguments required.*/
    public static final int ARGUMENTS_REQUIRED = 4;
    /** The exact number of arguments required for test.*/
    public static final int TEST_ARGUMENTS_REQUIRED = 4;
    /** The maximum integer value in a character.*/
    public static final int TEST_OFFSET_INTEGER_MAXIMUM_VALUE = 9;
    /** Directories to install under baseFileDir.*/
    public static final String[] BASEFILEDIR_SUBDIRECTORIES = {
            dk.netarkivet.archive.Constants.FILE_DIRECTORY_NAME,
            dk.netarkivet.archive.Constants.TEMPORARY_DIRECTORY_NAME,
            dk.netarkivet.archive.Constants.ATTIC_DIRECTORY_NAME
        };
    /** The offset for the digit to replace during test in the 
     * monitor jmx port.
     */
    static final int TEST_OFFSET_MONITOR_JMX_PORT = 2;
    /** The offset for the digit to replace during test in the 
     * monitor rmi port.
     */
    static final int TEST_OFFSET_MONITOR_RMI_PORT = 2;
    /** The offset for the digit to replace during test in the 
     * heritrix jmx port.
     */
    static final int TEST_OFFSET_HERITRIX_JMX_PORT = 1;
    /** The offset for the digit to replace during test in the 
     * heritrix gui port.
     */
    static final int TEST_OFFSET_HERITRIX_GUI_PORT = 1;

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
    /** For giving the optional test data.*/
    public static final String ARG_TEST = "T";
    /** For giving the optional reset directory argument.*/
    public static final String ARG_RESET = "R";
    /** For giving the optional argument.*/
    public static final String ARG_EVALUATE = "E";
    
    // Argument values
    /** The long yes argument.*/
    public static final String YES_LONG = "yes";
    /** The short yes argument.*/
    public static final String YES_SHORT = "y";
    /** The long no argument.*/
    public static final String NO_LONG = "no";
    /** The short no argument.*/
    public static final String NO_SHORT = "n";
    
    // Other string constants
    /** Regular expression for finding the '.' character.*/
    public static final String REGEX_DOT_CHARACTER = "[.]";
    /** Regular expression for finding the ',' character.*/
    public static final String REGEX_COMMA_CHARACTER = "[,]";
    /** Regular expression for finding the '/' character.*/
    public static final String REGEX_SLASH_CHARACTER = "[/]";
    /** Regular expression for finding the '/' character.*/
    public static final String REGEX_BACKSLASH_CHARACTER = "[\\\\]";
    /** Argument.*/
    public static final String INIT_ARGUMENTS_LIST = "Arguments:";
    
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
    /** The error message when no deploy-config file is given.*/
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
    /** The error message for wrong deploy-config file extension.*/
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
    /** The error message when test wrong number of test arguments.*/
    public static final String MSG_ERROR_TEST_ARGUMENTS = 
        "There have to be " + TEST_ARGUMENTS_REQUIRED + " test arguments.";
    /** The error message when offset value are too different from httpport.*/
    public static final String MSG_ERROR_TEST_OFFSET = 
        "Difference between Offset and http not between 0 and 10, as required.";
    /** The error message when reset directory has wrong argument.*/
    public static final String MSG_ERROR_RESET_ARGUMENT = 
        "Wrong argument for resetting the directory.";
    /** The error message when a physical location has no name attribute.*/
    public static final String MSG_ERROR_PHYSICAL_LOCATION_NO_NAME =
        "A Physical Location has no name!";
    /** The warning when more than one jmxremote.access or jmxremote.password 
     * file path is defined.*/
    public static final String MSG_WARN_TOO_MANY_JMXREMOTE_FILE_PATHS = 
        "Too many instances of jmxremote.password or jmxremote.access "
        + "files defined.";
    
    // functions
    /**
     * Create the beginning of a scope in XML (e.g. html = \< html \>).
     * 
     * @param scope The name of the XML-scope to have the start created.
     * @return The beginning of the XML-scope.
     */
    public static String changeToXMLBeginScope(String scope) {
        return LESS_THAN + scope + GREATER_THAN;
    }

    /**
     * Create the ending of a scope in XML (e.g. html = \< \html \>).
     * 
     * @param scope The name of the XML-scope to have the end created.
     * @return The ending of the XML-scope.
     */
    public static String changeToXMLEndScope(String scope) {
        return LESS_THAN + SLASH + scope + GREATER_THAN;
    }
}
