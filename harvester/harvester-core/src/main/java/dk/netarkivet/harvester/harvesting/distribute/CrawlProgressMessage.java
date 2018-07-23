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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import dk.netarkivet.harvester.harvesting.report.Heritrix1Constants;

/**
 * This class wraps information stored in the Heritrix MBeans, CrawlService and CrawlService.Job, and represents the
 * crawl progress.
 * <p>
 * Additionally this object extends {@link HarvesterMessage} so that it can be sent on the JMS bus to be processed by
 * {@link HarvestMonitor}.
 */
@SuppressWarnings({"serial"})
public class CrawlProgressMessage extends HarvesterMessage implements Serializable {

	/** The logger for this class. */
	
    private static final Logger log = LoggerFactory.getLogger(CrawlProgressMessage.class);
	
    /**
     * The general status of a job in NAS.
     */
    public static enum CrawlStatus {
        /**
         * Initial status of a job: Heritrix has not yet started crawling.
         */
        PRE_CRAWL,
        /**
         * Heritrix is actively crawling.
         */
        CRAWLER_ACTIVE,
        /**
         * Heritrix is crawling but is currently pausing.
         */
        CRAWLER_PAUSING,
        /**
         * Heritrix is crawling but has been paused by the user.
         */
        CRAWLER_PAUSED,
        /**
         * Heritrix has finished crawling, post processing of metadata and ARC files remains to be done.
         */
        CRAWLING_FINISHED
    }

    /**
     * Wraps CrawlService MBean attributes.
     */
    public class CrawlServiceInfo implements Serializable {

        /** The number of alerts raised by Heritrix. */
        private int alertCount;

        /** Flag is set to true when Heritrix is crawling or paused. */
        private boolean isCrawling;

        /** Contains the UID of the current job. */
        private String currentJob;

        public int getAlertCount() {
            return alertCount;
        }

        public void setAlertCount(int alertCount) {
            this.alertCount = alertCount;
        }

        public boolean isCrawling() {
            return isCrawling;
        }

        public void setCrawling(boolean isCrawling) {
            this.isCrawling = isCrawling;
        }

        public String getCurrentJob() {
            return currentJob;
        }

        public void setCurrentJob(String currentJob) {
            this.currentJob = currentJob;
        }
    }

    /**
     * Wraps CrawlService.Job MBean attributes.
     */
    public class CrawlServiceJobInfo implements Serializable {

        /** The number of URIs currently discovered. */
        private long discoveredFilesCount;

        /** The number of URIs currently harvested. */
        private long downloadedFilesCount;

        /** A summary of the frontier queues. */
        private String frontierShortReport;

        /** The time in seconds elapsed since the crawl began. */
        private long elapsedSeconds;

        /** The current download rate in KB/sec. */
        private long currentProcessedKBPerSec;

        /** The average download rate in KB/sec. */
        private long processedKBPerSec;

        /** The current download rate in URI/sec. */
        private double currentProcessedDocsPerSec;

        /** The average download rate in URI/sec. */
        private double processedDocsPerSec;

        /** The number of active toe threads for this job. */
        private int activeToeCount;

        /** A textual summary of the crawler activity. */
        private String progressStatistics;

        /** The job status. */
        private String status;

        public long getDiscoveredFilesCount() {
            return discoveredFilesCount;
        }

        public void setDiscoveredFilesCount(long discoveredFilesCount) {
            this.discoveredFilesCount = discoveredFilesCount;
        }

        public long getDownloadedFilesCount() {
            return downloadedFilesCount;
        }

        public void setDownloadedFilesCount(long downloadedFilesCount) {
            this.downloadedFilesCount = downloadedFilesCount;
        }

        public String getFrontierShortReport() {
            return frontierShortReport;
        }

        public void setFrontierShortReport(String frontierShortReport) {
            this.frontierShortReport = frontierShortReport;
        }

        public long getElapsedSeconds() {
            return elapsedSeconds;
        }

        public void setElapsedSeconds(long elapsedSeconds) {
            this.elapsedSeconds = elapsedSeconds;
        }

        public long getCurrentProcessedKBPerSec() {
            return currentProcessedKBPerSec;
        }

        public void setCurrentProcessedKBPerSec(long currentProcessedKBPerSec) {
            this.currentProcessedKBPerSec = currentProcessedKBPerSec;
        }

        public long getProcessedKBPerSec() {
            return processedKBPerSec;
        }

        public void setProcessedKBPerSec(long processedKBPerSec) {
            this.processedKBPerSec = processedKBPerSec;
        }

        public double getCurrentProcessedDocsPerSec() {
            return currentProcessedDocsPerSec;
        }

        public void setCurrentProcessedDocsPerSec(double currentProcessedDocsPerSec) {
            this.currentProcessedDocsPerSec = currentProcessedDocsPerSec;
        }

        public double getProcessedDocsPerSec() {
            return processedDocsPerSec;
        }

        public void setProcessedDocsPerSec(double processedDocsPerSec) {
            this.processedDocsPerSec = processedDocsPerSec;
        }

        public int getActiveToeCount() {
            return activeToeCount;
        }

        public void setActiveToeCount(int activeToeCount) {
            this.activeToeCount = activeToeCount;
        }

        public String getProgressStatistics() {
            return progressStatistics;
        }

        public void setProgressStatistics(String progressStatistics) {
            this.progressStatistics = progressStatistics;
        }

        /**
         * Helper method that approximates the number of queued URIs.
         *
         * @return the number of queued URIs
         */
        public long getQueuedUriCount() {
            return discoveredFilesCount - downloadedFilesCount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }

    /** The unique identifier of the job. */
    private final long jobID;

    /** The unique identifier of the associated harvest definition. */
    private long harvestID;

    /** The URL to the host Heritrix admin UI. */
    private String hostUrl;

    /** The job's status. */
    private CrawlStatus status;

    /** A legend, fetched only once, for the {@link CrawlServiceJobInfo#progressStatistics} property. */
    private final String progressStatisticsLegend;

    /** The information provided by the CrawlService MBean. */
    private CrawlServiceInfo heritrixStatus = new CrawlServiceInfo();

    /** The information provided by the CrawlService.Job MBean. */
    private CrawlServiceJobInfo jobStatus = new CrawlServiceJobInfo();

    /**
     * Builds an empty message. MBean wrapper values are not set and the appropriate getters should be used to do so.
     *
     * @param harvestID the harvest definition ID
     * @param jobId the job ID
     * @param progressStatisticsLegend the legend of the progress statistics summary string
     * @see CrawlProgressMessage#progressStatisticsLegend
     */
    public CrawlProgressMessage(long harvestID, long jobId, String progressStatisticsLegend) {
        super(HarvestMonitor.HARVEST_MONITOR_CHANNEL_ID, Channels.getError());
        this.harvestID = harvestID;
        this.jobID = jobId;
        this.status = CrawlStatus.PRE_CRAWL;
        this.progressStatisticsLegend = progressStatisticsLegend;
    }

    /**
     * Builds an empty message. MBean wrapper values are not set and the appropriate getters should be used to do so.
     * The progressStatisticsLegend is set to the empty string.
     *
     * @param harvestID the harvest definition ID
     * @param jobId the job ID
     */
    public CrawlProgressMessage(long harvestID, long jobId) {
        this(harvestID, jobId, "");
    }

    public long getHarvestID() {
        return harvestID;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public void setStatus(CrawlStatus status) {
        this.status = status;
    }

    public long getJobID() {
        return jobID;
    }

    public String getProgressStatisticsLegend() {
        return progressStatisticsLegend;
    }

    public CrawlServiceInfo getHeritrixStatus() {
        return heritrixStatus;
    }

    public CrawlServiceJobInfo getJobStatus() {
        return jobStatus;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Returns true if the crawler has been paused, and thus not supposed to fetch anything. Heritrix may still be
     * fetching stuff, as it takes some time for it to go into full pause mode. This method can be used as an indicator
     * that we should not be worried if Heritrix appears to be idle.
     *
     * @return True if the crawler has been paused, e.g. by using the Heritrix GUI.
     */
    public boolean isPaused() {
        return CrawlStatus.CRAWLER_PAUSED.equals(status);
    }

    /**
     * Checks whether Heritrix has finished crawling the job.
     *
     * @return true if Heritrix has finished crawling the job, false otherwise.
     */
    public boolean crawlIsFinished() {
    	// Evidently heritrixStatus.currentJob is set to "", if no job is crawling
        boolean jobInProgress = heritrixStatus.isCrawling() && !heritrixStatus.getCurrentJob().isEmpty();

        if (!jobInProgress) {
        	// FIXME does this work for H3 as well (If not modify the above logic)
        	log.info("Job {} seems to be no longer in progress. ", jobID);
            return true;
        }
        
        String statusAsString = getJobStatus().getStatus();
        
        if (statusAsString != null) {
        	// FIXME probably only works for H1 equals to the String "FINISHED"
        	log.info("StatusAsString = '{}'", statusAsString);
            return statusAsString.equals(Heritrix1Constants.CRAWLCONTROLLER_FINISHED);
        } 
        // statusAsString is null
        log.info("statusAsString is null for job {}. Considering the crawl to be not finished", jobID);
        
        return false;
    }

}
