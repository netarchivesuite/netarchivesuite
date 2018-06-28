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

package dk.netarkivet.harvester.heritrix3;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jwat.common.ANVLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.archive.ArchiveProfile;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.PersistentJobData;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriterWarc;

/**
 * This class contains code for documenting a H3 harvest. Metadata is read from the directories associated with a given
 * harvest-job-attempt (i.e. one DoCrawlMessage sent to a harvest server). The collected metadata are written to a new
 * metadata file that is managed by IngestableFiles. Temporary metadata files will be deleted after this metadata file
 * has been written.
 */
public class HarvestDocumentation {

    private static final Logger log = LoggerFactory.getLogger(HarvestDocumentation.class);

    /**
     * Documents the harvest under the given dir in a packaged metadata arc file in a directory 'metadata' under the
     * current dir. Only documents the files belonging to the given jobID, the rest are moved to oldjobs.
     * <p>
     * In the current implementation, the documentation consists of CDX indices over all ARC files (with one CDX record
     * per harvested ARC file), plus packaging of log files.
     * <p>
     * If this method finishes without an exception, it is guaranteed that metadata is ready for upload.
     * <p>
     * TODO Place preharvestmetadata in IngestableFiles-defined area 
     * TODO This method may be a good place to copy deduplicate information from the crawl log to the cdx file.
     *
     * @param ingestables Information about the finished crawl (crawldir, jobId, harvestID).
     * @throws ArgumentNotValid if crawlDir is null or does not exist, or if jobID or harvestID is negative.
     * @throws IOFailure if - reading ARC files or temporary files fails - writing a file to arcFilesDir fails
     */
    public static void documentHarvest(IngestableFiles ingestables) throws IOFailure {
        ArgumentNotValid.checkNotNull(ingestables, "ingestables");

        File crawlDir = ingestables.getCrawlDir();
        Long jobID = ingestables.getJobId();
        Long harvestID = ingestables.getHarvestID();

        // Prepare metadata-arcfile for ingestion of metadata, and enumerate
        // items to ingest.

        // If metadata-arcfile already exists, we are done
        // See bug 722
        if (ingestables.isMetadataReady()) {
            log.warn("The metadata-file '{}' already exists, so we don't make another one!", ingestables
                    .getMetadataFile().getAbsolutePath());
            return;
        }
        List<File> filesAddedAndNowDeletable = null;

        try {
            MetadataFileWriter mdfw;
            mdfw = ingestables.getMetadataWriter();

            if (mdfw instanceof MetadataFileWriterWarc) {
                // add warc-info record
                ANVLRecord infoPayload = new ANVLRecord();
                infoPayload.addLabelValue("software",
                        "NetarchiveSuite/" + dk.netarkivet.common.Constants.getVersionString(false) + "/"
                                + dk.netarkivet.common.Constants.PROJECT_WEBSITE);
                infoPayload.addLabelValue("ip", SystemUtils.getLocalIP());
                infoPayload.addLabelValue("hostname", SystemUtils.getLocalHostName());
                infoPayload.addLabelValue("conformsTo",
                        "http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");

                PersistentJobData psj = new PersistentJobData(crawlDir);
                infoPayload.addLabelValue("isPartOf", "" + psj.getJobID());
                MetadataFileWriterWarc mfww = (MetadataFileWriterWarc) mdfw;
                mfww.insertInfoRecord(infoPayload);
            }

            // Fetch any serialized preharvest metadata objects, if they exists.
            List<MetadataEntry> storedMetadata = getStoredMetadata(crawlDir);
            try {
                for (MetadataEntry m : storedMetadata) {
                    mdfw.write(m.getURL(), m.getMimeType(), SystemUtils.getLocalIP(), System.currentTimeMillis(),
                            m.getData());
                }
            } catch (IOException e) {
                log.warn("Unable to write pre-metadata to metadata archivefile", e);
            }

            // Insert the harvestdetails into metadata archivefile.
            filesAddedAndNowDeletable = writeHarvestDetails(jobID, harvestID, ingestables, mdfw, Constants.getHeritrix3VersionString());
            
            boolean cdxGenerationSucceeded = false;

            // Try to create CDXes over ARC and WARC files.
            File arcFilesDir = ingestables.getArcsDir();
            File warcFilesDir = ingestables.getWarcsDir();

            if (arcFilesDir.isDirectory() && FileUtils.hasFiles(arcFilesDir)) {
                addCDXes(ingestables, arcFilesDir, mdfw, ArchiveProfile.ARC_PROFILE);
                cdxGenerationSucceeded = true;
            }
            if (warcFilesDir.isDirectory() && FileUtils.hasFiles(warcFilesDir)) {
                addCDXes(ingestables, warcFilesDir, mdfw, ArchiveProfile.WARC_PROFILE);
                cdxGenerationSucceeded = true;
            }
            
            if (!cdxGenerationSucceeded) {
                log.warn("Found no archive directory with ARC og WARC files. Looked for dirs '{}' and '{}'.",
                        arcFilesDir.getAbsolutePath(), warcFilesDir.getAbsolutePath());
            }
            ingestables.closeMetadataFile();
        } finally {
            // If at this point metadata is not ready, an error occurred.
            if (!ingestables.isMetadataReady()) {
                ingestables.setErrorState(true);
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
        moveAwayForeignFiles(profile, archiveDir, files);
        File cdxFilesDir = FileUtils.createUniqueTempDir(files.getTmpMetadataDir(), "cdx");
        CDXUtils.generateCDX(profile, archiveDir, cdxFilesDir);
        writer.insertFiles(cdxFilesDir, FileUtils.CDX_FILE_FILTER, Constants.CDX_MIME_TYPE, 
        		files.getHarvestID(), files.getJobId());
    }

    /**
     * Restore serialized MetadataEntry objects from the "metadata" subdirectory of the crawldir.
     *
     * @param crawlDir the given crawl directory
     * @return a set of deserialized MetadataEntry objects
     */
    private static List<MetadataEntry> getStoredMetadata(File crawlDir) {
        File metadataDir = new File(crawlDir, IngestableFiles.METADATA_SUB_DIR);
        if (!metadataDir.isDirectory()) {
            log.warn("Should have an metadata directory '{}' but there wasn't", metadataDir.getAbsolutePath());
            return new ArrayList<MetadataEntry>();
        } else {
            return MetadataEntry.getMetadataFromDisk(metadataDir);
        }
    }

    /**
     * Iterates over the (W)ARC files in the given dir and moves away files that do not belong to the given job into a
     * "lost-files" directory under oldjobs named with a timestamp.
     *
     * @param archiveProfile archive profile including filters, patterns, etc.
     * @param dir A directory containing one or more (W)ARC files.
     * @param files Information about the files produced by heritrix (jobId and harvestnamePrefix)
     */
    private static void moveAwayForeignFiles(ArchiveProfile archiveProfile, File dir, IngestableFiles files) {
        File[] archiveFiles = dir.listFiles(archiveProfile.filename_filter);
        File oldJobsDir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR));
        File lostfilesDir = new File(oldJobsDir, "lost-files-" + new Date().getTime());
        List<File> movedFiles = new ArrayList<File>();
        log.info("Looking for files not having harvestprefix '{}'", files.getHarvestnamePrefix());
        for (File archiveFile : archiveFiles) {
            if (!(archiveFile.getName().startsWith(files.getHarvestnamePrefix()))) {
                // move unidentified file to lostfiles directory
                log.info("removing unidentified file {}", archiveFile.getAbsolutePath());
                try {
                    if (!lostfilesDir.exists()) {
                        FileUtils.createDir(lostfilesDir);
                    }
                    File moveTo = new File(lostfilesDir, archiveFile.getName());
                    archiveFile.renameTo(moveTo);
                    movedFiles.add(moveTo);
                } catch (PermissionDenied e) {
                    log.warn("Not allowed to make oldjobs dir '{}'", lostfilesDir.getAbsolutePath(), e);
                }

            }
        }
        if (!movedFiles.isEmpty()) {
            log.warn("Found files not belonging to job {}, the following files have been stored for later: {}",
                    files.getJobId(), movedFiles);
        }
    }

    /**
     * Write harvestdetails to archive file(s). This includes the order.xml, seeds.txt, specific settings.xml for
     * certain domains, the harvestInfo.xml, All available reports (subset of HeritrixFiles.HERITRIX_REPORTS), All
     * available logs (subset of HeritrixFiles.HERITRIX_LOGS).
     *
     * @param jobID the given job Id
     * @param harvestID the id for the harvestdefinition, which created this job
     * @param crawlDir the directory where the crawljob took place
     * @param mdfw an MetadaFileWriter used to store the harvest configuration, and harvest logs and reports.
     * @param heritrixVersion the heritrix version used by the harvest.
     * @return a list of files that can now be deleted
     * @throws ArgumentNotValid If null arguments occur
     */
    private static List<File> writeHarvestDetails(long jobID, long harvestID, IngestableFiles ingestableFiles, MetadataFileWriter mdfw,
            String heritrixVersion) {
        List<File> filesAdded = new ArrayList<File>();
        
        // We will sort the files by URL
        TreeSet<MetadataFile> files = new TreeSet<MetadataFile>();

        // look for files in the crawldir and the ${heritrix3jobdir}, and ${heritrix3jobdir}/latest
        // - reports is relative to ${heritrix3jobdir}/latest/ 
        // - logs is relative to ${heritrix3jobdir}
        File crawlDir = ingestableFiles.getCrawlDir();
        File jobsDir = ingestableFiles.getHeritrix3JobDir();
        File reportsDir = ingestableFiles.getReportsDir();
        
        log.info("Looking for heritrix files in the following directories: {},{}, {}",
        		crawlDir.getAbsolutePath(), jobsDir.getAbsolutePath(), reportsDir.getAbsolutePath());
        
        // Find and add Heritrix files in the crawl directory
        File[] heritrixFiles = crawlDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.isFile() && f.getName().matches(MetadataFile.HERITRIX_FILE_PATTERN));
            }
        });
        for (File hf : heritrixFiles) {
            files.add(new MetadataFile(hf, harvestID, jobID, heritrixVersion));
        }

        // Find and add Heritrix files in the heritrixjobdir (if it exists)
        if (jobsDir.exists()) {
        	File[] heritrixFilesJobDir = jobsDir.listFiles(new FileFilter() {
        		@Override
        		public boolean accept(File f) {
        			return (f.isFile() && f.getName().matches(MetadataFile.HERITRIX_FILE_PATTERN));
        		}
        	});
        	for (File hf : heritrixFilesJobDir) {
        		files.add(new MetadataFile(hf, harvestID, jobID, heritrixVersion));
        	}
        } else {
        	log.warn("The directory {} does not exist", jobsDir.getAbsolutePath()); 
        }
        
        // Find and add Heritrix files in the heritrixReportsDir (if it exists)
        if (reportsDir.exists()) {
        	File[] heritrixFilesReports = reportsDir.listFiles(new FileFilter() {
        		@Override
        		public boolean accept(File f) {
        			return (f.isFile() && f.getName().matches(MetadataFile.HERITRIX_FILE_PATTERN));
        		}
        	});

        	for (File hf : heritrixFilesReports) {
        		files.add(new MetadataFile(hf, harvestID, jobID, heritrixVersion));
        	} 
        } else {
        	log.warn("The directory {} does not exist", reportsDir.getAbsolutePath());
        }
        
        // Generate an arcfiles-report.txt if configured to do so.
        // This is not possible to extract from the crawl.log, but we will make one from just listing the files harvested by Heritrix3
        
        boolean genArcFilesReport = Settings.getBoolean(Heritrix3Settings.METADATA_GENERATE_ARCHIVE_FILES_REPORT);
        if (genArcFilesReport) {
        	String reportName = ArchiveFilesReportGenerator.REPORT_FILE_NAME;
        	try {
        		log.debug("Creating an " + reportName + " file, if not already created");
        		files.add(new MetadataFile(new ArchiveFilesReportGenerator(ingestableFiles).generateReport(), harvestID, jobID,
                    heritrixVersion));
        		log.debug("The report '" + reportName + "' was created successfully or existed already.");
        	} catch (IOException e) {
        		log.warn("Skipping the addition of the " + reportName + ". It was not created successfully", e);
        	}
        } else {
            log.debug("Creation of the arcfiles-report.txt has been disabled by the setting '{}'!",
            		Heritrix3Settings.METADATA_GENERATE_ARCHIVE_FILES_REPORT);
        }

        // Add log files
        File logDir = new File(jobsDir, "logs");
        if (logDir.exists()) {
            File[] heritrixLogFiles = logDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return (f.isFile() && f.getName().matches(MetadataFile.LOG_FILE_PATTERN));
                }
            });
            for (File logFile : heritrixLogFiles) {
                files.add(new MetadataFile(logFile, harvestID, jobID, heritrixVersion));
                log.info("Found Heritrix log file {}", logFile.getName());
            }
        } else {
            log.debug("No logs dir found in crawldir: {}", crawlDir.getAbsolutePath());
        }
        
        // Write files in order to metadata archive file.
        for (MetadataFile mdf : files) {
            File heritrixFile = mdf.getHeritrixFile();
            String heritrixFileName = heritrixFile.getName();
            String mimeType = (heritrixFileName.endsWith(".xml") ? "text/xml" : "text/plain");
            if (mdfw.writeTo(heritrixFile, mdf.getUrl(), mimeType)) {
                filesAdded.add(heritrixFile);
            } else {
                log.warn("The Heritrix file '{}' was not included in the metadata archivefile due to some error.",
                        heritrixFile.getAbsolutePath());
            }
        }

        // All these files just added to the metadata archivefile can now be deleted
        // except for the files we need for later processing):
        // - crawl.log is needed to create domainharvestreport later
        // - harvestInfo.xml is needed to upload stored data after
        // crashes/stops on the harvesters
        // - progress-statistics.log is needed to find out if crawl ended due
        // to hitting a size limit, or due to other completion
        Iterator<File> iterator = filesAdded.iterator();
        
        Set<String> excludedFilenames = new HashSet<String>(); 
        excludedFilenames.add("crawl.log");
        excludedFilenames.add("harvestInfo.xml");
        excludedFilenames.add("progress-statistics.log");
        while (iterator.hasNext()) {
            File f = iterator.next();
            String name = f.getName();
            if (excludedFilenames.contains(name)) {
                iterator.remove();
            }
        }
        return filesAdded; // Files now added to the metadata file - and now deletable
    }
  
}
