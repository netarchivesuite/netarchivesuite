/* File:        $Id: GetRecord.java 79 2007-09-26 08:27:29Z kfc $
 * Revision:    $Revision: 79 $
 * Author:      $Author: kfc $
 * Date:        $Date: 2007-09-26 10:27:29 +0200 (Wed, 26 Sep 2007) $
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

package dk.netarkivet.archive.tools;

import java.io.File;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;

/**
 * A command-line tool to get ARC files from the bitarchive.
 *
 * Usage: 
 * 	java dk.netarkivet.archive.tools.GetFile arcfilename [destination-file]
 */

public class GetFile extends ToolRunnerBase {

    /**
     * Main method. Retrieves a file from the bitarchive and copies it to
     * current working directory.
     * Setup, teardown and run is delegated to the GetFileTool class.
     * Management of this, exception handling etc. is delegated to
     * ToolRunnerBase class.
     *
     * @param argv Takes one or two command line parameter: 
     *    the name of the file to retrieve. 
     *    optionally, the name of the destination file.
     */
    public static void main(String[] argv) {
        GetFile instance = new GetFile();
        instance.runTheTool(argv);
    }

    protected SimpleCmdlineTool makeMyTool() {
        return new GetFileTool();
    }

    private static class GetFileTool implements SimpleCmdlineTool {
        /**
         * This instance is declared outside of run method to ensure reliable
         * teardown in case of exceptions during execution.
         */
        private ViewerArcRepositoryClient arcrep;
        
        /**
         * the bitarchive location requested to deliver the file.
         */
        Location myLocation;

        /**
         * Accept 1 or 2 parameters.
         *
         * @param args the arguments
         * @return true, if length of args list is 1 or 2; 
         * 	returns false otherwise
         */
        public boolean checkArgs(String... args) {
            return (args.length > 0 && args.length < 3);
        }

        /**
         * Create the ArcRepositoryClient instance here for reliable execution
         * of close method in teardown.
         *
         * @param args the arguments (not used)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
            myLocation = Location.get(Settings.get(
            		Settings.ENVIRONMENT_THIS_LOCATION)
            		);
        }

        /**
         * Ensure reliable execution of the ArcRepositoryClient.close() method.
         * Remember to check if arcrep was actually created. Also reliably
         * cleans up the JMSConnection.
         */
        public void tearDown() {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }

        /**
         * Perform the actual work. Procure the necessary information from
         * command line parameters and system settings required to run the
         * ViewerArcRepositoryClient.getFile(), and perform the operation.
         * Creating and closing the ArcRepositoryClient (arcrep) is
         * done in setup and teardown methods.
         *
         * @param args the arguments
         */
        public void run(String... args) {
            try {
            	String filename = args[0];
            	File destfile;
            	if (args.length != 2) {
            		destfile = new File(filename);
            	} else {
            		destfile = new File(args[1]);
            	}
            	System.out.println("Retrieving file '" + filename
            			+ "' from location '" + myLocation.getName()
            			+ "' as file " + destfile.getAbsolutePath());
            	arcrep.getFile(filename, myLocation, destfile);
            	
            } catch (NetarkivetException e) {
               System.out.println("Execution of arcrep.getFile(arcfilename, " +
               		"location, toFile) failed).");
               e.printStackTrace();
               System.exit(1);
            }
        }

        /**
         * Return the list of parameters accepted by the GetFileTool class.
         *
         * @return the list of parameters accepted.
         */
        public String listParameters() {
            return "filename [destination-file]";
        }

    }
}
