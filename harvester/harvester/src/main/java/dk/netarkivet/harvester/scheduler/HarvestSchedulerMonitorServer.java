/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.MessageListener;

/**
 * Submitted harvesting jobs are registered with this singleton. The class
 * listens for CrawlStatusMessages on the THE_SCHED queue and processes
 * completed harvests.
 *
 */
public class HarvestSchedulerMonitorServer extends HarvesterMessageHandler
        implements MessageListener, ComponentLifeCycle {
    /**
     * The JobDAO.
     */
    private final JobDAO jobDAO = JobDAO.getInstance();

    /** The private logger for this class. */
    private final Log log = LogFactory.getLog(getClass().getName());

    @Override
    public void start() {
        JMSConnectionFactory.getInstance().setListener(
                Channels.getTheSched(), this);
    }

    /**
     * Updates the job status from the current status to that specified in
     * message if it legal to do so. Logs a warning if messages arrive out of
     * order.
     *
     * @param cmsg The CrawlStatusMessage received
     * @throws ArgumentNotValid if the current job status is Job.STATUS_NEW
     */
    private void processCrawlStatusMessage(CrawlStatusMessage cmsg)
            throws ArgumentNotValid {
        long jobID = cmsg.getJobID();
        JobStatus newStatus = cmsg.getStatusCode();
        Job job = jobDAO.read(Long.valueOf(jobID));
        JobStatus oldStatus = job.getStatus();
        // Update the job status

        // a NEW job should never get a message
        if (oldStatus == JobStatus.NEW) {
            String msg = "CrawlStatusMessage received on new job: " + job;
            log.warn(msg);
        }

        switch (newStatus) {
            case RESUBMITTED:
            case SUBMITTED:
            case NEW:
                // crawl status should never update to
                // new/submitted/resubmitted, because these statuses should not
                // be used by the harvesters!
                String msg = "CrawlStatusMessage tried to update job "
                             + job + " to status " + newStatus;
                log.warn(msg);
                throw new ArgumentNotValid(msg);
            case STARTED:
                if (oldStatus == JobStatus.SUBMITTED
                    || oldStatus == JobStatus.NEW) {
                    if (oldStatus == JobStatus.NEW) {
                        log.warn("CrawlStatusMessage updated job in unexpected "
                                 + "state " + oldStatus + " to "
                                 + newStatus + "\n"
                                 + job.toString());
                    }
                    // The usual case submitted -> started
                    job.setStatus(newStatus);

                    // Send the initial progress message
                    JMSConnectionFactory.getInstance().send(
                            new CrawlProgressMessage(
                                    job.getOrigHarvestDefinitionID(),
                                    job.getJobID()));

                    log.debug(job + " has started crawling.");
                    jobDAO.update(job);
                } else {
                    // Must not change status back to STARTED
                    log.warn("CrawlStatusMessage tried to update job status"
                             + " for job " + job.getJobID()
                             + " from " + oldStatus + " to " + newStatus
                             + ". Ignoring.");
                }
                break;
            case DONE:
            case FAILED:
                if (oldStatus == JobStatus.STARTED
                    || oldStatus == JobStatus.SUBMITTED
                    || oldStatus == JobStatus.RESUBMITTED
                    || oldStatus == JobStatus.NEW) {
                    // Received done or failed on non-ended job - okay
                    if (oldStatus != JobStatus.STARTED) {
                        //we expect "started" first, but it's not serious. Just
                        //log.
                        log.warn("CrawlStatusMessage updated job in unexpected "
                                 + "state " + oldStatus + " to "
                                 + newStatus + "\n"
                                 + job.toString());
                    }
                    if (newStatus == JobStatus.FAILED) {
                        String errors = "HarvestErrors = "
                            + cmsg.getHarvestErrors()
                            + "\nHarvestErrorDetails = "
                            + cmsg.getHarvestErrorDetails() 
                            + "\nUploadErrors = "
                            + cmsg.getUploadErrors()
                            + "\nUploadErrorDetails = "
                            + cmsg.getUploadErrorDetails();
                        
                        log.warn("Job " + jobID + " failed: " + errors);
                    } else {
                        log.info("Job " + jobID + " succesfully completed");
                    }
                    job.setStatus(newStatus);
                    job.appendHarvestErrors(cmsg.getHarvestErrors());
                    job.appendHarvestErrorDetails(
                            cmsg.getHarvestErrorDetails());
                    job.appendUploadErrors(cmsg.getUploadErrors());
                    job.appendUploadErrorDetails(cmsg.getUploadErrorDetails());
                    jobDAO.update(job);
                } else {
                    // Received done or failed on already dead job. Bad!
                    String message = "CrawlStatusMessage tried to update "
                                     + "job status "
                                     + " from " + oldStatus + " to "
                                     + newStatus
                                     + ". Marking job FAILED";
                    log.warn(message);
                    job.setStatus(JobStatus.FAILED);
                    job.appendHarvestErrors(cmsg.getHarvestErrors());
                    job.appendHarvestErrors(message);
                    job.appendHarvestErrorDetails(cmsg.getHarvestErrors());
                    job.appendHarvestErrorDetails(message);
                    log.warn("Job " + jobID + " failed: "
                             + job.getHarvestErrorDetails());
                    jobDAO.update(job);
                }
                //Always process the data!
                processCrawlData(job, cmsg.getDomainHarvestReport());

                // Send message to notify HarvestMonitor that
                // it should stop monitoring this job
                JMSConnectionFactory.getInstance().send(
                        new JobEndedMessage(job.getJobID(), newStatus));

                break;
            default:
                log.warn("CrawlStatusMessage tried to update job status to "
                         + "unsupported status " + newStatus);
                break;
        }
    }

    /**
     * Takes the crawl report from the job and updates the domain information
     * with harvesting history.
     * If the crawler was unable to generate a {@link HarvestReport},
     * it will do nothing.
     * @param job the completed job
     * @param dhr the domain harvest report, or null if none available.
     * @throws ArgumentNotValid if job is null
     */
    private void processCrawlData(Job job, HarvestReport dhr)
    throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(job, "job");

        //If the crawler was unable to generate a HarvestReport,
        //we will do nothing.

        if (dhr == null) {
            return;
        }

        // Post-process the report.
        dhr.postProcess(job);
    }

    /**
     * @param msg a given CrawlStatusMessage
     * @see dk.netarkivet.harvester.distribute.HarvesterMessageHandler#visit(
     * dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage)
     */
    public void visit(CrawlStatusMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        processCrawlStatusMessage(msg);
    }

    /**
     * Removes the HarvestSchedulerMonitorServer as listener
     * to the JMS scheduler Channel.
     */
    @Override
    public void shutdown() {
        // FIXME This command fail when shutting down properly. (kill $PID)
        // instead of kill -9 $PID. See NAS-1976
        //JMSConnectionFactory.getInstance().removeListener(
        //        Channels.getTheSched(), this);
    }

    @Override
    public void visit(IndexReadyMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        processIndexReadyMessage(msg);
    }
    
    /**
     * Process an incoming IndexReadyMessage.
     * @param msg the message
     */
    private void processIndexReadyMessage(IndexReadyMessage msg) {
        // Set isindexready to true
        Long harvestId = msg.getHarvestId();
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        if (dao.isSnapshot(harvestId)) {
            dao.setIndexIsReady(harvestId, true);
            log.info("Got message from IndexServer, that index is ready for"
                    + " harvest # " + harvestId);
        } else {
            log.debug("Ignoring IndexreadyMesssage sent on behalf on "
                    + "selective harvest w/id " + harvestId);
        }
    }
}
