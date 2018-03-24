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
package dk.netarkivet.harvester.heritrix3.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.netarchivesuite.heritrix3wrapper.EngineResult;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper.CrawlControllerState;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.ResultStatus;
import org.netarchivesuite.heritrix3wrapper.ScriptResult;
import org.netarchivesuite.heritrix3wrapper.jaxb.JobShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.HeritrixLaunchException;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;

/**
 * This implementation of the HeritrixController interface starts Heritrix3 as a separate process and uses JMX to
 * communicate with it. Each instance executes exactly one process that runs exactly one crawl job.
 */
public class HeritrixController extends AbstractRestHeritrixController {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(HeritrixController.class);

    /**
     * The name that Heritrix3 gives to the job we ask it to create. 
     */
    private String jobName;

    /** The header line (legend) for the statistics report. */
    private String progressStatisticsLegend;

    private int heritrix3EngineRetries;
    private int heritrix3EngineIntervalBetweenRetriesInMillis;
    
    private String baseUrl;
    
    /**
     * Create a BnfHeritrixController object.
     *
     * @param files Files that are used to set up Heritrix3.
     */
    public HeritrixController(Heritrix3Files files, String jobName) {
        super(files);
        this.jobName = jobName;
    }

    /**
     * Initialize the JMXconnection to the Heritrix3.
     *
     * @throws IOFailure If Heritrix3 dies before initialisation, or we encounter any problems during the initialisation.
     * @see IHeritrixController#initialize()
     */
    @Override
    public void initialize() {
        
    	/////////////////////////////////////////////////////
        // Initialize H3 wrapper 
    	/////////////////////////////////////////////////////

        //TODO these numbers could be settings
        this.heritrix3EngineRetries = 60;
        this.heritrix3EngineIntervalBetweenRetriesInMillis = 1000; // 1 second
      
        
        h3wrapper = Heritrix3Wrapper.getInstance(getHostName(), getGuiPort(), 
        		null, null, getHeritrixAdminName(), getHeritrixAdminPassword());

        EngineResult engineResult;
        try {
        	engineResult = h3wrapper.waitForEngineReady(heritrix3EngineRetries, heritrix3EngineIntervalBetweenRetriesInMillis);
        } catch (Throwable e){
        	e.printStackTrace();
        	throw new IOFailure("Heritrix3 engine not started: " + e);
        }

        if (engineResult != null) {
        	if (engineResult.status != ResultStatus.OK) {
        	    String errMsg = "Heritrix3 wrapper could not connect to Heritrix3. Resultstate = " + engineResult.status;
            	log.error(errMsg, engineResult.t);
            	throw new IOFailure(errMsg, engineResult.t);
        	}
        } else {
        	throw new IOFailure("Unexpected error: Heritrix3 wrapper returned null engine result.");
        }
        
        baseUrl = "https://" + getHostName() + ":" + Integer.toString(getGuiPort()) + "/engine/";

        // POST: Heritrix3 is up and running and responds nicely
        log.info("Heritrix3 REST interface up and running");
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

  		// PRE: h3 is running, and the job files copied to their final location
  		EngineResult engineResult = null;
  		try {
      		engineResult = h3wrapper.rescanJobDirectory();
      		log.info("H3 jobs available for building: {}", knownJobsToString(engineResult));
      		
      		log.trace("Result of rescanJobDirectory() operation: " + new String(engineResult.response, "UTF-8"));
      		
      		jobResult = h3wrapper.buildJobConfiguration(jobName);
      		log.trace("Result of buildJobConfiguration() operation: " + new String(jobResult.response, "UTF-8"));
      		if (jobResult.status == ResultStatus.OK) {
      		  if (jobResult.job.statusDescription.equalsIgnoreCase("Unbuilt")) {
      		      throw new HeritrixLaunchException("The job '" + jobName + "' could not be built. Last loglines are " + StringUtils.join(jobResult.job.jobLogTail, "\n"));
      		  } else if (jobResult.job.statusDescription.equalsIgnoreCase("Ready")) {
      		      log.info("Job {} built successfully", jobName);
      		  } else if (jobResult.job.statusDescription.startsWith("Finished")) { // Created but not launchable
      		      log.warn("The job {} seems unlaunchable. Tearing down the job. Last loglines are ", jobName, 
                          StringUtils.join(jobResult.job.jobLogTail, "\n"));
      		      jobResult = h3wrapper.teardownJob(jobName);
      		      log.trace("Result of teardown() operation: " + new String(jobResult.response, "UTF-8"));
      		      throw new HeritrixLaunchException("Job '" + jobName + "' failed to launch: " + StringUtils.join(jobResult.job.jobLogTail, "\n"));
      		  } else {
      		      throw new IllegalState("Unknown job.statusdescription returned from h3: " + jobResult.job.statusDescription);
      		  }
      		} else {
      		    throw new IllegalState("Unknown ResultStatus returned from h3wrapper: " 
      		            + ResultStatus.toString(jobResult.status));   
      		}
      		
      		jobResult = h3wrapper.waitForJobState(jobName, CrawlControllerState.NASCENT, 60, 1000);
      		if (jobResult.job.crawlControllerState.equalsIgnoreCase(CrawlControllerState.NASCENT.toString())) {
      		  log.info("The H3 job {} in now in state CrawlControllerState.NASCENT",  jobName);
      		} else {
      		  log.warn("The job state is now {}. Should have been CrawlControllerState.NASCENT",  jobResult.job.crawlControllerState);
      		}
      		jobResult = h3wrapper.launchJob(jobName);
      		
      		log.trace("Result of launchJob() operation: " + new String(jobResult.response, "UTF-8"));
      		jobResult = h3wrapper.waitForJobState(jobName, CrawlControllerState.PAUSED, 60, 1000);
      		if (jobResult.job.crawlControllerState.equalsIgnoreCase(CrawlControllerState.PAUSED.toString())) {
      		    log.info("The H3 job {} in now in state CrawlControllerState.PAUSED",  jobName);
      		} else {
      		    log.warn("The job state is now {}. Should have been CrawlControllerState.PAUSED",  jobResult.job.crawlControllerState);
      		}
      		
      		//check if param pauseAtStart is true
      		ScriptResult scriptResult = h3wrapper.ExecuteShellScriptInJob(jobName, "groovy", "rawOut.println crawlController.pauseAtStart\n");
      		boolean pauseAtStart = false;
      		if (scriptResult != null && scriptResult.script != null) {
      			String rawOutput = scriptResult.script.rawOutput; //false\n or true\n
      			if(rawOutput.endsWith("\n") || rawOutput.endsWith("\r")) {
      				rawOutput = rawOutput.substring(0, rawOutput.length()-1);
      			}
      			pauseAtStart = Boolean.parseBoolean(rawOutput);
      		}
      		log.info("The parameter pauseAtStart is {}", pauseAtStart);
      		//if param pauseAtStart is false
      		if(pauseAtStart == false) {
      			jobResult = h3wrapper.unpauseJob(jobName);
	      		log.info("The job {} is now in state {}", jobName, jobResult.job.crawlControllerState);
	      		
	      		// POST: h3 is running, and the job with name 'jobName' is running
	            log.trace("h3-State after unpausing job '{}': {}", jobName, new String(jobResult.response, "UTF-8"));
      		} else {
      			log.info("The job {} is now in state {}", jobName, jobResult.job.crawlControllerState);
      		}
            
            
      	} catch (UnsupportedEncodingException e) {
      	    throw new IOFailure("Unexpected error during communication with heritrix3", e);
        }
    }

    @Override
    public void requestCrawlStop(String reason) {
    	log.info("Terminating job {}. Reason: {}", this.jobName, reason);
    	JobResult jobResult = h3wrapper.job(jobName);
    	if (jobResult != null) {
    		if (jobResult.job.isRunning) {
    			JobResult result = h3wrapper.terminateJob(this.jobName);
    			if (!result.job.isRunning) {
    				log.warn("Job '{}' terminated", this.jobName);
    			} else {
                    log.warn("Job '{}' not terminated correctly", this.jobName);
                }
    		} else {
    			log.warn("Job '{}' not terminated, as it was not running", this.jobName);
    		}
    	} else {
    		log.warn("Job '{}' has maybe already been terminated and/or heritrix3 is no longer running", this.jobName); 
    	}
    }

    @Override
    public void stopHeritrix() {
        log.debug("Stopping Heritrix3");
        try {
            // Check if a heritrix3 process still exists for this jobName
            ProcessBuilder processBuilder = new ProcessBuilder("pgrep", "-f", jobName);
            log.info("Looking up heritrix3 process with. " + processBuilder.command());
            if (processBuilder.start().waitFor() == 0) { // Yes, ask heritrix3 to shutdown, ignoring any jobs named jobName
                log.info("Heritrix running, requesting heritrix to stop and ignoring running job '{}'", jobName);
                h3wrapper.exitJavaProcess(Arrays.asList(new String[] {jobName}));
            } else { 
                log.info("Heritrix3 process not running for job '{}'", jobName);
            }
            // Check again
            if (processBuilder.start().waitFor() == 0) { // The process is still alive, kill it
                log.info("Heritrix3 process still running, pkill'ing heritrix3 ");
                ProcessBuilder killerProcessBuilder = new ProcessBuilder("pkill", "-f", jobName);
                int pkillExitValue = killerProcessBuilder.start().exitValue();
                if (pkillExitValue != 0) {
                    log.warn("Non xero exit value ({}) when trying to pkill Heritrix3.", pkillExitValue);
                } else {
                    log.info("Heritrix process terminated successfully with the pkill command {}", killerProcessBuilder.command());
                }
            } else {
                log.info("Heritrix3 stopped successfully.");
            }
        } catch (IOException e) {
            log.warn("Exception while trying to shutdown heritrix", e);
        } catch (InterruptedException e) {
            log.debug("stopHeritrix call interupted", e);
        }
    }

    /**
     * Return the URL for monitoring this instance.
     *
     * @return the URL for monitoring this instance.
     */
    public String getHeritrixConsoleURL() {
        return "https://" + SystemUtils.getLocalHostName() + ":" + getGuiPort() + "/engine/job/";
    }
    
    /**
     * Return the URL for monitoring the job of this instance.
     *
     * @return the URL for monitoring the job of this instance.
     */
    public String getHeritrixJobConsoleURL() {
        return getHeritrixConsoleURL() + files.getCrawlDir().getName();
    }

    /**
     * Cleanup after an Heritrix3 process. This entails sending the shutdown command to the Heritrix3 process, and killing
     * it forcefully, if it is still alive after waiting the period of time specified by the
     * CommonSettings.PROCESS_TIMEOUT setting.
     *
     * @param crawlDir the crawldir to cleanup (argument is currently not used) 
     * @see IHeritrixController#cleanup()
     */
    public void cleanup(File crawlDir) {
        JobResult jobResult;
        try {
            // Check engine status
            EngineResult engineResult = h3wrapper.rescanJobDirectory();
            if (engineResult != null){
                List<JobShort> knownJobs = engineResult.engine.jobs;
                if (knownJobs.size() != 1) {
                    log.warn("Should be one job but there is {} jobs: {}", knownJobs.size(), knownJobsToString(engineResult));
                }
            } else {
                log.warn("Unresponsive Heritrix3 engine. Let's try continuing the cleanup anyway"); 
            }
            
            // Check that job jobName still exists in H3 engine
            jobResult = h3wrapper.job(jobName);
            if (jobResult != null) {
                if (jobResult.status == ResultStatus.OK && jobResult.job.crawlControllerState != null) {
                    String TEARDOWN = "teardown";
                    if (jobResult.job.availableActions.contains(TEARDOWN)) {
                        log.info("Tearing down h3 job {}" , jobName);  
                        jobResult = h3wrapper.teardownJob(jobName);
                    } else {
                        String errMsg = "Tearing down h3 job '" + jobName + "' not possible. Not one of the actions available: " + StringUtils.join(jobResult.job.availableActions, ",");   
                        log.warn(errMsg);
                        throw new IOFailure(errMsg);
                    }
                }
            } else {
                throw new IOFailure("Unexpected error during communication with heritrix3 during cleanup");
            }
            // Wait for the state: jobResult.job.crawlControllerState == null (but we only try ten times with 1 second interval 
            jobResult = h3wrapper.waitForJobState(jobName, null, 10, heritrix3EngineIntervalBetweenRetriesInMillis);
            // Did we get the expected state?
            if (jobResult.job.crawlControllerState != null) {
                log.warn("The job {} is still lurking about. Shutdown heritrix3 and ignore the job", jobName);
                List<String> jobsToIgnore = new ArrayList<String>(); 
                jobsToIgnore.add(jobName);
                EngineResult result = h3wrapper.exitJavaProcess(jobsToIgnore);
                if (result == null || (result.status != ResultStatus.RESPONSE_EXCEPTION && result.status != ResultStatus.OFFLINE)) {
                    throw new IOFailure("Heritrix3 could not be shut down");
                }
            } else {
                EngineResult result = h3wrapper.exitJavaProcess(null);
                if (result == null || (result.status != ResultStatus.RESPONSE_EXCEPTION && result.status != ResultStatus.OFFLINE)) {
                    throw new IOFailure("Heritrix3 could not be shut down");
                }
            }
        } catch (Throwable e) {
            throw new IOFailure("Unknown error during communication with heritrix3", e);
        }
    }


    private String knownJobsToString(EngineResult engineResult) {
        String result = "";
        if (engineResult == null || engineResult.engine == null || engineResult.engine.jobs == null) {
            result = null;
        } else {
            List<JobShort> knownjobs = engineResult.engine.jobs;
            for (JobShort js: knownjobs) {
                result += js.shortName + " ";
            }
        }

        return result;
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
        cpm.setHostUrl(getHeritrixJobConsoleURL());
        JobResult jobResult = h3wrapper.job(jobName);
        if (jobResult != null) {
        	getCrawlServiceAttributes(cpm, jobResult);
        } else {
        	log.warn("Unable to get Heritrix3 status for job '{}'", jobName);
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

/*
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
2015-04-29T12:42:54Z         774         573          185        0.9(2.31)     49(41)            16             2        61249        270848            1         456         114
*/
        /*
        jStatus.setProgressStatistics(newProgressStats);
        if (progressStatisticsLegend == null) {
            progressStatisticsLegend = (String) executeMBeanOperation(CrawlServiceJobOperation.progressStatisticsLegend);
        }
        */

    	long totalUriCount = job.job.uriTotalsReport.totalUriCount;
        long downloadedUriCount = job.job.uriTotalsReport.downloadedUriCount;
        Double progress;
        if (totalUriCount == 0) {
        	progress = 0.0;
        } else {
            progress = downloadedUriCount * 100.0 / totalUriCount;
        }
        jStatus.setProgressStatistics(progress + "%");

    	Long elapsedSeconds = job.job.elapsedReport.elapsedMilliseconds;
        if (elapsedSeconds == null) {
        	elapsedSeconds = -1L;
        } else {
            elapsedSeconds = elapsedSeconds / 1000L; 
        }
        jStatus.setElapsedSeconds(elapsedSeconds);

        Double currentProcessedDocsPerSec = job.job.rateReport.currentDocsPerSecond;
        if (currentProcessedDocsPerSec == null) {
            currentProcessedDocsPerSec = new Double(-1L);
        }
        jStatus.setCurrentProcessedDocsPerSec(currentProcessedDocsPerSec);

        Double processedDocsPerSec = job.job.rateReport.averageDocsPerSecond;
        if (processedDocsPerSec == null) {
            processedDocsPerSec = new Double(-1L);
        }
        jStatus.setProcessedDocsPerSec(processedDocsPerSec);

        Integer kbRate = job.job.rateReport.currentKiBPerSec;
        if (kbRate == null) {
        	kbRate = -1;
        }
        jStatus.setCurrentProcessedKBPerSec(kbRate);

        Integer processedKBPerSec = job.job.rateReport.averageKiBPerSec;
        if (processedKBPerSec == null) {
            processedKBPerSec = -1;
        }
        jStatus.setProcessedKBPerSec(processedKBPerSec);

        Long discoveredFilesCount = job.job.uriTotalsReport.totalUriCount;
        if (discoveredFilesCount == null) {
        	discoveredFilesCount = -1L;
        }
        jStatus.setDiscoveredFilesCount(discoveredFilesCount);

        Long downloadedCount = job.job.uriTotalsReport.downloadedUriCount;
        if (downloadedCount == null) {
        	downloadedCount = -1L;
        }
        jStatus.setDownloadedFilesCount(downloadedCount);
/*
27 queues: 5 active (1 in-process; 0 ready; 4 snoozed); 0 inactive; 0 retired; 22 exhausted
*/
        String frontierShortReport = String.format("%d queues: %d active (%d in-process; %d ready; %d snoozed); %d inactive; %d retired; %d exhausted",
        		job.job.frontierReport.totalQueues,
        		job.job.frontierReport.activeQueues,
        		job.job.frontierReport.inProcessQueues,
        		job.job.frontierReport.readyQueues,
        		job.job.frontierReport.snoozedQueues,
        		job.job.frontierReport.inactiveQueues,
        		job.job.frontierReport.retiredQueues,
        		job.job.frontierReport.exhaustedQueues);
        jStatus.setFrontierShortReport(frontierShortReport);

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

        Integer currentActiveToecount = job.job.loadReport.busyThreads;
        if (currentActiveToecount == null) {
            currentActiveToecount = -1;
        }
        jStatus.setActiveToeCount(currentActiveToecount);
    }

    /**
     * Generates a full frontier report from H3 using an REST call (Groovy script)
     *
     * @return a Full frontier report.
     */
    public FullFrontierReport getFullFrontierReport() {
    	//construct script request to send
    	HttpPost postRequest = new HttpPost(baseUrl + "job/" + jobName + "/script");
        StringEntity postEntity = null;
		try {
			postEntity = new StringEntity("engine=beanshell&script="+dk.netarkivet.harvester.heritrix3.Constants.FRONTIER_REPORT_GROOVY_SCRIPT);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        postEntity.setContentType("application/x-www-form-urlencoded");
        postRequest.addHeader("Accept", "application/xml");
        postRequest.setEntity(postEntity);
    	ScriptResult result = h3wrapper.scriptResult(postRequest);
        return FullFrontierReport.parseContentsAsXML(
                jobName, result.response, dk.netarkivet.harvester.heritrix3.Constants.XML_RAWOUT_TAG);
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
