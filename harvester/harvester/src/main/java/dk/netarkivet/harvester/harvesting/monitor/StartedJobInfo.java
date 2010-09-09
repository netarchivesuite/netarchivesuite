/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.harvester.harvesting.monitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;

/**
 * This class is a simple bean storing information about a started job.
 *
 * This class is a persistent entity as per Berkeley DB JE DPL API.
 */
public class StartedJobInfo implements Comparable<StartedJobInfo> {

    /**list of the compare criteria.*/
    public enum Criteria { JOBID, HOST, PROGRESS, ELAPSED,
        QFILES, TOTALQ, ACTIVEQ, EXHAUSTEDQ };
    /**current compare criteria.*/
    private StartedJobInfo.Criteria compareCriteria =
        StartedJobInfo.Criteria.JOBID;

    private static final String NOT_AVAILABLE_STRING = "";
    private static final long NOT_AVAILABLE_NUM = -1L;

    /**
     * A text format used to parse Heritrix's short frontier report.
     */
    private static final MessageFormat FRONTIER_SHORT_FMT = new MessageFormat(
            "{0} queues: {1} active ({2} in-process; "
                    + "{3} ready; {4} snoozed); {5} inactive; "
                    + "{6} retired; {7} exhausted");

    /**
     * The job identifier.
     */
    private long jobId;

    /**
     * The name of the harvest this job belongs to.
     */
    private String harvestName;

    /**
     * Creation date of the record.
     */
    private Date timestamp;

    /**
     * URL to the Heritrix admin console.
     */
    private String hostUrl;

    /**
     * A percentage indicating the crawl progress.
     */
    private double progress;

    /**
     * The number of URIs queued, awaiting to be processed by Heritrix.
     */
    private long queuedFilesCount;

    /**
     * The number of URIS harvested by Heritrix since the beginning of the
     * crawl.
     */
    private long downloadedFilesCount;

    /**
     * The total number of queues in the frontier. Queues are per-domain.
     */
    private long totalQueuesCount;

    /**
     * The number of queues in process.
     */
    private long activeQueuesCount;

    /**
     * The number of queues that have been retired when they hit their quota.
     */
    // private long retiredQueuesCount;
    /**
     * Number of queues entirely processed.
     */
    private long exhaustedQueuesCount;

    /**
     * Time in seconds elapsed since Heritrix began crawling this job.
     */
    private long elapsedSeconds;

    /**
     * Current download rate in KB/sec.
     */
    private long currentProcessedKBPerSec;

    /**
     * Average download rate (over a period of 20 seconds) in KB/sec.
     */
    private long processedKBPerSec;

    /**
     * Current download rate in URI/sec.
     */
    private double currentProcessedDocsPerSec;

    /**
     * Average download rate (over a period of 20 seconds) in URI/sec.
     */
    private double processedDocsPerSec;

    /**
     * Number of active Heritrix worker threads.
     */
    private int activeToeCount;

    /**
     * Number of alerts raised by Heritrix since the crawl began.
     */
    private long alertsCount;

    /**
     * Current job status.
     */
    private CrawlStatus status;

    /**
     * Needed by BDB DPL.
     */
    public StartedJobInfo() {

    }

    /**
     * Instantiates all readable fields with default values.
     *
     * @param harvestName
     *            the name of the harvest
     * @param jobId
     *            the ID of the job
     */
    public StartedJobInfo(String harvestName, long jobId) {
        this();
        this.timestamp = new Date(System.currentTimeMillis());
        this.jobId = jobId;
        this.harvestName = harvestName;
        this.hostUrl = NOT_AVAILABLE_STRING;
        this.progress = NOT_AVAILABLE_NUM;
        this.queuedFilesCount = NOT_AVAILABLE_NUM;
        this.totalQueuesCount = NOT_AVAILABLE_NUM;
        this.activeQueuesCount = NOT_AVAILABLE_NUM;
        // this.retiredQueuesCount = NOT_AVAILABLE_NUM;
        this.exhaustedQueuesCount = NOT_AVAILABLE_NUM;
        this.elapsedSeconds = NOT_AVAILABLE_NUM;
        this.alertsCount = NOT_AVAILABLE_NUM;
        this.downloadedFilesCount = NOT_AVAILABLE_NUM;
        this.currentProcessedKBPerSec = NOT_AVAILABLE_NUM;
        this.processedKBPerSec = NOT_AVAILABLE_NUM;
        this.currentProcessedDocsPerSec = NOT_AVAILABLE_NUM;
        this.processedDocsPerSec = NOT_AVAILABLE_NUM;
        this.activeToeCount = (int) NOT_AVAILABLE_NUM;
        this.status = CrawlStatus.PRE_CRAWL;
    }

    /**
     * @return the job ID.
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * @return the harvest name.
     */
    public String getHarvestName() {
        return harvestName;
    }

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @return the name of the host on which Heritrix is crawling this job.
     */
    public String getHostName() {
        if (NOT_AVAILABLE_STRING.equals(hostUrl)) {
            return NOT_AVAILABLE_STRING;
        }
        try {
            return new URL(hostUrl).getHost();
        } catch (MalformedURLException e) {
            return NOT_AVAILABLE_STRING;
        }
    }

    /**
     * @return the URL of the Heritrix admin console for the instance crawling
     *         this job.
     */
    public String getHostUrl() {
        return hostUrl;
    }

    /**
     * @return the crawl progress as a numeric percentage.
     */
    public double getProgress() {
        return progress;
    }

    /**
     * @return the number of queued files reported by Heritrix.
     */
    public long getQueuedFilesCount() {
        return queuedFilesCount;
    }

    /**
     * @return the number of queues reported by Heritrix.
     */
    public long getTotalQueuesCount() {
        return totalQueuesCount;
    }

    /**
     * @return the number of active queues reported by Heritrix.
     */
    public long getActiveQueuesCount() {
        return activeQueuesCount;
    }

    // public long getRetiredQueuesCount() {
    // return retiredQueuesCount;
    // }

    /**
     * @return the number of exhausted queues reported by Heritrix.
     */
    public long getExhaustedQueuesCount() {
        return exhaustedQueuesCount;
    }

    /**
     * @return the formatted duration of the crawl.
     */
    public String getElapsedTime() {
        return StringUtils.formatDuration(elapsedSeconds);
    }

    /**
     * @return the duration of the crawl so far.
     */
    public Long getElapsedSeconds() {
        return elapsedSeconds;
    }

    /**
     * @return the number of alerts raised by Heritrix.
     */
    public long getAlertsCount() {
        return alertsCount;
    }

    /**
     * @return the number of downloaded URIs reported by Heritrix.
     */
    public long getDownloadedFilesCount() {
        return downloadedFilesCount;
    }

    /**
     * @return the current download rate in KB/sec reported by Heritrix.
     */
    public long getCurrentProcessedKBPerSec() {
        return currentProcessedKBPerSec;
    }

    /**
     * @return the average download rate in KB/sec reported by Heritrix.
     */
    public long getProcessedKBPerSec() {
        return processedKBPerSec;
    }

    /**
     * @return the current download rate in URI/sec reported by Heritrix.
     */
    public double getCurrentProcessedDocsPerSec() {
        return currentProcessedDocsPerSec;
    }

    /**
     * @return the average download rate in URI/sec reported by Heritrix.
     */
    public double getProcessedDocsPerSec() {
        return processedDocsPerSec;
    }

    /**
     * @return the number of active processor threads reported by Heritrix.
     */
    public int getActiveToeCount() {
        return activeToeCount;
    }

    /**
     * @return the job status
     * @see CrawlStatus
     */
    public CrawlStatus getStatus() {
        return status;
    }

    @Override
    public int compareTo(StartedJobInfo o) {

        if (compareCriteria == StartedJobInfo.Criteria.HOST) {
            return hostUrl.compareTo(o.hostUrl);
        }
        if (compareCriteria == StartedJobInfo.Criteria.PROGRESS) {
            return new Double(progress).compareTo(new Double(o.progress));
        }
        if (compareCriteria == StartedJobInfo.Criteria.ELAPSED) {
            return new Long(elapsedSeconds).
            compareTo(new Long(o.elapsedSeconds));
        }
        if (compareCriteria == StartedJobInfo.Criteria.QFILES) {
            return new Long(queuedFilesCount).
            compareTo(new Long(o.queuedFilesCount));
        }
        if (compareCriteria == StartedJobInfo.Criteria.TOTALQ) {
            return new Long(totalQueuesCount).
            compareTo(new Long(o.totalQueuesCount));
        }
        if (compareCriteria == StartedJobInfo.Criteria.ACTIVEQ) {
            return new Long(activeQueuesCount).
            compareTo(new Long(o.activeQueuesCount));
        }
        if (compareCriteria == StartedJobInfo.Criteria.EXHAUSTEDQ) {
            return new Long(exhaustedQueuesCount).
            compareTo(new Long(o.exhaustedQueuesCount));
        }
        return new Long(jobId).compareTo(new Long(o.jobId));
    }

    public void chooseCompareCriteria(StartedJobInfo.Criteria criteria) {
        compareCriteria = criteria;
    }

    @Override
    public String toString() {
        return harvestName + " - " + jobId + " {" + "\n\tstatus="
                + status.name() + "\n\telapsedSeconds=" + elapsedSeconds
                + "\n\thostUrl=" + hostUrl + "\n\tprogress=" + progress
                + "\n\tactiveToeCount=" + activeToeCount + "\n\talertsCount="
                + alertsCount + "\n\tcurrentProcessedKBPerSec="
                + currentProcessedKBPerSec + "\n\tprocessedKBPerSec="
                + processedKBPerSec + "\n\tcurrentProcessedDocsPerSec="
                + currentProcessedDocsPerSec + "\n\tprocessedDocsPerSec="
                + processedDocsPerSec + "\n\tdownloadedFilesCount="
                + downloadedFilesCount + "\n\tqueuedFilesCount="
                + queuedFilesCount + "\n\tactiveQueuesCount="
                + activeQueuesCount + "\n\texhaustedQueuesCount="
                + exhaustedQueuesCount + "\n\ttotalQueuesCount="
                + totalQueuesCount + "\n}";
    }

    /**
     * Updates the members from a {@link CrawlProgressMessage} instance.
     * 
     * @param msg
     *            the {@link CrawlProgressMessage} to process.
     */
    public static StartedJobInfo build(CrawlProgressMessage msg) {

        String harvestName = HarvestDefinitionDAO.getInstance().read(
                msg.getHarvestID()).getName();
        StartedJobInfo sji = new StartedJobInfo(harvestName, msg.getJobID());

        CrawlServiceInfo heritrixInfo = msg.getHeritrixStatus();
        CrawlServiceJobInfo jobInfo = msg.getJobStatus();

        CrawlStatus newStatus = msg.getStatus();
        switch (newStatus) {
        case PRE_CRAWL:
            // Initialize statistics-variables before starting the crawl.
            sji.activeQueuesCount = 0;
            sji.activeToeCount = 0;
            sji.alertsCount = 0;
            sji.currentProcessedDocsPerSec = 0;
            sji.currentProcessedKBPerSec = 0;
            sji.downloadedFilesCount = 0;
            sji.elapsedSeconds = 0;
            sji.hostUrl = "";
            sji.processedDocsPerSec = 0;
            sji.processedKBPerSec = 0;
            sji.progress = 0;
            sji.queuedFilesCount = 0;
            sji.totalQueuesCount = 0;
            break;

        case CRAWLER_ACTIVE:
        case CRAWLER_PAUSING:
        case CRAWLER_PAUSED:
            // Update statistics for the crawl
            double discoveredCount = jobInfo.getDiscoveredFilesCount();
            double downloadedCount = jobInfo.getDownloadedFilesCount();
            sji.progress = 100 * downloadedCount / discoveredCount;

            String frontierShortReport = jobInfo.getFrontierShortReport();
            if (frontierShortReport != null) {
                try {
                    Object[] params = FRONTIER_SHORT_FMT
                            .parse(frontierShortReport);
                    sji.totalQueuesCount = Long.parseLong((String) params[0]);
                    sji.activeQueuesCount = Long.parseLong((String) params[1]);
                    // sji.retiredQueuesCount =
                    // Long.parseLong((String) params[6]);
                    sji.exhaustedQueuesCount = Long
                            .parseLong((String) params[7]);
                } catch (ParseException e) {
                    throw new ArgumentNotValid(frontierShortReport, e);
                }
            }

            sji.activeToeCount = jobInfo.getActiveToeCount();
            sji.alertsCount = heritrixInfo.getAlertCount();
            sji.currentProcessedDocsPerSec = jobInfo
                    .getCurrentProcessedDocsPerSec();
            sji.currentProcessedKBPerSec = jobInfo
                    .getCurrentProcessedKBPerSec();
            sji.downloadedFilesCount = jobInfo.getDownloadedFilesCount();
            sji.elapsedSeconds = jobInfo.getElapsedSeconds();
            sji.hostUrl = msg.getHostUrl();
            sji.processedDocsPerSec = jobInfo.getProcessedDocsPerSec();
            sji.processedKBPerSec = jobInfo.getProcessedKBPerSec();
            sji.queuedFilesCount = jobInfo.getQueuedUriCount();
            break;

        case CRAWLING_FINISHED:
            // Set progress to 100 %, and reset the other values .
            sji.progress = 100;
            sji.hostUrl = "";
            sji.activeQueuesCount = 0;
            sji.activeToeCount = 0;
            sji.currentProcessedDocsPerSec = 0;
            sji.currentProcessedKBPerSec = 0;
            sji.processedDocsPerSec = 0;
            sji.processedKBPerSec = 0;
            sji.queuedFilesCount = 0;
            sji.totalQueuesCount = 0;
            break;
        }
        sji.status = newStatus;
        
        return sji;
    }

    /**
     * @param hostUrl the hostUrl to set
     */
    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * @param queuedFilesCount the queuedFilesCount to set
     */
    public void setQueuedFilesCount(long queuedFilesCount) {
        this.queuedFilesCount = queuedFilesCount;
    }

    /**
     * @param downloadedFilesCount the downloadedFilesCount to set
     */
    public void setDownloadedFilesCount(long downloadedFilesCount) {
        this.downloadedFilesCount = downloadedFilesCount;
    }

    /**
     * @param totalQueuesCount the totalQueuesCount to set
     */
    public void setTotalQueuesCount(long totalQueuesCount) {
        this.totalQueuesCount = totalQueuesCount;
    }

    /**
     * @param activeQueuesCount the activeQueuesCount to set
     */
    public void setActiveQueuesCount(long activeQueuesCount) {
        this.activeQueuesCount = activeQueuesCount;
    }

    /**
     * @param exhaustedQueuesCount the exhaustedQueuesCount to set
     */
    public void setExhaustedQueuesCount(long exhaustedQueuesCount) {
        this.exhaustedQueuesCount = exhaustedQueuesCount;
    }

    /**
     * @param elapsedSeconds the elapsedSeconds to set
     */
    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    /**
     * @param currentProcessedKBPerSec the currentProcessedKBPerSec to set
     */
    public void setCurrentProcessedKBPerSec(long currentProcessedKBPerSec) {
        this.currentProcessedKBPerSec = currentProcessedKBPerSec;
    }

    /**
     * @param processedKBPerSec the processedKBPerSec to set
     */
    public void setProcessedKBPerSec(long processedKBPerSec) {
        this.processedKBPerSec = processedKBPerSec;
    }

    /**
     * @param currentProcessedDocsPerSec the currentProcessedDocsPerSec to set
     */
    public void setCurrentProcessedDocsPerSec(
            double currentProcessedDocsPerSec) {
        this.currentProcessedDocsPerSec = currentProcessedDocsPerSec;
    }

    /**
     * @param processedDocsPerSec the processedDocsPerSec to set
     */
    public void setProcessedDocsPerSec(double processedDocsPerSec) {
        this.processedDocsPerSec = processedDocsPerSec;
    }

    /**
     * @param activeToeCount the activeToeCount to set
     */
    public void setActiveToeCount(int activeToeCount) {
        this.activeToeCount = activeToeCount;
    }

    /**
     * @param alertsCount the alertsCount to set
     */
    public void setAlertsCount(long alertsCount) {
        this.alertsCount = alertsCount;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(CrawlStatus status) {
        this.status = status;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
