/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.util.anvl.ANVLRecord;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.archive.ArchiveProfile;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriterWarc;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData;

/**
 * This class contains code for documenting a harvest.
 * Metadata is read from the directories associated with a given
 * harvest-job-attempt (i.e. one DoCrawlMessage sent to a harvest server).
 * The collected metadata are written to a new metadata file that is managed
 * by IngestableFiles. Temporary metadata files will be deleted after this
 * metadata file has been written.
 */
public class HarvestDocumentation {

    private static Log log
            = LogFactory.getLog(HarvestDocumentation.class);

    /** Constants used in constructing URI for CDX content. */
    private static final String CDX_URI_SCHEME =
        "metadata";
    private static final String CDX_URI_AUTHORITY_HOST =
        Settings.get(CommonSettings.ORGANIZATION);
    private static final String CDX_URI_PATH =
        "/crawl/index/cdx";
    private static final String CDX_URI_VERSION_PARAMETERS =
        "majorversion=1&minorversion=0";
    private static final String ALTERNATE_CDX_URI_VERSION_PARAMETERS =
            "majorversion=2&minorversion=0";
        
    private static final String CDX_URI_HARVEST_ID_PARAMETER_NAME =
        "harvestid";
    private static final String CDX_URI_JOB_ID_PARAMETER_NAME =
        "jobid";
    private static final String CDX_URI_TIMESTAMP_PARAMETER_NAME =
        "timestamp";
    private static final String CDX_URI_SERIALNO_PARAMETER_NAME =
        "serialno";
    private static final String CDX_URI_FILENAME_PARAMETER_NAME = "filename";

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
     * TODO Place preharvestmetadata in IngestableFiles-defined area
     * TODO This method may be a good place to copy deduplicate information from the
     * crawl log to the cdx file.
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
        ArgumentNotValid.checkExistsDirectory(crawlDir, "crawlDir");
        // Prepare metadata-arcfile for ingestion of metadata, and enumerate
        // items to ingest.
        IngestableFiles ingestables = new IngestableFiles(crawlDir, jobID);

        // If metadata-arcfile already exists, we are done
        // See bug 722
        if (ingestables.isMetadataReady()) {
            log.warn("The metadata-file '" 
                    + ingestables.getMetadataFile().getAbsolutePath() 
                    + "' already exists, so we don't make another one!");
            return;
        }
        List<File> filesAddedAndNowDeletable = null;
        
        try {
            MetadataFileWriter mdfw = null;
            mdfw = ingestables.getMetadataWriter();
            
            if (mdfw instanceof MetadataFileWriterWarc) {
                // add warc-info record
                ANVLRecord infoPayload = new ANVLRecord(3);
                infoPayload.addLabelValue("software", "NetarchiveSuite/" 
                        + dk.netarkivet.common.Constants.getVersionString() 
                        + "/" + dk.netarkivet.common.Constants.PROJECT_WEBSITE);
                infoPayload.addLabelValue("ip", SystemUtils.getLocalIP());
                infoPayload.addLabelValue("hostname", SystemUtils.getLocalHostName());
                infoPayload.addLabelValue("conformsTo", "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");
                
                PersistentJobData psj = new PersistentJobData(crawlDir);
                infoPayload.addLabelValue("isPartOf", "" + psj.getJobID());
                MetadataFileWriterWarc mfww = (MetadataFileWriterWarc) mdfw; 
                mfww.insertInfoRecord(infoPayload);
            }
            
            //  Fetch any serialized preharvest metadata objects, if they exists.
            List<MetadataEntry> storedMetadata = getStoredMetadata(crawlDir);
            try {
                for (MetadataEntry m : storedMetadata) {
                     mdfw.write(m.getURL(), m.getMimeType(),
                            SystemUtils.getLocalIP(),
                            System.currentTimeMillis(), m.getData());
                }
            } catch (IOException e) {
                log.warn("Unable to write pre-metadata to metadata archivefile", e);
            }
            
            // Insert the harvestdetails into metadata archivefile.
            filesAddedAndNowDeletable = writeHarvestDetails(jobID, harvestID,
                       crawlDir, mdfw, Constants.getHeritrixVersionString());
            // All these files just added to the metadata archivefile can now be deleted 
            // except for the files we need for later processing):
            //  - crawl.log is needed to create domainharvestreport later
            //  - harvestInfo.xml is needed to upload stored data after
            //    crashes/stops on the harvesters
            //  - progress-statistics.log is needed to find out if crawl ended due
            //    to hitting a size limit, or due to other completion
            
            Iterator<File> iterator = filesAddedAndNowDeletable.iterator();
            while (iterator.hasNext()) {
                File f = iterator.next();
                if (f.getName().equals("crawl.log")
                        || f.getName().equals("harvestInfo.xml")
                        || f.getName().equals("progress-statistics.log")) {
                    iterator.remove();
                }
            }
            
            boolean cdxGenerationSucceeded = false;
            
            // Try to create CDXes over ARC and WARC files.            
            File arcFilesDir = ingestables.getArcsDir();
            File warcFilesDir = ingestables.getWarcsDir();
            
            if(arcFilesDir.isDirectory() && FileUtils.hasFiles(arcFilesDir)) {
                addCDXes(ingestables, arcFilesDir, mdfw, ArchiveProfile.ARC_PROFILE);
                cdxGenerationSucceeded = true;
            }
            if (warcFilesDir.isDirectory() && FileUtils.hasFiles(warcFilesDir)) {
                addCDXes(ingestables, warcFilesDir, mdfw, ArchiveProfile.WARC_PROFILE);
                cdxGenerationSucceeded = true;
            }
            
            if (cdxGenerationSucceeded) {
                // This indicates, that either the files in the arcsdir or in the warcsdir 
                // have now been CDX-processed.
                //
                // TODO refactor, as this call has too many sideeffects
                ingestables.setMetadataGenerationSucceeded(true);
            } else {
                log.warn("Found no archive directory with ARC og WARC files. Looked for dirs '" 
                        + arcFilesDir.getAbsolutePath() 
                        + "' and '" + warcFilesDir.getAbsolutePath() + "'.");
            }
        } finally {
            // If at this point metadata is not ready, an error occurred.
            if (!ingestables.isMetadataReady()) {
                ingestables.setMetadataGenerationSucceeded(false);
            } else {
                for (File fileAdded : filesAddedAndNowDeletable) {
                    FileUtils.remove(fileAdded);
                }
               ingestables.cleanup();
            }
        }
    }

    
    private static void addCDXes(IngestableFiles files, File archiveDir, MetadataFileWriter writer, 
            ArchiveProfile profile) {
        moveAwayForeignFiles(profile, archiveDir, files.getJobId());
        File cdxFilesDir = FileUtils.createUniqueTempDir(files.getTmpMetadataDir(), "cdx");
        CDXUtils.generateCDX(profile, archiveDir, cdxFilesDir);
        writer.insertFiles(cdxFilesDir, FileUtils.CDX_FILE_FILTER, Constants.CDX_MIME_TYPE);
    }
    
    
    /**
     * Restore serialized MetadataEntry objects from the "metadata" subdirectory of
     * the crawldir.
     * @param crawlDir the given crawl directory
     * @return a set of deserialized MetadataEntry objects
     */
    private static List<MetadataEntry> getStoredMetadata(File crawlDir) {
        File metadataDir = new File(crawlDir, IngestableFiles.METADATA_SUB_DIR);
        if (!metadataDir.isDirectory()) {
            log.warn("Should have an metadata directory '" + metadataDir.getAbsolutePath()
                    +  "' but there wasn't");
            return new ArrayList<MetadataEntry>();
        } else {
            return MetadataEntry.getMetadataFromDisk(metadataDir);
        }
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
     * @deprecated Should use the {@link HarvestDocumentation#getAlternateCDXURI(long, String)
     */
    public static URI getCDXURI(
            String harvestID,
            String jobID,
            String timeStamp,
            String serialNumber)
    throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(harvestID, "harvestID");
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        ArgumentNotValid.checkNotNull(timeStamp, "timeStamp");
        ArgumentNotValid.checkNotNull(serialNumber, "serialNumber");
        URI result;
        try {
            result =
                new URI(
                    CDX_URI_SCHEME,
                    null, //Don't include user info (e.g. "foo@")
                    CDX_URI_AUTHORITY_HOST,
                    -1, //Don't include port no. (e.g. ":8080")
                    CDX_URI_PATH,
                    getCDXURIQuery(harvestID, jobID, timeStamp, serialNumber),
                    null); //Don't include fragment (e.g. "#foo")
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
     * Generates a URI identifying CDX info for one harvested ARC file.
     *
     * @param jobID The number of the job that generated the ARC file.
     * @param filename the filename.
     * 
     * @return A URI in the proprietary schema "metadata".
     * @throws ArgumentNotValid if any parameter is null.
     * @throws UnknownID if something goes terribly wrong in our URI
     * construction.
     */
    public static URI getAlternateCDXURI(
            long jobID,
            String filename)
    throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        ArgumentNotValid.checkNotNull(filename, "filename");
        URI result;
        try {
            result =
                new URI(
                    CDX_URI_SCHEME,
                    null, //Don't include user info (e.g. "foo@")
                    CDX_URI_AUTHORITY_HOST,
                    -1, //Don't include port no. (e.g. ":8080")
                    CDX_URI_PATH,
                    getAlternateCDXURIQuery(jobID, filename),
                    null); //Don't include fragment (e.g. "#foo")
        } catch (URISyntaxException e) {
            throw new UnknownID(
                    "Failed to generate URI for "
                    + jobID + ","
                    + filename + ",",
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
     * @deprecated Should use instead {@link HarvestDocumentation#getAlternateCDXURIQuery(long, String)
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
     * Generate the query part of a CDX URI. Alternate version
     * @param jobID The number of the job that generated the ARC file.
     * @param filename the filename of the arcfile
     * @return An appropriate list of assigned parameters,
     * separated by the "&" character.
     */
    private static String getAlternateCDXURIQuery(
            long jobID,
            String filename
            ) {
        String result = ALTERNATE_CDX_URI_VERSION_PARAMETERS;
        result += "&" + CDX_URI_JOB_ID_PARAMETER_NAME + "=" + jobID;
        result += "&" + CDX_URI_FILENAME_PARAMETER_NAME + "=" + filename;
        return result;
    }
    
    
    

    /**
     * Iterates over the ARC files in the given dir and moves away files
     * that do not belong to the given job.  Files whose jobid we can parse
     * will be moved to a directory under oldjobs named with that jobid,
     * otherwise they will go into a directory under oldjobs named with a
     * timestamp.
     *
     * @param archiveProfile archive profile including filters, patterns, etc.
     * @param dir A directory containing one or more ARC files.
     * @param jobID ID of the job whose directory we're in.
     */
    private static void moveAwayForeignFiles(ArchiveProfile archiveProfile,
            File dir, long jobID) {
        File[] archiveFiles = dir.listFiles(archiveProfile.filename_filter);
        File oldJobsDir = new File(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
        File unknownJobDir = new File(oldJobsDir,
                "lost-files-" + new Date().getTime());
        List<File> movedFiles = new ArrayList<File>();
        for (File arcFile : archiveFiles) {
            long foundJobID = -1;
            try {
                ArchiveFilenameParser parser = 
                        ArchiveFileNamingFactory.getInstance()
                            .getArchiveFilenameParser(arcFile);
                foundJobID = Long.parseLong(parser.getJobID());
            } catch (UnknownID e) {
                // Non-Heritrix-generated ARC file
                Matcher matcher =
                    archiveProfile.metadataFilenamePattern.matcher(arcFile.getName());
                if (matcher.matches()) {
                    foundJobID = Long.parseLong(matcher.group(1));
                }
            }
            if (foundJobID != jobID) {
                File arcsDir;
                if (foundJobID == -1) {
                    arcsDir = new File(unknownJobDir,
                            archiveProfile.archive_directory);
                } else {
                    arcsDir = new File(oldJobsDir,
                                foundJobID + "-lost-files/"
                                + archiveProfile.archive_directory);
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
     * Write harvestdetails to archive file(s).
     * This includes the order.xml, seeds.txt,
     * specific settings.xml for certain domains,
     * the harvestInfo.xml,
     * All available reports (subset of HeritrixFiles.HERITRIX_REPORTS),
     * All available logs (subset of HeritrixFiles.HERITRIX_LOGS).
     *
     * @param jobID the given job Id
     * @param harvestID the id for the harvestdefinition, which created this job
     * @param crawlDir the directory where the crawljob took place
     * @param writer an MetadaFileWriter used to store the harvest configuration,
     *      and harvest logs and reports.
     * @param heritrixVersion the heritrix version used by the harvest. 
     * @throws ArgumentNotValid If null arguments occur
     * @return a list of files added to the archive file.
     */
    private static List<File> writeHarvestDetails(long jobID,
            long harvestID, File crawlDir, MetadataFileWriter mdfw,
            String heritrixVersion) {
        List<File> filesAdded = new ArrayList<File>();

        // We will sort the files by URL
        TreeSet<MetadataFile> files = new TreeSet<MetadataFile>();

        // List heritrix files in the crawl directory
        File[] heritrixFiles = crawlDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.isFile() && f.getName().matches(
                        MetadataFile.HERITRIX_FILE_PATTERN));
            }
        });

        // Add files in the crawl directory
        for (File hf : heritrixFiles) {
            files.add(new MetadataFile(hf, harvestID, jobID, heritrixVersion));
        }   
        // Generate an arcfiles-report.txt if configured to do so.
        boolean genArcFilesReport = Settings.getBoolean(
                HarvesterSettings.METADATA_GENERATE_ARCFILES_REPORT);
        if (genArcFilesReport) {
            log.debug("Creating an arcfiles-report.txt");
            files.add(new MetadataFile(new ArcFilesReportGenerator(crawlDir)
                    .generateReport(), harvestID, jobID, heritrixVersion));
        } else {
            log.debug("Creation of the arcfiles-report.txt has been disabled"
                    + "by the setting '" + HarvesterSettings.METADATA_GENERATE_ARCFILES_REPORT
                    + "'!");
        }
        
        // Add log files
        File logDir = new File(crawlDir, "logs");
        if (logDir.exists()) {
            File[] heritrixLogFiles = logDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return (f.isFile() && f.getName().matches(
                            MetadataFile.LOG_FILE_PATTERN));
                }
            });
            for (File logFile : heritrixLogFiles) {
                files.add(
                        new MetadataFile(
                                logFile, harvestID, jobID, heritrixVersion));
                log.info("Found Heritrix log file " + logFile.getName());
            }
        } else {
            log.debug("No logs dir found in crawldir: "
                      + crawlDir.getAbsolutePath());
        }

        // Check if exists any settings directory (domain-specific settings)
        // if yes, add any settings.xml hiding in this directory.
        // TODO Delete any settings-files found in the settings directory */
        File settingsDir = new File(crawlDir, "settings");
        if (settingsDir.isDirectory()) {
            Map<File, String> domainSettingsFiles =
                findDomainSpecificSettings(settingsDir);
            for (Map.Entry<File, String> entry : domainSettingsFiles
                    .entrySet()) {

                File dsf = entry.getKey();
                String domain = entry.getValue();
                files.add(
                        new MetadataFile(
                                dsf,
                                harvestID, jobID, heritrixVersion,
                                domain));
            }
        } else {
            log.debug("No settings directory found in crawldir: "
                    + crawlDir.getAbsolutePath());
        }

        // Write files in order to metadata archive file.
        for (MetadataFile mdf : files) {
            File heritrixFile = mdf.getHeritrixFile();
            String heritrixFileName = heritrixFile.getName();
            String mimeType =
                (heritrixFileName.endsWith(".xml") ? "text/xml" : "text/plain");
            if (mdfw.writeTo(heritrixFile, mdf.getUrl(), mimeType)) {
                filesAdded.add(heritrixFile);
            } else {
                log.warn("The Heritrix file '" + heritrixFile.getAbsolutePath() 
                        + "' was not included in the metadata archivefile due to some error.");
            }
        }

        return filesAdded;
    }

    /** Finds domain-specific configurations in the settings subdirectory of the
     * crawl directory.
     * @param settingsDir the given settings directory
     * @return the settings file paired with their domain..
     */
    private static Map<File, String> findDomainSpecificSettings(
            File settingsDir) {

        // find any domain specific configurations (settings.xml)
        List<String> reversedDomainsWithSettings =
            findAllDomainsWithSettings(settingsDir, "");

        Map<File, String> settingsFileToDomain = new HashMap<File, String>();
        if (reversedDomainsWithSettings.isEmpty()) {
            log.debug("No settings/<domain> directories exists: "
                    + "no domain-specific configurations available");
        } else {
            for (String reversedDomain: reversedDomainsWithSettings) {
                String domain = reverseDomainString(reversedDomain);
                File settingsXmlFile = new File(
                        settingsDir
                            + reversedDomain.replaceAll("\\.", File.separator),
                        MetadataFile.DOMAIN_SETTINGS_FILE);
                if (!settingsXmlFile.isFile()) {
                    log.debug("Directory settings/"
                              + domain + "/" + MetadataFile.DOMAIN_SETTINGS_FILE
                              + " does not exist.");
                } else  {
                    settingsFileToDomain.put(settingsXmlFile, domain);
                }
            }
        }
        return settingsFileToDomain;
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
                if (fileInDir.getName().equals(
                        MetadataFile.DOMAIN_SETTINGS_FILE)) {
                    // Store the domain, so that we can find the file later
                    filesToReturn.add(domainReversed);
                }
            }
        }
        return filesToReturn;
    }

    /**
     * Reverses a domain string, e.g. reverses "com.amazon" to "amazon.com"
     * @param reversedDomain the domain name to reverse
     * @return the reversed domain string
     */
    private static String reverseDomainString(String reversedDomain) {
        String domain = "";
        String remaining = reversedDomain;
        int lastDotIndex = remaining.lastIndexOf(".");
        while (lastDotIndex != -1) {
            domain += remaining.substring(lastDotIndex + 1) + ".";
            remaining = remaining.substring(0, lastDotIndex);
            lastDotIndex = remaining.lastIndexOf(".");
        }
        return domain.substring(0, domain.length() - 1);
    }
}
