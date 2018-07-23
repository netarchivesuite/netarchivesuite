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

import java.util.Date;

import javax.inject.Provider;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.Notifications;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;

/**
 * Submitted harvesting jobs are registered by listening for CrawlStatusMessages on the
 * THE_SCHED queue and processes completed harvests.
 */
public class HarvestSchedulerMonitorServer
        extends HarvesterMessageHandler
        implements MessageListener, ComponentLifeCycle {
    private static final Logger log = LoggerFactory.getLogger(HarvestSchedulerMonitorServer.class);

    private final Provider<JMSConnection> jmsConnectionProvider;
    private final Provider<JobDAO> jobDAOProvider;
    private final Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider;
    private final Provider<Notifications> notificationsProvider;

    public HarvestSchedulerMonitorServer(
            Provider<JMSConnection> jmsConnectionProvider,
            Provider<JobDAO> jobDAOProvider,
            Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider,
            Provider<Notifications> notificationsProvider) {
        this.jmsConnectionProvider = jmsConnectionProvider;
        this.jobDAOProvider = jobDAOProvider;
        this.harvestDefinitionDAOProvider = harvestDefinitionDAOProvider;
        this.notificationsProvider = notificationsProvider;
    }

    @Override
    public void start() {
        jmsConnectionProvider.get().setListener(HarvesterChannels.getTheSched(), this);
    }

    /**
     * Updates the job status with the information in the message and notifies the HarvestMonitor.
     * If a DomainHarvestReport is included in either a DOne or FAILED message, the DomainHarvestReport is processed.
     */
    private void processCrawlStatusMessage(CrawlStatusMessage cmsg) {
        long jobID = cmsg.getJobID();
        JobStatus newStatus = cmsg.getStatusCode();
        Job job = jobDAOProvider.get().read(Long.valueOf(jobID));
        JobStatus oldStatus = job.getStatus();
        boolean ignoreDomainHarvestReport = false; // Don't ignore DomainHarvestReport unless job already in DONE or FAILED state  
        switch (newStatus) {
        case STARTED:
            if (oldStatus == JobStatus.NEW) {
                log.warn("Received new status 'Started' for unsubmitted job {}", jobID);
            }
            if (oldStatus == JobStatus.SUBMITTED || oldStatus == JobStatus.NEW) {
                log.info("Job #{} has been started by the harvester.", job.getJobID());

                job.setStatus(newStatus);
                jobDAOProvider.get().update(job);

                notifyRunningJobMonitor(new CrawlProgressMessage(job.getOrigHarvestDefinitionID(), jobID));
            } else {
                log.warn("Received new status 'Started' for job {} with current status {}, ignoring.",
                        job.getJobID(), oldStatus);
            }
            break;
        case DONE:
        case FAILED:
            if (oldStatus == JobStatus.STARTED ||
                    oldStatus == JobStatus.SUBMITTED ||
                    oldStatus == JobStatus.RESUBMITTED ||
                    oldStatus == JobStatus.NEW) {
                if (oldStatus != JobStatus.STARTED) {
                    log.warn("Received unexpected CrawlStatusMessage for job {} with new status {}, current state is {}",
                            jobID, newStatus ,oldStatus);
                }
                if (newStatus == JobStatus.FAILED) {
                    log.warn("Job {} failed: HarvestErrors = {}\n" + "HarvestErrorDetails = {}\n"
                                    + "UploadErrors = {}\n" + "UploadErrorDetails = {}", jobID, cmsg.getHarvestErrors(),
                            cmsg.getHarvestErrorDetails(), cmsg.getUploadErrors(), cmsg.getUploadErrorDetails());
                } else {
                    log.info("Job #{} succesfully completed", jobID);
                }
                job.setStatus(newStatus);
                job.appendHarvestErrors(cmsg.getHarvestErrors());
                job.appendHarvestErrorDetails(cmsg.getHarvestErrorDetails());
                job.appendUploadErrors(cmsg.getUploadErrors());
                job.appendUploadErrorDetails(cmsg.getUploadErrorDetails());
            } else {
                // Received done or failed on already dead job. Bad!
            	// Marking as FAILED, unless oldStatus is DONE (issue NAS-2612)
            	JobStatus newStatus1 = JobStatus.FAILED;
            	// Ignore domainharvestreport if oldstatus either done or failed
            	if (oldStatus.equals(JobStatus.DONE)) {
            		newStatus1 = JobStatus.DONE;
            		ignoreDomainHarvestReport = true;
            	} else if (oldStatus.equals(JobStatus.FAILED)) {
                    ignoreDomainHarvestReport = true;
                }
            	
            	String message = "Received unexpected CrawlStatusMessage for job " + jobID + " with new status " + newStatus +
                        ", current state is " + oldStatus+ ". Marking job as " + newStatus1.name() + ". Reported harvestErrors on job: " +  cmsg.getHarvestErrors();
                job.setStatus(newStatus1);                
                job.appendHarvestErrors(cmsg.getHarvestErrors());
                job.appendHarvestErrors(message);
                job.appendHarvestErrorDetails(cmsg.getHarvestErrors());
                job.appendHarvestErrorDetails(message);
                log.warn(message);
            }

            jobDAOProvider.get().update(job);

            if (!ignoreDomainHarvestReport && cmsg.getDomainHarvestReport() != null) { 
                cmsg.getDomainHarvestReport().postProcess(job);
            }

            notifyRunningJobMonitor(new JobEndedMessage(job.getJobID(), newStatus));
            break;
        default:
            log.warn("CrawlStatusMessage tried to update job status to unsupported status {} for job {}",
                    newStatus, jobID);
            break;
        }
    }

    private void notifyRunningJobMonitor(NetarkivetMessage message) {
        jmsConnectionProvider.get().send(message);
    }

    /**
     * @param msg a given CrawlStatusMessage
     * @see dk.netarkivet.harvester.distribute.HarvesterMessageHandler#visit(dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage)
     */
    public void visit(CrawlStatusMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        processCrawlStatusMessage(msg);
    }

    /**
     * Removes the HarvestSchedulerMonitorServer as listener to the JMS scheduler Channel.
     */
    @Override
    public void shutdown() {
        // FIXME See NAS-1976
        jmsConnectionProvider.get().removeListener(HarvesterChannels.getTheSched(), this);
    }

    @Override
    public void visit(IndexReadyMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        processIndexReadyMessage(msg);
    }

    private void processIndexReadyMessage(IndexReadyMessage msg) {
        // Set isindexready to true if Indexisready is true
        Long harvestId = msg.getHarvestId();
        boolean indexisready = msg.getIndexOK();
        if (harvestDefinitionDAOProvider.get().isSnapshot(harvestId)) {
            harvestDefinitionDAOProvider.get().setIndexIsReady(harvestId, indexisready);
            if (indexisready) {
                log.info("Got message from the IndexServer, that the index is ready for harvest #{}", harvestId);
            } else {
                String errMsg = "Got message from IndexServer, that it failed to generate index for" + " harvest # "
                        + harvestId + ". Deactivating harvest";
                log.warn(errMsg);
                HarvestDefinition hd = harvestDefinitionDAOProvider.get().read(harvestId);
                hd.setActive(false);
                StringBuilder commentsBuf = new StringBuilder(hd.getComments());
                commentsBuf.append("\n" + (new Date())
                        + ": Deactivated by the system because indexserver failed to generate index");
                hd.setComments(commentsBuf.toString());
                harvestDefinitionDAOProvider.get().update(hd);
                notificationsProvider.get().notify(errMsg, NotificationType.ERROR);
            }
        } else {
            log.debug("Ignoring IndexreadyMesssage sent on behalf on selective harvest w/id {}", harvestId);
        }
    }
}
