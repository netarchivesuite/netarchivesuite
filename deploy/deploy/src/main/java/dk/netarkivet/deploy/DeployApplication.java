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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import dk.netarkivet.common.utils.Settings;

/** 
 * The application that is run to generate install and start/stop scripts
 * for all physical locations, machines and applications.
 * 
 * The actual deployment has to be done from an Linux/Unix machine, 
 * and this application should therefore not be run on Windows.
 */
public final class DeployApplication {
    /**
     * Private constructor to disallow instantiation of this class.
     */
    private DeployApplication() {}
    
    static {
        Settings.addDefaultClasspathSettings(
                Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH
        );
    }
    /** The configuration for this deploy. */
    private static DeployConfiguration deployConfig;
    /** Argument parameter. */
    private static ArgumentParameters ap = new ArgumentParameters();
    /** The deploy-config file. */
    private static File deployConfigFile;
    /** The NetarchiveSuite file.*/
    private static File netarchiveSuiteFile;
    /** The security policy file.*/
    private static File secPolicyFile;
    /** The log property file.*/
    private static File logPropFile;
    /** The database file.*/
    private static File dbFile;
    /** The arguments for resetting tempDir.*/
    private static boolean resetDirectory;
    /** The archive database file.*/
    private static File arcDbFile;

    /**
     * Run the new deploy.
     * 
     * @param args The Command-line arguments in no particular order:
     * 
     * -C  The deploy configuration file (ends with .xml).
     * -Z  The NetarchiveSuite file to be unpacked (ends with .zip).
     * -S  The security policy file (ends with .policy).
     * -L  The logging property file (ends with .prop).
     * -O  [OPTIONAL] The output directory
     * -D  [OPTIONAL] The harvest definition database
     * -T  [OPTIONAL] The test arguments (httpportoffset, port, 
     *                                    environmentName, mailReceiver)
     * -R  [OPTIONAL] For resetting the tempDir (takes arguments 'y' or 'yes')
     * -E  [OPTIONAL] Evaluating the deployConfig file (arguments: 'y' or 'yes')
     * -A  [OPTIONAL] For archive database.
     */
    public static void main(String[] args) {
        try {
            // Make sure the arguments can be parsed.
            if(!ap.parseParameters(args)) {
                System.err.print(Constants.MSG_ERROR_PARSE_ARGUMENTS);
                System.out.println(ap.listArguments());
                System.exit(1);
            }

            // Check arguments
            if(ap.getCommandLine().getOptions().length 
                    < Constants.ARGUMENTS_REQUIRED) {
                System.err.print(Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS);
                System.out.println();
                System.out.println(
                        "Use DeployApplication with following arguments:");
                System.out.println(ap.listArguments());
                System.out.println(
                        "outputdir defaults to "
                        + "./environmentName (set in config file)");
                System.exit(1);
            }
            // test if more arguments than options is given 
            if (args.length > ap.getOptions().getOptions().size()) {
                System.err.print(
                        Constants.MSG_ERROR_TOO_MANY_ARGUMENTS);
                System.out.println();
                System.out.println("Maximum " 
                        + ap.getOptions().getOptions().size() 
                        + "arguments.");
                System.exit(1);
            }

            // Retrieving the configuration filename
            String deployConfigFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_CONFIG_FILE);
            // Retrieving the NetarchiveSuite filename
            String netarchiveSuiteFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_NETARCHIVE_SUITE_FILE);
            // Retrieving the security policy filename
            String secPolicyFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_SECURITY_FILE);
            // Retrieving the log property filename
            String logPropFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_LOG_PROPERTY_FILE);
            // Retrieving the output directory name
            String outputDir = ap.getCommandLine().getOptionValue(
                    Constants.ARG_OUTPUT_DIRECTORY);
            // Retrieving the database filename
            String databaseFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_DATABASE_FILE);
            // Retrieving the test arguments
            String testArguments = ap.getCommandLine().getOptionValue(
                    Constants.ARG_TEST);
            // Retrieving the reset argument
            String resetArgument = ap.getCommandLine().getOptionValue(
                    Constants.ARG_RESET);
            // Retrieving the evaluate argument
            String evaluateArgument = ap.getCommandLine().getOptionValue(
                    Constants.ARG_EVALUATE);
            // Retrieve the archive database filename.
            String arcDbFileName = ap.getCommandLine().getOptionValue(
                    Constants.ARG_ARC_DB);

            // check deployConfigFileName and retrieve the corresponding file
            initConfigFile(deployConfigFileName);
            
            // check netarchiveSuiteFileName and retrieve the corresponding file
            initNetarchiveSuiteFile(netarchiveSuiteFileName);

            // check secPolicyFileName and retrieve the corresponding file
            initSecPolicyFile(secPolicyFileName);

            // check logPropFileName and retrieve the corresponding file
            initLogPropFile(logPropFileName);

            // check database
            initDatabase(databaseFileName);
            
            // check and apply the test arguments
            initTestArguments(testArguments);
            
            // check reset arguments.
            initReset(resetArgument);
            
            // evaluates the config file
            initEvaluate(evaluateArgument);
            
            // check the archive database
            initArchiveDatabase(arcDbFileName);
            
            // Make the configuration based on the input data
            deployConfig = new DeployConfiguration(
                    deployConfigFile,
                    netarchiveSuiteFile,
                    secPolicyFile,
                    logPropFile,
                    outputDir,
                    dbFile,
                    arcDbFile,
                    resetDirectory); 

            // Write the scripts, directories and everything
            deployConfig.write();
        } catch (SecurityException e) {
            // This problem should only occur in tests -> thus not err message. 
            System.out.println("SECURITY ERROR: ");
            e.printStackTrace();
        } catch (Throwable e) {
            System.err.println("DEPLOY APPLICATION ERROR: ");
            e.printStackTrace();
        }
    }
    
    /** 
     * Checks the configuration file argument and retrieves the file.
     * 
     * @param deployConfigFileName The configuration file argument.
     */
    private static void initConfigFile(String deployConfigFileName) {
        // check whether deploy-config file name is given as argument
        if(deployConfigFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_CONFIG_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether deploy-config file has correct extensions
        if(!deployConfigFileName.endsWith(Constants.EXTENSION_XML_FILES)) {
            System.err.print(
                    Constants.MSG_ERROR_CONFIG_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        deployConfigFile = new File(deployConfigFileName);
        // check whether the deploy-config file exists.
        if(!deployConfigFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND);
            System.out.println();
            System.exit(1);
        }
    }

    /** 
     * Checks the NetarchiveSuite file argument and retrieves the file.
     * 
     * @param netarchiveSuiteFileName The NetarchiveSuite argument.
     */
    private static void initNetarchiveSuiteFile(String 
            netarchiveSuiteFileName) {
        // check whether NetarchiveSuite file name is given as argument
        if(netarchiveSuiteFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether the NetarchiveSuite file has correct extensions
        if(!netarchiveSuiteFileName.endsWith(Constants.EXTENSION_ZIP_FILES)) {
            System.err.print(
                    Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        netarchiveSuiteFile = new File(netarchiveSuiteFileName);
        // check whether the NetarchiveSuite file exists.
        if(!netarchiveSuiteFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " 
                    + netarchiveSuiteFile.getAbsolutePath());
            System.exit(1);
        }
    }
    
    /** 
     * Checks the security policy file argument and retrieves the file.
     * 
     * @param secPolicyFileName The security policy argument.
     */
    private static void initSecPolicyFile(String secPolicyFileName) {
        // check whether security policy file name is given as argument
        if(secPolicyFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_SECURITY_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether security policy file has correct extensions
        if(!secPolicyFileName.endsWith(Constants.EXTENSION_POLICY_FILES)) {
            System.err.print(
                    Constants.MSG_ERROR_SECURITY_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        secPolicyFile = new File(secPolicyFileName);
        // check whether the security policy file exists.
        if(!secPolicyFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " 
                    + secPolicyFile.getAbsolutePath());
            System.exit(1);
        }
    }
    
    /** 
     * Checks the log property file argument and retrieves the file.
     * 
     * @param logPropFileName The log property argument.
     */
    private static void initLogPropFile(String logPropFileName) {
        // check whether log property file name is given as argument
        if(logPropFileName == null) {
            System.err.print(
                    Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether the log property file has correct extensions
        if(!logPropFileName.endsWith(Constants.EXTENSION_LOG_PROPERTY_FILES)) {
            System.err.print(
                    Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        logPropFile = new File(logPropFileName);
        // check whether the log property file exists.
        if(!logPropFile.exists()) {
            System.err.print(
                    Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " 
                    + logPropFile.getAbsolutePath());
            System.exit(1);
        }
    }
    
    /**
     * Checks the database argument (if any) for extension and existence.
     * 
     * @param databaseFileName The name of the database file.
     */
    private static void initDatabase(String databaseFileName) {
        dbFile = null;
        // check the extension on the database, if it is given as argument 
        if(databaseFileName != null) {
            if(!databaseFileName.endsWith(Constants.EXTENSION_JAR_FILES) 
                    && !databaseFileName.endsWith(
                            Constants.EXTENSION_ZIP_FILES)) {
                System.err.print(
                        Constants.MSG_ERROR_DATABASE_EXTENSION);
                System.out.println();
                System.exit(1);
            }
            
            // get the file
            dbFile = new File(databaseFileName);
            // check whether the database file exists.
            if(!dbFile.isFile()) {
                System.err.print(
                            Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND);
                System.out.println();
                System.out.println("Couldn't find file: " 
                        + dbFile.getAbsolutePath());
                System.exit(1);
            }
        }
    }
    
    /**
     * Checks the arguments for resetting the directory.
     * Only the arguments 'y' or 'yes' resets the database directory. 
     * Default is 'no'.
     * 
     * If another argument than 'y', 'yes', 'n' or 'no' is given, an warning 
     * is given. 
     * 
     * @param resetArgument The argument for resetting given.
     */
    private static void initReset(String resetArgument) {
        if(resetArgument != null) {
            if(resetArgument.equalsIgnoreCase(Constants.YES_SHORT)
                    || resetArgument.equalsIgnoreCase(Constants.YES_LONG)) {
                // if positive argument, then set to true.
                resetDirectory = true;
            } else if (resetArgument.equalsIgnoreCase(Constants.NO_SHORT)
                    || resetArgument.equalsIgnoreCase(Constants.NO_LONG)) {
                // if negative argument, then set to false.
                resetDirectory = false;
            } else {
                // if wrong argument, notify and set to false.
                System.err.println(Constants.MSG_ERROR_RESET_ARGUMENT);
                resetDirectory = false;
            }
        } else {
            // if no arguments, then 
            resetDirectory = false;
        }
    }
    
    /**
     * Checks the arguments for evaluating the config file.
     * Only the arguments 'y' or 'yes' is accepted for evaluation.
     * Anything else (including argument set to null) does not evaluate the
     * deployConfigFile. 
     * 
     * @param evaluateArgument The argument for evaluation.
     */
    public static void initEvaluate(String evaluateArgument) {
        // check if argument is given and it is acknowledgement ('y' or 'yes')
        if((evaluateArgument != null) && (!evaluateArgument.isEmpty()) 
                && (evaluateArgument.equalsIgnoreCase(Constants.YES_SHORT)
                    || evaluateArgument.equalsIgnoreCase(Constants.YES_LONG))) {
            // if yes, then evaluate config file
            EvaluateConfigFile evf = 
                new EvaluateConfigFile(deployConfigFile);
            evf.evaluate();
        }
    }
    
    /**
     * Checks the argument for the archive database.
     * 
     * @param arcDbFileName The path to the archive database.
     */
    public static void initArchiveDatabase(String arcDbFileName) {
        arcDbFile = null;
        // check the extension on the database, if it is given as argument 
        if(arcDbFileName != null) {
            if(!arcDbFileName.endsWith(Constants.EXTENSION_JAR_FILES) 
                    && !arcDbFileName.endsWith(
                            Constants.EXTENSION_ZIP_FILES)) {
                System.err.print(
                        Constants.MSG_ERROR_BPDB_EXTENSION);
                System.out.println();
                System.exit(1);
            }
            
            // get the file
            arcDbFile = new File(arcDbFileName);
            // check whether the database file exists.
            if(!arcDbFile.isFile()) {
                System.err.print(
                            Constants.MSG_ERROR_NO_BPDB_FILE_FOUND);
                System.out.println();
                System.out.println("Couldn't find file: " 
                        + arcDbFile.getAbsolutePath());
                System.exit(1);
            }
        }
    }
       
    /**
     * Applies the test arguments.
     * 
     * If the test arguments are given correctly, the configuration file is 
     * loaded and changed appropriately, then written to a test configuration 
     * file.
     * 
     * The new test configuration file has the same name as the original 
     * configuration file, except ".xml" is replaced by "_text.xml".
     * Thus "path/config.xml" -> "path/config_test.xml".  
     * 
     * @param testArguments The test arguments.
     */
    private static void initTestArguments(String testArguments) {
        // test if any test arguments (if none, don't apply, just stop).
        if(testArguments == null || testArguments.isEmpty()) {
            return;
        }

        String[] changes = testArguments.split(Constants.REGEX_COMMA_CHARACTER);
        if(changes.length != Constants.TEST_ARGUMENTS_REQUIRED) {
            System.err.print(
                    Constants.MSG_ERROR_TEST_ARGUMENTS);
            System.out.println();
            System.out.println(changes.length + " arguments was given and "
                    + Constants.TEST_ARGUMENTS_REQUIRED + " was expected.");
            System.out.println("Received: " + testArguments);
            System.exit(1);
        }

        try {
            CreateTestInstance cti = new CreateTestInstance(deployConfigFile);

            // apply the arguments
            cti.applyTestArguments(
                    changes[Constants.TEST_ARGUMENT_OFFSET_INDEX], 
                    changes[Constants.TEST_ARGUMENT_HTTP_INDEX], 
                    changes[Constants.TEST_ARGUMENT_ENVIRONMENT_NAME_INDEX], 
                    changes[Constants.TEST_ARGUMENT_MAIL_INDEX]); 

            // replace ".xml" with "_test.xml"
            String tmp = deployConfigFile.getPath();
            // split this into two ("path/config.xml" = {"path/config", ".xml"})
            String[] configFile = tmp.split(Constants.REGEX_DOT_CHARACTER);
            // take the first part and add test ending 
            // ("path/config" + "_test.xml" = "path/config_test.xml")
            String nameOfNewConfig =  configFile[0] 
                    + Constants.TEST_CONFIG_FILE_REPLACE_ENDING;

            // create and use new config file.
            cti.createConfigurationFile(nameOfNewConfig);
            deployConfigFile = new File(nameOfNewConfig);
        } catch (IOException e) {
            System.out.println("Error in test arguments: " + e);
            System.exit(1);
        }
    }
    
    /**
     * Handles the incoming arguments.
     * 
     */
    private static class ArgumentParameters {
        /** Options object for parameters.*/
        private Options options = new Options();
        /** Parser for parsing the command line arguments.*/
        private CommandLineParser parser = new PosixParser();
        /** The command line.*/
        private CommandLine cmd;
        /** Whether the options has an argument.*/
        private static final boolean HAS_ARG = true;
        /**
         * Initialise options by setting legal parameters for batch jobs.
         */
        ArgumentParameters() {
            options.addOption(Constants.ARG_CONFIG_FILE, 
                    HAS_ARG, "Config file.");
            options.addOption(Constants.ARG_NETARCHIVE_SUITE_FILE, 
                    HAS_ARG, "The NetarchiveSuite package file.");
            options.addOption(Constants.ARG_SECURITY_FILE, 
                    HAS_ARG, "Security property file.");
            options.addOption(Constants.ARG_LOG_PROPERTY_FILE, 
                    HAS_ARG, "Log property file.");
            options.addOption(Constants.ARG_OUTPUT_DIRECTORY, 
                    HAS_ARG, "[OPTIONAL] output directory.");
            options.addOption(Constants.ARG_DATABASE_FILE, 
                    HAS_ARG, "[OPTIONAL] Database file.");
            options.addOption(Constants.ARG_TEST, 
                    HAS_ARG, "[OPTIONAL] Tests arguments (offset for http port,"
                    + " http port, environment name, mail receiver).");
            options.addOption(Constants.ARG_RESET,
                    HAS_ARG, "[OPTIONAL] Reset temp directory ('y' or 'yes'"
                    + "means reset, anything else means do not reset."
                    + " Different from 'y', 'yes', 'n' or 'no' gives"
                    + " an error message.");
            options.addOption(Constants.ARG_EVALUATE, 
                    HAS_ARG, "[OPTIONAL] Evaluate the config file.");
            options.addOption(Constants.ARG_ARC_DB,
                    HAS_ARG, "[OPTIONAL] The archive database file");
        }

        /**
         * Parsing the input arguments.
         * 
         * @param args The input arguments.
         * @return Whether it parsed correctly or not.
         */
        Boolean parseParameters(String[] args) {
            try {
                // parse the command line arguments
                cmd = parser.parse(options, args);
            } catch(ParseException exp) {
                System.out.println("Parsing error: " + exp);
                return false;
            }
            return true;
        }
        
        /**
         * Get the list of possible arguments with their description.
         * 
         * @return The list describing the possible arguments.
         */
        String listArguments() {
            StringBuilder res = new StringBuilder();
            res.append(Constants.NEWLINE);
            res.append(Constants.INIT_ARGUMENTS_LIST);
            // add options
            for (Object o: options.getOptions()) {
                Option op = (Option) o;
                res.append(Constants.NEWLINE);
                res.append(Constants.DASH);
                res.append(op.getOpt());
                res.append(Constants.SPACE);
                res.append(op.getDescription());
            }
            return res.toString();
        }
        
        /**
         * For retrieving the options.
         * 
         * @return The options.
         */
        public Options getOptions() {
            return options;
        }
        
        /**
         * For retrieving the commandLine.
         * 
         * @return The cmd.
         */
        public CommandLine getCommandLine() {
            return cmd;
        }
    }
}
