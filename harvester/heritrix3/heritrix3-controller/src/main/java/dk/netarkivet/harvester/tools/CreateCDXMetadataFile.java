/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jwat.common.ANVLRecord;

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
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.cdx.ArchiveExtractCDXJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriterWarc;

/**
 * This tool creates a CDX metadata file for a given job's jobID and harvestPrefix by running a batch job on the
 * bitarchive and processing the results to give a metadata file. Use option -w to select WARC output, and -a to select
 * ARC output: If no option available, then warc mode is selected
 * <p>
 * Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile -w --jobID 2 --harvestID 5 --harvestnamePrefix 2-1 Usage: java
 * dk.netarkivet.harvester.tools.CreateCDXMetadataFile -a --jobID 2 --harvestID 5 --harvestnamePrefix 2-1 Usage: java
 * dk.netarkivet.harvester.tools.CreateCDXMetadataFile --jobID 2 --harvestID 5 --harvestnamePrefix 2-1
 * <p>
 * The CDX records is slightly different from the one produced normally. As we are not able to extract the timestamp,
 * and harvestID from the (W) arcfilenames, this information is not part of the CXDURI.
 */
public class CreateCDXMetadataFile extends ToolRunnerBase {

    public static final String ARCMODE = "arc";
    public static final String WARCMODE = "warc";
    public static final String usageString = "[-a|w] --jobID X --harvestID Y --harvestnamePrefix somePrefix";

    /**
     * Main method. Creates and runs the tool object responsible for batching over the bitarchive and creating a
     * metadata file for a job.
     *
     * @param argv Arguments to the tool: jobID harvestnamePrefix
     */
    public static void main(String[] argv) {
        new CreateCDXMetadataFile().runTheTool(argv);
    }

    /**
     * Create the tool instance.
     *
     * @return A new tool object.
     */
    protected SimpleCmdlineTool makeMyTool() {
        return new CreateCDXMetadataFileTool();
    }

    /**
     * The actual tool object that creates CDX files.
     */
    private static class CreateCDXMetadataFileTool implements SimpleCmdlineTool {
        /** Write output mode. Is it arc or warc mode. */
        private boolean isWarcOutputMode;
        /** Which jobId to process. */
        private long jobId;
        /** Which harvestId to process. */
        private long harvestId;
        /** HarvestnamePrefix used to locate the files for the job. */
        private String harvestnamePrefix;

        /** The connection to the arc repository. */
        private ViewerArcRepositoryClient arcrep;

        /**
         * The file pattern that matches an ARC or WARC file name without the jobID. If combined with a
         * harvestnameprefix, this will match filenames that begin with the given harvestname prefix.
         */
        private static final String REMAINING_ARCHIVE_FILE_PATTERN = ".*";

        /**
         * Checks that a valid jobID were given. This does not check whether jobs actually exist for that ID.
         *
         * @param args The args given on the command line.
         * @return True if the args are legal.
         */
        public boolean checkArgs(String... args) {
            final String ARC_OPTION_KEY = "a";
            final String WARC_OPTION_KEY = "w";
            final String JOBID_OPTION_KEY = "jobID";
            final String HARVESTID_OPTION_KEY = "harvestID";
            final String HARVESTNAMEPREFIX_OPTION_KEY = "harvestnamePrefix";

            OptionGroup metadataGroup = new OptionGroup();
            Option arcOption = new Option(ARC_OPTION_KEY, false, "write an metadata ARC file");
            Option warcOption = new Option(WARC_OPTION_KEY, false, "write an metadata WARC file");
            metadataGroup.addOption(arcOption);
            metadataGroup.addOption(warcOption);
            metadataGroup.setRequired(false);
            OptionGroup jobIDGroup = new OptionGroup();
            Option jobIdOption = new Option(JOBID_OPTION_KEY, true, "The JobID");
            jobIDGroup.addOption(jobIdOption);
            jobIDGroup.setRequired(true);

            OptionGroup harvestIDGroup = new OptionGroup();
            Option harvestIdOption = new Option(HARVESTID_OPTION_KEY, true, "The HarvestID");
            harvestIDGroup.addOption(harvestIdOption);
            harvestIDGroup.setRequired(true);

            Option harvestprefixOption = new Option(HARVESTNAMEPREFIX_OPTION_KEY, true, "The harvestnamePrefix");
            OptionGroup harvestnamePrefixGroup = new OptionGroup();
            harvestnamePrefixGroup.addOption(harvestprefixOption);
            harvestnamePrefixGroup.setRequired(true);
            Options options = new Options();
            options.addOptionGroup(metadataGroup);
            options.addOptionGroup(jobIDGroup);
            options.addOptionGroup(harvestIDGroup);
            options.addOptionGroup(harvestnamePrefixGroup);
            String jobIdString = null;
            String harvestIdString = null;

            CommandLineParser parser = new PosixParser();
            CommandLine cli = null;
            try {
                cli = parser.parse(options, args);
            } catch (MissingArgumentException e) {
                System.err.println("Missing or wrong arguments given");
                printUsage();
                return false;
            } catch (ParseException e) {
                System.err.println("Missing or wrong arguments given");
                printUsage();
                return false;
            }

            isWarcOutputMode = true; // the default
            // Only need to check for the ARC option, as the WARC option cannot be set at the same time
            // It is either one or none of them.
            if (cli.hasOption(ARC_OPTION_KEY)) {
                isWarcOutputMode = false;
            }
            jobIdString = cli.getOptionValue(JOBID_OPTION_KEY);
            harvestIdString = cli.getOptionValue(HARVESTID_OPTION_KEY);
            this.harvestnamePrefix = cli.getOptionValue(HARVESTNAMEPREFIX_OPTION_KEY);

            try {
                this.jobId = Long.parseLong(jobIdString);
                if (jobId < 1) {
                    System.err.println("'" + jobIdString + "' is not a valid job ID");
                    return false;
                }
            } catch (NumberFormatException e) {
                System.err.println("'" + jobIdString + "' is not a valid job ID");
                return false;
            }

            try {
                this.harvestId = Long.parseLong(harvestIdString);
                if (harvestId < 1) {
                    System.err.println("'" + harvestIdString + "' is not a valid harvest ID");
                    return false;
                }
            } catch (NumberFormatException e) {
                System.err.println("'" + harvestIdString + "' is not a valid harvest ID");
                return false;
            }

            return true;
        }

        /**
         * Create required resources here (the ArcRepositoryClient instance). Resources created here should be released
         * in tearDown, which is guaranteed to be run.
         *
         * @param args The arguments that were given on the command line (not used here)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
        }

        /**
         * Closes all resources we are using, which is only the ArcRepositoryClient. This is guaranteed to be called at
         * shutdown.
         */
        public void tearDown() {
            if (arcrep != null) {
                arcrep.close();
                if (arcrep.getClass().getName()
                        .equals("dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient")) {
                    JMSConnectionFactory.getInstance().cleanup();
                }
            }
        }

        /**
         * The workhorse method of this tool: Runs the batch job, copies the result, then turns the result into a proper
         * metadata file. This method assumes that the args have already been read by the checkArgs method, and thus
         * jobId has been parsed, and the isWarcOutputMode established
         *
         * @param args Arguments given on the command line.
         */
        public void run(String... args) {
            final long jobID = this.jobId;
            final long harvestId = this.harvestId;
            final String harvestPrefix = this.harvestnamePrefix;
            FileBatchJob job = new ArchiveExtractCDXJob();
            Settings.set(HarvesterSettings.METADATA_FORMAT, (isWarcOutputMode) ? "warc" : "arc");
            final String filePattern = harvestPrefix + REMAINING_ARCHIVE_FILE_PATTERN;

            System.out.println("Creating cdx-" + ((isWarcOutputMode) ? "warcfile" : "arcfile")
                    + " from file matching pattern '" + filePattern + "'.");
            job.processOnlyFilesMatching(filePattern);

            BatchStatus status = arcrep.batch(job, Settings.get(CommonSettings.USE_REPLICA_ID));
            if (status.hasResultFile()) {
                System.out.println("Got results from archive. Processing data");
                File resultFile = null;
                try {
                    resultFile = File.createTempFile("extract-batch", ".cdx", FileUtils.getTempDir());
                    resultFile.deleteOnExit();
                    status.copyResults(resultFile);
                    arcifyResultFile(resultFile, jobID, harvestId);
                } catch (IOException e) {
                    throw new IOFailure("Error getting results for job " + jobID, e);
                } finally {
                    if (resultFile != null) {
                        FileUtils.remove(resultFile);
                    }
                }
            } else {
                System.err.println("Got new results from archive. Program ending now");
            }
        }

        /**
         * Turns a raw CDX file for the given jobID into a metadatafile containing the CDX lines in one archive record
         * per each ARC or WARC file indexed. The output is put into a file called &lt;jobID&gt;-metadata-1.arc.
         *
         * @param resultFile The CDX file returned by a ExtractCDXJob for the given jobID.
         * @param jobID The jobID we work on.
         * @throws IOException If an I/O error occurs, or the resultFile does not exist
         */
        private void arcifyResultFile(File resultFile, long jobID, long harvestID) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(resultFile));

            File outputFile = new File(MetadataFileWriter.getMetadataArchiveFileName(Long.toString(jobID), harvestID));
            System.out.println("Writing cdx to file '" + outputFile.getAbsolutePath() + "'.");
            try {
                MetadataFileWriter writer = MetadataFileWriter.createWriter(outputFile);
                if (writer instanceof MetadataFileWriterWarc) {
                    insertWarcInfo((MetadataFileWriterWarc) writer, jobID);
                }
                try {
                    String line;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String lastFilename = null;
                    String newFilename = null;

                    while ((line = reader.readLine()) != null) {
                        // parse filename out of line
                        newFilename = parseLine(line, harvestnamePrefix);
                        if (newFilename == null) { // Bad line, try the next
                            continue;
                        }
                        if (lastFilename != null && !newFilename.equals(lastFilename)) {
                            // When we reach the end of a block of lines from
                            // one ARC/WARC file, we write those as a single entry.
                            writeCDXEntry(writer, newFilename, baos.toByteArray());
                            baos.reset();
                        }
                        baos.write(line.getBytes());
                        baos.write("\n".getBytes());
                        lastFilename = newFilename;
                    }
                    if (newFilename != null) {
                        writeCDXEntry(writer, newFilename, baos.toByteArray());
                    }
                } finally {
                    writer.close();
                }
            } finally {
                reader.close();
            }
        }

        private void insertWarcInfo(MetadataFileWriterWarc writer, Long jobID) {
            ANVLRecord infoPayload = new ANVLRecord();
            infoPayload.addLabelValue("software",
                    "NetarchiveSuite/" + dk.netarkivet.common.Constants.getVersionString(false) + "/"
                            + dk.netarkivet.common.Constants.PROJECT_WEBSITE);
            infoPayload.addLabelValue("ip", SystemUtils.getLocalIP());
            infoPayload.addLabelValue("hostname", SystemUtils.getLocalHostName());
            infoPayload
                    .addLabelValue("conformsTo", "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");
            infoPayload.addLabelValue("isPartOf", "" + jobID);
            writer.insertInfoRecord(infoPayload);
        }

        /**
         * Utility method to parse out the parts of a CDX line. If a different jobID is found in the CDX line than we're
         * given, or the CDX line is unparsable, we print an error message and return null, expecting processing to
         * continue.
         *
         * @param line The line to parse.
         * @param harvestnamePrefix .
         * @return An object containing the salient parts of the filename of the ARC file as mentioned in the given CDX
         * line, or null if the filename didn't match the job we're working on.
         */
        private String parseLine(String line, String harvestnamePrefix) {
            try {
                String filename = new CDXRecord(line).getArcfile();
                if (!filename.startsWith(harvestnamePrefix)) {
                    System.err.println("Found CXD-entry with unexpected filename '" + filename
                            + "': does not match harvestnamePrefix '" + harvestnamePrefix + "' in " + line);
                    return null;
                }
                return filename;
            } catch (NetarkivetException e) {
                System.err.println("Error parsing CDX line '" + line + "': " + e);
                return null;
            }
        }

        /**
         * Writes a full entry of CDX files to the ARCWriter.
         *
         * @param writer The writer we're currently writing to.
         * @param filename The filename of all the entries stored. This is used to generate the URI for the
         * entry.
         * @param bytes The bytes of the CDX records to be written under this entry.
         * @throws IOFailure if the write fails for any reason
         */
        private void writeCDXEntry(MetadataFileWriter writer, String filename, byte[] bytes) throws IOFailure {
            try {
                writer.write(MetadataFileWriter.getAlternateCDXURI(this.jobId, filename).toString(),
                        Constants.CDX_MIME_TYPE, SystemUtils.getLocalIP(), System.currentTimeMillis(), bytes);
            } catch (IOException e) {
                throw new IOFailure("Failed to write ARC/WARC entry with CDX lines " + "for " + filename, e);
            }
        }

        /**
         * Return a string describing the parameters accepted by the CreateCDXMetadataFile tool.
         *
         * @return String with description of parameters.
         */
        public String listParameters() {
            return usageString;
        }

        private static void printUsage() {
            System.err.println("Usage 1: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile"
                    + " -w --jobID 2 --harvestnamePrefix 2-1");
            System.err.println("Usage 2: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile"
                    + " -a --jobID 2 --harvestnamePrefix 2-1");
            System.err.println("Usage 3: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile"
                    + " --jobID 2 --harvestnamePrefix 2-1");
        }
    }
}
