/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and 
 * University Library, the National Library of France and the Austrian 
 * National Library.
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
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
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData.HarvestDefinitionInfo;

/**
 * This class handles dispatching of Harvest jobs to the Harvesters.
 */
public class JobDispatcher {
    /** The logger to use.    */
    private final Log log = LogFactory.getLog(getClass());
    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection;
    private final HarvestDefinitionDAO harvestDefinitionDAO;
    private final JobDAO jobDao;
    
    /**
     * @param jmsConnection The JMS connection to use.
     * @param hDao The HarvestDefinitionDAO to use.
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
     * Submit the next new job (the one with the lowest ID) with the given
     * priority, and updates the internal counter as needed. If no jobs are 
     * ready for the given priority, nothing is done
     * @param channel the Channel to use for the job.
     */
    protected void submitNextNewJob(HarvestChannel channel) {
        Job jobToSubmit = prepareNextJobForSubmission(channel);
        if (jobToSubmit == null) {
            if (log.isTraceEnabled()) {
                log.trace("No " + channel.getName() + " jobs to be run at this time");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Submitting new " + channel.getName() + " job"
                        + jobToSubmit.getJobID());
            }
            try {
                List<MetadataEntry> metadata = createMetadata(jobToSubmit);
                
                // Extract documentary information about the harvest
                String hName = harvestDefinitionDAO.getHarvestName(
                        jobToSubmit.getOrigHarvestDefinitionID());

                String schedule = "";
                String hdComments = "";
                String hdAudience = "";
                SparseFullHarvest fh = harvestDefinitionDAO.getSparseFullHarvest(hName);
                if (fh != null) {
                    hdComments = fh.getComments();
                } else {
                    SparsePartialHarvest ph =
                            harvestDefinitionDAO.getSparsePartialHarvest(hName);

                    if (ph == null) {
                        throw new ArgumentNotValid("No harvest definition "
                                + "found for id '"
                                + jobToSubmit.getOrigHarvestDefinitionID()
                                + "', named '" + hName + "'");
                    }

                    // The schedule name can only be documented for
                    // selective crawls.
                    schedule = ph.getScheduleName();

                    hdComments = ph.getComments();
                    hdAudience = ph.getAudience();
                }

                doOneCrawl(jobToSubmit, hName, hdComments, schedule, channel, hdAudience, metadata);

                log.info("Job #" + jobToSubmit.getJobID() + " submitted");

            } catch (Throwable e) {
                String message = "Error while dispatching job " + 
                        jobToSubmit.getJobID()
                        + ". Job status changed to FAILED";
                log.warn(message, e);
                if (jobToSubmit != null) {
                    jobToSubmit.setStatus(JobStatus.FAILED);
                    jobToSubmit.appendHarvestErrors(message);
                    jobToSubmit.appendHarvestErrorDetails(
                            ExceptionUtils.getStackTrace(e));
                    jobDao.update(jobToSubmit);
                }
            }
        }
    }
    
    /**
     * Will read the next job ready to run from the db and set the job to 
     * submitted. If no jobs are ready, null will be returned.
     * 
     * Note the operation is synchronized, so only one thread may start the 
     * submission of a job.
     * @param channel the job channel.
     * @return A job ready to be submitted.
     */
    private synchronized Job prepareNextJobForSubmission(HarvestChannel channel) {
        Iterator<Long> jobsToSubmit = 
                jobDao.getAllJobIds(JobStatus.NEW, channel);
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
     * Creates the metadata for the indicated job. This includes: <ol>
     * <li> Alias metadata.
     * <li>DuplicationReduction MetadataEntry, if Deduplication
     *   //is enabled.
     * @param job The job to create meta data for.
     */
    private List<MetadataEntry> createMetadata(Job job) {
        List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
        MetadataEntry aliasMetadataEntry =
                MetadataEntry.makeAliasMetadataEntry(
                        job.getJobAliasInfo(),
                        job.getOrigHarvestDefinitionID(),
                        job.getHarvestNum(),
                        job.getJobID());
        if (aliasMetadataEntry != null) {
            // Add an entry documenting that this job 
            // contains domains that has aliases
            metadata.add(aliasMetadataEntry);
        }

        if (HeritrixTemplate.isDeduplicationEnabledInTemplate(
                job.getOrderXMLdoc())) {
            MetadataEntry duplicateReductionMetadataEntry
            = MetadataEntry.makeDuplicateReductionMetadataEntry(
                    jobDao.getJobIDsForDuplicateReduction(job.getJobID()),
                    job.getOrigHarvestDefinitionID(),
                    job.getHarvestNum(),
                    job.getJobID()
                    );
            // Always add a duplicationReductionMetadataEntry when deduplication is enabled
            // even if the list of JobIDs for deduplication is empty!
            metadata.add(duplicateReductionMetadataEntry);
        }
        return metadata;
    }

    /**
     * Submit an doOneCrawl request to a HarvestControllerServer.
     * @param job the specific job to send
     * @param origHarvestName the harvest definition's name
     * @param origHarvestDesc the harvest definition's description
     * @param origHarvestSchedule the harvest definition schedule name
     * @param channel the channel to which the job should be sent
     * @param metadata pre-harvest metadata to store in arcfile.
     * @param origHarvestAudience
     * @throws ArgumentNotValid one of the parameters are null
     * @throws IOFailure if unable to send the doOneCrawl request to a
     * harvestControllerServer
     */
    public void doOneCrawl(
            Job job,
            String origHarvestName,
            String origHarvestDesc,
            String origHarvestSchedule,
            HarvestChannel channel,
            String origHarvestAudience,
            List<MetadataEntry> metadata)
                    throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");
        
        if (origHarvestAudience != null && !origHarvestAudience.isEmpty()) {
            job.setHarvestAudience(origHarvestAudience);
        }
        DoOneCrawlMessage nMsg = new DoOneCrawlMessage(
                job,
                HarvesterChannels.getHarvestJobChannelId(channel),
                new HarvestDefinitionInfo(
                        origHarvestName, origHarvestDesc, origHarvestSchedule),
                        metadata);
        if (log.isDebugEnabled()) {
            log.debug("Send crawl request: " + nMsg);
        }
        jmsConnection.send(nMsg);
    }
    
}