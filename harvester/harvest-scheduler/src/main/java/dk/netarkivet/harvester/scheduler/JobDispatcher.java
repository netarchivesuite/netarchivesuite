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
package dk.netarkivet.harvester.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;

/**
 * This class handles dispatching of Harvest jobs to the Harvesters.
 */
public class JobDispatcher {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);

    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection;

    private final HarvestDefinitionDAO harvestDefinitionDAO;
    private final JobDAO jobDao;

    /**
     * @param jmsConnection The JMS connection to use.
     * @param hDao The HarvestDefinitionDAO to use.
     * @param jobDao The JobDAO to use.
     */
    public JobDispatcher(JMSConnection jmsConnection, HarvestDefinitionDAO hDao, JobDAO jobDao) {
        log.info("Creating JobDispatcher");
        ArgumentNotValid.checkNotNull(jmsConnection, "jmsConnection");
        ArgumentNotValid.checkNotNull(hDao, "hDao");
        ArgumentNotValid.checkNotNull(jobDao, "jobDao");
        this.jmsConnection = jmsConnection;
        this.harvestDefinitionDAO = hDao;
        this.jobDao = jobDao;
    }

    /**
     * Submit the next new job (the one with the lowest ID) with the given priority, and updates the internal counter as
     * needed. If no jobs are ready for the given priority, nothing is done
     *
     * @param channel the Channel to use for the job.
     */
    protected void submitNextNewJob(HarvestChannel channel) {
        Job jobToSubmit = prepareNextJobForSubmission(channel);
        if (jobToSubmit == null) {
            log.trace("No {} jobs to be run at this time", channel.getName());
        } else {
            log.debug("Submitting new {} job {}", channel.getName(), jobToSubmit.getJobID());
            try {
                List<MetadataEntry> metadata = createMetadata(jobToSubmit);

                // Extract documentary information about the harvest
                String hName = harvestDefinitionDAO.getHarvestName(jobToSubmit.getOrigHarvestDefinitionID());

                String schedule = "";
                String hdComments = "";
                String hdAudience = "";
                SparseFullHarvest fh = harvestDefinitionDAO.getSparseFullHarvest(hName);
                if (fh != null) {
                    hdComments = fh.getComments();
                } else {
                    SparsePartialHarvest ph = harvestDefinitionDAO.getSparsePartialHarvest(hName);

                    if (ph == null) {
                        throw new ArgumentNotValid("No harvest definition found for id '"
                                + jobToSubmit.getOrigHarvestDefinitionID() + "', named '" + hName + "'");
                    }

                    // The schedule name can only be documented for
                    // selective crawls.
                    schedule = ph.getScheduleName();

                    hdComments = ph.getComments();
                    hdAudience = ph.getAudience();
                }

                doOneCrawl(jobToSubmit, hName, hdComments, schedule, channel, hdAudience, metadata);

                log.info("Job #{} submitted", jobToSubmit.getJobID());
            } catch (Throwable t) {
                String message = "Error while dispatching job " + jobToSubmit.getJobID()
                        + ". Job status changed to FAILED";
                log.warn(message, t);
                if (jobToSubmit != null) {
                    jobToSubmit.setStatus(JobStatus.FAILED);
                    jobToSubmit.appendHarvestErrors(message);
                    jobToSubmit.appendHarvestErrorDetails(ExceptionUtils.getStackTrace(t));
                    jobDao.update(jobToSubmit);
                }
            }
        }
    }

    /**
     * Will read the next job ready to run from the database and set the job to submitted. If no jobs are ready, null will be
     * returned.
     * <p>
     * Note the operation is synchronized, so only one thread may start the submission of a job.
     *
     * @param channel the job channel.
     * @return A job ready to be submitted.
     */
    private synchronized Job prepareNextJobForSubmission(HarvestChannel channel) {
        Iterator<Long> jobsToSubmit = jobDao.getAllJobIds(JobStatus.NEW, channel);
        if (!jobsToSubmit.hasNext()) {
            return null;
        } else {
            final long jobID = jobsToSubmit.next();
            Job jobToSubmit = jobDao.read(jobID);
            jobToSubmit.setStatus(JobStatus.SUBMITTED);
            jobToSubmit.setSubmittedDate(new Date());
            jobDao.update(jobToSubmit);
            return jobToSubmit;
        }
    }

    /**
     * Creates the metadata for the indicated job. This includes:
     * <ol>
     * <li>Alias metadata.
     * <li>DuplicationReduction MetadataEntry, if Deduplication //is enabled.
     *
     * @param job The job to create meta data for.
     */
    private List<MetadataEntry> createMetadata(Job job) {
        List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
        List<AliasInfo> aliasInfos = jobDao.getJobAliasInfo(job);
        MetadataEntry aliasMetadataEntry = MetadataEntry.makeAliasMetadataEntry(aliasInfos,
                job.getOrigHarvestDefinitionID(), job.getHarvestNum(), job.getJobID());
        if (aliasMetadataEntry != null) {
            // Add an entry documenting that this job
            // contains domains that has aliases
            metadata.add(aliasMetadataEntry);
            log.info("Added alias metadataEntry for job {} ", job.getJobID());
        } 

        if (job.getOrderXMLdoc().IsDeduplicationEnabled()) {
            MetadataEntry duplicateReductionMetadataEntry = MetadataEntry.makeDuplicateReductionMetadataEntry(
                    jobDao.getJobIDsForDuplicateReduction(job.getJobID()), job.getOrigHarvestDefinitionID(),
                    job.getHarvestNum(), job.getJobID());
            // Always add a duplicationReductionMetadataEntry when deduplication is enabled
            // even if the list of JobIDs for deduplication is empty!
            metadata.add(duplicateReductionMetadataEntry);
            log.info("Added duplicateReductionMetadataEntry metadataEntry for job {} ", job.getJobID());
        }
        return metadata;
    }

    /**
     * Submit an doOneCrawl request to a HarvestControllerServer.
     *
     * @param job the specific job to send
     * @param origHarvestName the harvest definition's name
     * @param origHarvestDesc the harvest definition's description
     * @param origHarvestSchedule the harvest definition schedule name
     * @param channel the channel to which the job should be sent
     * @param metadata pre-harvest metadata to store in (w)arcfile.
     * @param origHarvestAudience the audience for the data generated by harvest definitions.
     * @throws ArgumentNotValid one of the parameters are null
     * @throws IOFailure if unable to send the doOneCrawl request to a harvestControllerServer
     */
    public void doOneCrawl(Job job, String origHarvestName, String origHarvestDesc, String origHarvestSchedule,
            HarvestChannel channel, String origHarvestAudience, List<MetadataEntry> metadata) throws ArgumentNotValid,
            IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");

        if (origHarvestAudience != null && !origHarvestAudience.isEmpty()) {
            job.setHarvestAudience(origHarvestAudience);
        }
        if (usingWarcAsArchiveFormat()) {
        	log.info("As we're using WARC as archiveFormat WarcInfoMetadata is now added to the template");
        	HeritrixTemplate ht = job.getOrderXMLdoc();
                ht.insertWarcInfoMetadata(job, origHarvestName, origHarvestDesc, origHarvestSchedule,
                        Settings.get(HarvesterSettings.PERFORMER));
            job.setOrderXMLDoc(ht);
        } else {
        	log.info("As we're using ARC as archiveFormat no WarcInfoMetadata was added to the template");
        }
        
        DoOneCrawlMessage nMsg = new DoOneCrawlMessage(job, HarvesterChannels.getHarvestJobChannelId(channel),
                new HarvestDefinitionInfo(origHarvestName, origHarvestDesc, origHarvestSchedule), metadata);
        log.debug("Send crawl request: {}", nMsg);
        jmsConnection.send(nMsg);
    }
    
	private boolean usingWarcAsArchiveFormat() {
		return Settings.get(HarvesterSettings.HERITRIX_ARCHIVE_FORMAT).equalsIgnoreCase("warc");
	}

}
