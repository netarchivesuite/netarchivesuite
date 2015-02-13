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
package dk.netarkivet.harvester.harvesting.controller;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpRequestBase;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.Heritrix3Files;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport;

/**
 * This implementation of the HeritrixController interface starts Heritrix as a separate process and uses JMX to
 * communicate with it. Each instance executes exactly one process that runs exactly one crawl job.
 */
public class BnfHeritrixController extends AbstractRestHeritrixController {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(BnfHeritrixController.class);

    /**
     * The below commands and attributes are copied from the attributes and operations exhibited by the Heritrix MBeans
     * of type CrawlJob and CrawlService.Job, as they appear in JConsole.
     * <p>
     * Only operations and attributes used in NAS are listed.
     */
    private static enum CrawlServiceAttribute {
        /** The number of alerts raised by Heritrix. */
        AlertCount,
        /** True if Heritrix is currently crawling, false otherwise. */
        IsCrawling,
        /** The ID of the job being currently crawled by Heritrix. */
        CurrentJob;

        /**
         * Returns the {@link CrawlServiceAttribute} enum value matching the given name. Throws {@link UnknownID} if no
         * match is found.
         *
         * @param name the attribute name
         * @return the corresponding {@link CrawlServiceAttribute} enum value.
         */
        public static CrawlServiceAttribute fromString(String name) {
            for (CrawlServiceAttribute att : values()) {
                if (att.name().equals(name)) {
                    return att;
                }
            }
            throw new UnknownID(name + " : unknown CrawlServiceAttribute !");
        }
    }

    /**
     * Enum listing the different job attributes available.
     */
    private static enum CrawlServiceJobAttribute {
        /** The time in seconds elapsed since the crawl began. */
        CrawlTime,
        /** The current download rate in URI/s. */
        CurrentDocRate,
        /** The current download rate in kB/s. */
        CurrentKbRate,
        /** The number of URIs discovered by Heritrix. */
        DiscoveredCount,
        /** The average download rate in URI/s. */
        DocRate,
        /** The number of URIs downloaded by Heritrix. */
        DownloadedCount,
        /** A string summarizing the Heritrix frontier. */
        FrontierShortReport,
        /** The average download rate in kB/s. */
        KbRate,
        /** The job status (Heritrix status). */
        Status,
        /** The number of active toe threads. */
        ThreadCount;

        /**
         * Returns the {@link CrawlServiceJobAttribute} enum value matching the given name. Throws {@link UnknownID} if
         * no match is found.
         *
         * @param name the attribute name
         * @return the corresponding {@link CrawlServiceJobAttribute} enum value.
         */
        public static CrawlServiceJobAttribute fromString(String name) {
            for (CrawlServiceJobAttribute att : values()) {
                if (att.name().equals(name)) {
                    return att;
                }
            }
            throw new UnknownID(name + " : unknown CrawlServiceJobAttribute !");
        }
    }

    /**
     * Enum class defining the general operations available to the Heritrix operator.
     */
    private static enum CrawlServiceOperation {
        /** Adds a new job to an Heritrix instance. */
        addJob,
        /** Fetches the identifiers of pending jobs. */
        pendingJobs,
        /** Fetches the identifiers of completed jobs. */
        completedJobs,
        /** Shuts down an Heritrix instance. */
        shutdown,
        /** Instructs an Heritrix instance to starts crawling jobs. */
        startCrawling,
        /** Instructs an Heritrix instance to terminate the current job. */
        terminateCurrentJob;
    }

    /**
     * Enum class defining the Job-operations available to the Heritrix operator.
     */
    private static enum CrawlServiceJobOperation {
        /** Fetches the progress statistics string from an Heritrix instance. */
        progressStatistics,
        /**
         * Fetches the progress statistics legend string from an Heritrix instance.
         */
        progressStatisticsLegend,
        /** Fetches the frontier report. */
        frontierReport;
    }

    /**
     * Shall we abort, if we lose the connection to Heritrix.
     */
    private static final boolean ABORT_IF_CONN_LOST = Settings.getBoolean(HarvesterSettings.ABORT_IF_CONNECTION_LOST);

   
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
    public BnfHeritrixController(Heritrix3Files files) {
        super(files);
    }

    /**
     * Initialize the JMXconnection to the Heritrix.
     *
     * @throws IOFailure If Heritrix dies before initialization, or we encounter any problems during the initialization.
     * @see HeritrixController#initialize()
     */
    @Override
    public void initialize() {
    	/*
        if (processHasExited()) {
            String errMsg = "Heritrix process of " + this + " died before initialization";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        } 
        */

        //FIXME establish initial connection to H3 using REST
        
        log.info("Abort, if we lose the connection to Heritrix, is {}", ABORT_IF_CONN_LOST);
        
        // TODO define a new H3 job with the given CXML file and seeds.txt
        // After this, H3 process knows about a job called 'jobName' 
        String jobname = "job-" + Long.toString(System.currentTimeMillis());
        try {
			h3wrapper.createNewJob(jobname);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      
        
        
        
        
        
        
        
        
        
 
        //initJMXConnection();

        //log.info("JMX connection initialized successfully");

        /*
        crawlServiceBeanName = "org.archive.crawler:" + JmxUtils.NAME + "=Heritrix," + JmxUtils.TYPE + "=CrawlService,"
                + JmxUtils.JMX_PORT + "=" + getJmxPort() + "," + JmxUtils.GUI_PORT + "=" + getGuiPort() + ","
                + JmxUtils.HOST + "=" + getHostName();

        // We want to be sure there are no jobs when starting, in case we got
        // an old Heritrix or somebody added jobs behind our back.
        TabularData doneJobs = (TabularData) executeMBeanOperation(CrawlServiceOperation.completedJobs);
        TabularData pendingJobs = (TabularData) executeMBeanOperation(CrawlServiceOperation.pendingJobs);
        if (doneJobs != null && doneJobs.size() > 0 || pendingJobs != null && pendingJobs.size() > 0) {
            throw new IllegalState("This Heritrix instance is in a illegalState! "
                    + "This instance has either old done jobs (" + doneJobs + "), or old pending jobs (" + pendingJobs
                    + ").");
        }
        
        // From here on, we can assume there's only the one job we make.
        // We'll use the arc file prefix to name the job, since the prefix
        // already contains the harvest id and job id.
        HeritrixFiles files = getHeritrixFiles();
        executeMBeanOperation(CrawlServiceOperation.addJob, files.getOrderXmlFile().getAbsolutePath(),
                files.getArchiveFilePrefix(), getJobDescription(), files.getSeedsTxtFile().getAbsolutePath());
        jobName = getJobName();

        crawlServiceJobBeanName = "org.archive.crawler:" + JmxUtils.NAME + "=" + jobName + "," + JmxUtils.TYPE
                + "=CrawlService.Job," + JmxUtils.JMX_PORT + "=" + getJmxPort() + "," + JmxUtils.MOTHER + "=Heritrix,"
                + JmxUtils.HOST + "=" + getHostName();
                
        */
    }

    @Override
    public void requestCrawlStart() {
    	
    	
    	
    	
    	
    	
    	
    	//FIXME implement me
    	// Build, launch, and start the job
    	
    	//executeMBeanOperation(CrawlServiceOperation.startCrawling);
    }

    @Override
    public void requestCrawlStop(String reason) {
    	//FIXME implement me
        //executeMBeanOperation(CrawlServiceOperation.terminateCurrentJob);
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
     * @see HeritrixController#cleanup()
     */
    public void cleanup(File crawlDir) {
        // Before cleaning up, we need to wait for the reports to be generated
        waitForReportGeneration(crawlDir);

        // FIXME shutdown down the heritrix process.
        //Object engineResult = h3wrapper.exitJavaProcess(null);
        /*
        try {
            executeMBeanOperation(CrawlServiceOperation.shutdown);
        } catch (IOFailure e) {
            log.error("JMX error while cleaning up Heritrix controller", e);
        }*/

        //closeJMXConnection();

        waitForHeritrixProcessExit();
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

        getCrawlServiceAttributes(cpm);

        if (cpm.crawlIsFinished()) {
            cpm.setStatus(CrawlStatus.CRAWLING_FINISHED);
            // No need to go further, CrawlService.Job bean does not exist
            return cpm;
        }

        fetchCrawlServiceJobAttributes(cpm);

        return cpm;
    }

    /**
     * Retrieve the values of the crawl service attributes and add them to the CrawlProgressMessage being put together.
     *
     * @param cpm the crawlProgress message being prepared
     */
    private void getCrawlServiceAttributes(CrawlProgressMessage cpm) {

    	/* List<Attribute> heritrixAtts = getMBeanAttributes(new CrawlServiceAttribute[] {
                CrawlServiceAttribute.AlertCount, CrawlServiceAttribute.IsCrawling, CrawlServiceAttribute.CurrentJob});
		*/
    	
        CrawlServiceInfo hStatus = cpm.getHeritrixStatus();
        // FIXME from h3 find out the currentJob, AlertCount, IsCrawling attribute
        // and update hStatus.setAlertCount(alertCount);
        // and update hStatus.setCurrentJob(newCurrentJob);
        // and update hStatus.setCrawling(newCrawling);
        
        /*
        for (Attribute att : heritrixAtts) {
            Object value = att.getValue();
            CrawlServiceAttribute crawlServiceAttribute = CrawlServiceAttribute.fromString(att.getName());
            switch (crawlServiceAttribute) {
            case AlertCount:
                Integer alertCount = -1;
                if (value != null) {
                    alertCount = (Integer) value;
                }
                hStatus.setAlertCount(alertCount);
                break;
            case CurrentJob:
                String newCurrentJob = "";
                if (value != null) {
                    newCurrentJob = (String) value;
                }
                hStatus.setCurrentJob(newCurrentJob);
                break;
            case IsCrawling:
                Boolean newCrawling = false;
                if (value != null) {
                    newCrawling = (Boolean) value;
                }
                hStatus.setCrawling(newCrawling);
                break;
            default:
                log.debug("Unhandled attribute: {}", crawlServiceAttribute);
            }
        }
        */
    }

    /**
     * Retrieve the values of the crawl service job attributes and add them to the CrawlProgressMessage being put
     * together.
     *
     * @param cpm the crawlProgress message being prepared
     */
    private void fetchCrawlServiceJobAttributes(CrawlProgressMessage cpm) {
    	//FIXME add Heritrix3 information to the CrawlProgressMessage
        // H1 attribute CrawlTime =>    elapsedSeconds
        // H1 attribute CurrentDocRate => processedDocsPerSec
        // H1 CurrentKbRate => processedKBPerSec 	
   /*     
    H1 DiscoveredCount => DiscoveredFilesCount
    H1 DocRate => ProcessedDocsPerSec
    H1 DownloadedCount => DownloadedFilesCount
    H1 FrontierShortReport => FrontierShortReport
    H1 KbRate => CurrentProcessedKBPerSec
    H1 Status => one of CrawlStatus.CRAWLER_PAUSING, CrawlStatus.CRAWLER_PAUSED, CrawlStatus.CRAWLER_ACTIVE);
   */ 
    	
    	/*
        String progressStats = (String) executeMBeanOperation(CrawlServiceJobOperation.progressStatistics);
        CrawlServiceJobInfo jStatus = cpm.getJobStatus();
        String newProgressStats = "?";
        if (progressStats != null) {
            newProgressStats = progressStats;
        }
        jStatus.setProgressStatistics(newProgressStats);

        if (progressStatisticsLegend == null) {
            progressStatisticsLegend = (String) executeMBeanOperation(CrawlServiceJobOperation.progressStatisticsLegend);
        }

        List<Attribute> jobAtts = getMBeanAttributes(CrawlServiceJobAttribute.values());

        for (Attribute att : jobAtts) {
            Object value = att.getValue();
            CrawlServiceJobAttribute aCrawlServiceJobAttribute = CrawlServiceJobAttribute.fromString(att.getName());
            switch (aCrawlServiceJobAttribute) {
            case CrawlTime:
                Long elapsedSeconds = -1L;
                if (value != null) {
                    elapsedSeconds = (Long) value;
                }
                jStatus.setElapsedSeconds(elapsedSeconds);
                break;
            case CurrentDocRate:
                Double processedDocsPerSec = new Double(-1L);
                if (value != null) {
                    processedDocsPerSec = (Double) value;
                }
                jStatus.setCurrentProcessedDocsPerSec(processedDocsPerSec);
                break;
            case CurrentKbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                Long processedKBPerSec = -1L;
                if (value != null) {
                    processedKBPerSec = (Long) value;
                }
                jStatus.setProcessedKBPerSec(processedKBPerSec);
                break;
            case DiscoveredCount:
                Long discoveredCount = -1L;
                if (value != null) {
                    discoveredCount = (Long) value;
                }
                jStatus.setDiscoveredFilesCount(discoveredCount);
                break;
            case DocRate:
                Double docRate = new Double(-1L);
                if (value != null) {
                    docRate = (Double) value;
                }
                jStatus.setProcessedDocsPerSec(docRate);
                break;
            case DownloadedCount:
                Long downloadedCount = -1L;
                if (value != null) {
                    downloadedCount = (Long) value;
                }
                jStatus.setDownloadedFilesCount(downloadedCount);
                break;
            case FrontierShortReport:
                String frontierShortReport = "?";
                if (value != null) {
                    frontierShortReport = (String) value;
                }
                jStatus.setFrontierShortReport(frontierShortReport);
                break;
            case KbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                Long kbRate = -1L;
                if (value != null) {
                    kbRate = (Long) value;
                }
                jStatus.setCurrentProcessedKBPerSec(kbRate);
                break;
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
                break;
            case ThreadCount:
                Integer currentActiveToecount = -1;
                if (value != null) {
                    currentActiveToecount = (Integer) value;
                }
                jStatus.setActiveToeCount(currentActiveToecount);
                break;
            default:
                log.debug("Unhandled attribute: {}", aCrawlServiceJobAttribute);
            }
        }
        */
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
     *
     * @param crawlDir the crawl directory to scan.
     */
    private void waitForReportGeneration(File crawlDir) {
        log.info("Started waiting for Heritrix report generation.");

        long currentTime = System.currentTimeMillis();
        long waitSeconds = Settings.getLong(HarvesterSettings.WAIT_FOR_REPORT_GENERATION_TIMEOUT);
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

            boolean crawlServiceJobExists = false;
            //FIXME see if job is finished, if so, reports can be considered to ready as well?????
    /*        
            try {
                if (crawlServiceJobBeanName != null) {
                    crawlServiceJobExists = getMBeanServerConnection().isRegistered(
                            JMXUtils.getBeanName(crawlServiceJobBeanName));
                } else {
                    // An error occurred when initializing the controller
                    // Simply log a warning for the record
                    log.warn("crawlServiceJobBeanName is null, earlier initialization of controller did not complete.");
                }
            } catch (IOException e) {
                log.warn("IOException", e);
                continue;
            }

            if (!crawlServiceJobExists) {
                log.info("{} MBean not found, report generation is finished. Exiting wait loop.",
                        crawlServiceJobBeanName);
                break;
            }

            String status = "";
            try {
                List<Attribute> atts = getAttributesNoRetry(crawlServiceJobBeanName,
                        new String[] {CrawlServiceJobAttribute.Status.name()});
                status = (String) atts.get(0).getValue();
            } catch (IOFailure e) {
                log.warn("IOFailure", e);
                continue;
            } catch (IndexOutOfBoundsException e) {
                // sometimes the array is empty TODO find out why
                log.warn("IndexOutOfBoundsException", e);
                continue;
            }

            if (CrawlController.FINISHED.equals(status)) {
                log.info("{} status is FINISHED, report generation is complete. Exiting wait loop.",
                        crawlServiceJobBeanName);
                return;
            }
*/
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
