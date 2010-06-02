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
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;

/**
 * This class is a simple bean storing information about a started job.
 */
public class StartedJobInfo implements Comparable<StartedJobInfo> {
	
	private static final String NOT_AVAILABLE_STRING = "";
	private static final long NOT_AVAILABLE_NUM = -1L;

	private static final DecimalFormat DECIMAL = new DecimalFormat("###.##");

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
	 * The number of URIS harvested by Heritrix 
	 * since the beginning of the crawl.
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
//	private long retiredQueuesCount;
	
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
	 * Instantiates all readable fields with default values.
	 * @param harvestId the ID of the harvest
	 * @param jobId the ID of the job
	 */
	protected StartedJobInfo(long harvestId, long jobId) {
		this.jobId = jobId;
		this.harvestName = 
			HarvestDefinitionDAO.getInstance().read(harvestId).getName();		
		this.hostUrl = NOT_AVAILABLE_STRING;
		this.progress = NOT_AVAILABLE_NUM;
		this.queuedFilesCount = NOT_AVAILABLE_NUM;
		this.totalQueuesCount = NOT_AVAILABLE_NUM;
		this.activeQueuesCount = NOT_AVAILABLE_NUM;
//		this.retiredQueuesCount = NOT_AVAILABLE_NUM;
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
	 * @return the URL of the Heritrix admin console for the instance 
	 * crawling this job.
	 */
	public String getHostUrl() {
		return hostUrl;
	}

	/**
	 * @return the crawl progress as a percentage.
	 */
	public String getProgress() {
		return DECIMAL.format(progress) + "%";
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

//	public long getRetiredQueuesCount() {
//		return retiredQueuesCount;
//	}

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
	public String getCurrentProcessedKBPerSec() {
		return DECIMAL.format(currentProcessedKBPerSec);
	}
	
	/**
	 * @return the average download rate in KB/sec reported by Heritrix.
	 */
	public String getProcessedKBPerSec() {
		return DECIMAL.format(processedKBPerSec);
	}

	/**
	 * @return the current download rate in URI/sec reported by Heritrix.
	 */
	public String getCurrentProcessedDocsPerSec() {
		return DECIMAL.format(currentProcessedDocsPerSec);
	}
	
	/**
	 * @return the average download rate in URI/sec reported by Heritrix.
	 */
	public String getProcessedDocsPerSec() {
		return DECIMAL.format(processedDocsPerSec);
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
		return new Long(jobId).compareTo(new Long(o.jobId));
	}

	@Override
	public String toString() {
		return harvestName + " - " + jobId + " {"
			+ "\n\tstatus=" + status.name()
			+ "\n\telapsedSeconds=" + elapsedSeconds
			+ "\n\thostUrl=" + hostUrl
			+ "\n\tprogress=" + progress
			+ "\n\tactiveToeCount=" + activeToeCount
			+ "\n\talertsCount=" + alertsCount
			+ "\n\tcurrentProcessedKBPerSec=" + currentProcessedKBPerSec
			+ "\n\tprocessedKBPerSec=" + processedKBPerSec
			+ "\n\tcurrentProcessedDocsPerSec=" + currentProcessedDocsPerSec
			+ "\n\tprocessedDocsPerSec=" + processedDocsPerSec
			+ "\n\tdownloadedFilesCount=" + downloadedFilesCount
			+ "\n\tqueuedFilesCount=" + queuedFilesCount
			+ "\n\tactiveQueuesCount=" + activeQueuesCount
			+ "\n\texhaustedQueuesCount=" + exhaustedQueuesCount
			+ "\n\ttotalQueuesCount=" + totalQueuesCount
			+ "\n}";
	}
	
	/**
	 * Updates the members from a {@link CrawlProgressMessage} instance.
	 * @param msg the {@link CrawlProgressMessage} to process.
	 */
	public void update(CrawlProgressMessage msg) {
		
		CrawlServiceInfo heritrixInfo = msg.getHeritrixStatus();
		CrawlServiceJobInfo jobInfo = msg.getJobStatus();
		
		CrawlStatus newStatus = msg.getStatus();
		switch (newStatus) {
			case PRE_CRAWL:
				// Initialize statistics-variables before starting the crawl.
				this.activeQueuesCount = 0;
				this.activeToeCount = 0;
				this.alertsCount = 0;
				this.currentProcessedDocsPerSec = 0;
				this.currentProcessedKBPerSec = 0;
				this.downloadedFilesCount = 0;
				this.elapsedSeconds = 0;
				this.hostUrl = "";
				this.processedDocsPerSec = 0;
				this.processedKBPerSec = 0;
				this.progress = 0;
				this.queuedFilesCount = 0;
				this.totalQueuesCount = 0;
				break;
				
			case CRAWLER_ACTIVE:
			case CRAWLER_PAUSED:
				// Update statistics for the crawl
				double discoveredCount = jobInfo.getDiscoveredFilesCount();
				double downloadedCount = jobInfo.getDownloadedFilesCount();
				this.progress  = 100 * downloadedCount / discoveredCount;

				String frontierShortReport = jobInfo.getFrontierShortReport();
				if (frontierShortReport != null) {
					try {
						Object[] params = FRONTIER_SHORT_FMT.parse(
								frontierShortReport);
						this.totalQueuesCount = 
							Long.parseLong((String) params[0]);
						this.activeQueuesCount = 
							Long.parseLong((String) params[1]);
						// this.retiredQueuesCount = 
						// Long.parseLong((String) params[6]);
						this.exhaustedQueuesCount = 
							Long.parseLong((String) params[7]);
					} catch (ParseException e) {
						throw new ArgumentNotValid(frontierShortReport, e);
					}
				}				

				this.activeToeCount = jobInfo.getActiveToeCount();
				this.alertsCount = heritrixInfo.getAlertCount();
				this.currentProcessedDocsPerSec = 
					jobInfo.getCurrentProcessedDocsPerSec();
				this.currentProcessedKBPerSec = 
					jobInfo.getCurrentProcessedKBPerSec();
				this.downloadedFilesCount = 
					jobInfo.getDownloadedFilesCount();
				this.elapsedSeconds = jobInfo.getElapsedSeconds();
				this.hostUrl = msg.getHostUrl();
				this.processedDocsPerSec = jobInfo.getProcessedDocsPerSec();
				this.processedKBPerSec = jobInfo.getProcessedKBPerSec();
				this.queuedFilesCount = jobInfo.getQueuedUriCount();
				break;

			case CRAWLING_FINISHED:
				// Set progress to 100 %, and reset the other values .
				this.progress = 100;
				this.hostUrl = "";
				this.activeQueuesCount = 0;
				this.activeToeCount = 0;
				this.currentProcessedDocsPerSec = 0;
				this.currentProcessedKBPerSec = 0;
				this.processedDocsPerSec = 0;
				this.processedKBPerSec = 0;
				this.queuedFilesCount = 0;
				this.totalQueuesCount = 0;
				break;
		}
		this.status = newStatus;
	}
	

}
