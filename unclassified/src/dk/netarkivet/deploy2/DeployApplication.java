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

public class DeployApplication {

	/** The configuration for this deploy */
	DeployConfiguration itConfig;

	/**
	 * Run the new deploy!
	 * 
	 * @param args The Command-line arguments
	 * 
	 */
	public static void main(String[] args) {
		try {

			// Check arguments
			if(args.length < 4) {
				System.out.println(
						"Usage: Deploy "
						+ "it-config.xml "
						+ "NetarchiveSuite-xxx.zip " 
						+ "security.policy "
						+ "log.prop "
						+ "[outputdir]");
				System.out.println(
						"outputdir defaults to "
						+ "./environmentName (set in config file)");
				System.out.println(
						"Example: Deploy "
						+ "./conf/it-config.xml "
						+ "./NetarchiveSuite-1.zip "
						+ "./conf/security.policy "
						+ "./conf/log.prop");
				System.exit(0);
			}

			// 1st argument is the name of configuration file
			String itConfigFileName = args[0];
			// 2nd argument is the name of NetarchiveSuite file
			String netarchiveSuiteFileName = args[1];
			// 3rd argument is the security policy file
			String secPolicyFileName = args[2];
			// 4th argument is the logging property file
			String logPropFileName = args[3];

			String outputDir = null;
			
			// check it-config file extensions
			if(!itConfigFileName.endsWith(".xml")) {
				System.out.println("Config file must be '.xml'!");
				System.exit(0);
			}
			
			// check NetarchiveSuite file extensions
			if(!netarchiveSuiteFileName.endsWith(".zip")) {
				System.out.println("NetarchiveSuite file must be '.zip'");
				System.exit(0);
			}

			// check security policy file file extensions
			if(!secPolicyFileName.endsWith(".policy")) {
				System.out.println("Security policy file must be '.policy'");
				System.exit(0);
			}

			// check log property file extensions
			if(!logPropFileName.endsWith(".prop")) {
				System.out.println("Log property file must be '.prop'");
				System.exit(0);
			}

			// if more than 4 arguments, get the output directory 
			// and handle other arguments
			if(args.length > 4) {
				outputDir = args[4];
				
				// if too many arguments
				if(args.length > 5) {
					System.out.println("I can only handle 4 or 5 arguments");
					System.out.println("UNUSED ARGUMENTS: ");
					for(int i=5; i<args.length; i++) {
						System.out.println((i+1) + ": " + args[i]);
					}
				}
			}
			
			// Make the configuration based on the input data
			DeployConfiguration itConfig = new DeployConfiguration(
					itConfigFileName,
					netarchiveSuiteFileName,
					secPolicyFileName,
					logPropFileName,
					outputDir); 
			
			// Write the scripts
			itConfig.Write();

		} catch (Exception e) {
			System.out.println("ERROR: " + e);
		}
	}
}
