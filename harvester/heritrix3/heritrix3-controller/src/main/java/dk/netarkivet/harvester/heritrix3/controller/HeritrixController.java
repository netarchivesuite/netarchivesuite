/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.harvester.heritrix3.controller;

import java.io.File;
import java.io.IOException;

import org.netarchivesuite.heritrix3wrapper.EngineResult;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper.CrawlControllerState;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;
import dk.netarkivet.harvester.heritrix3.Heritrix3Settings;

/**
 * This implementation of the HeritrixController interface starts Heritrix as a separate process and uses JMX to
 * communicate with it. Each instance executes exactly one process that runs exactly one crawl job.
 */
public class HeritrixController extends AbstractRestHeritrixController {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(HeritrixController.class);

    /**
     * Enum listing the different job attributes available.
     */
//    private static enum CrawlServiceJobAttribute {
//        /** The time in seconds elapsed since the crawl began. */
//        CrawlTime,
//        /** The current download rate in URI/s. */
//        CurrentDocRate,
//        /** The current download rate in kB/s. */
//        CurrentKbRate,
//        /** The number of URIs discovered by Heritrix. */
//        DiscoveredCount,
//        /** The average download rate in URI/s. */
//        DocRate,
//        /** The number of URIs downloaded by Heritrix. */
//        DownloadedCount,
//        /** A string summarizing the Heritrix frontier. */
//        FrontierShortReport,
//        /** The average download rate in kB/s. */
//        KbRate,
//        /** The job status (Heritrix status). */
//        Status,
//        /** The number of active toe threads. */
//        ThreadCount;
//
//        /**
//         * Returns the {@link CrawlServiceJobAttribute} enum value matching the given name. Throws {@link UnknownID} if
//         * no match is found.
//         *
//         * @param name the attribute name
//         * @return the corresponding {@link CrawlServiceJobAttribute} enum value.
//         */
//        public static CrawlServiceJobAttribute fromString(String name) {
//            for (CrawlServiceJobAttribute att : values()) {
//                if (att.name().equals(name)) {
//                    return att;
//                }
//            }
//            throw new UnknownID(name + " : unknown CrawlServiceJobAttribute !");
//        }
//    }

//    /**
//     * Enum class defining the general operations available to the Heritrix operator.
//     */
//    private static enum CrawlServiceOperation {
//        /** Adds a new job to an Heritrix instance. */
//        addJob,
//        /** Fetches the identifiers of pending jobs. */
//        pendingJobs,
//        /** Fetches the identifiers of completed jobs. */
//        completedJobs,
//        /** Shuts down an Heritrix instance. */
//        shutdown,
//        /** Instructs an Heritrix instance to starts crawling jobs. */
//        startCrawling,
//        /** Instructs an Heritrix instance to terminate the current job. */
//        terminateCurrentJob;
//    }
//
//    /**
//     * Enum class defining the Job-operations available to the Heritrix operator.
//     */
//    private static enum CrawlServiceJobOperation {
//        /** Fetches the progress statistics string from an Heritrix instance. */
//        progressStatistics,
//        /**
//         * Fetches the progress statistics legend string from an Heritrix instance.
//         */
//        progressStatisticsLegend,
//        /** Fetches the frontier report. */
//        frontierReport;
//    }

    /**
     * Shall we abort, if we lose the connection to Heritrix.
     */
    private static final boolean ABORT_IF_CONN_LOST = Settings.getBoolean(Heritrix3Settings.ABORT_IF_CONNECTION_LOST);

    /**
     * The name that Heritrix gives to the job we ask it to create. This is part of the name of the MBean for that job,
     * but we can only retrieve the name after the MBean has been created.
     */
    private String jobName;

    /** The header line (legend) for the statistics report. */
    private String progressStatisticsLegend;

    /**
     * Create a BnfHeritrixController object.
     *
     * @param files Files that are used to set up Heritrix.
     */
    public HeritrixController(Heritrix3Files files) {
        super(files);
    }

    /**
     * Initialize the JMXconnection to the Heritrix.
     *
     * @throws IOFailure If Heritrix dies before initialization, or we encounter any problems during the initialization.
     * @see IHeritrixController#initialize()
     */
    @Override
    public void initialize() {

    	/////////////////////////////////////////////////////
        // Initialize H3 wrapper 
    	/////////////////////////////////////////////////////

    	//File keystoreFile= null;
        //String keyStorePassword = null;

        h3wrapper = Heritrix3Wrapper.getInstance(getHostName(), getGuiPort(), 
        		null, null, getHeritrixAdminName(), getHeritrixAdminPassword());

        EngineResult engineResult;
        try {
        	//TODO these numbers should be settings
        	int tries = 60;
        	int millisecondsBetweenTries = 1000;
        	engineResult = h3wrapper.waitForEngineReady(tries, millisecondsBetweenTries);
        } catch (Throwable e){
        	e.printStackTrace();
        	throw new IOFailure("Heritrix not started: " + e);
        }

        if (engineResult != null) {
        	if (engineResult.status != ResultStatus.OK) {
            	log.error("Heritrix3 wrapper could not connect to Heritrix3. Resultstate = {}", engineResult.status, engineResult.t);
            	throw new IOFailure("Heritrix3 wrapper could not connect to Heritrix3. Resultstate = " + engineResult.status);
        	}
        } else {
        	throw new IOFailure("Heritrix3 wrapper returned null engine result.");
        }
        
        // POST: Heritrix3 is up and running and responds nicely
        log.info("Heritrix3 REST interface connectable.");
    }

    @Override
    public void requestCrawlStart() {
    	// Create a new job 
        File cxmlFile = getHeritrixFiles().getOrderFile();
        File seedsFile = getHeritrixFiles().getSeedsFile();
        JobResult jobResult;

  		File jobDir = files.getHeritrixJobDir();
  		if (!jobDir.exists()) {
  			jobDir.mkdirs();
  		}

  		try {
  			log.info("Copying the crawler-beans.cxml file and seeds.txt to the heritrix3 jobdir '{}'", jobDir);
  			Heritrix3Wrapper.copyFile( cxmlFile, jobDir );
  			Heritrix3Wrapper.copyFileAs( seedsFile, jobDir, "seeds.txt" ); 
  		} catch (IOException e) {
  			throw new IOFailure("Problem occurred during the copying of files to our heritrix job", e);
  		}

  		// PRE: h3 is running, and the job files copied to their final location? 
  		EngineResult engineResult = null;
  		try {
      		engineResult = h3wrapper.rescanJobDirectory();
      		log.debug("Result of rescanJobDirectory() operation: " + new String(engineResult.response, "UTF-8"));
      		jobResult = h3wrapper.buildJobConfiguration(jobName);
      		log.debug("Result of buildJobConfiguration() operation: " + new String(jobResult.response, "UTF-8"));
      		jobResult = h3wrapper.waitForJobState(jobName, CrawlControllerState.NASCENT, 60, 1000);
      		jobResult = h3wrapper.launchJob(jobName);
      		log.debug("Result of launchJob() operation: " + new String(jobResult.response, "UTF-8"));
      		jobResult = h3wrapper.waitForJobState(jobName, CrawlControllerState.PAUSED, 60, 1000);
      		jobResult = h3wrapper.unpauseJob(jobName);
      	} catch (Throwable e) {
      		throw new IOFailure("Unknown error during communication with heritrix3", e);
      	}

  		// POST: h3 is running, and the job with name 'jobName' is running
  		log.debug("h3-State after unpausing job '{}': {}", jobName, jobResult.response);
    }

    @Override
    public void requestCrawlStop(String reason) {
    	log.info("Terminating job {}. Reason: {}", this.jobName,  reason);
    	JobResult jobResult = h3wrapper.job(jobName);
    	if (jobResult != null) {
    		if (jobResult.job.isRunning) {
    			JobResult result = h3wrapper.terminateJob(this.jobName);
    			if (!result.job.isRunning) {
    				log.warn("Job '{}' terminated", this.jobName);
    			}
    		} else {
    			log.warn("Job '{}' not terminated, as it was not running", this.jobName);
    		}
    	} else {
    		log.warn("Job '{}' has maybe already been terminated and/or heritrix3 is no longer running", this.jobName); 
    	}
        
    }

    /**
     * Return the URL for monitoring this instance.
     *
     * @return the URL for monitoring this instance.
     */
    public String getHeritrixConsoleURL() {
        return "https://" + SystemUtils.getLocalHostName() + ":" + getGuiPort() + "/engine";
    }

    /**
     * Cleanup after an Heritrix process. This entails sending the shutdown command to the Heritrix process, and killing
     * it forcefully, if it is still alive after waiting the period of time specified by the
     * CommonSettings.PROCESS_TIMEOUT setting.
     *
     * @param crawlDir the crawldir to cleanup
     * @see IHeritrixController#cleanup()
     */
    public void cleanup(File crawlDir) {
        // Before cleaning up, we need to wait for the reports to be generated
        //waitForReportGeneration(crawlDir);
        // TODO Should we teardown job as well????
        EngineResult result = h3wrapper.exitJavaProcess(null);
        // TODO examine the result of exitJavaProcess
        h3launcher.process.destroy(); // TODO Should we catch something here
    }

    /**
     * Return the URL for monitoring this instance.
     *
     * @return the URL for monitoring this instance.
     */
    public String getAdminInterfaceUrl() {
        return "https://" + SystemUtils.getLocalHostName() + ":" + getGuiPort() + "/engine";
    }

    /**
     * Gets a message that stores the information summarizing the crawl progress.
     *
     * @return a message that stores the information summarizing the crawl progress.
     */
    public CrawlProgressMessage getCrawlProgress() {
        Heritrix3Files files = getHeritrixFiles();
        CrawlProgressMessage cpm = new CrawlProgressMessage(files.getHarvestID(), files.getJobID(),
                progressStatisticsLegend);
        cpm.setHostUrl(getHeritrixConsoleURL());
        JobResult jobResult = h3wrapper.job(jobName);
        if (jobResult != null) {
        	getCrawlServiceAttributes(cpm, jobResult);
        } else {
        	log.warn("Unable to engineStatus for job '{}'", jobName);
        }
        if (cpm.crawlIsFinished()) {
            cpm.setStatus(CrawlStatus.CRAWLING_FINISHED);
            // No need to go further, CrawlService.Job bean does not exist
            return cpm;
        }
        if (jobResult != null) {
        	fetchCrawlServiceJobAttributes(cpm, jobResult);
        } else {
        	log.warn("Unable to get JobAttributes for job '{}'", jobName);
        }
        return cpm;
    }

    /**
     * Retrieve the values of the crawl service attributes and add them to the CrawlProgressMessage being put together.
     *
     * @param cpm the crawlProgress message being prepared
     */
    private void getCrawlServiceAttributes(CrawlProgressMessage cpm, JobResult job) {
    	// TODO check job state??
        CrawlServiceInfo hStatus = cpm.getHeritrixStatus();
        hStatus.setAlertCount(job.job.alertCount); // info taken from job information
        hStatus.setCurrentJob(this.jobName); // Note:Information not taken from H3
    	hStatus.setCrawling(job.job.isRunning);// info taken from job information
    }

    /**
     * Retrieve the values of the crawl service job attributes and add them to the CrawlProgressMessage being put
     * together.
     *
     * @param cpm the crawlProgress message being prepared
     */
    private void fetchCrawlServiceJobAttributes(CrawlProgressMessage cpm, JobResult job) {
    	CrawlServiceJobInfo jStatus = cpm.getJobStatus();
        
    	/* progressStatistics. FIXME relevant in H3 */
        String newProgressStats = "?"; // Fetched from H1 bean: CrawlServiceJobOperation.progressStatistics);
        jStatus.setProgressStatistics(newProgressStats);
        
        /* FIXME relevant in H3?
        if (progressStatisticsLegend == null) {
            progressStatisticsLegend = (String) executeMBeanOperation(CrawlServiceJobOperation.progressStatisticsLegend);
        }*/

        /*    	
        case CrawlTime:
            Long elapsedSeconds = -1L;
            if (value != null) {
                elapsedSeconds = (Long) value;
            }
            jStatus.setElapsedSeconds(elapsedSeconds);
            break;
        */
    	
    	Long value = job.job.elapsedReport.elapsedMilliseconds;
    	Long elapsedSeconds = -1L;
        if (value != null) {
            elapsedSeconds = value / 1000L; 
        }
        jStatus.setElapsedSeconds(elapsedSeconds);
/*        
    case CurrentDocRate:
        Double processedDocsPerSec = new Double(-1L);
        if (value != null) {
            processedDocsPerSec = (Double) value;
        }
        jStatus.setCurrentProcessedDocsPerSec(processedDocsPerSec);
        break;
*/
        Double Dvalue = job.job.rateReport.currentDocsPerSecond;
        Double processedDocsPerSec = new Double(-1L);
        if (Dvalue != null) {
            processedDocsPerSec = Dvalue;
        }
        jStatus.setCurrentProcessedDocsPerSec(processedDocsPerSec);
       
/*        
    case CurrentKbRate:
        // NB Heritrix seems to store the average value in
        // KbRate instead of CurrentKbRate...
        // Inverse of doc rates.
        Long processedKBPerSec = -1L;
        if (value != null) {
            processedKBPerSec = (Long) value;
        }
        jStatus.setProcessedKBPerSec(processedKBPerSec);
*/
        Integer valueI = job.job.rateReport.currentKiBPerSec;
        Integer processedKBPerSec = -1;
        if (valueI != null) {
            processedKBPerSec = valueI;
        }
        jStatus.setProcessedKBPerSec(processedKBPerSec);
/*        
    case DiscoveredCount:
        Long discoveredCount = -1L;
        if (value != null) {
            discoveredCount = (Long) value;
        }
        jStatus.setDiscoveredFilesCount(discoveredCount);
        break;
*/
        Long discoveredCount = -1L; // This value is not found in H3???
        // value = job.job.sizeTotalsReport.totalCount; //FIXME correct???
        jStatus.setDiscoveredFilesCount(discoveredCount);
/*
    case DocRate:
        Double docRate = new Double(-1L);
        if (value != null) {
            docRate = (Double) value;
        }
        jStatus.setProcessedDocsPerSec(docRate);
*/
        Double docRate = new Double(-1L);
        Dvalue = job.job.rateReport.averageDocsPerSecond;
        if (Dvalue != null) {
            docRate = (Double) Dvalue;
        }
        jStatus.setProcessedDocsPerSec(docRate);
/*
    case DownloadedCount:
        Long downloadedCount = -1L;
        if (value != null) {
            downloadedCount = (Long) value;
        }
        jStatus.setDownloadedFilesCount(downloadedCount);
  */
        Long downloadedCount = -1L;
        // FIXME available in H3???
        jStatus.setDownloadedFilesCount(downloadedCount);
/*        
    case FrontierShortReport:
        String frontierShortReport = "?";
        if (value != null) {
            frontierShortReport = (String) value;
        }
        jStatus.setFrontierShortReport(frontierShortReport);
        break;
*/     
        String frontierShortReport = "?"; // Available in H3????
        jStatus.setFrontierShortReport(frontierShortReport);
/*    case KbRate:
        // NB Heritrix seems to store the average value in
        // KbRate instead of CurrentKbRate...
        // Inverse of doc rates.
        Long kbRate = -1L;
        if (value != null) {
            kbRate = (Long) value;
        }
        jStatus.setCurrentProcessedKBPerSec(kbRate);
*/     
    
        Long kbRate = -1L;
        valueI = job.job.rateReport.averageKiBPerSec;
        if (valueI != null) {
        	kbRate = (Long) value;
        }
        jStatus.setCurrentProcessedKBPerSec(kbRate);
 
        /*
        case Status:
            String newStatus = "?";
            if (value != null) {
                newStatus = (String) value;
            }
            jStatus.setStatus(newStatus);
            
            if (value != null) {
                String status = (String) value;
                if (CrawlController.PAUSING.equals(status)) {
                    cpm.setStatus(CrawlStatus.CRAWLER_PAUSING);
                } else if (CrawlController.PAUSED.equals(status)) {
                    cpm.setStatus(CrawlStatus.CRAWLER_PAUSED);
                } else {
                    cpm.setStatus(CrawlStatus.CRAWLER_ACTIVE);
                }
            }
         */
        
        String newStatus = "?";
        String StringValue = job.job.crawlControllerState;
        if (StringValue != null) {
            newStatus = (String) StringValue;
        }
        jStatus.setStatus(newStatus);
        String status = (String) StringValue;
        if (status.contains("PAUSE")) { // FIXME this is not correct
            cpm.setStatus(CrawlStatus.CRAWLER_PAUSED);
        } else {
            cpm.setStatus(CrawlStatus.CRAWLER_ACTIVE);
        }
/*        
    case ThreadCount:
        Integer currentActiveToecount = -1;
        if (value != null) {
            currentActiveToecount = (Integer) value;
        }
        jStatus.setActiveToeCount(currentActiveToecount);
        break;
*/    
        valueI = job.job.threadReport.toeCount;
        Integer currentActiveToecount = -1;
        if (valueI != null) {
            currentActiveToecount = (Integer) valueI;
        }
        jStatus.setActiveToeCount(currentActiveToecount);
    }

    /**
     * Generates a full frontier report.
     *
     * @return a Full frontier report.
     */
    public FullFrontierReport getFullFrontierReport() {
    	//FIXME get frontier report from H3 using an appropriate REST call.
    	// Is the following OK: No!!!
    	//https://localhost:8444/engine/job/testjob/jobdir/20150210135411/reports/frontier-summary-report.txt
    	
    	return null;
    	/*		
        return FullFrontierReport.parseContentsAsString(
                jobName,
                (String) executeOperationNoRetry(crawlServiceJobBeanName,
                        CrawlServiceJobOperation.frontierReport.name(), "all"));
                        
        */                
    }

    /**
     * Periodically scans the crawl dir to see if Heritrix has finished generating the crawl reports. The time to wait
     * is bounded by {@link HarvesterSettings#WAIT_FOR_REPORT_GENERATION_TIMEOUT}.
     * Currently not used
     * 
     * @param crawlDir the crawl directory to scan.
     */
    @Deprecated
    private void waitForReportGeneration(File crawlDir) {
        log.info("Started waiting for Heritrix report generation.");

        long currentTime = System.currentTimeMillis();
        long waitSeconds = Settings.getLong(Heritrix3Settings.WAIT_FOR_REPORT_GENERATION_TIMEOUT);
        long waitDeadline = currentTime + TimeUtils.SECOND_IN_MILLIS * waitSeconds;

       
        // While the deadline is not attained, periodically perform the
        // following checks:
        // 1) Verify that the crawl job MBean still exists. If not then
        // the job is over, no need to wait more and exit the loop.
        // 2) Read the job(s status. Since Heritrix 1.14.4, a FINISHED status
        // guarantees that all reports have been generated. In this case
        // exit the loop.
        while (currentTime <= waitDeadline) {
            currentTime = System.currentTimeMillis();
            
            //FIXME see if job is finished, if so, reports can be considered to ready as well?????
            try {
                // Wait 20 seconds
                Thread.sleep(20 * TimeUtils.SECOND_IN_MILLIS);
            } catch (InterruptedException e) {
                log.trace("Received InterruptedException", e);
            }
        }
        log.info("Waited {} for report generation. Will proceed with cleanup.", StringUtils.formatDuration(waitSeconds));
    }
    
    @Override
    public boolean atFinish() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void beginCrawlStop() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void cleanup() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean crawlIsEnded() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int getActiveToeCount() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int getCurrentProcessedKBPerSec() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getHarvestInformation() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public String getProgressStats() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public long getQueuedUriCount() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean isPaused() {
        throw new NotImplementedException("Not implemented");
    }

}
