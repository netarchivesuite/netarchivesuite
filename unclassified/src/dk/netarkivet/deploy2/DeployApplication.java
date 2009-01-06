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
     * @param args The Command-line arguments in the following order:
     * 
     * 1: The it-configuration file (ends with .xml).
     * 2: The NetarchiveSuite file to be unpacked (ends with .zip).
     * 3: The security policy file (ends with .policy).
     * 4: The logging property file (ends with .prop).
     * 5: [OPTIONAL] The output directory
     */
    public static void main(String[] args) {
        try {
/*            System.out.println("Arguments: ");
            for(String st : args) {
                System.out.println(st);
            }
            System.out.println();
/* */
            // Make sure the arguments can be parsed.
            if(!ap.parseParameters(args)) {
                System.out.println("ERROR!");
                System.exit(0);
            }

            // RESTRUCTURE
            // Check arguments
            if(args.length < 4) {
                System.out.println(
                        "Use DeployApplication with following arguments:");
                System.out.println("config file: -C");
                System.out.println("NetarchiveSuite package file: -Z");
                System.out.println("security policy file: -S");
                System.out.println("log property file: -L");
                System.out.println("[OPTIONAL] output directory: -O");
                System.out.println("[OPTIONAL] database file: -D");
                System.out.println(
                        "outputdir defaults to "
                        + "./environmentName (set in config file)");
                System.out.println(
                        "Database defaults to "
                        + "?? (from NetarchiveSuite.zip)");
                System.out.println(
                        "Example: DeployApplication "
                        + "-C./conf/it-config.xml "
                        + "-Z./NetarchiveSuite-1.zip "
                        + "-S./conf/security.policy "
                        + "-L./conf/log.prop");
                System.exit(0);
            }
            if (args.length > ap.cmd.getOptions().length) {
                System.err.println("Too many arguments");
                System.err.println("Maximum " + ap.cmd.getOptions().length 
                        + "arguments.");
                System.exit(0);
            }
            
            // Retrieving the configuration filename
            String itConfigFileName = ap.cmd.getOptionValue("C");
            // Retrieving the NetarchiveSuite filename
            String netarchiveSuiteFileName = ap.cmd.getOptionValue("Z");
            // Retrieving the security policy filename
            String secPolicyFileName = ap.cmd.getOptionValue("S");
            // Retrieving the log property filename
            String logPropFileName = ap.cmd.getOptionValue("L");
            // Retrieving the output directory name
            String outputDir = ap.cmd.getOptionValue("O");
            // Retrieving the database filename
            String databaseName = ap.cmd.getOptionValue("D");
            
            // check whether it-config file has correct extensions
            if(!itConfigFileName.endsWith(".xml")) {
                System.out.println("Config file must be '.xml'!");
                System.exit(0);
            }
            // check whether the NetarchiveSuite file has correct extensions
            if(!netarchiveSuiteFileName.endsWith(".zip")) {
                System.out.println("NetarchiveSuite file must be '.zip'");
                System.exit(0);
            }
            // check whether security policy file has correct extensions
            if(!secPolicyFileName.endsWith(".policy")) {
                System.out.println("Security policy file must be '.policy'");
                System.exit(0);
            }
            // check whether the log property file has correct extensions
            if(!logPropFileName.endsWith(".prop")) {
                System.out.println("Log property file must be '.prop'");
                System.exit(0);
            }
/*
            System.out.println("itConfigFileName: " + itConfigFileName);
            System.out.println("netarchiveSuiteFileName: " 
                    + netarchiveSuiteFileName);
            System.out.println("secPolicyFileName: " + secPolicyFileName);
            System.out.println("logPropFileName: " + logPropFileName);
            System.out.println("outputDir: " + outputDir);
            System.out.println("database: " + databaseName);
/* */
            // Make the configuration based on the input data
            itConfig = new DeployConfiguration(
                    itConfigFileName,
                    netarchiveSuiteFileName,
                    secPolicyFileName,
                    logPropFileName,
                    outputDir); 

            // Write the scripts, directories and everything
            itConfig.write();
        } catch (Exception e) {
            // handle exceptions?
            System.out.println("ERROR: " + e);
        }
    }
    
    /**
     * Handles the incoming arguments.
     */
    private static class ArgumentParameters {
        /** Options object for parameters.*/
        private Options options = new Options();
        /** Parser for parsing the command line arguments.*/
        private CommandLineParser parser = new PosixParser();
        /** The command line.*/
        public CommandLine cmd;
        //HelpFormatter only prints directly, thus this is not used at
        //the moment
        //HelpFormatter formatter = new HelpFormatter();
        //Instead the method listArguments is defined
        
        /**
         * Initialize options by setting legal parameters for batch jobs.
         */
        ArgumentParameters() {
            options.addOption("C", true, "Config file.");
            options.addOption("Z", true, "The NetarchiveSuite package file.");
            options.addOption("S", true, "Security property file.");
            options.addOption("L", true, "Log property file.");
            options.addOption("O", true, "[OPTIONAL] output directory.");
            options.addOption("D", true, "[OPTIONAL] Database.");
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
            String s = "\nwith arguments:\n";
            // add options
            for (Object o: options.getOptions()) {
                Option op = (Option) o;
                s += "-" + op.getOpt() + " " + op.getDescription() + "\n";
            }
            //delete last delimitter
            if (s.length() > 0) {
                s = s.substring(0, s.length()-1);
            }
            return s;
        }
    }
}
