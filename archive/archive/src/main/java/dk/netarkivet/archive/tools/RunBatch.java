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

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.arc.LoadableFileBatchJob;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.Collection;

/**
 * A command-line tool to run batch jobs in the bitarchive.
 *
 * Usage:
 * 	java dk.netarkivet.archive.tools.RunBatch \
 *       classfile [regexp [location [outputfile]]
 *
 * where classfile is a file containing a FileBatchJob implementation
 *       regexp is a regular expression that will be matched against
 *              file names in the archive, by default .*
 *       location is the bitarchive location this should be run on, by
 *              default taken from settings.
 *       outputfile is a file where the output from the batch job will be
 *              written.  By default, it goes to stdout.
 *
 * Example:
 *
 * java dk.netarkivet.archive.tools.RunBatch FindMime.class 10-*.arc SB mimes
 *
 * Note that you probably want to set the HTTP port setting
 * (settings.common.port.http) to something other than its default value to
 * avoid clashing with other channel listeners.
 */
public class RunBatch extends ToolRunnerBase {
    /**
     * Main method.  Runs a batch job in the bitarchive.
     * Setup, teardown and run is delegated to the RunBatchTool class.
     * Management of this, exception handling etc. is delegated to
     * ToolRunnerBase class.
     *
     * @param argv Takes one to four command line parameters, only the first is
     * required:
     *   the name of a file containing an implementation of FileBatchJob
     *   a regular expression
     *   a bitarchive location
     *   a filename for output
     */
    public static void main(String[] argv) {
        RunBatch instance = new RunBatch();
        instance.runTheTool(argv);
    }

    /** Create an instance of the actual RunBatchTool. */
    protected SimpleCmdlineTool makeMyTool() {
        return new RunBatchTool();
    }

    /** The implementation of SimpleCmdlineTool for RunBatch. */
    private static class RunBatchTool implements SimpleCmdlineTool {
        /**
         * This instance is declared outside of run method to ensure reliable
         * teardown in case of exceptions during execution.
         */
        private ViewerArcRepositoryClient arcrep;

        /**
         * Accept 1 to 4 parameters and checks them for validity.
         *
         * @param args the arguments
         * @return true, if given arguments are valid
         * 	returns false otherwise
         */
        public boolean checkArgs(String... args) {
            if (args.length < 1) {
                System.err.println("Missing required argument: class file");
                return false;
            }
            if (args.length > 4) {
                System.err.println("Too many arguments");
                return false;
            }
            if (!new File(args[0]).canRead()) {
                System.err.println("File not found: '" + args[0] + "'");
                return false;
            }
            if (args.length > 1) {
                try {
                    Pattern.compile(args[1]);
                } catch (PatternSyntaxException e) {
                    System.err.println("Illegal pattern syntax: '"
                                       + args[1] + "'");
                    return false;
                }
            }
            if (args.length > 2 &&
                !Location.isKnownLocation(args[2])) {
                System.err.println("Unknown location '" + args[2]
                                   + "', known location are "
                                   + Location.getKnown());
                return false;
            }
            if (args.length > 3 &&
                !new File(args[3]).canWrite()) {
                System.err.println("Output file '" + args[3]
                                   + "' cannot be written to");
                return false;
            }
            return true;
        }

        /**
         * Create the ArcRepositoryClient instance here for reliable execution
         * of close method in teardown.
         *
         * @param args the arguments (not used)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
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
         * ViewerArcRepositoryClient.batch(), and perform the operation.
         * Creating and closing the ArcRepositoryClient (arcrep) is
         * done in setup and teardown methods.
         *
         * @param args the arguments
         */
        public void run(String... args) {
            String filename = args[0];
            LoadableFileBatchJob job
                    = new LoadableFileBatchJob(new File(filename));
            String regexp = ".*";
            if (args.length > 1) {
                regexp = args[1];
                job.processOnlyFilesMatching(regexp);
            }
            Location myLocation = Location.get(Settings.get(
                    Settings.ENVIRONMENT_THIS_LOCATION)
            );
            if (args.length > 2) {
                myLocation = Location.get(args[2]);
            }
            File outputFile = null;
            if (args.length > 3) {
                outputFile = new File(args[3]);
            }
            System.out.println("Running batch job '" + filename
                               + "' on files matching '" + regexp
                               + "' on location '" + myLocation.getName()
                               + "'");
            BatchStatus status = arcrep.batch(job, myLocation.getName());
            final Collection<File> failedFiles = status.getFilesFailed();
            System.out.println("Processed " + status.getNoOfFilesProcessed()
                               + " files with "
                               + failedFiles.size()
                               + " failures");
            if (outputFile != null) {
                status.copyResults(outputFile);
            } else {
                status.appendResults(System.out);
            }
            if (failedFiles.size() != 0) {
                System.out.println("Failed files:");
                for (File f : failedFiles) {
                    System.out.println(f.getName());
                }
            }
        }

        /**
         * Return the list of parameters accepted by the RunBatchTool class.
         *
         * @return the list of parameters accepted.
         */
        public String listParameters() {
            return "classfile [regexp [location]]";
        }

    }
}