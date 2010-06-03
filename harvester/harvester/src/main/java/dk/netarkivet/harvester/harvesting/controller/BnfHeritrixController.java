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
package dk.netarkivet.harvester.harvesting.controller;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.util.JmxUtils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.JMXUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.MetadataFile;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlServiceJobInfo;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;

/**
 * This implementation of the HeritrixController interface starts Heritrix as a
 * separate process and uses JMX to communicate with it. Each instance executes
 * exactly one process that runs exactly one crawl job.
 */
public class BnfHeritrixController extends AbstractJMXHeritrixController {

    /** The logger for this class. */
    private static final Log log = LogFactory
            .getLog(BnfHeritrixController.class);

    /*
     * The below commands and attributes are copied from the attributes and
     * operations exhibited by the Heritrix MBeans of type CrawlJob and
     * CrawlService.Job, as they appear in JConsole.
     * 
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
         * Returns the {@link CrawlServiceAttribute} enum value matching the
         * given name. Throws {@link UnknownID} if no match is found.
         * 
         * @param name
         *            the attribute name
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
         * Returns the {@link CrawlServiceJobAttribute} enum value matching the
         * given name. Throws {@link UnknownID} if no match is found.
         * 
         * @param name
         *            the attribute name
         * @return the corresponding {@link CrawlServiceJobAttribute} enum
         *         value.
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

    private static enum CrawlServiceJobOperation {
        /** Fetches the progress statistics string from an Heritrix instance. */
        progressStatistics,
        /**
         * Fetches the progress statistics legend string from an Heritrix
         * instance.
         */
        progressStatisticsLegend;
    }

    private static final boolean ABORT_IF_CONN_LOST = Settings
            .getBoolean(HarvesterSettings.ABORT_IF_CONNECTION_LOST);

    /**
     * The part of the Job MBean name that designates the unique id. For some
     * reason, this is not included in the normal Heritrix definitions in
     * JmxUtils, otherwise we wouldn't have to define it. I have committed a
     * feature request: http://webteam.archive.org/jira/browse/HER-1618
     */
    private static final String UID_PROPERTY = "uid";

    /**
     * The name that Heritrix gives to the job we ask it to create. This is part
     * of the name of the MBean for that job, but we can only retrieve the name
     * after the MBean has been created.
     */
    private String jobName;

    /** The header line (legend) for the statistics report. */
    private String progressStatisticsLegend;

    /**
     * The connector to the Heritrix MBeanServer.
     */
    private JMXConnector jmxConnector;

    /**
     * Max tries for a JMX operation.
     */
    private final int jmxMaxTries = JMXUtils.getMaxTries();

    /**
     * The name of the MBean for the submitted job.
     */
    private String crawlServiceJobBeanName;

    /**
     * The name of the main Heritrix MBean.
     */
    private String crawlServiceBeanName;

    /*
     * The possible values of a request of the status attribute. Copied from
     * private values in {@link org.archive.crawler.framework.CrawlController}
     * 
     * These strings are currently not visible from outside the CrawlController
     * class. See http://webteam.archive.org/jira/browse/HER-1285
     */
    public static enum HeritrixStatus {
        // NASCENT,
        // RUNNING,
        PAUSED, PAUSING,
        // CHECKPOINTING,
        // STOPPING,
        FINISHED,
        // STARTED,
        // PREPARING,
        ILLEGAL;
    }

    /**
     * Create a BnfHeritrixController object.
     * 
     * @param files
     *            Files that are used to set up Heritrix.
     */
    public BnfHeritrixController(HeritrixFiles files) {
        super(files);
    }

    /**
     * @throws IOFailure
     *             If Heritrix dies before initialization, or we encounter any
     *             problems during the initialization.
     * @see HeritrixController#initialize()
     */
    public void initialize() {
        if (processHasExited()) {
            String errMsg = "Heritrix process of " + this
                    + " died before initialization";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }

        initJMXConnection();

        crawlServiceBeanName = "org.archive.crawler:" + JmxUtils.NAME
                + "=Heritrix," + JmxUtils.TYPE + "=CrawlService,"
                + JmxUtils.JMX_PORT + "=" + getJmxPort() + ","
                + JmxUtils.GUI_PORT + "=" + getGuiPort() + "," + JmxUtils.HOST
                + "=" + getHostName();

        // We want to be sure there are no jobs when starting, in case we got
        // an old Heritrix or somebody added jobs behind our back.
        TabularData doneJobs = (TabularData) executeMBeanOperation(
                CrawlServiceOperation.completedJobs);
        TabularData pendingJobs = (TabularData) executeMBeanOperation(
                CrawlServiceOperation.pendingJobs);
        if (doneJobs != null && doneJobs.size() > 0 || pendingJobs != null
                && pendingJobs.size() > 0) {
            throw new IllegalState(
                    "This Heritrix instance is in a illegalState! "
                            + "This instance has either old done jobs ("
                            + doneJobs + "), or old pending jobs ("
                            + pendingJobs + ").");
        }
        // From here on, we can assume there's only the one job we make.
        // We'll use the arc file prefix to name the job, since the prefix
        // already contains the harvest id and job id.
        HeritrixFiles files = getHeritrixFiles();
        executeMBeanOperation(CrawlServiceOperation.addJob, files
                .getOrderXmlFile().getAbsolutePath(), files.getArcFilePrefix(),
                getJobDescription(), files.getSeedsTxtFile().getAbsolutePath());

        jobName = getJobName();

        crawlServiceJobBeanName = "org.archive.crawler:" + JmxUtils.NAME + "="
                + jobName + "," + JmxUtils.TYPE + "=CrawlService.Job,"
                + JmxUtils.JMX_PORT + "=" + getJmxPort() + ","
                + JmxUtils.MOTHER + "=Heritrix," + JmxUtils.HOST + "="
                + getHostName();

    }

    /**
     * @throws IOFailure
     *             if unable to communicate with Heritrix
     * @see HeritrixController#requestCrawlStart()
     */
    public void requestCrawlStart() {
        executeMBeanOperation(CrawlServiceOperation.startCrawling);
    }

    /** @see HeritrixController#requestCrawlStop(String) */
    public void requestCrawlStop(String reason) {
        executeMBeanOperation(CrawlServiceOperation.terminateCurrentJob);
    }

    /**
     * Return the URL for monitoring this instance.
     * 
     * @return the URL for monitoring this instance.
     */
    public String getHeritrixConsoleURL() {
        return "http://" + SystemUtils.getLocalHostName() + ":" + getGuiPort();
    }

    /**
     * Cleanup after an Heritrix process. This entails sending the shutdown
     * command to the Heritrix process, and killing it forcefully, if it is
     * still alive after waiting the period of time specified by the
     * CommonSettings.PROCESS_TIMEOUT setting.
     * 
     * @see HeritrixController#cleanup()
     */
    public void cleanup(File crawlDir) {
        // Before cleaning up, we need to wait for the reports to be generated
        waitForReportGeneration(crawlDir);

        try {
            executeMBeanOperation(CrawlServiceOperation.shutdown);
        } catch (IOFailure e) {
            log.error("JMX error while cleaning up Heritrix controller", e);
        }

        closeJMXConnection();

        waitForHeritrixProcessExit();
    }

    /**
     * Return the URL for monitoring this instance.
     * 
     * @return the URL for monitoring this instance.
     */
    public String getAdminInterfaceUrl() {
        return "http://" + SystemUtils.getLocalHostName() + ":" + getGuiPort();
    }

    /**
     * Gets a message that stores the information summarizing the crawl
     * progress.
     * 
     * @return a message that stores the information summarizing the crawl
     *         progress.
     */
    public CrawlProgressMessage getCrawlProgress() {

        HeritrixFiles files = getHeritrixFiles();
        CrawlProgressMessage cpm = new CrawlProgressMessage(files
                .getHarvestID(), files.getJobID(), progressStatisticsLegend);

        cpm.setHostUrl(getHeritrixConsoleURL());

        // First, get CrawlService attributes

        List<Attribute> heritrixAtts = getMBeanAttributes(
                new CrawlServiceAttribute[] {
                CrawlServiceAttribute.AlertCount,
                CrawlServiceAttribute.IsCrawling,
                CrawlServiceAttribute.CurrentJob });

        CrawlServiceInfo hStatus = cpm.getHeritrixStatus();
        for (Attribute att : heritrixAtts) {
            Object value = att.getValue();
            switch (CrawlServiceAttribute.fromString(att.getName())) {
            case AlertCount:
                hStatus.setAlertCount(value != null ? (Integer) value : -1);
                break;
            case CurrentJob:
                hStatus.setCurrentJob(value != null ? (String) value : "");
                break;
            case IsCrawling:
                hStatus.setCrawling(value != null ? (Boolean) value : false);
                break;
            }
        }

        boolean crawlIsFinished = cpm.crawlIsFinished();
        if (crawlIsFinished) {
            cpm.setStatus(CrawlStatus.CRAWLING_FINISHED);
            // No need to go further, CrawlService.Job bean does not exist
            return cpm;
        }

        // Fetch CrawlService.Job attributes

        String progressStats = (String) executeMBeanOperation(
                CrawlServiceJobOperation.progressStatistics);
        CrawlServiceJobInfo jStatus = cpm.getJobStatus();
        jStatus.setProgressStatistics(progressStats != null ? progressStats
                : "?");

        if (progressStatisticsLegend == null) {
            progressStatisticsLegend = (String) executeMBeanOperation(
                    CrawlServiceJobOperation.progressStatisticsLegend);
        }

        List<Attribute> jobAtts = getMBeanAttributes(CrawlServiceJobAttribute
                .values());

        for (Attribute att : jobAtts) {
            Object value = att.getValue();
            switch (CrawlServiceJobAttribute.fromString(att.getName())) {
            case CrawlTime:
                jStatus.setElapsedSeconds(value != null ? (Long) value : -1);
                break;
            case CurrentDocRate:
                jStatus.setCurrentProcessedDocsPerSec(
                        value != null ? (Double) value : -1);
                break;
            case CurrentKbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                jStatus.setProcessedKBPerSec(value != null ? (Long) value : -1);
                break;
            case DiscoveredCount:
                jStatus.setDiscoveredFilesCount(value != null ? (Long) value
                        : -1);
                break;
            case DocRate:
                jStatus.setProcessedDocsPerSec(value != null ? (Double) value
                        : -1);
                break;
            case DownloadedCount:
                jStatus.setDownloadedFilesCount(value != null ? (Long) value
                        : -1);
                break;
            case FrontierShortReport:
                jStatus.setFrontierShortReport(value != null ? (String) value
                        : "?");
                break;
            case KbRate:
                // NB Heritrix seems to store the average value in
                // KbRate instead of CurrentKbRate...
                // Inverse of doc rates.
                jStatus.setCurrentProcessedKBPerSec(
                        value != null ? (Long) value : -1);
                break;
            case Status:
                jStatus.setStatus(value != null ? (String) value : "?");
                if (value != null) {
                    String status = (String) value;
                    if (HeritrixStatus.PAUSED.name().equals(status)
                            || HeritrixStatus.PAUSING.name().equals(status)) {
                        cpm.setStatus(CrawlStatus.CRAWLER_PAUSED);
                    } else {
                        cpm.setStatus(CrawlStatus.CRAWLER_ACTIVE);
                    }
                }
                break;
            case ThreadCount:
                jStatus.setActiveToeCount(value != null ? (Integer) value : -1);
                break;
            }
        }

        return cpm;
    }

    /**
     * Get the name of the one job we let this Heritrix run. The handling of
     * done jobs depends on Heritrix not being in crawl. This call may take
     * several seconds to finish.
     * 
     * @return The name of the one job that Heritrix has.
     * @throws IOFailure
     *             if the job created failed to initialize or didn't appear in
     *             time.
     * @throws IllegalState
     *             if more than one job in done list, or more than one pending
     *             job
     */
    private String getJobName() {
        /*
         * This is called just after we've told Heritrix to create a job. It may
         * take a while before the job is actually created, so we have to wait
         * around a bit.
         */
        TabularData pendingJobs = null;
        TabularData doneJobs;
        int retries = 0;
        while (retries++ < JMXUtils.getMaxTries()) {
            // If the job turns up in Heritrix' pending jobs list, it's ready
            pendingJobs = (TabularData) executeMBeanOperation(
                    CrawlServiceOperation.pendingJobs);
            if (pendingJobs != null && pendingJobs.size() > 0) {
                break; // It's ready, we can move on.
            }

            // If there's an error in the job configuration, the job will be put
            // in Heritrix' completed jobs list.
            doneJobs = (TabularData) executeMBeanOperation(
                    CrawlServiceOperation.completedJobs);
            if (doneJobs != null && doneJobs.size() >= 1) {
                // Since we haven't allowed Heritrix to start any crawls yet,
                // the only way the job could have ended and then put into
                // the list of completed jobs is by error.
                if (doneJobs.size() > 1) {
                    throw new IllegalState("More than one job in done list: "
                            + doneJobs);
                } else {
                    CompositeData job = JMXUtils.getOneCompositeData(doneJobs);
                    throw new IOFailure("Job " + job + " failed: "
                            + job.get(CrawlServiceJobAttribute.Status.name()));
                }
            }
            if (retries < JMXUtils.getMaxTries()) {
                TimeUtils.exponentialBackoffSleep(retries);
            }
        }
        // If all went well, we now have exactly one job in the pending
        // jobs list.
        if (pendingJobs == null || pendingJobs.size() == 0) {
            throw new IOFailure("Heritrix has not created a job after "
                    + (Math.pow(2, JMXUtils.getMaxTries()) / 1000)
                    + " seconds, giving up.");
        } else if (pendingJobs.size() > 1) {
            throw new IllegalState("More than one pending job: " + pendingJobs);
        } else {
            // Note that we may actually get through to here even if the job
            // is malformed. The job will then die as soon as we tell it to
            // start crawling.
            CompositeData job = JMXUtils.getOneCompositeData(pendingJobs);
            String name = job.get(JmxUtils.NAME) + "-" + job.get(UID_PROPERTY);
            log.info("Heritrix created a job with name " + name);
            return name;
        }
    }

    /**
     * Peridically scans the crawl dir to see if Heritrix has finished
     * generating the crawl reports. The time to wait is bounded by
     * {@link HarvesterSettings#WAIT_FOR_REPORT_GENERATION_TIMEOUT}.
     * 
     * @param crawlDir
     *            the crawl directory to scan.
     */
    private void waitForReportGeneration(File crawlDir) {

        // Verify that crawlDir is present and can be read
        if (!crawlDir.isDirectory() || !crawlDir.canRead()) {
            String message = "'" + crawlDir.getAbsolutePath()
                    + "' does not exist or is not a directory, "
                    + "or can't be read.";
            log.warn(message);
            throw new ArgumentNotValid(message);
        }

        // Scan for report files
        HashMap<String, Long> reportSizes = findReports(crawlDir);
        long currentTime = System.currentTimeMillis();
        long waitDeadline = currentTime
                + 1000
                * Settings.getLong(
                        HarvesterSettings.WAIT_FOR_REPORT_GENERATION_TIMEOUT);
        boolean changed = true;
        while (changed && (currentTime <= waitDeadline)) {
            try {
                // Wait 20 seconds
                Thread.sleep(20000);
            } catch (InterruptedException e) {

            }
            HashMap<String, Long> newReportSizes = findReports(crawlDir);
            changed = !reportSizes.equals(newReportSizes);
            currentTime = System.currentTimeMillis();
            reportSizes.clear();
            reportSizes.putAll(newReportSizes);
        }
    }

    /**
     * Scans the crawl directory for files matching the desired crawl reports,
     * as defined by {@link MetadataFile#REPORT_FILE_PATTERN}.
     * 
     * @param crawlDir
     *            the directory to scan
     * @return a map where key are the report filenames, and values their size
     *         in bytes.
     */
    private HashMap<String, Long> findReports(File crawlDir) {
        HashMap<String, Long> reportSizes = new HashMap<String, Long>();

        File[] files = crawlDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.isFile() && f.getName().matches(
                        MetadataFile.REPORT_FILE_PATTERN));
            }
        });

        for (File report : files) {
            reportSizes.put(report.getName(), report.length());
        }

        return reportSizes;
    }

    /**
     * Execute a single command.
     * 
     * @param operation
     *            the operation to execute
     * @return Whatever the command returned.
     */
    private Object executeMBeanOperation(CrawlServiceOperation operation,
            String... arguments) {
        return executeOperation(crawlServiceBeanName, operation.name(),
                arguments);
    }

    /**
     * Execute a single command
     * 
     * @param operation
     *            the operation to execute
     * @return Whatever the command returned.
     */
    private Object executeMBeanOperation(CrawlServiceJobOperation operation,
            String... arguments) {
        return executeOperation(crawlServiceJobBeanName, operation.name(),
                arguments);
    }

    /**
     * Get the value of several attributes.
     * 
     * @param attributes
     *            The attributes to get.
     * @return Whatever the command returned.
     */
    private List<Attribute> getMBeanAttributes(
            CrawlServiceJobAttribute[] attributes) {

        String[] attNames = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attNames[i] = attributes[i].name();
        }

        return getAttributes(crawlServiceJobBeanName, attNames);
    }

    /**
     * Get the value of several attributes.
     * 
     * @param attributes
     *            The attributes to get.
     * @return Whatever the command returned.
     */
    private List<Attribute> getMBeanAttributes(
            CrawlServiceAttribute[] attributes) {

        String[] attNames = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            attNames[i] = attributes[i].name();
        }

        return getAttributes(crawlServiceBeanName, attNames);
    }

    /**
     * Execute a command on a bean.
     * 
     * @param connection
     *            Connection to the server holding the bean.
     * @param beanName
     *            Name of the bean.
     * @param operation
     *            Command to execute.
     * @param args
     *            Arguments to the command. Only string arguments are possible
     *            at the moment.
     * @return The return value of the executed command.
     */
    private Object executeOperation(String beanName, String operation,
            String... args) {
        return jmxCall(beanName, true, new String[] {operation}, args);
    }

    /**
     * Get the value of several attributes from a bean.
     * 
     * @param beanName
     *            Name of the bean to get an attribute for.
     * @param attributes
     *            Name of the attributes to get.
     * @return Value of the attribute.
     */
    @SuppressWarnings("unchecked")
    private List<Attribute> getAttributes(String beanName,
            String[] attributes) {
        return (List<Attribute>) jmxCall(beanName, false, attributes);
    }

    /**
     * Executes a JMX call (attribute read or single operation) on a given bean.
     * 
     * @param beanName
     *            the MBean name.
     * @param isOperation
     *            true if the call is an operation, false if it's an attribute
     *            read.
     * @param names
     *            name of operation or name of attributes
     * @param args
     *            optional arguments for operations
     * @return the object returned by the distant MBean
     */
    private Object jmxCall(String beanName, boolean isOperation,
            String[] names, String... args) {

        MBeanServerConnection connection = getMBeanServeConnection();

        int tries = 0;
        Throwable lastException;
        do {
            tries++;
            try {
                if (isOperation) {
                    final String[] signature = new String[args.length];
                    Arrays.fill(signature, String.class.getName());
                    return connection.invoke(JMXUtils.getBeanName(beanName),
                            names[0], args, signature);
                } else {
                    return connection.getAttributes(
                            JMXUtils.getBeanName(beanName), names).asList();
                }
            } catch (IOException e) {
                lastException = e;
            } catch (ReflectionException e) {
                lastException = e;
            } catch (InstanceNotFoundException e) {
                lastException = e;
            } catch (MBeanException e) {
                lastException = e;
            }

            if (tries < jmxMaxTries) {
                TimeUtils.exponentialBackoffSleep(tries);
            }

        } while (tries < jmxMaxTries);

        String msg = "";
        if (isOperation) {
            msg = "Failed to execute " + names[0] + " with args "
                    + Arrays.toString(args) + " on " + beanName;
        } else {
            msg = "Failed to read attributes " + Arrays.toString(names)
                    + " of " + beanName;
        }
        msg += (lastException != null ? "last exception was "
                + lastException.getClass().getName() : "")
                + " after " + tries + " attempts";

        throw new IOFailure(msg, lastException);
    }

    /**
     * Initializes the JMX connection.
     */
    private void initJMXConnection() {

        // Initialize the connection to Heritrix' MBeanServer
        this.jmxConnector = JMXUtils.getJMXConnector(SystemUtils.LOCALHOST,
                getJmxPort(), Settings
                        .get(HarvesterSettings.HERITRIX_JMX_USERNAME), Settings
                        .get(HarvesterSettings.HERITRIX_JMX_PASSWORD));
    }

    /**
     * Closes the JMX connection.
     */
    private void closeJMXConnection() {
        // Close the connection to the MBean Server
        try {
            jmxConnector.close();
        } catch (IOException e) {
            log.error("JMX error while closing connection to Heritrix", e);
        }
    }

    private MBeanServerConnection getMBeanServeConnection() {

        MBeanServerConnection connection = null;
        int tries = 0;
        IOException ioe = null;
        while (tries < jmxMaxTries && connection == null) {
            tries++;
            try {
                connection = jmxConnector.getMBeanServerConnection();
            } catch (IOException e) {
                ioe = e;
                log.info("IOException while getting MBeanServerConnection"
                        + ", will renew JMX connection");
                // When an IOException is raised in RMIConnector, a terminated
                // flag is set to true, even if the underlying connection is
                // not closed. This seems to be part of a mechanism to prevent
                // deadlocks, but can cause trouble for us.
                // So if this happens, we close and reinitialize
                // the JMX connector itself.
                closeJMXConnection();
                initJMXConnection();
                log.info("Successfully renewed JMX connection");
                TimeUtils.exponentialBackoffSleep(tries);
            }
        }

        if (connection == null) {
            RuntimeException rte;
            if (ABORT_IF_CONN_LOST) {
                // HeritrixLauncher#doCrawlLoop catches IOFailures,
                // so we throw a RuntimeException
                rte = new RuntimeException("Failed to connect to MBeanServer",
                        ioe);
            } else {
                rte = new IOFailure("Failed to connect to MBeanServer", ioe);
            }
            throw rte;
        }
        return connection;
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
