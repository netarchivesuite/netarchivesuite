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
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobChannelUtil;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.harvester.harvesting.distribute.PersistentJobData.HarvestDefinitionInfo;

/**
 * This class handles dispatching of Harvest jobs to the Harvesters.
 */
public class JobDispatcher {
    /** The logger to use.    */
    private final Log log = LogFactory.getLog(getClass());
    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection;
    
    /**
     * @param jmsConnection The JMS connection to use.
     */
    public JobDispatcher(JMSConnection jmsConnection) {
        log.info("Creating JobDispatcher");
        ArgumentNotValid.checkNotNull(jmsConnection, "jmsConnection");
        this.jmsConnection = jmsConnection;
    }

    /**
     * Submit the next new job (the one with the lowest ID) with the given
     * priority, and updates the internal counter as needed. If no jobs are 
     * ready for the given priority, nothing is done
     * @param priority the job priority
     */
    protected void submitNextNewJob(JobPriority priority) {
        Job jobToSubmit = prepareNextJobForSubmission(priority);
        if (jobToSubmit == null) {
            if (log.isTraceEnabled()) {
                log.trace("No " + priority + " jobs to be run at this time");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Submitting new " + priority + " job");
            }
            try {
                List<MetadataEntry> metadata = createMetadata(jobToSubmit);
                
                // Extract documentary information about the harvest
                HarvestDefinitionDAO hDao = HarvestDefinitionDAO.getInstance();
                String hName = hDao.getHarvestName(
                        jobToSubmit.getOrigHarvestDefinitionID());

                String schedule = "";
                String hdComments = "";
                SparseFullHarvest fh = hDao.getSparseFullHarvest(hName);
                if (fh != null) {
                    hdComments = fh.getComments();
                } else {
                    SparsePartialHarvest ph =
                            hDao.getSparsePartialHarvest(hName);

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
                }

                doOneCrawl(jobToSubmit, hName, hdComments, schedule, metadata);

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
                    JobDAO.getInstance().update(jobToSubmit);
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
     * @param priority the job priority.
     * @return A job ready to be submitted.
     */
    private synchronized Job prepareNextJobForSubmission(JobPriority priority) {
        JobDAO jobDao = JobDAO.getInstance();
        Iterator<Long> jobsToSubmit = 
                jobDao.getAllJobIds(JobStatus.NEW, priority);
        if (!jobsToSubmit.hasNext()) {
            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Submitting new " + priority + " job");
            }
            final long jobID = jobsToSubmit.next();
            Job jobToSubmit = null;
            jobToSubmit = jobDao.read(jobID);

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
        final JobDAO dao = JobDAO.getInstance();
        List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
        MetadataEntry aliasMetadataEntry =
                MetadataEntry.makeAliasMetadataEntry(
                        job.getJobAliasInfo(),
                        job.getOrigHarvestDefinitionID(),
                        job.getHarvestNum(),
                        job.getJobID());
        if (aliasMetadataEntry != null) {
            metadata.add(aliasMetadataEntry);
        }

        if (HeritrixLauncher.isDeduplicationEnabledInTemplate(
                job.getOrderXMLdoc())) {
            MetadataEntry duplicateReductionMetadataEntry
            = MetadataEntry.makeDuplicateReductionMetadataEntry(
                    dao.getJobIDsForDuplicateReduction(job.getJobID()),
                    job.getOrigHarvestDefinitionID(),
                    job.getHarvestNum(),
                    job.getJobID()
                    );

            if (duplicateReductionMetadataEntry != null) {
                metadata.add(duplicateReductionMetadataEntry);
            }
        }
        return metadata;
    }

    /**
     * Submit an doOneCrawl request to a HarvestControllerServer.
     * @param job the specific job to send
     * @param origHarvestName the harvest definition's name
     * @param origHarvestDesc the harvest definition's description
     * @param origHarvestSchedule the harvest definition schedule name
     * @param metadata pre-harvest metadata to store in arcfile.
     * @throws ArgumentNotValid one of the parameters are null
     * @throws IOFailure if unable to send the doOneCrawl request to a
     * harvestControllerServer
     */
    public void doOneCrawl(
            Job job,
            String origHarvestName,
            String origHarvestDesc,
            String origHarvestSchedule,
            List<MetadataEntry> metadata)
                    throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");

        DoOneCrawlMessage nMsg = new DoOneCrawlMessage(
                job,
                JobChannelUtil.getChannel(job.getPriority()),
                new HarvestDefinitionInfo(
                        origHarvestName, origHarvestDesc, origHarvestSchedule),
                        metadata);
        if (log.isDebugEnabled()) {
            log.debug("Send crawl request: " + nMsg);
        }
        jmsConnection.send(nMsg);
    }
}