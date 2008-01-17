/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.scheduler;

import javax.jms.MessageListener;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DomainHarvestReport;

/**
 * Submitted harvesting jobs are registered with this singleton. The class
 * listens for CrawlStatusMessages on the THE_SCHED queue and processes
 * completed harvests.
 *
 */
public class HarvestSchedulerMonitorServer extends HarvesterMessageHandler
        implements MessageListener {
    /**
     * The JobDAO.
     */
    private final JobDAO jobDAO = JobDAO.getInstance();

    /**
     * The unique instance of this class.
     */
    private static HarvestSchedulerMonitorServer instance;

    /** The private logger for this class. */
    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * private to ensure use of getInstance method.
     */
    private HarvestSchedulerMonitorServer() {
        JMSConnectionFactory.getInstance().setListener(Channels.getTheSched(), this);
    }

    /**
     * Get the instance of the HarvestSchedulerMonitor.
     *
     * @return The HarvestScheduler instance
     */
    static HarvestSchedulerMonitorServer getInstance() {
        if (instance == null) {
            instance =
            new HarvestSchedulerMonitorServer();
        }
        return instance;
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
        JobStatus new_status = cmsg.getStatusCode();
        Job job = jobDAO.read(new Long(jobID));
        JobStatus old_status = job.getStatus();
        // Update the job status

        // a NEW job should never get a message
        if (old_status == JobStatus.NEW) {
            String msg = "CrawlStatusMessage received on new job: " + job;
            log.warn(msg);
        }

        switch (new_status) {
            case RESUBMITTED:
            case SUBMITTED:
            case NEW:
                // crawl status should never update to
                // new/submitted/resubmitted, because these statuses should not
                // be used by the harvesters!
                String msg = "CrawlStatusMessage tried to update job "
                             + job + " to status " + new_status;
                log.warn(msg);
                throw new ArgumentNotValid(msg);
            case STARTED:
                if (old_status == JobStatus.SUBMITTED
                    || old_status == JobStatus.NEW) {
                    if (old_status == JobStatus.NEW) {
                        log.warn("CrawlStatusMessage updated job in unexpected "
                                 + "state " + old_status + " to "
                                 + new_status + "\n"
                                 + job.toString());
                    }
                    // The usual case submitted -> started
                    job.setStatus(new_status);
                    log.debug(job + " has started crawling.");
                    jobDAO.update(job);
                } else {
                    // Must not change status back to STARTED
                    log.warn("CrawlStatusMessage tried to update job status"
                             + " for job " + job.getJobID()
                             + " from " + old_status + " to " + new_status
                             + ". Ignoring.");
                }
                break;
            case DONE:
            case FAILED:
                if (old_status == JobStatus.STARTED
                    || old_status == JobStatus.SUBMITTED
                    || old_status == JobStatus.RESUBMITTED
                    || old_status == JobStatus.NEW) {
                    // Received done or failed on non-ended job - okay
                    if (old_status != JobStatus.STARTED) {
                        //we expect "started" first, but it's not serious. Just
                        //log.
                        log.warn("CrawlStatusMessage updated job in unexpected "
                                 + "state " + old_status + " to "
                                 + new_status + "\n"
                                 + job.toString());
                    }
                    if (new_status == JobStatus.FAILED) {
                        log.warn("Job " + jobID + " failed: "
                                 + cmsg.getErrMsg());
                    } else {
                        log.info("Job " + jobID + " succesfully completed");
                    }
                    job.setStatus(new_status);
                    job.appendHarvestErrors(cmsg.getHarvestErrors());
                    job.appendHarvestErrorDetails(cmsg.getHarvestErrorDetails());
                    job.appendUploadErrors(cmsg.getUploadErrors());
                    job.appendUploadErrorDetails(cmsg.getUploadErrorDetails());
                    jobDAO.update(job);
                } else {
                    // Received done or failed on already dead job. Bad!
                    String message = "CrawlStatusMessage tried to update "
                                     + "job status "
                                     + " from " + old_status + " to "
                                     + new_status
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
                break;
            default:
                log.warn("CrawlStatusMessage tried to update job status to "
                         + "unsupported status " + new_status);
                break;
        }
    }

    /**
     * Takes the crawl report from the job and updates the domain information
     * with harvesting history.
     * If the crawler was unable to generate a DomainHarvestReport,
     * it will do nothing.
     * @param job the completed job
     * @param dhr the domain harvest report, or null if none available.
     * @throws ArgumentNotValid if job is null
     */
    private void processCrawlData(Job job, DomainHarvestReport dhr)
    throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(job, "job");

        //If the crawler was unable to generate a DomainHarvestReport,
        //we will do nothing.

        if (dhr == null) {
            return;
        }

        // Get the map from domain names to domain configurations
        Map<String, String> configurationMap = job.getDomainConfigurationMap();

        // For each domain harvested, check if it corresponds to a
        // domain configuration for this Job and if so add a new HarvestInfo
        // to the DomainHistory of the corresponding Domain object.
        // TODO:  Information about the domains harvested by the crawler
        // without a domain configuration for this job is deleted!
        // Should this information be saved in some way (perhaps stored
        // in metadata.arc-files?)

        final Set<String> domainNames = new HashSet<String>();
        domainNames.addAll(dhr.getDomainNames());
        domainNames.retainAll(configurationMap.keySet());
        final DomainDAO dao = DomainDAO.getInstance();
        for (String domainName : domainNames) {
            Domain domain = dao.read(domainName);

            // Retrieve crawl data from log and add it to HarvestInfo
            StopReason stopReason = dhr.getStopReason(domainName);
            long countObjectRetrieved = dhr.getObjectCount(domainName);
            long bytesReceived = dhr.getByteCount(domainName);

            //If StopReason is SIZE_LIMIT, we check if it's the harvests' size
            //limit, or rather a configuration size limit.

            //A harvest is considered to have hit the configuration limit if
            //1) The limit is lowest, or
            //2) The number of harvested bytes is greater than the limit

            // Note: Even though the per-config-byte-limit might have changed
            // between the time we calculated the job and now, it's okay we
            // compare with the new limit, since it gives us the most accurate
            // result for whether we want to harvest any more.
            if (stopReason == StopReason.SIZE_LIMIT) {
                long maxBytesPerDomain = job.getMaxBytesPerDomain();
                long configMaxBytes = domain.getConfiguration(
                        configurationMap.get(domainName)).getMaxBytes();
                if (NumberUtils.compareInf(configMaxBytes, maxBytesPerDomain)
                    <= 0
                    || NumberUtils.compareInf(configMaxBytes, bytesReceived)
                       <= 0) {
                    stopReason = StopReason.CONFIG_SIZE_LIMIT;
                }
            }
            // Create the HarvestInfo object
            HarvestInfo hi = new HarvestInfo(
                    job.getOrigHarvestDefinitionID(), job.getJobID(),
                    domain.getName(), configurationMap.get(domain.getName()),
                    new Date(), bytesReceived, countObjectRetrieved,
                    stopReason);

            // Add HarvestInfo to Domain and make data persistent
            // by updating DAO
            domain.getHistory().addHarvestInfo(hi);
            dao.update(domain);
        }
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
     * Close down the HarvestSchedulerMonitorServer singleton.
     * This removes the HarvestSchedulerMonitorServer as listener
     * to the JMS scheduler Channel, and resets the singleton.
     *
     */
    public void close() {
        JMSConnectionFactory.getInstance().removeListener(
                Channels.getTheSched(), this);
        cleanup();
    }

    /**
     *  Cleanup method. Resets HarvestSchedulerMonitorServer singleton.
     *  Note: this cleanup() methodm is called from
     *  HarvestScheduler.cleanup(), therefore it needs to be public
     */
    public void cleanup() {
        instance = null;
    }
}
