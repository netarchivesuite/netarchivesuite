/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.archive.io.arc.ARCWriter;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;

/**
 * This tool creates a CDX metadata file for a given jobID by running a
 * batch job on the bitarchive and processing the results to give a metadata
 * file.
 *
 * Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobId
 *
 */
public class CreateCDXMetadataFile extends ToolRunnerBase {
    /** Main method.  Creates and runs the tool object responsible for
     * batching over the bitarchive and creating a metadata file for a job.
     *
     * @param argv Arguments to the tool: jobID
     */
    public static void main(String[] argv) {
        new CreateCDXMetadataFile().runTheTool(argv);
    }

    /** Create the tool instance.
     *
     * @return A new tool object.
     */
    protected SimpleCmdlineTool makeMyTool() {
        return new CreateCDXMetadataFileTool();
    }

    /** The actual tool object that creates CDX files.
     */
    private static class CreateCDXMetadataFileTool
            implements SimpleCmdlineTool {
        /** The connection to the arc repository. */
        private ViewerArcRepositoryClient arcrep;
        /** The file pattern that matches an ARC file name without the jobID.
         * If combined with a jobID, this will match filenames like
         * 42-117-20051212141240-00001-sb-test-har-001.statsbiblioteket.dk.arc
         */
        private static final String REMAINING_ARC_FILE_PATTERN =
                "-\\d+-\\d+-\\d+-.*";

        /** Check that a valid jobID were given.  This does not check whether
         * jobs actually exist for that ID.
         *
         * @param args The args given on the command line.
         * @return True if the args are legal.
         */
        public boolean checkArgs(String... args) {
            if (args.length < 1) {
                System.err.println("Missing jobID argument");
                return false;
            }
            if (args.length > 1) {
                System.err.println("Too many arguments: '"
                        + StringUtils.conjoin("', '", Arrays.asList(args))
                        + "'");
                return false;
            }
            try {
                if (Long.parseLong(args[0]) < 1) {
                    System.err.println("" + args[0]
                            + " is not a valid job ID");
                    return false;
                }
                return true;
            } catch (NumberFormatException e) {
                System.err.println("'" + args[0] + "' is not a valid job ID");
                return false;
            }
        }

        /**
         * Create required resources here (the ArcRepositoryClient instance).
         * Resources created here should be released in tearDown, which is
         * guaranteed to be run.
         *
         * @param args The arguments that were given on the command line
         * (not used here)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
        }

        /**
         * Closes all resources we are using, which is only the
         * ArcRepositoryClient.  This is guaranteed to be called at shutdown.
         */
        public void tearDown() {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }

        /** The workhorse method of this tool: Runs the batch job,
         * copies the result, then turns the result into a proper
         * metadata file.
         *
         * @param args Arguments given on the command line.
         */
        public void run(String... args) {
            long jobID = Long.parseLong(args[0]);
            FileBatchJob job = new ExtractCDXJob();
            job.processOnlyFilesMatching(jobID + REMAINING_ARC_FILE_PATTERN);
            BatchStatus status = arcrep.batch(
                    job, Settings.get(CommonSettings.USE_REPLICA_ID));
            if (status.hasResultFile()) {
                File resultFile = null;
                try {
                    resultFile = File.createTempFile("extract-batch", ".cdx",
                            FileUtils.getTempDir());
                    resultFile.deleteOnExit();
                    status.copyResults(resultFile);
                    arcifyResultFile(resultFile, jobID);
                } catch (IOException e) {
                    throw new IOFailure("Error getting results for job "
                            + jobID, e);
                } finally {
                    if (resultFile != null) {
                        FileUtils.remove(resultFile);
                    }
                }
            }
        }

        /** Turns a raw CDX file for the given jobID into a metadatafile
         * containing the CDX lines in one ARC entry per ARC file indexed.
         * The output is put into a file called &lt;jobID&gt;-metadata-1.arc.
         *
         * @param resultFile The CDX file returned by a ExtractCDXJob for the
         * given jobID.
         * @param jobID The jobID we work on.
         * @throws IOException If an I/O error occurs, or the resultFile
         * does not exist
         */
        private void arcifyResultFile(File resultFile, long jobID)
                throws IOException {
            BufferedReader reader
                    = new BufferedReader(new FileReader(resultFile));
            try {
                ARCWriter writer = ARCUtils.createARCWriter(
                        new File(HarvestDocumentation.getMetadataARCFileName(
                                        Long.toString(jobID))));
                try {
                    String line;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String lastFilename = null;
                    FileUtils.FilenameParser parser = null;
                    while ((line = reader.readLine()) != null) {
                        // parse filename out of line
                        parser = parseLine(line, jobID);
                        if (parser == null) { // Bad line, try the next
                            continue;
                        }
                        if (!parser.getFilename().equals(lastFilename)) {
                            // When we reach the end of a block of lines from
                            // one ARC file, we write those as a single entry.
                            writeCDXEntry(writer, parser, baos.toByteArray());
                            baos.reset();
                            lastFilename = parser.getFilename();
                        }
                        baos.write(line.getBytes());
                        baos.write("\n".getBytes());
                    }
                    if (parser != null) {
                        writeCDXEntry(writer, parser, baos.toByteArray());
                    }
                } finally {
                    writer.close();
                }
            } finally {
                reader.close();
            }
        }

        /** Utility method to parse out the parts of a CDX line.
         * If a different jobID is found in the CDX line than we're given,
         * or the CDX line is unparsable, we print an error message and return
         * null, expecting processing to continue.
         *
         * @param line The line to parse.
         * @param jobID The job we're working on.
         * @return An object containing the salient parts of the filename
         * of the ARC file as mentioned in the given CDX line, or null if
         * the filename didn't match the job we're working on.
         */
        private FileUtils.FilenameParser parseLine(String line, long jobID) {
            try {
                String filename = new CDXRecord(line).getArcfile();
                FileUtils.FilenameParser filenameParser =
                        new FileUtils.FilenameParser(new File(filename));
                if (!filenameParser.getJobID().equals(Long.toString(jobID))) {
                    System.err.println("Found entry for job "
                            + filenameParser.getJobID() + " while looking for "
                            + jobID + " in " + line);
                    return null;
                }
                return filenameParser;
            } catch (NetarkivetException e) {
                System.err.println("Error parsing CDX line '" + line + "': "
                        + e);
                return null;
            }
        }

        /** Writes a full entry of CDX files to the ARCWriter.
         *
         * @param writer The writer we're currently writing to.
         * @param parser The filename of all the entries stored in baos.  This
         * is used to generate the URI for the entry.
         * @param bytes The bytes of the CDX records to be written under this
         * entry.
         * @throws IOFailure if the write fails for any reason
         */
        private void writeCDXEntry(ARCWriter writer,
                                   FileUtils.FilenameParser parser,
                                   byte[] bytes)
                throws IOFailure {
            try {
                ByteArrayInputStream bais
                        = new ByteArrayInputStream(bytes);
                writer.write(HarvestDocumentation.getCDXURI(
                        parser.getHarvestID(), parser.getJobID(),
                        parser.getTimeStamp(), parser.getSerialNo()).toString(),
                        Constants.CDX_MIME_TYPE,
                        SystemUtils.getLocalIP(),
                        System.currentTimeMillis(),
                        bytes.length, bais);
            } catch (IOException e) {
                throw new IOFailure("Failed to write ARC entry with CDX lines "
                        + "for " + parser.getFilename(), e);
            }
        }

        /** Return a string describing the parameters accepted by the
         * CreateCDXMetadataFile tool.
         *
         * @return String with description of parameters.
         */
        public String listParameters() {
            return "jobID";
        }
    }
}
