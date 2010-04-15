/* File:       $Id: RunningJobInfo.java 752 2009-03-05 18:09:21Z svc $
 * Revision:   $Revision: 752 $
 * Author:     $Author: svc $
 * Date:       $Date: 2009-03-05 19:09:21 +0100 (to, 05 mar 2009) $
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
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.STATUS;

/**
 * This class is a simple bean storing information about a started job.
 */
public class StartedJobInfo implements Comparable<StartedJobInfo> {
	
	/**
	 * Utility class, formats strings representing durations.
	 */
	private static class DurationFormatter {
		private static final long DAY = 60 * 60 * 24;
		private static final long HOUR = 60 * 60;
		private static final long MINUTE = 60;		
		public static String format(long seconds) {
			if (seconds > 0L) {
				long lRest;

				String strDays = lpad(String.valueOf(seconds / DAY)) + "d ";
				lRest = seconds % DAY;
				
				String strHours = lpad(String.valueOf(lRest / HOUR)) + ":";
				lRest %= HOUR;
				
				String strMinutes = lpad(String.valueOf(lRest / MINUTE)) + ":";
				lRest %= MINUTE;
				
				String strSeconds = lpad(String.valueOf(lRest));

				return  strDays + strHours + strMinutes + strSeconds ;
			
			} else if (seconds == 0L) {
				return "0d 00:00:00";
			} else {
				return "-1";
			}
		}
	
	    private static String lpad(String s) {
	    	return (s.length() == 1 ? "0" + s : s);
	    }
	
	}

	private static final String NOT_AVAILABLE_STRING = "";
	private static final long NOT_AVAILABLE_NUM = -1L;

	private static final DecimalFormat DECIMAL = new DecimalFormat("###.##");

	private static final MessageFormat FRONTIER_SHORT_FMT = new MessageFormat(
			"{0} queues: {1} active ({2} in-process; "
					+ "{3} ready; {4} snoozed); {5} inactive; "
					+ "{6} retired; {7} exhausted");

	/**
	 * The job identifier.
	 */
	private long jobId;
	
	/**
	 * The owner harvest's name.
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
	 * Number of active Heritrix worker threads
	 */
	private int activeToeCount;
	
	/**
	 * Number of alerts raised by Heritrix since the crawl began.
	 */
	private long alertsCount;
	
	/**
	 * Current job status.
	 */
	private STATUS status;

	/**
	 * Instantiates all readable fields with default values.
	 * @param harvestId
	 * @param jobId
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
		this.status = STATUS.PRE_CRAWL;
	}

	public long getJobId() {
		return jobId;
	}

	public String getHarvestName() {
		return harvestName;
	}

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

	public String getHostUrl() {
		return hostUrl;
	}

	public String getProgress() {
		return DECIMAL.format(progress) + "%";
	}

	public long getQueuedFilesCount() {
		return queuedFilesCount;
	}

	public long getTotalQueuesCount() {
		return totalQueuesCount;
	}

	public long getActiveQueuesCount() {
		return activeQueuesCount;
	}

//	public long getRetiredQueuesCount() {
//		return retiredQueuesCount;
//	}

	public long getExhaustedQueuesCount() {
		return exhaustedQueuesCount;
	}

	public String getElapsedTime() {
		return DurationFormatter.format(elapsedSeconds);
	}

	public long getAlertsCount() {
		return alertsCount;
	}

	public long getDownloadedFilesCount() {
		return downloadedFilesCount;
	}

	public String getCurrentProcessedKBPerSec() {
		return DECIMAL.format(currentProcessedKBPerSec);
	}
	
	public String getProcessedKBPerSec() {
		return DECIMAL.format(processedKBPerSec);
	}

	public String getCurrentProcessedDocsPerSec() {
		return DECIMAL.format(currentProcessedDocsPerSec);
	}
	
	public String getProcessedDocsPerSec() {
		return DECIMAL.format(processedDocsPerSec);
	}

	public int getActiveToeCount() {
		return activeToeCount;
	}

	public STATUS getStatus() {
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
	
	public void update(CrawlProgressMessage msg) {
		
		CrawlServiceInfo heritrixInfo = msg.getHeritrixStatus();
		CrawlServiceJobInfo jobInfo = msg.getJobStatus();
		
		STATUS newStatus = msg.getStatus();
		switch (newStatus) {
			case PRE_CRAWL:
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
						//						this.retiredQueuesCount = 
						//						Long.parseLong((String) params[6]);
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
