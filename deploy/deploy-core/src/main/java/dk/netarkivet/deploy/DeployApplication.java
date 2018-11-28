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
import java.nio.charset.Charset;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import dk.netarkivet.common.utils.Settings;

// JDK8-import
//import java.util.Optional;


/**
 * The application that is run to generate install and start/stop scripts for all physical locations, machines and
 * applications.
 * <p>
 * The actual deployment has to be done from an Linux/Unix machine, and this application should therefore not be run on
 * Windows.
 */
public final class DeployApplication {

    static {
        Settings.addDefaultClasspathSettings(Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
    }

    /** The configuration for this deploy. */
    private static DeployConfiguration deployConfig;
    /** Argument parameter. */
    private static ArgumentParameters ap = new ArgumentParameters();
    /** The deploy-config file. */
    private static File deployConfigFile;
    /** The NetarchiveSuite file. */
    private static File netarchiveSuiteFile;
    /** The security policy file. */
    private static File secPolicyFile;
    /** SLF4J xml configuration file. */
    private static File slf4jConfigFile;
    /** The database file. */
    private static File dbFile;
    /** The arguments for resetting tempDir. */
    private static boolean resetDirectory;
    /** The archive database file. */
    private static File arcDbFile;
    /** The folder with the external libraries to be deployed. */
    private static File externalJarFolder;
    
    //private static Optional<File> defaultBundlerZip;
    private static File defaultBundlerZip;
    
    /** The logo png file. */
    private static File logoFile;
    /** The menulogo png file. */
    private static File menulogoFile;
    
    /**
     * Private constructor to disallow instantiation of this class.
     */
    private DeployApplication() {
    }

    /**
     * Run deploy.
     *
     * @param args The Command-line arguments in no particular order:
     *
     * -C The deploy configuration file (ends with .xml). -Z The NetarchiveSuite file to be unpacked (ends with .zip).
     * -S The security policy file (ends with .policy). -L The logging property file (ends with .prop). -O [OPTIONAL]
     * The output directory -D [OPTIONAL] The harvest definition database -T [OPTIONAL] The test arguments
     * (httpportoffset, port, environmentName, mailReceiver) -R [OPTIONAL] For resetting the tempDir (takes arguments
     * 'y' or 'yes') -E [OPTIONAL] Evaluating the deployConfig file (arguments: 'y' or 'yes') -A [OPTIONAL] For archive
     * database. -J [OPTIONAL] For deploying with external jar files. Must be the total path to the directory containing
     * jar-files. -l [OPTIONAL] for logo png file. -m [OPTIONAL] for menulogo png file 
     * These external files will be placed on every machine, and they have to manually be put into the
     * classpath, where they should be used.
     */
    public static void main(String[] args) {
        try {
            // Make sure the arguments can be parsed.
            if (!ap.parseParameters(args)) {
                System.err.print(Constants.MSG_ERROR_PARSE_ARGUMENTS);
                System.out.println(ap.listArguments());
                System.exit(1);
            }

            // Check arguments
            if (ap.getCommandLine().getOptions().length < Constants.ARGUMENTS_REQUIRED) {
                System.err.print(Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS);
                System.out.println();
                System.out.println("Use DeployApplication with following arguments:");
                System.out.println(ap.listArguments());
                System.out.println("outputdir defaults to ./environmentName (set in config file)");
                System.exit(1);
            }
            // test if more arguments than options is given
            if (args.length > ap.getOptions().getOptions().size()) {
                System.err.print(Constants.MSG_ERROR_TOO_MANY_ARGUMENTS);
                System.out.println();
                System.out.println("Maximum " + ap.getOptions().getOptions().size() + "arguments.");
                System.exit(1);
            }

            // Retrieving the configuration filename
            String deployConfigFileName = ap.getCommandLine().getOptionValue(Constants.ARG_CONFIG_FILE);
            // Retrieving the NetarchiveSuite filename
            String netarchiveSuiteFileName = ap.getCommandLine().getOptionValue(Constants.ARG_NETARCHIVE_SUITE_FILE);
            // Retrieving the security policy filename
            String secPolicyFileName = ap.getCommandLine().getOptionValue(Constants.ARG_SECURITY_FILE);
            // Retrieving the SLF4J xml filename
            String slf4jConfigFileName = ap.getCommandLine().getOptionValue(Constants.ARG_SLF4J_CONFIG_FILE);
            // Retrieving the output directory name
            String outputDir = ap.getCommandLine().getOptionValue(Constants.ARG_OUTPUT_DIRECTORY);
            // Retrieving the database filename
            String databaseFileName = ap.getCommandLine().getOptionValue(Constants.ARG_DATABASE_FILE);
            // Retrieving the test arguments
            String testArguments = ap.getCommandLine().getOptionValue(Constants.ARG_TEST);
            // Retrieving the reset argument
            String resetArgument = ap.getCommandLine().getOptionValue(Constants.ARG_RESET);
            // Retrieving the evaluate argument
            String evaluateArgument = ap.getCommandLine().getOptionValue(Constants.ARG_EVALUATE);
            // Retrieve the archive database filename.
            String arcDbFileName = ap.getCommandLine().getOptionValue(Constants.ARG_ARC_DB);
            // Retrieves the jar-folder name.
            String jarFolderName = ap.getCommandLine().getOptionValue(Constants.ARG_JAR_FOLDER);
            // Retrieves the logo filename.
            String logoFilename = ap.getCommandLine().getOptionValue(Constants.ARG_LOGO);
            // Retrieves the menulogo filename.
            String menulogoFilename = ap.getCommandLine().getOptionValue(Constants.ARG_MENULOGO);
            
            // Retrieves the source encoding.
            // If not specified get system default
            String sourceEncoding = ap.getCommandLine().getOptionValue(Constants.ARG_SOURCE_ENCODING);
            String msgTail = "";
            if (sourceEncoding == null || sourceEncoding.isEmpty()) {
                sourceEncoding = Charset.defaultCharset().name();
                msgTail = " (defaulted)";
            }
            System.out.println("Will read source files using encoding '" + sourceEncoding + "'" + msgTail);

            // check deployConfigFileName and retrieve the corresponding file
            initConfigFile(deployConfigFileName);

            // check netarchiveSuiteFileName and retrieve the corresponding file
            initNetarchiveSuiteFile(netarchiveSuiteFileName);

            // check secPolicyFileName and retrieve the corresponding file
            initSecPolicyFile(secPolicyFileName);

            initSLF4JXmlFile(slf4jConfigFileName);

            // check database
            initDatabase(databaseFileName);

            // check and apply the test arguments
            initTestArguments(testArguments);

            // check reset arguments.
            initReset(resetArgument);

            // evaluates the config file
            initEvaluate(evaluateArgument, sourceEncoding);

            // check the archive database
            initArchiveDatabase(arcDbFileName);

            // check the external jar-files library folder.
            initJarFolder(jarFolderName);

            //initBundlerZip(Optional.ofNullable(
            //        ap.getCommandLine().getOptionValue(Constants.ARG_DEFAULT_BUNDLER_ZIP)));
            initBundlerZip(ap.getCommandLine().getOptionValue(Constants.ARG_DEFAULT_BUNDLER_ZIP));
            
            // check the logo files
            initLogos(logoFilename, menulogoFilename);
            
            // Make the configuration based on the input data
            deployConfig = new DeployConfiguration(deployConfigFile, netarchiveSuiteFile, secPolicyFile,
                    slf4jConfigFile, outputDir, dbFile, arcDbFile, resetDirectory, externalJarFolder, sourceEncoding,
                    defaultBundlerZip, logoFile, menulogoFile);

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
        if (deployConfigFileName == null) {
            System.err.print(Constants.MSG_ERROR_NO_CONFIG_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether deploy-config file has correct extensions
        if (!deployConfigFileName.endsWith(Constants.EXTENSION_XML_FILES)) {
            System.err.print(Constants.MSG_ERROR_CONFIG_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        deployConfigFile = new File(deployConfigFileName);
        // check whether the deploy-config file exists.
        if (!deployConfigFile.exists()) {
            System.err.print(Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND);
            System.out.println();
            System.exit(1);
        }
    }

    /**
     * Checks the NetarchiveSuite file argument and retrieves the file.
     *
     * @param netarchiveSuiteFileName The NetarchiveSuite argument.
     */
    private static void initNetarchiveSuiteFile(String netarchiveSuiteFileName) {
        // check whether NetarchiveSuite file name is given as argument
        if (netarchiveSuiteFileName == null) {
            System.err.print(Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether the NetarchiveSuite file has correct extensions
        if (!netarchiveSuiteFileName.endsWith(Constants.EXTENSION_ZIP_FILES)) {
            System.err.print(Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        netarchiveSuiteFile = new File(netarchiveSuiteFileName);
        // check whether the NetarchiveSuite file exists.
        if (!netarchiveSuiteFile.isFile()) {
            System.err.print(Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " + netarchiveSuiteFile.getAbsolutePath());
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
        if (secPolicyFileName == null) {
            System.err.print(Constants.MSG_ERROR_NO_SECURITY_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether security policy file has correct extensions
        if (!secPolicyFileName.endsWith(Constants.EXTENSION_POLICY_FILES)) {
            System.err.print(Constants.MSG_ERROR_SECURITY_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        secPolicyFile = new File(secPolicyFileName);
        // check whether the security policy file exists.
        if (!secPolicyFile.exists()) {
            System.err.print(Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " + secPolicyFile.getAbsolutePath());
            System.exit(1);
        }
    }

    /**
     * Checks the SLF4J config file argument and retrieves the file.
     *
     * @param slf4jXmlFileName The SLF4J config argument.
     */
    private static void initSLF4JXmlFile(String slf4jXmlFileName) {
        // check whether SLF4J config file name is given as argument
        if (slf4jXmlFileName == null) {
            System.err.print(Constants.MSG_ERROR_NO_SLF4J_CONFIG_FILE_ARG);
            System.out.println();
            System.exit(1);
        }
        // check whether the SLF4J xml file has correct extensions
        if (!slf4jXmlFileName.endsWith(Constants.EXTENSION_XML_FILES)) {
            System.err.print(Constants.MSG_ERROR_SLF4J_CONFIG_EXTENSION);
            System.out.println();
            System.exit(1);
        }
        // get the file
        slf4jConfigFile = new File(slf4jXmlFileName);
        // check whether the SLF4J xml file exists.
        if (!slf4jConfigFile.exists()) {
            System.err.print(Constants.MSG_ERROR_NO_SLF4J_CONFIG_FILE_FOUND);
            System.out.println();
            System.out.println("Couldn't find file: " + slf4jConfigFile.getAbsolutePath());
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
        if (databaseFileName != null) {
            if (!databaseFileName.endsWith(Constants.EXTENSION_JAR_FILES)
                    && !databaseFileName.endsWith(Constants.EXTENSION_ZIP_FILES)) {
                System.err.print(Constants.MSG_ERROR_DATABASE_EXTENSION);
                System.out.println();
                System.exit(1);
            }

            // get the file
            dbFile = new File(databaseFileName);
            // check whether the database file exists.
            if (!dbFile.isFile()) {
                System.err.print(Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND);
                System.out.println();
                System.out.println("Couldn't find file: " + dbFile.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    /**
     * Checks the arguments for resetting the directory. Only the arguments 'y' or 'yes' resets the database directory.
     * Default is 'no'.
     * <p>
     * If another argument than 'y', 'yes', 'n' or 'no' is given, an warning is given.
     *
     * @param resetArgument The argument for resetting given.
     */
    private static void initReset(String resetArgument) {
        if (resetArgument != null) {
            if (resetArgument.equalsIgnoreCase(Constants.YES_SHORT)
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
     * Checks the arguments for evaluating the config file. Only the arguments 'y' or 'yes' is accepted for evaluation.
     * Anything else (including argument set to null) does not evaluate the deployConfigFile.
     *
     * @param evaluateArgument The argument for evaluation.
     * @param encoding the encoding to use to read from the input file
     */
    public static void initEvaluate(String evaluateArgument, String encoding) {
        // check if argument is given and it is acknowledgement ('y' or 'yes')
        if ((evaluateArgument != null)
                && (!evaluateArgument.isEmpty())
                && (evaluateArgument.equalsIgnoreCase(Constants.YES_SHORT) || evaluateArgument
                        .equalsIgnoreCase(Constants.YES_LONG))) {
            // if yes, then evaluate config file
            EvaluateConfigFile evf = new EvaluateConfigFile(deployConfigFile, encoding);
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
        if (arcDbFileName != null) {
            if (!arcDbFileName.endsWith(Constants.EXTENSION_JAR_FILES)
                    && !arcDbFileName.endsWith(Constants.EXTENSION_ZIP_FILES)) {
                System.err.print(Constants.MSG_ERROR_BPDB_EXTENSION);
                System.out.println();
                System.exit(1);
            }

            // get the file
            arcDbFile = new File(arcDbFileName);
            // check whether the database file exists.
            if (!arcDbFile.isFile()) {
                System.err.print(Constants.MSG_ERROR_NO_BPDB_FILE_FOUND);
                System.out.println();
                System.out.println("Couldn't find file: " + arcDbFile.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    /**
     * Checks the argument for the external jar-folder.
     *
     * @param folderName The path to the folder. Global path.
     */
    public static void initJarFolder(String folderName) {
        externalJarFolder = null;
        if (folderName != null && !folderName.isEmpty()) {
            externalJarFolder = new File(folderName);

            if (!externalJarFolder.isDirectory()) {
                System.err.print(Constants.MSG_ERROR_NO_JAR_FOLDER);
                System.out.println();
                System.out.println("Couldn't find directory: " + externalJarFolder.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    /**
     * Checks if the default bundler zip file exists if defined.
     *
     * @param defaultBundlerZipName The path to the default bundler zip file to use.
     */
    public static void initBundlerZip(String defaultBundlerZipName) {
        if (defaultBundlerZipName != null) {
            defaultBundlerZip = new File(defaultBundlerZipName);
            if (!defaultBundlerZip.exists()) {
                System.err.print(Constants.MSG_ERROR_NO_BUNDLER_ZIP_FILE);
                System.out.println();
                System.out.println("Couldn't find the default bundler file: " + defaultBundlerZip.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    /**
     * Checks if the logo files exists
     *
     * @param logoFilename The absolute path to the logo png file.
     * @param menulogoFilename The absoluet path to the menu logo png file.
     */
    public static void initLogos(String logoFilename, String menulogoFilename) {
    	if (logoFilename != null) {
        	logoFile = new File(logoFilename);
        	if (!logoFile.exists()) {
        		logoFile = null;
        	}
    	}
    	
    	if (menulogoFilename != null) {
        	menulogoFile = new File(menulogoFilename);
        	if (!menulogoFile.exists()) {
        		menulogoFile = null;
        	}
    	}
    }
    
    /**
     * Applies the test arguments.
     * <p>
     * If the test arguments are given correctly, the configuration file is loaded and changed appropriately, then
     * written to a test configuration file.
     * <p>
     * The new test configuration file has the same name as the original configuration file, except ".xml" is replaced
     * by "_text.xml". Thus "path/config.xml" -> "path/config_test.xml".
     *
     * @param testArguments The test arguments.
     */
    private static void initTestArguments(String testArguments) {
        // test if any test arguments (if none, don't apply, just stop).
        if (testArguments == null || testArguments.isEmpty()) {
            return;
        }

        String[] changes = testArguments.split(Constants.REGEX_COMMA_CHARACTER);
        if (changes.length != Constants.TEST_ARGUMENTS_REQUIRED) {
            System.err.print(Constants.MSG_ERROR_TEST_ARGUMENTS);
            System.out.println();
            System.out.println(changes.length + " arguments was given and " + Constants.TEST_ARGUMENTS_REQUIRED
                    + " was expected.");
            System.out.println("Received: " + testArguments);
            System.exit(1);
        }

        try {
            CreateTestInstance cti = new CreateTestInstance(deployConfigFile);

            // apply the arguments
            cti.applyTestArguments(changes[Constants.TEST_ARGUMENT_OFFSET_INDEX],
                    changes[Constants.TEST_ARGUMENT_HTTP_INDEX],
                    changes[Constants.TEST_ARGUMENT_ENVIRONMENT_NAME_INDEX],
                    changes[Constants.TEST_ARGUMENT_MAIL_INDEX]);

            // replace ".xml" with "_test.xml"
            String tmp = deployConfigFile.getPath();
            // split this into two ("path/config.xml" = {"path/config", ".xml"})
            String[] configFile = tmp.split(Constants.REGEX_DOT_CHARACTER);
            // take the first part and add test ending
            // ("path/config" + "_test.xml" = "path/config_test.xml")
            String nameOfNewConfig = configFile[0] + Constants.TEST_CONFIG_FILE_REPLACE_ENDING;

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
     */
    private static class ArgumentParameters {
        /** Options object for parameters. */
        private Options options = new Options();
        /** Parser for parsing the command line arguments. */
        private CommandLineParser parser = new PosixParser();
        /** The command line. */
        private CommandLine cmd;
        /** Whether the options has an argument. */
        private static final boolean HAS_ARG = true;

        /**
         * Initialise options by setting legal parameters for batch jobs.
         */
        ArgumentParameters() {
            options.addOption(Constants.ARG_CONFIG_FILE, HAS_ARG, "Config file.");
            options.addOption(Constants.ARG_NETARCHIVE_SUITE_FILE, HAS_ARG, "The NetarchiveSuite package file.");
            options.addOption(Constants.ARG_SECURITY_FILE, HAS_ARG, "Security property file.");
            options.addOption(Constants.ARG_SLF4J_CONFIG_FILE, HAS_ARG, "SLF4J config file.");
            options.addOption(Constants.ARG_OUTPUT_DIRECTORY, HAS_ARG, "[OPTIONAL] output directory.");
            options.addOption(Constants.ARG_DATABASE_FILE, HAS_ARG, "[OPTIONAL] Database file.");
            options.addOption(Constants.ARG_TEST, HAS_ARG, "[OPTIONAL] Tests arguments (offset for http port,"
                    + " http port, environment name, mail receiver).");
            options.addOption(Constants.ARG_RESET, HAS_ARG, "[OPTIONAL] Reset temp directory ('y' or 'yes'"
                    + "means reset, anything else means do not reset."
                    + " Different from 'y', 'yes', 'n' or 'no' gives" + " an error message.");
            options.addOption(Constants.ARG_EVALUATE, HAS_ARG, "[OPTIONAL] Evaluate the config file.");
            options.addOption(Constants.ARG_ARC_DB, HAS_ARG, "[OPTIONAL] The archive database file");
            options.addOption(Constants.ARG_JAR_FOLDER, HAS_ARG, "[OPTIONAL] Installing the external jar library "
                    + "files within the given folder.");
            options.addOption(Constants.ARG_SOURCE_ENCODING, HAS_ARG, "[OPTIONAL] Encoding to use for source files.");
            options.addOption(Constants.ARG_DEFAULT_BUNDLER_ZIP, HAS_ARG, "[OPTIONAL] The bundled harvester to use. "
                    + "If not provided here the bundler needs to be provided in the settings for each (H3) harvester");
            options.addOption(null, Constants.ARG_LOGO, HAS_ARG, "[OPTIONAL] The Logo png file to use. "
                    + "(this is only working for Linux)");
            options.addOption(null, Constants.ARG_MENULOGO, HAS_ARG, "[OPTIONAL] The Menulogo png file to use. "
                    + "(this is only working for Linux)");
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
            } catch (ParseException exp) {
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
            for (Object o : options.getOptions()) {
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
