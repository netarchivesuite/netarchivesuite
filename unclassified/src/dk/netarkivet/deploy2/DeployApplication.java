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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/** 
 * The application that is run to generate install and start/stop scripts
 * for all physical locations, machines and applications.
 */
public class DeployApplication {
    /** The configuration for this deploy. */
    private static DeployConfiguration itConfig;
    /** Argument parameter. */
    private static ArgumentParameters ap = new ArgumentParameters();

    /**
     * Run the new deploy.
     * 
     * @param args The Command-line arguments in no particular order:
     * 
     * -C  The it-configuration file (ends with .xml).
     * -Z  The NetarchiveSuite file to be unpacked (ends with .zip).
     * -S  The security policy file (ends with .policy).
     * -L  The logging property file (ends with .prop).
     * -O  [OPTIONAL] The output directory
     * -D  [OPTIONAL] The database
     */
    public static void main(String[] args) {
        try {
            // Make sure the arguments can be parsed.
            if(!ap.parseParameters(args)) {
                System.err.print(Constants.MSG_ERROR_PARSE_ARGUMENTS);
                System.exit(0);
            }

            // Check arguments
            if(ap.cmd.getOptions().length < Constants.ARGUMENTS_REQUIRED) {
                System.err.print(Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS);
                System.out.println(
                        "Use DeployApplication with following arguments:");
                System.out.println(ap.listArguments());
                System.out.println(
                        "outputdir defaults to "
                        + "./environmentName (set in config file)");
                System.out.println(
                        "Database defaults to "
                        + "?? (from NetarchiveSuite.zip)");
                System.out.println("Example: ");
                System.out.println(
                        "DeployApplication "
                        + "-C./conf/it-config.xml "
                        + "-Z./NetarchiveSuite-1.zip "
                        + "-S./conf/security.policy "
                        + "-L./conf/log.prop");
                System.exit(0);
            }
            // test if more arguments than options is given 
            if (args.length > ap.options.getOptions().size()) {
                System.err.print(
                        Constants.MSG_ERROR_TOO_MANY_ARGUMENTS);
                System.out.println("Maximum " + ap.options.getOptions().size() 
                        + "arguments.");
                System.exit(0);
            }
           
            // Retrieving the configuration filename
            String itConfigFileName = ap.cmd.getOptionValue(
                    Constants.ARG_CONFIG_FILE);
            // Retrieving the NetarchiveSuite filename
            String netarchiveSuiteFileName = ap.cmd.getOptionValue(
                    Constants.ARG_NETARCHIVE_SUITE_FILE);
            // Retrieving the security policy filename
            String secPolicyFileName = ap.cmd.getOptionValue(
                    Constants.ARG_SECURITY_FILE);
            // Retrieving the log property filename
            String logPropFileName = ap.cmd.getOptionValue(
                    Constants.ARG_LOG_PROPERTY_FILE);
            // Retrieving the output directory name
            String outputDir = ap.cmd.getOptionValue(
                    Constants.ARG_OUTPUT_DIRECTORY);
            // Retrieving the database filename
            String databaseName = ap.cmd.getOptionValue(
                    Constants.ARG_DATABASE_FILE);
            
            // check whether it-config file name is given as argument
            if(itConfigFileName == null) {
                System.err.print(
                        Constants.MSG_ERROR_NO_CONFIG_FILE);
                System.exit(0);
            }
            // check whether it-config file has correct extensions
            if(!itConfigFileName.endsWith(".xml")) {
                System.err.print(
                        Constants.MSG_ERROR_CONFIG_EXTENSION);
                System.exit(0);
            }
            // check whether NetarchiveSuite file name is given as argument
            if(netarchiveSuiteFileName == null) {
                System.err.print(
                        Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE);
                System.exit(0);
            }
            // check whether the NetarchiveSuite file has correct extensions
            if(!netarchiveSuiteFileName.endsWith(".zip")) {
                System.err.print(
                        Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION);
                System.exit(0);
            }
            // check whether security policy file name is given as argument
            if(secPolicyFileName == null) {
                System.err.print(
                        Constants.MSG_ERROR_NO_SECURITY_FILE);
                System.exit(0);
            }
            // check whether security policy file has correct extensions
            if(!secPolicyFileName.endsWith(".policy")) {
                System.err.print(
                        Constants.MSG_ERROR_SECURITY_EXTENSION);
                System.exit(0);
            }
            // check whether log property file name is given as argument
            if(logPropFileName == null) {
                System.err.print(
                        Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE);
                System.exit(0);
            }
            // check whether the log property file has correct extensions
            if(!logPropFileName.endsWith(".prop")) {
                System.err.print(
                        Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION);
                System.exit(0);
            }
            // check the extension on the database, if it is given as argument 
            if(databaseName != null) {
        	if(!databaseName.endsWith(".jar") 
        		&& !databaseName.endsWith(".zip")) {
        	    System.err.print(
        		    Constants.MSG_ERROR_DATABASE_EXTENSION);
        	    System.exit(0);
        	}
            }

            // Make the configuration based on the input data
            itConfig = new DeployConfiguration(
                    itConfigFileName,
                    netarchiveSuiteFileName,
                    secPolicyFileName,
                    logPropFileName,
                    outputDir); 

            // Write the scripts, directories and everything
            itConfig.write();
        } catch (SecurityException e) {
            // This problem should only occur in tests -> thus not err message. 
            System.out.println("SECURITY ERROR: " + e);
        } catch (Exception e) {
            // handle other exceptions?
            System.err.println("DEPLOY APPLICATION ERROR: " + e);
        }
    }
    
    /**
     * Handles the incoming arguments.
     * 
     */
    private static class ArgumentParameters {
        /** Options object for parameters.*/
        public Options options = new Options();
        /** Parser for parsing the command line arguments.*/
        private CommandLineParser parser = new PosixParser();
        /** The command line.*/
        public CommandLine cmd;
         
        /**
         * Initialise options by setting legal parameters for batch jobs.
         */
        ArgumentParameters() {
            options.addOption(Constants.ARG_CONFIG_FILE, 
                    true, "Config file.");
            options.addOption(Constants.ARG_NETARCHIVE_SUITE_FILE, 
                    true, "The NetarchiveSuite package file.");
            options.addOption(Constants.ARG_SECURITY_FILE, 
                    true, "Security property file.");
            options.addOption(Constants.ARG_LOG_PROPERTY_FILE, 
                    true, "Log property file.");
            options.addOption(Constants.ARG_OUTPUT_DIRECTORY, 
                    true, "[OPTIONAL] output directory.");
            options.addOption(Constants.ARG_DATABASE_FILE, 
                    true, "[OPTIONAL] Database.");
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
            String s = "\n" + "Arguments:";
            // add options
            for (Object o: options.getOptions()) {
                Option op = (Option) o;
                s += "\n" + "-" + op.getOpt() + " " + op.getDescription();
            }
            return s;
        }
    }
}
