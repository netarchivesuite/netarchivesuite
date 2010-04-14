/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 *  USA
 */
package dk.netarkivet.archive.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;

import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaCacheDatabase;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;

/**
 * Method for reestablishing the admin database from a 'admin.data' file. 
 */
public class ReestablishAdminDatabase extends ToolRunnerBase {
    /**
     * Main method. 
     * Instantiates the tool and runs it.
     * 
     * @param argv The list of arguments.
     */
    public static void main(String[] argv) {
        ReestablishAdminDatabase instance = new ReestablishAdminDatabase();
        instance.runTheTool(argv);
    }
    
    /** 
     * Create an instance of the actual ReestablishAdminDatabaseTool.
     * 
     * @return an instance of ReestablishAdminDatabaseTool.
     */
    protected SimpleCmdlineTool makeMyTool() {
        return new ReestablishAdminDatabaseTool();
    }
    
    /**
     * The implementation of SimpleCmdlineTool for ReestablishAdminDatabase.
     */
    private static class ReestablishAdminDatabaseTool 
            implements SimpleCmdlineTool {
        /** The access to the database.*/
        private ReplicaCacheDatabase rcd;
        /** The admin data file to convert into the database.*/
        private File adminFile;
        /** The default name of the admin data file, if none is given.*/
        private static final String DEFAULT_ADMIN_DATA = "admin.data";
        /** The 'last modified' date for the admin.data file.*/
        private Date fileDate;
        
        /**
         * Method for testing validating the arguments.
         * 
         * @param args The list of arguments given to the tool.
         * @return Whether the arguments are valid.
         */
        public boolean checkArgs(String... args) {
            // Check if more than 1 argument is given
            if(args.length > 1) {
                System.err.println("Can handle at most 1 argument.");
                return false;
            }
            // Get file dependent on argument
            File checkFile;
            if(args.length < 1) {
                checkFile = new File(DEFAULT_ADMIN_DATA);
                System.out.println("Using default admin.data: " 
                        + checkFile.getAbsolutePath());
            } else {
                checkFile = new File(args[0]);
                System.out.println("Using given admin.data: "
                        + checkFile.getAbsolutePath());
            }
            // Check whether the file exists and is a file (not directory).
            if(!checkFile.isFile()) {
                System.err.println("The file '" + checkFile.getAbsolutePath() 
                        + "' is not a valid file.");
                return false;
            }
            // check whether the file can be read.
            if(!checkFile.canRead()) {
                System.err.println("Cannot read the file '" 
                        + checkFile.getAbsolutePath() + "'");
                return false;
            }
            
            // Ensure that the database is empty.
            if(!ReplicaCacheDatabase.getInstance().isEmpty()) {
                System.err.println("The database is not empty.");
                return false;
            }
            
            return true;
        }

        /**
         * Write the parameters for this tool.
         * It optionally takes the admin.data file as argument. If this 
         * argument is not given, then it is assumed to be located in the 
         * folder where the tool is run.
         * 
         * @return The parameters for this tool.
         */
        @Override
        public String listParameters() {
            return "[admin.data]";
        }

        /**
         * Execution of the tool. Retrieves the lines of the admin file, and
         * inserts them into the database. Also updates the date for date for
         * the filelist and checksumslist updates based on the 'last modified'
         * date of the admin file.
         * 
         * @param args The arguments for the tool.
         */
        @Override
        public void run(String... args) {
            // read and handle each line individually.
            BufferedReader in = null;
            long badlines = 0;
            long linesRead = 0;
            try {
                try {
                    in = new BufferedReader(new FileReader(adminFile));
                    String line = in.readLine();
                    linesRead++;
                    if(!line.contains(AdminData.VERSION_NUMBER)) {
                        System.err.println("The first line in Admin.data "
                                + "tells the version. Expected 0.4, but got: "
                                + line + ". Continues any way.");
                    } else {
                        System.out.println("Reading admin.data version " + line);
                    }
                    while ((line = in.readLine()) != null) {
                        linesRead++;
                        if(!rcd.insertAdminEntry(line)) {
                            // bad lines
                            badlines++;
                            System.err.println("Bad line(#" + badlines +"): ");
                            System.err.println(line);
                        } 
                        if ((linesRead % 10000) == 0) {
                            System.out.println("[" + new java.util.Date()
                                    + "] Processed " 
                                    + linesRead + " admin data lines");
                        }
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException e) {
                String msg = "An error occurred during reading the admin data "
                    + "file " + adminFile.getAbsolutePath();
                throw new IOFailure(msg, e);
            }
            
            // update the filelist and checksumlist dates for the replicas.
            rcd.setAdminDate(fileDate);
        }

        /**
         * Sets up the variables for the tool based on the arguments.
         * Retrieves the admin data file and the last modified date for this
         * file.
         * 
         * @param args The arguments for the tool.
         */
        @Override
        public void setUp(String... args) {
            if(args.length < 1) {
                adminFile = new File(DEFAULT_ADMIN_DATA);
            } else {
                adminFile = new File(args[0]);
            }
            
            // retrieve the last modified date for the file.
            fileDate = new Date(adminFile.lastModified());
            
            // initialize the connection to the database.
            rcd = ReplicaCacheDatabase.getInstance();
        }

        /**
         * Method for cleaning up afterwards.
         * Closes the connection to the database.
         */
        @Override
        public void tearDown() {
            rcd.cleanup();
        }
    }
}
