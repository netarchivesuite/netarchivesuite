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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.PersistentJobData;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

public class HarvestJob {

    /** The instance logger. */
    private static final Logger log = LoggerFactory.getLogger(HarvestJob.class);

    private HarvestControllerServer hcs;

    /** The harvester Job in this thread. */
    private Job job;

    /**
     * Constructor.
     * @param hcs a HarvestControllerServer instance
     */
	public HarvestJob(HarvestControllerServer hcs) {
		this.hcs = hcs;
	}

    private File crawlDir;

    private Heritrix3Files files;

    private String jobName;
   
    /**
     * Initialization of the harvestJob.
     * @param job A job from the jobs table in the harvestdatabase
     * @param origHarvestInfo metadata about the harvest
     * @param metadataEntries entries for the metadata file for the harvest
     */
    public void init(Job job, HarvestDefinitionInfo origHarvestInfo, List<MetadataEntry> metadataEntries) {
        this.job = job;
        jobName = job.getJobID() + "_" + System.currentTimeMillis();
        crawlDir = createCrawlDir();
        files = writeHarvestFiles(crawlDir, job, origHarvestInfo, metadataEntries);
	}
    /**
     * @return the Heritrix3Files object initialized with the init() method.
     */
    public Heritrix3Files getHeritrix3Files() {
    	return files;
    }

    /**
     * Creates the actual HeritrixLauncher instance and runs it, after the various setup files have been written.
     *
     * @throws ArgumentNotValid if an argument isn't valid.
     */
    public void runHarvest() throws ArgumentNotValid {
        log.info("Starting crawl of job : {}", job.getJobID());
        HeritrixLauncherAbstract hl = HeritrixLauncherFactory.getInstance(files, jobName);
        hl.doCrawl();
    }

    /**
     * Create the crawl dir, but make sure a message is sent if there is a problem.
     *
     * @return The directory that the crawl will take place in.
     * @throws PermissionDenied if the directory cannot be created.
     */
    public File createCrawlDir() {
        // The directory where arcfiles are stored (crawldir in the above
        // description)
        File crawlDir = null;
        // Create the crawldir. This is done here in order to be able
        // to send a proper message if something goes wrong.
        try {
            File baseCrawlDir = new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR));
            crawlDir = new File(baseCrawlDir, jobName);
            FileUtils.createDir(crawlDir);
            log.info("Created crawl directory: '{}'", crawlDir);
            return crawlDir;
        } catch (PermissionDenied e) {
            String message = "Couldn't create the directory for job " + job.getJobID();
            log.warn(message, e);
            hcs.sendErrorMessage(job.getJobID(), message, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * Writes the files needed to start a harvest.. 
     * 
     * @param crawldir The directory that the crawl should take place in.
     * @param job The Job object containing various harvest setup data.
     * @param hdi The object encapsulating documentary information about the harvest.
     * @param metadataEntries Any metadata entries sent along with the job that should be stored for later use.
     * @return An object encapsulating where these files have been written.
     */
    public Heritrix3Files writeHarvestFiles(File crawldir, Job job, HarvestDefinitionInfo hdi,
            List<MetadataEntry> metadataEntries) {
    	
        final Heritrix3Files files = Heritrix3Files.getH3HeritrixFiles(crawldir, job);

        // If this job is a job that tries to continue a previous job
        // using the Heritrix recover.gz log, and this feature is enabled,
        // then try to fetch the recover.log from the metadata-arc-file.
        if (job.getContinuationOf() != null && Settings.getBoolean(HarvesterSettings.RECOVERlOG_CONTINUATION_ENABLED)) {
        	log.warn("Continuation of crawl from a RecoverLog is not implemented for Heritrix3!");
        }
        
        // Create harvestInfo file in crawldir
        // & create preharvest-metadata-1.arc
        log.debug("Writing persistent job data for job {} to crawldir '{}'", job.getJobID(), crawldir);
        if (!PersistentJobData.existsIn(crawldir)) {
            // Write job data to persistent storage (harvestinfo file)
            new PersistentJobData(crawldir).write(job, hdi);
        } else {
            throw new IllegalState("We already found a harvestInfo.xml for the crawldir " + crawldir.getAbsolutePath());
        }
        
        // Create jobId-preharvest-metadata-1.arc for this job
        writePreharvestMetadata(job, metadataEntries, crawldir);

        files.writeSeedsTxt(job.getSeedListAsString());

        files.writeOrderXml(job.getOrderXMLdoc());
        // Only retrieve index if deduplication is not disabled in the template.
        if (job.getOrderXMLdoc().IsDeduplicationEnabled()) {
            log.debug("Deduplication enabled. Fetching deduplication index..");
            files.setIndexDir(fetchDeduplicateIndex(metadataEntries));
        } else {
            log.debug("Deduplication disabled.");
        }

        return files;
    }

    /**
     * Writes pre-harvest metadata to the "metadata" directory.
     *
     * @param harvestJob a given Job.
     * @param metadata the list of metadata entries to write to metadata file.
     * @param crawlDir the directory, where the metadata will be written.
     * @throws IOFailure If there are errors in writing the metadata.
     */
    private void writePreharvestMetadata(Job harvestJob, List<MetadataEntry> metadata, File crawlDir) throws IOFailure {
        if (metadata.size() == 0) {
            // Do not generate preharvest metadata file for empty list
            return;
        }

        // make sure that metadata directory exists
        File metadataDir = new File(crawlDir, IngestableFiles.METADATA_SUB_DIR);
        metadataDir.mkdir();
        if (!(metadataDir.exists() && metadataDir.isDirectory())) {
            throw new IOFailure("Unable to write preharvest metadata for job '" + harvestJob.getJobID()
                    + "' to directory '" + metadataDir.getAbsolutePath() + "', as directory does not exist.");
        }

        // Serializing the MetadataEntry objects to the metadataDir
        MetadataEntry.storeMetadataToDisk(metadata, metadataDir);
    }

    /**
     * Get an index for deduplication. This will make a call to the index server, requesting an index for the given IDs.
     * The files will then be cached locally.
     * <p>
     * If we request index for IDs that don't exist/have problems, we get a smaller set of IDs in our cache files, and
     * next time we ask for the same index, we will call the index server again. This will be handled well, though,
     * because if the ids are still missing, we will get a reply telling us to use the cached smaller index anyway.
     *
     * @param metadataEntries list of metadataEntries top get jobIDs from.
     * @return a directory containing the index itself.
     * @throws IOFailure on errors retrieving the index from the client. 
     * FIXME Better forgiving handling of no index available. Add setting for disable deduplication if no index available
     */
    private File fetchDeduplicateIndex(List<MetadataEntry> metadataEntries) {
        // Get list of jobs, which should be used for duplicate reduction
        // and retrieve a luceneIndex from the IndexServer
        // based on the crawl.logs from these jobs and their CDX'es.
        Set<Long> jobIDsForDuplicateReduction = new HashSet<Long>(parseJobIDsForDuplicateReduction(metadataEntries));

        // The client for requesting job index.
        JobIndexCache jobIndexCache = IndexClientFactory.getDedupCrawllogInstance();

        // Request the index and return the index file.
        Index<Set<Long>> jobIndex = jobIndexCache.getIndex(jobIDsForDuplicateReduction);
        // Check which jobs didn't become part of the index.
        Set<Long> diffSet = new HashSet<Long>(jobIDsForDuplicateReduction);
        diffSet.removeAll(jobIndex.getIndexSet());
        if (log.isDebugEnabled()) {
            log.debug("Received deduplication index containing {} jobs. {}", jobIndex.getIndexSet().size(),
                    ((diffSet.size() > 0) ? "Missing jobs: " + StringUtils.conjoin(",", diffSet) : ""));
        }

        return jobIndex.getIndexFile();
    }

    /**
     * Retrieve the list of jobs for deduplicate reduction.
     * <p>
     * Runs through all metadata entries, finding duplicate reduction entries, and parsing all jobIDs in them, warning
     * only on errors.
     *
     * @param metadataEntries list of metadataEntries.
     * @return the list of jobs for deduplicate reduction.
     */
    private List<Long> parseJobIDsForDuplicateReduction(List<MetadataEntry> metadataEntries) {
        // find metadataEntry for duplicatereduction if any.
        List<Long> result = new ArrayList<Long>();
        for (MetadataEntry me : metadataEntries) {
            if (me.isDuplicateReductionMetadataEntry()) {
                String s = new String(me.getData());
                if (s.isEmpty()) { // An empty string is now possible
                    continue;
                }
                String[] longs = s.split(",");
                for (String stringLong : longs) {
                    try {
                        result.add(Long.parseLong(stringLong));
                    } catch (NumberFormatException e) {
                        log.warn("Unable to convert String '{}' in duplicate reduction jobid list metadataEntry '{}'"
                                + " to a jobID. Ignoring.", stringLong, s, e);
                    }
                }
            }
        }
        return result;
    }

}
