/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCWriter;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.FileUtils.FilenameParser;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;

/**
 * This class contains code for documenting a harvest.
 * Metadata is read from the directories associated with a given
 * harvest-job-attempt (i.e. one DoCrawlMessage sent to a harvest server).
 * The collected metadata are written to a new ARC file that is managed
 * by IngestableFiles. Temporary metadata files will be deleted after this
 * metadata-ARC file has been written.
 */
public class HarvestDocumentation {
    private static Log log
            = LogFactory.getLog(HarvestDocumentation.class);

    /** Constants used in constructing URI for CDX content. */
    private static final String CDX_URI_SCHEME =
        "metadata";
    private static final String CDX_URI_AUTHORITY_HOST =
        Constants.ORGANIZATION_NAME;
    private static final String CDX_URI_PATH =
        "/crawl/index/cdx";
    private static final String CDX_URI_VERSION_PARAMETERS =
        "majorversion=1&minorversion=0";
    private static final String CDX_URI_HARVEST_ID_PARAMETER_NAME =
        "harvestid";
    private static final String CDX_URI_JOB_ID_PARAMETER_NAME =
        "jobid";
    private static final String CDX_URI_TIMESTAMP_PARAMETER_NAME =
        "timestamp";
    private static final String CDX_URI_SERIALNO_PARAMETER_NAME =
        "serialno";
    public static final Pattern metadataFilenamePattern =
            Pattern.compile("([0-9]+)-metadata-([0-9]+).arc");


    /**
     * Documents the harvest under the given dir in a packaged metadata arc
     * file in a directory 'metadata' under the current dir.
     * Only documents the files belonging to the given jobID, the rest are
     * moved to oldjobs.
     *
     * In the current implementation, the documentation consists
     * of CDX indices over all ARC files (with one CDX record per harvested
     * ARC file), plus packaging of log files.
     *
     * If this method finishes without an exception, it is guaranteed that
     * metadata is ready for upload.
     *
     * @param crawlDir The directory the crawl was performed in
     * @param jobID the ID of the job for this harvest
     * @param harvestID the ID of the harvestdefinition which created this job.
     * @throws ArgumentNotValid if crawlDir is null or does not exist, or if
     * jobID or harvestID is negative.
     * @throws IOFailure if
     *   - reading ARC files or temporary files fails
     *   - writing a file to arcFilesDir fails
     */
    public static void documentHarvest(File crawlDir, long jobID,
            long harvestID) throws IOFailure {
        ArgumentNotValid.checkNotNull(crawlDir, "crawlDir");
        ArgumentNotValid.checkNotNegative(jobID, "jobID");
        ArgumentNotValid.checkNotNegative(harvestID, "harvestID");
        //Verify parameter consistency.
        if (!crawlDir.isDirectory()) {
            String message = "'" + crawlDir.getAbsolutePath()
                             + "' does not exist or is not a directory.";
            log.warn(message);
            throw new ArgumentNotValid(message);
        }


        // Prepare metadata-arcfile for ingestion of metadata, and enumerate
        // items to ingest.
        IngestableFiles ingestables = new IngestableFiles(crawlDir, jobID);

        // If metadata-arcfile already exists, we are done
        // See bug 722
        if (ingestables.isMetadataReady()) {
            log.debug("The metadata-arc already exists, "
                      + "so we don't make another one!");
            return;
        }

        try {
            ARCWriter aw = null;
            aw = ingestables.getMetadataArcWriter();

            // insert the pre-harvest metadata file, if it exists.
            // TODO: Place preharvestmetadata in IngestableFiles-defined area
            File preharvestMetadata = new File(crawlDir,
                    getPreharvestMetadataARCFileName(jobID));
            if (preharvestMetadata.exists()) {
                ARCUtils.insertARCFile(preharvestMetadata, aw);
            }

            // Insert harvestdetails into metadata arcfile.
            List<File> filesAdded =
               writeHarvestDetails(jobID, harvestID,
                       crawlDir, aw, Constants.getHeritrixVersionString());
            // Note: we assume, that the ARCwriter is flushed after each write.
            // We can't do it specifically.

            // Delete the added files (except files we need later):
            // crawl.log is needed to create domainharvestreport later
            // harvestInfo.xml is needed to upload stored data after
            // crashes/stops on the harvesters
            // progress-statistics.log is needed to find out if crawl ended due
            // to hitting a size limit, or due to other completion
            for (File fileAdded : filesAdded) {
                if (!fileAdded.getName().equals("crawl.log")
                        && !fileAdded.getName().equals("harvestInfo.xml")
                        && !fileAdded.getName().equals("progress-statistics.log")) {
                    try {
                        FileUtils.remove(fileAdded);
                    } catch (IOFailure e) {
                        log.warn("Couldn't delete file '"
                                 + fileAdded.getAbsolutePath()
                                 + "' after adding in metadata file, ignoring.",
                                 e);
                    }
                }
            }

            // Create CDX over ARC files.
            File arcFilesDir = new File(crawlDir, Constants.ARCDIRECTORY_NAME);
            if (arcFilesDir.isDirectory()) {
                moveAwayForeignFiles(arcFilesDir, jobID);
                //Generate CDX
                // TODO: Place results in IngestableFiles-defined area
                File cdxFilesDir = FileUtils.createUniqueTempDir(crawlDir,
                                                                 "cdx");
                CDXUtils.generateCDX(arcFilesDir, cdxFilesDir);

                //For each CDX file...
                File[] cdxFiles
                        = cdxFilesDir.listFiles(FileUtils.CDX_FILE_FILTER);
                for (File cdxFile : cdxFiles) {
                    //...write its content to the ARCWriter
                    ARCUtils.writeFileToARC(aw, cdxFile,
                            getURIforFileName(cdxFile).toASCIIString(),
                            Constants.CDX_MIME_TYPE);
                    //...and delete it afterwards
                    try {
                        FileUtils.remove(cdxFile);
                    } catch (IOFailure e) {
                        log.warn("Couldn't delete file '"
                                 + cdxFile.getAbsolutePath()
                                 + "' after adding in metadata file, ignoring.",
                                 e);
                    }
                }
                ingestables.setMetadataGenerationSucceeded(true);
            } else {
                log.warn("No directory with ARC files found in '"
                         + arcFilesDir.getAbsolutePath() + "'");
            }

        } finally {
            // If at this point metadata is not ready, an error occurred.
            if (!ingestables.isMetadataReady()) {
                ingestables.setMetadataGenerationSucceeded(false);
            }
        }
    }

    /**
     * Parses the name of the given file
     * and generates a URI representation of it.
     * @param cdx A CDX file.
     * @return A URI appropriate for identifying the
     * file's content in Netarkivet.
     * @throws UnknownID if something goes terribly wrong in the CDX URI
     * construction.
     */
    private static URI getURIforFileName(File cdx)
        throws UnknownID {
        FilenameParser parser = new FilenameParser(cdx);
        return getCDXURI(
                parser.getHarvestID(),
                parser.getJobID(),
                parser.getTimeStamp(),
                parser.getSerialNo());
    }


    /**
     * Generates a URI identifying CDX info for one harvested ARC file.
     * In Netarkivet, all of the parameters below are in the ARC file's name.
     * @param harvestID The number of the harvest that generated the ARC file.
     * @param jobID The number of the job that generated the ARC file.
     * @param timeStamp The timestamp in the name of the ARC file.
     * @param serialNumber The serial no. in the name of the ARC file.
     * @return A URI in the proprietary schema "metadata".
     * @throws ArgumentNotValid if any parameter is null.
     * @throws UnknownID if something goes terribly wrong in our URI
     * construction.
     */
    public static URI getCDXURI(
            String harvestID,
            String jobID,
            String timeStamp,
            String serialNumber)
    throws ArgumentNotValid,UnknownID {
        ArgumentNotValid.checkNotNull(harvestID, "harvestID");
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        ArgumentNotValid.checkNotNull(timeStamp, "timeStamp");
        ArgumentNotValid.checkNotNull(serialNumber, "serialNumber");
        URI result;
        try {
            result =
                new URI(
                    CDX_URI_SCHEME,
                    null,//Don't include user info (e.g. "foo@")
                    CDX_URI_AUTHORITY_HOST,
                    -1,//Don't include port no. (e.g. ":8080")
                    CDX_URI_PATH,
                    getCDXURIQuery(harvestID, jobID, timeStamp, serialNumber),
                    null);//Don't include fragment (e.g. "#foo")
        } catch (URISyntaxException e) {
            throw new UnknownID(
                    "Failed to generate URI for "
                    + harvestID + ","
                    + jobID + ","
                    + timeStamp + ","
                    + serialNumber + ",",
                    e);
        }
        return result;
   }

    /**
     * Generate the query part of a CDX URI.
     * @param harvestID The number of the harvest that generated the ARC file.
     * @param jobID The number of the job that generated the ARC file.
     * @param timeStamp The timestamp in the name of the ARC file.
     * @param serialNumber The serial no. in the name of the ARC file.
     * @return An appropriate list of assigned parameters,
     * separated by the "&" character.
     */
    private static String getCDXURIQuery(
            String harvestID,
            String jobID,
            String timeStamp,
            String serialNumber) {
        String result = CDX_URI_VERSION_PARAMETERS;
        result += "&" + CDX_URI_HARVEST_ID_PARAMETER_NAME + "=" + harvestID;
        result += "&" + CDX_URI_JOB_ID_PARAMETER_NAME + "=" + jobID;
        result += "&" + CDX_URI_TIMESTAMP_PARAMETER_NAME + "=" + timeStamp;
        result += "&" + CDX_URI_SERIALNO_PARAMETER_NAME + "=" + serialNumber;
        return result;
    }

    /**
     * Generates a name for an ARC file containing "preharvest" metadata
     * regarding a given job (e.g. excluded alises).
     *
     * @param jobID the number of the harvester job
     * @return The file name to use for the preharvest metadata, as a String.
     * @throws ArgumentNotValid If jobId is negative
     */
    public static String getPreharvestMetadataARCFileName(long jobID)
        throws ArgumentNotValid {
        ArgumentNotValid.checkNotNegative(jobID, "jobID");
        return jobID + "-preharvest-metadata-" + 1 + ".arc";
    }

    /**
     * Iterates over the ARC files in the given dir and moves away files
     * that do not belong to the given job.  Files whose jobid we can parse
     * will be moved to a directory under oldjobs named with that jobid,
     * otherwise they will go into a directory under oldjobs named with a
     * timestamp.
     *
     * @param dir A directory containing one or more ARC files.
     * @param jobID ID of the job whose directory we're in.
     * @throws UnknownID If ?????????????????????????????????????
     */
    private static void moveAwayForeignFiles(File dir, long jobID)
        throws UnknownID {
        File[] arcFiles = dir.listFiles(FileUtils.ARCS_FILTER);
        File oldJobsDir = new File(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
        File unknownJobDir = new File(oldJobsDir,
                "lost-files-" + new Date().getTime());
        List<File> movedFiles = new ArrayList<File>();
        for (File arcFile : arcFiles) {
            long foundJobID = -1;
            try {
                FileUtils.FilenameParser parser = new FilenameParser(arcFile);
                foundJobID = Long.parseLong(parser.getJobID());
            } catch (UnknownID e) {
                // Non-Heritrix-generated ARC file
                Matcher matcher =
                    metadataFilenamePattern.matcher(arcFile.getName());
                if (matcher.matches()) {
                    foundJobID = Long.parseLong(matcher.group(1));
                }
            }
            if (foundJobID != jobID) {
                File arcsDir;
                if (foundJobID == -1) {
                    arcsDir = new File(unknownJobDir,
                            Constants.ARCDIRECTORY_NAME);
                } else {
                    arcsDir = new File(oldJobsDir,
                                foundJobID + "-lost-files/"
                                + Constants.ARCDIRECTORY_NAME);
                }
                try {
                    FileUtils.createDir(arcsDir);
                    File moveTo = new File(arcsDir, arcFile.getName());
                    arcFile.renameTo(moveTo);
                    movedFiles.add(moveTo);
                } catch (PermissionDenied e) {
                    log.warn("Not allowed to make oldjobs dir '"
                             + arcsDir.getAbsolutePath() + "'", e);
                }
            }
        }
        if (!movedFiles.isEmpty()) {
            log.warn("Found files not belonging to job " + jobID
                    + ", the following files have been stored for later: "
                    + movedFiles);
        }
    }

    /**
     * Write harvestdetails to arcfile(s).
     * This includes the order.xml, seeds.txt,
     * specific settings.xml for certain domains,
     * the harvestInfo.xml,
     * All available reports (subset of HeritrixFiles.HERITRIX_REPORTS),
     * All available logs (subset of HeritrixFiles.HERITRIX_LOGS).
     *
     * @param jobID the given job Id
     * @param harvestID the id for the harvestdefinition, which created this job
     * @param crawlDir the directory where the crawljob took place
     * @param writer an ARCWriter used to store the harvest configuration,
     *      and harvest logs and reports.
     * @param heritrixVersion the heritrix version used by the harvest.
     * @throws ArgumentNotValid If null arguments occur
     * @return a list of files added to the arc-file.
     */
    private static List<File> writeHarvestDetails(long jobID,
            long harvestID, File crawlDir, ARCWriter writer,
            String heritrixVersion) {
        List<File> filesAdded = new ArrayList<File>();
        HeritrixFiles harvestFiles =
            new HeritrixFiles(crawlDir, jobID, harvestID);
        String urlformatSuffix = "?heritrixVersion=%s&harvestid=%s&jobid=%s";
        String setupUrlFormat = "metadata://netarkivet.dk/crawl/setup/%s"
            + urlformatSuffix;
        String reportUrlFormat = "metadata://netarkivet.dk/crawl/reports/%s"
            + urlformatSuffix;
        String logUrlFormat = "metadata://netarkivet.dk/crawl/logs/%s"
            + urlformatSuffix;

        // Store order.xml, if it exists.
        File orderXML = new File(crawlDir, "order.xml");
        if (writeToArc(writer, orderXML,
                makeMetadataURL(setupUrlFormat, "order.xml",
                        harvestID, jobID, heritrixVersion),
                "text/xml")) {
            filesAdded.add(orderXML);
        }

        /* Check if exists any settings directory
           if yes, store any settings.xml hiding in this directory.
           TODO: Delete any settings-files found in the settings directory */
        File settingsDir = new File(crawlDir, "settings");
        if (settingsDir.isDirectory()) {
            storeSettingsXml(settingsDir,
                    makeMetadataURL(setupUrlFormat, "settings.xml",
                                harvestID, jobID, heritrixVersion),
                    writer);
        } else {
            log.debug("No settings directory found in crawldir: "
                      + crawlDir.getAbsolutePath());
        }

        // Store harvestInfo.xml, if it exists
        File harvestInfoFile = new File(crawlDir, "harvestInfo.xml");
        if (writeToArc(writer, harvestInfoFile,
                makeMetadataURL(setupUrlFormat, "harvestInfo.xml",
                harvestID, jobID, heritrixVersion),
                "text/xml")) {
            filesAdded.add(harvestInfoFile);
        }

        // Store seeds.txt, if it exists
        File seedsTxtFile = new File(crawlDir, "seeds.txt");
        if (writeToArc(writer, seedsTxtFile,
                makeMetadataURL(setupUrlFormat, "seeds.txt",
                        harvestID, jobID, heritrixVersion),
                "text/plain")) {
            filesAdded.add(seedsTxtFile);
        }

        // Store all available Heritrix Reports

        for (File reportFile : harvestFiles.getHeritrixReports()) {
            if (writeToArc(writer, reportFile,
                    makeMetadataURL(reportUrlFormat, reportFile.getName(),
                               harvestID, jobID, heritrixVersion),
                    "text/plain")) {
                filesAdded.add(reportFile);
            }
        }

        // Store all available Heritrix logs
        File logDir = new File(crawlDir, "logs");
        if (logDir.exists()) {
            for (File logFile : harvestFiles.getHeritrixLogs()) {

                if (writeToArc(writer, logFile,
                        makeMetadataURL(
                                logUrlFormat, logFile.getName(),
                                harvestID, jobID, heritrixVersion),
                        "text/plain")) {
                    filesAdded.add(logFile);
                }
            }
        } else {
            log.debug("No logs dir found in crawldir: "
                      + crawlDir.getAbsolutePath());
        }

        File outFile = new File(crawlDir, "heritrix.out");
        if (writeToArc(writer, outFile,
                       makeMetadataURL(
                               logUrlFormat, outFile.getName(),
                               harvestID, jobID, heritrixVersion),
                       "text/plain")) {
            filesAdded.add(outFile);
        }
        return filesAdded;
    }

    private static String makeMetadataURL(String urlFormat,
            String name, long harvestID, long jobID, String heritrixVersion) {
        return
            String.format(
                urlFormat,
                    name, heritrixVersion,
                    Long.toString(harvestID),
                    Long.toString(jobID)
        );
    }

    /** Writes a File to an ARCWriter, if available,
     * otherwise logs the failure to the class-logger.
     * @param writer the given ARCWriter
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     */
    private static boolean writeToArc(ARCWriter writer,
            File fileToArchive, String URL, String mimetype) {
        if (fileToArchive.isFile()) {
            try {
                ARCUtils.writeFileToARC(writer, fileToArchive,
                        URL, mimetype);
            } catch (IOFailure e) {
                log.warn("Error writing file '"
                        + fileToArchive.getAbsolutePath()
                        + "' to metadata ARC: ", e);
                return false;
            }
            log.debug("Wrote '" + fileToArchive.getAbsolutePath() + "' to '"
                      + writer.getFile().getAbsolutePath() + "'.");
            return true;
        } else {
            log.debug("No '" + fileToArchive.getName()
                      + "' found in dir: " + fileToArchive.getParent());
            return false;
        }
    }

    /** Store domain-specific configurations in ARC.
     * @param settingsDir the given settings directory
     * @param urlPrefix
     * @param writer The ARCWriter responsible for the writing.
     * @return list of files added to the ARCfile represented by
     *      the ARCWriter.
     */
    private static List<File> storeSettingsXml(File settingsDir, String urlPrefix,
            ARCWriter writer) {

        // find any domain specific configurations (settings.xml)
        List<String> reversedDomainsWithSettings =
            findAllDomainsWithSettings(settingsDir, "");
        List<File> filesAdded = new ArrayList<File>();
        if (reversedDomainsWithSettings.isEmpty()) {
            log.debug("No settings/<domain> directories exists: "
                    + "no domain-specific configurations available");
        } else {
            for (String reversedDomain: reversedDomainsWithSettings) {
                String domain =
                    new StringBuilder(reversedDomain).reverse().toString();
                File settingsXmlFile =
                    new File(settingsDir, reversedDomain.replace('.', '/'));
                if (!settingsXmlFile.isFile()) {
                    log.debug("Directory settings/"
                              + domain + "/settings.xml does not exist.");
                } else  {
                    if (writeToArc(writer, settingsXmlFile,
                            urlPrefix + "&domain=" + domain,
                    "text/xml")) {
                        filesAdded.add(settingsXmlFile);
                    }
                }
            }
        }
        return filesAdded;
    }

    /**
     * Find all domains which have a settings.xml file in the given directory.
     * @param directory a given directory
     * @param domainReversed the domain reversed
     * @return a list of domains (in reverse), which contained
     *  a file with given filename
     */
    private static List<String> findAllDomainsWithSettings(File directory,
            String domainReversed) {
        final String SETTINGSXML = "settings.xml";
        if (!directory.isDirectory()) {
            return new ArrayList<String>(0);
        }
        // List to hold the files temporarily
        List<String> filesToReturn = new ArrayList<String>();

        for (File fileInDir: directory.listFiles()) {
            // if the given file is a dir, then call
            // the method recursively.
            if (fileInDir.isDirectory()) {
                List<String> resultList =
                    findAllDomainsWithSettings(fileInDir,
                            domainReversed + "." + fileInDir.getName());
                if (!resultList.isEmpty()) {
                    filesToReturn.addAll(resultList);
                }
            } else {
                if (fileInDir.getName().equals(SETTINGSXML)) {
                    // Store the domain, so that we can find the file later
                    filesToReturn.add(domainReversed);
                }
            }
        }
        return filesToReturn;
    }

    /**
     * Document an old job from an oldjobs directory on the harvesters.
     * Generates a file named <jobid>-metadata-2.arc.
     * Note: This method sets "heritrixVersion" to the heritrix-version
     * written in the user-agent.
     * This version may not be correct!
     *
     * @param crawlDir the given crawlDir
     * @param jobID the given job-identifier
     * @param harvestID the given harvest-identifier
     * @return a list of files added to the arcfile
     */
    public static List<File> documentOldJob(File crawlDir, long jobID,
            long harvestID) {
        String jobIdString = Long.toString(jobID);
        ARCWriter aw = null;
        String metadataFilename =
           getMetadataARCFileName(jobIdString)
               .replaceAll("-1\\.arc$", "-2.arc");

        File metadataFile = new File(metadataFilename);
        aw = ARCUtils.createARCWriter(metadataFile);

        HeritrixFiles hf = new HeritrixFiles(crawlDir, jobID, harvestID);

        // Insert harvestdetails into metadata arcfile.
        return writeHarvestDetails(jobID, harvestID, crawlDir, aw,
                                   getHeritrixVersion(hf.getOrderXmlFile()));
    }

    private static String getHeritrixVersion(File orderXml) {
        Document doc = XmlUtils.getXmlDoc(orderXml);
        Node userAgentNode = doc.selectSingleNode(
                HeritrixTemplate.HERITRIX_USER_AGENT_XPATH);
        String userAgent = userAgentNode.getText();
        //We expect to find this: Mozilla/5.0 (compatible;
        // heritrix/1.5.0-200506132127 +http://netarkivet.dk/website/info.html)
        String[] useragentParts = userAgent.split(" ");
        // heritrix/1.5.0-200506132127
        String heritrixPart = null;
        for (String part : useragentParts) {
            if (part.startsWith("heritrix")) {
                heritrixPart = part;
            }
        }
        if (heritrixPart != null) {
            return heritrixPart.split("/")[1];
        } else {
            return "null";
        }

    }

    /**
     * Generates a name for an ARC file containing metadata regarding
     * a given job.
     *
     * @param jobID The number of the job that generated the ARC file.
     * @return A "flat" file name (i.e. no path) containing the jobID parameter
     * and ending on "-metadata-N.arc", where N is the serial number of the
     * metadata files for this job, e.g. "42-metadata-1.arc".  Currently,
     * only one file is ever made.
     * @throws ArgumentNotValid if any parameter was null.
     */
    public static String getMetadataARCFileName(String jobID)
        throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        return jobID + "-metadata-" + 1 + ".arc";
    }
}
