/* File:        $Id:  $
 * Revision:    $Revision: 100 $
 * Author:      $Author: lars $
 * Date:        $Date: 2007-10-18 08:18:30 +0200 (Thu, 18 Oct 2007) $
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

package dk.netarkivet.harvester.harvesting;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.Heritrix;
import org.archive.crawler.admin.CrawlJob;
import org.archive.util.JmxUtils;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.jmx.JMXUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ProcessUtils;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;

/** This implementation of the HeritrixController interface starts Heritrix
 * as a separate process and uses JMX to communicate with it.  Each instance
 * executes exactly one process that runs exactly one crawl job.  */
public class JMXHeritrixController implements HeritrixController {
    private Log log = LogFactory.getLog(getClass());

    /* Commands and attributes from org.archive.crawler.admin.CrawlJob.
     * @see http://crawler.archive.org/apidocs/org/archive/crawler/admin/CrawlJob.html
     */
    private static final String ADD_JOB_COMMAND = "addJob";
    private static final String PROGRESS_STATISTICS_COMMAND
            = "progressStatistics";
    private static final String CURRENT_KB_RATE_ATTRIBUTE = "CurrentKbRate";
    private static final String THREAD_COUNT_ATTRIBUTE = "ThreadCount";
    private static final String DISCOVERED_COUNT_ATTRIBUTE = "DiscoveredCount";
    private static final String DOWNLOADED_COUNT_ATTRIBUTE = "DownloadedCount";
    private static final String STATUS_ATTRIBUTE = "Status";

    /* Commands and attributes from org.archive.crawler.Heritrix
     * @see http://crawler.archive.org/apidocs/org/archive/crawler/Heritrix.html
     */
    /* Note: The Heritrix JMX interface has two apparent ways to stop crawling:
     * stopCrawling and terminateCurrentJob.  stopCrawling merely makes Heritrix
     * not start any more jobs, but the old jobs continue.  Note that if we
     * start using more than one job at a time, terminateCurrentJob will only
     * stop one job.
     */
    private static final String START_CRAWLING_COMMAND = "startCrawling";
    /** Make the currently active (selected?) job stop */
    private static final String TERMINATE_CURRENT_JOB_COMMAND
            = "terminateCurrentJob";
    private static final String PENDING_JOBS_COMMAND = "pendingJobs";
    private static final String COMPLETED_JOBS_COMMAND = "completedJobs";
    private static final String SHUTDOWN_COMMAND = "shutdown";

    /** This is the prefix indicating a job has finished in one way or another
     * in the string returned by Heritrix for the Status command.
     */
    private static final String FINISHED_STATUS_PREFIX = "FINISHED";

    /** How long we're willing to wait for Heritrix to shutdown in a
     * shutdown hook.
     */
    private static final long SHUTDOWN_HOOK_MAX_WAIT = 1000L;

    /** The part of the Job MBean name that designates the unique id.  For some
     * reason, this is not included in the normal Heritrix definitions in
     * JmxUtils, otherwise we wouldn't have to define it.
     */
    private static final String UID_PROPERTY = "uid";

    /** The one-shot Heritrix process created in the constructor.  It will
     * only perform a single crawl before being shut down.
     */
    private final Process heritrixProcess;

    /** The shutdownHook that takes care of killing our process.  This is
     *  be removed in cleanup() if the process dies.
     */
    private Thread processKillerHook;

    /** The threads used to collect process output. */
    private Set<Thread> collectionThreads = new HashSet<Thread>(2);

    /** The name that Heritrix gives to the job we ask it to create.  This
     * is part of the name of the MBean for that job, but we can only find
     * the name after the MBean has been created. */
    private String jobName;

    /** The various files used by Heritrix. */
    private final HeritrixFiles files;

    /** Name of the JMX user that can control anything in Heritrix. */
    private static final String JMX_ADMIN_NAME = "controlRole";

    /** Create a JMXHeritrixController object
     *
     * @param files Files that are used to set up Heritrix.
     */
    public JMXHeritrixController(HeritrixFiles files) {
        this.files = files;
        SystemUtils.checkPortNotUsed(getGUIPort());
        SystemUtils.checkPortNotUsed(getJMXPort());
        try {
            log.info("Starting Heritrix for " + this);
            /* To start Heritrix, we need to do the following (taken from
               the Heritrix startup shell script):
            - set heritrix.home to base dir of Heritrix stuff
            - set com.sun.management.jmxremote.port to JMX port
            - set com.sun.management.jmxremote.ssl to false
            - set com.sun.management.jmxremote.password.file to JMX password
              file
            - set heritrix.out to heritrix_out.log
            - set java.protocol.handler.pkgs=org.archive.net
            - send processOutput & stderr into heritrix.out
            - give -p <GUI port>

            We also need to output something like the following to heritrix.out:
            `date Starting heritrix
            uname -a
            java -version
            JAVA_OPTS
            ulimit -a
             */
            ProcessBuilder builder = new ProcessBuilder(
                    new File(new File(System.getProperty("java.home"),
                                      "bin"), "java").getAbsolutePath(),
                    "-Xmx" + Settings.get(Settings.HERITRIX_HEAP_SIZE),

                    "-Dheritrix.home=" + files.getCrawlDir().getAbsolutePath(),
                    "-Dcom.sun.management.jmxremote.port=" + getJMXPort(),
                    "-Dcom.sun.management.jmxremote.ssl=false",
                    "-Dcom.sun.management.jmxremote.password.file="
                            + new File(Settings.get(Settings.JMX_PASSWORD_FILE))
                            .getAbsolutePath(),
                    "-Dheritrix.out=" + getOutputFile().getAbsolutePath(),
                    "-Djava.protocol.handler.pkgs=org.archive.net",
                    "-Ddk.netarkivet.settings.file="
                            + Settings.getSettingsFile().getAbsolutePath(),

                    Heritrix.class.getName(),
                    "--bind", "/",
                    "--port=" + getGUIPort(),
                    "--admin=" + getHeritrixAdminName()
                    + ":" + getHeritrixAdminPassword());
            updateEnvironment(builder.environment());
            FileUtils.copyDirectory(new File("lib/heritrix"),
                                    files.getCrawlDir());
            builder.directory(files.getCrawlDir());
            builder.redirectErrorStream(true);
            writeSystemInfo(getOutputFile(), builder);
            FileUtils.appendToFile(getOutputFile(), "Working directory: "
                                                    + files.getCrawlDir());
            addProcessKillerHook();
            heritrixProcess = builder.start();
            ProcessUtils.writeProcessOutput(heritrixProcess.getInputStream(),
                                            getOutputFile(),
                                            collectionThreads);
        } catch (IOException e) {
            throw new IOFailure("Error starting Heritrix process", e);
        }
    }

    /** @see HeritrixController#initialize()  */
    public void initialize() {
        if (processHasExited()) {
            log.warn("Heritrix process of " + this
                     + " died before initialization");
            return;
        }
        // We want to be sure there are no jobs when starting, in case we got
        // an old Heritrix or somebody added jobs behind our back.
        TabularData doneJobs =
                (TabularData) executeHeritrixCommand(COMPLETED_JOBS_COMMAND);
        TabularData pendingJobs =
                (TabularData) executeHeritrixCommand(PENDING_JOBS_COMMAND);
        if (doneJobs != null && doneJobs.size() > 0 ||
            pendingJobs != null && pendingJobs.size() > 0) {
            throw new IOFailure("This Heritrix is unclean!  Old done jobs are "
                                + doneJobs + ", old pending jobs are "
                                + pendingJobs);
        }
        // From here on, we can assume there's only the one job we make.
        // We'll use the arc file prefix to name the job, since the prefix
        // already contains the harvest id and job id.
        executeHeritrixCommand(ADD_JOB_COMMAND,
                             files.getOrderXmlFile().getAbsolutePath(),
                             files.getArcFilePrefix(), getJobDescription(),
                             files.getSeedsTxtFile().getAbsolutePath());
        jobName = getJobName();
    }

    /** @see HeritrixController#requestCrawlStart()
     */
    public void requestCrawlStart() {
        executeHeritrixCommand(START_CRAWLING_COMMAND);
    }

    /** @see HeritrixController#atFinish() */
    public boolean atFinish() {
        return crawlIsEnded();
    }

    /** @see HeritrixController#beginCrawlStop()  */
    public void beginCrawlStop() {
        executeHeritrixCommand(TERMINATE_CURRENT_JOB_COMMAND);
    }

    /** @see HeritrixController#getActiveToeCount()  */
    public int getActiveToeCount() {
        Integer activeToeCount =
                (Integer) getCrawlJobAttribute(THREAD_COUNT_ATTRIBUTE);
        if (activeToeCount == null) {
            return 0;
        }
        return activeToeCount;
    }

    /** @see HeritrixController#requestCrawlStop(String) */
    public void requestCrawlStop(String reason) {
        if (!atFinish()) {
            beginCrawlStop();
        }
    }

    /**
     * @see HeritrixController#getQueuedUriCount()
     * */
    public long getQueuedUriCount() {
        /* Implementation note:  This count is not as precise as what
         * StatisticsTracker could provide, but it's used only for a warning.
         */
        Long discoveredUris
                = (Long) getCrawlJobAttribute(DISCOVERED_COUNT_ATTRIBUTE);
        Long downloadedUris
                = (Long) getCrawlJobAttribute(DOWNLOADED_COUNT_ATTRIBUTE);
        if (discoveredUris == null) {
            return 0;
        }
        if (downloadedUris == null) {
            return discoveredUris;
        }
        return discoveredUris - downloadedUris;
    }

    /** @see HeritrixController#getCurrentProcessedKBPerSec() */
    public int getCurrentProcessedKBPerSec() {
        Long currentDownloadRate =
                (Long) getCrawlJobAttribute(CURRENT_KB_RATE_ATTRIBUTE);
        if (currentDownloadRate == null) {
            return 0;
        }
        return currentDownloadRate.intValue();
    }

    /** @see HeritrixController#getProgressStats() */
    public String getProgressStats() {
        String statistics =
                (String) executeCrawlJobCommand(PROGRESS_STATISTICS_COMMAND);
        if (statistics == null) {
            return "No progress statistics available";
        }
        return statistics;
    }

    /** @see HeritrixController#isPaused()  */
    public boolean isPaused() {
        String status = (String) getCrawlJobAttribute(STATUS_ATTRIBUTE);
        // Either Pausing or Paused.
        return status.equals(CrawlJob.STATUS_PAUSED) ||
               status.equals(CrawlJob.STATUS_WAITING_FOR_PAUSE);
    }

    /** Returns true if the crawl has ended, either because Heritrix finished
     * or because we terminated it.
     *
     * @return True if the crawl has ended, either because Heritrix finished
     * or because we terminated it.
     */
    public synchronized boolean crawlIsEnded() {
        // End of crawl can be seen in one of three ways:
        // 1) The Heritrix process has exited.
        // 2) The job has been moved to the completed jobs list in Heritrix.
        // 3) The job is in one of the FINISHED statii.
        if (processHasExited()) {
            return true;
        }
        TabularData jobs =
                (TabularData) executeHeritrixCommand(
                        COMPLETED_JOBS_COMMAND);
        if (jobs != null && jobs.size() > 0) {
            for (CompositeData value :
                    (Collection<CompositeData>) jobs.values()) {
                String thisJobID = value.get(JmxUtils.NAME)
                                   + "-" + value.get(UID_PROPERTY);
                if (thisJobID.equals(jobName)) {
                    return true;
                }
            }
        }
        String status = (String) getCrawlJobAttribute(STATUS_ATTRIBUTE);
        return status == null || status.startsWith(FINISHED_STATUS_PREFIX);
    }

    /** Return true if the Heritrix process has exited, logging the exit
     * value if so.
     *
     * @return True if the process has exited.
     */
    private boolean processHasExited() {
        // First check if the process has exited already
        try {
            int exitValue = heritrixProcess.exitValue();
            log.info("Process of " + this
                     + " returned exit code " + exitValue);
            return true;
        } catch (IllegalThreadStateException e) {
            // Not exited yet, that's fine
        }
        return false;
    }

    /** @see HeritrixController#cleanup() */
    public void cleanup() {
        try {
            executeHeritrixCommand(SHUTDOWN_COMMAND);
        } catch (IOFailure e) {
            log.error("JMX error while cleaning up Heritrix controller", e);
        }
        final long maxWait = Settings.getLong(Settings.PROCESS_TIMEOUT);
        Integer exitValue = ProcessUtils.waitFor(heritrixProcess,
                                                 maxWait);
        if (exitValue != null) {
            log.info("Heritrix process of " + this
                     + " exited with exit code " + exitValue);
        } else {
            log.warn("Hertrix process of " + this
                     + " not dead after " + maxWait
                     + " millis, killing it");
            heritrixProcess.destroy();
            exitValue = ProcessUtils.waitFor(heritrixProcess,
                                             maxWait);
            // If it's not dead now, there's little we can do.
            if (exitValue != null) {
                log.info("Heritrix process of " + this
                         + " exited with exit code " + exitValue);
            } else {
                log.warn("Heritrix process of " + this
                         + " not dead after destroy");
            }
        }
        Runtime.getRuntime().removeShutdownHook(processKillerHook);
        int attempt = 0;
        do {
            boolean anyAlive = false;
            for (Thread t : collectionThreads) {
                if (t.isAlive()) {
                    anyAlive = true;
                }
            }
            if (!anyAlive) {
                break;
            }
            JMXUtils.exponentialBackoffSleep(attempt);
        } while (attempt++ < JMXUtils.MAX_TRIES);
    }

    /** Change an environment to be suitable for running Heritrix.
     *
     * At the moment, this involves the following:
     *
     * Use Jar files from the lib/heritrix/lib dir.
     * Make sure the Heritrix jar file is at the front.
     *
     * @param environment The environment from a process builder
     */
    private void updateEnvironment(Map<String, String> environment) {
        final String[] pathArray = environment.get("CLASSPATH").split(":");
        List<String> pathParts
                = new ArrayList<String>(Arrays.asList(pathArray));
        File heritrixLibDir = new File("lib/heritrix/lib");
        File[] jars = heritrixLibDir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String string) {
                return string.endsWith(".jar");
            }
        });
        // Reverse sort the file list in order to add in alphabetical order
        // before the basic jars.
        Arrays.sort(jars, new Comparator<File>() {
            public int compare(File file, File file1) {
                return file1.compareTo(file);
            }
        });
        String heritixJar = null;
        for (File lib : jars) {
            final String jarPath = new File(heritrixLibDir, lib.getName())
                    .getAbsolutePath();
            if (lib.getName().startsWith("heritrix-")) {
                // Heritrix should be at the very head, as it redefines some
                // of the functions in its dependencies (!).  Thus, we have to
                // save it for later insertion at the head.
                heritixJar = jarPath;
            } else {
                pathParts.add(0, jarPath);
            }
        }
        if (heritixJar != null) {
            pathParts.add(0, heritixJar);
        } else {
            throw new IOFailure("Heritrix jar file not found");
        }
        environment.put("CLASSPATH", StringUtils.conjoin(pathParts, ":"));
    }

    /** Write various info on the system we're using into the given file.
     * This info will later get put into metadata for the crawl.
     *
     * @param outputFile A file to write to.
     * @param builder The ProcessBuilder being used to start the Heritrix
     * process
     */
    private void writeSystemInfo(File outputFile, ProcessBuilder builder) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFile));
            writer.println("The Heritrix process is started in the following"
                           + " environment\n (note that some entries will be"
                           + " changed by the starting JVM):");
            Map<String, String> env = builder.environment();
            List<String> keyList = new ArrayList<String>(env.keySet());
            Collections.sort(keyList);
            for (String key : keyList) {
                writer.println(key + "=" + env.get(key));
            }
            writer.println("Process properties:");
            Properties properties = System.getProperties();
            keyList = new ArrayList<String>((Set) properties.keySet());
            Collections.sort(keyList);
            for (String key : keyList) {
                writer.println(key + "=" + properties.get(key));
            }
        } catch (IOException e) {
            log.warn("Error writing basic properties to output file.",
                     e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /** Get a string that describes the current controller in terms of
     * job ID, harvest ID and other usable information.
     *
     * @return A human-readable string describing this controller.
     */
    public String toString() {
        if (heritrixProcess != null) {
            return "job " + files.getJobID()
                   + " of harvest " + files.getHarvestID()
                   + " in " + files.getCrawlDir()
                   + " running process " + heritrixProcess;
        } else {
            return "job " + files.getJobID()
                   + " of harvest " + files.getHarvestID()
                   + " in " + files.getCrawlDir();
        }
    }

    /** Add a shutdown hook that kills the process we've created.  Since this
     * hook will be run only in case of JVM shutdown, it cannot expect that
     * the standard logging framework is still usable, and therefore writes
     * to stdout insted.
     */
    private void addProcessKillerHook() {
        // Make sure that the process gets killed at the very end, at least
        processKillerHook = new Thread() {
            public void run() {
                try {
                    // Only non-blocking way to check for process liveness
                    int exitValue = heritrixProcess.exitValue();
                    System.out.println("Heritrix process of " + this
                                       + " exited with exit code " + exitValue);
                } catch (IllegalThreadStateException e) {
                    // Process is still alive, kill it.
                    System.out.println("Killing process of " + this);
                    heritrixProcess.destroy();
                    final Integer exitValue = ProcessUtils
                            .waitFor(heritrixProcess, SHUTDOWN_HOOK_MAX_WAIT);
                    if (exitValue != null) {
                        System.out.println("Process of " + this
                                           + " returned exit code "
                                           + exitValue);
                    } else {
                        System.out.println("Process of " + this
                                           + " never exited!");
                    }
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(processKillerHook);
    }

    /** Return a human-readable description of the job.  This will only be
     * visible in the Heritrix GUI.
     *
     * @return String containing various information grabbed from HeritrixFiles.
     */
    private String getJobDescription() {
        return "Job " + files.getJobID()
               + " for harvest " + files.getHarvestID()
               + " performed in " + files.getCrawlDir()
               + " with index in " + files.getIndexDir()
               + " and " + FileUtils.countLines(files.getSeedsTxtFile())
               + " seeds";
    }

    /** Get the name of the one job we let this Heritrix run.  The handling
     * of done jobs depends on crawling not being started yet.  This call
     * may take several seconds to finish.
     *
     * @return The name of the one job that Heritrix has.
     * @throws IOFailure if the job created failed to initialize or didn't
     * appear in time.
     */
    private String getJobName() {
        /* This is called just after we've told Heritrix to create a job.
         * It may take a while before the job is actually created, so we have
         * to wait around a bit.
         */
        TabularData pendingJobs = null;
        TabularData doneJobs;
        int retries = 0;
        while (retries++ < JMXUtils.MAX_TRIES) {
            // If the job turns up in Heritrix' pending jobs list, it's ready
            pendingJobs =
                    (TabularData) executeHeritrixCommand(
                            PENDING_JOBS_COMMAND);
            if (pendingJobs != null && pendingJobs.size() > 0) {
                break; // It's ready, we can move on.
            }

            // If there's an error in setup, the job will be put in Heritrix'
            // completed jobs list.
            doneJobs = (TabularData) executeHeritrixCommand(
                    COMPLETED_JOBS_COMMAND);
            if (doneJobs != null && doneJobs.size() >= 1) {
                // Since we haven't allowed starting crawls yet, the only
                // way the job could have ended is by error.
                if (doneJobs.size() > 1) {
                    throw new IOFailure("Too many jobs in done list: "
                                        + doneJobs);
                } else {
                    CompositeData job = JMXUtils.getOneCompositeData(doneJobs);
                    throw new IOFailure("Job " + job + " failed: "
                                        + job.get(STATUS_ATTRIBUTE));
                }
            }
            if (retries < JMXUtils.MAX_TRIES) {
                JMXUtils.exponentialBackoffSleep(retries);
            }
        }
        // If all went well, we now have exactly one job in the pending
        // jobs list.
        if (pendingJobs == null || pendingJobs.size() == 0) {
            throw new IOFailure("Heritrix has not created a job after "
                                + (Math.pow(2, JMXUtils.MAX_TRIES) / 1000)
                                + " seconds, giving up.");
        } else if (pendingJobs.size() > 1) {
            throw new IOFailure("Too many jobs: " + pendingJobs);
        } else {
            // Note that we may actually get through to here even if the job
            // is malformed.  The job will then die as soon as we tell it to
            // start crawling.
            CompositeData job = JMXUtils.getOneCompositeData(pendingJobs);
            String name = job.get(JmxUtils.NAME)
                          + "-" + job.get(UID_PROPERTY);
            log.info("Heritrix created job with " + name);
            return name;
        }
    }

    /** Return the local host name in the way that Heritrix understands it.
     *
     * @return The host name for this machine that matches what Heritrix
     * uses in its MBean names.
     */
    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IOFailure("Failed to find name of localhost", e);
        }
    }

    /** Get the login name for accessing the Heritrix GUI. This name can be
     * set in the settings.xml file.
     *
     * @return Name to use for accessing Heritrix web GUI
     */
    private String getHeritrixAdminName() {
        return Settings.get(Settings.HERITRIX_ADMIN_NAME);
    }

    /** Get the login password for accessing the Heritrix GUI.  This password
     * can be set in the settings.xml file.
     *
     * @return Password to use for accessing the Heritrix GUI
     */
    private String getHeritrixAdminPassword() {
        return Settings.get(Settings.HERITRIX_ADMIN_PASSWORD);
    }

    /** Get the name to use for logging on to Heritrix' JMX with full control.
     * The name cannot be set by the user.
     *
     * @return Name to use when connecting to Heritrix JMX
     */
    private String getJMXAdminName() {
        return JMX_ADMIN_NAME;
    }

    /** Get the password to use to access the Heritrix JMX as the user returned
     * by getJMXAdminName().  This password can be set in a file pointed to
     * in settings.xml.  The file has a format defined by the JMX standard,
     * @see <URL:http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#PasswordAccessFiles>
     *
     * @return Password for accessing Heritrix JMX
     */
    private String getJMXAdminPassword() {
        File filename = new File(Settings.get(Settings.JMX_PASSWORD_FILE));
        List<String> lines = FileUtils.readListFromFile(filename);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] parts = line.split(" +");
                if (parts.length >= 2 && parts[0].equals(getJMXAdminName())) {
                    return parts[1];
                }
            }
        }
        throw new IOFailure("No usable password found for '"
                            + getJMXAdminName() + "' in '" + filename + "'");
    }

    /** Get the file that stdout/stderr should be written to. Following
     * Heritrix, this will be heritrix.out in the crawl dir.
     *
     * @return File to write output from the process to.
     */
    private File getOutputFile() {
        return files.getHeritrixOutput();
    }

    /** Get the port to use for Heritrix JMX, as set in settings.xml.
     *
     * @return Port that Heritrix will expose its JMX interface on.
     */
    private int getJMXPort() {
        return Settings.getInt(Settings.HERITRIX_JMX_PORT);
    }

    /** Get the port to use for Heritrix GUI, as set in settings.xml.
     *
     * @return Port that Heritrix will expose its web interface on.
     */
    private int getGUIPort() {
        return Settings.getInt(Settings.HERITRIX_GUI_PORT);
    }

    /** Execute a command for the Heritrix process we're running.
     *
     * @param command The command to execute.
     * @param arguments Any arguments to the command.  These arguments can
     * only be of String type.
     * @return Whatever object was returned by the JMX invocation.
     */
    private Object executeHeritrixCommand(String command, String... arguments) {
        return JMXUtils.executeCommand(getHeritrixJMXConnector(),
                                       getHeritrixBeanName(),
                                       command, arguments);
    }

    /** Execute a command for the Heritrix job.  This must only be called after
     * initialize() has been run.
     *
     * @param command The command to execute.
     * @param arguments Any arguments to the command.  These arguments can
     * only be of String type.
     * @return Whatever object was returned by the JMX invocation.
     */
    private Object executeCrawlJobCommand(String command, String... arguments) {
        return JMXUtils.executeCommand(getHeritrixJMXConnector(),
                                       getCrawlJobBeanName(),
                                       command, arguments);
    }

    /** Get an attribute of the Heritrix process we're running.
     *
     * @param attribute The attribute to get.
     * @return The value of the attribute.
     */
    private Object getHeritrixAttribute(String attribute) {
        return JMXUtils.getAttribute(getHeritrixJMXConnector(),
                                     getHeritrixBeanName(),
                                     attribute);
    }

    /** Get an attribute of the Heritrix job.  This must only be called after
     * initialize() has been run.
     *
     * @param attribute The attribute to get.
     * @return The value of the attribute.
     */
    private Object getCrawlJobAttribute(String attribute) {
        return JMXUtils.getAttribute(getHeritrixJMXConnector(),
                                     getCrawlJobBeanName(),
                                     attribute);
    }

    /** Get the name for the main bean of the Heritrix instance.
     *
     * @return Bean name, to be passed into JMXUtils#getBeanName(String)
     */
    private String getHeritrixBeanName() {
        final String beanName = "org.archive.crawler:"
                                + JmxUtils.NAME + "=Heritrix,"
                                + JmxUtils.TYPE + "=CrawlService,"
                                + JmxUtils.JMX_PORT + "=" + getJMXPort() + ","
                                + JmxUtils.GUI_PORT + "=" + getGUIPort() + ","
                                + JmxUtils.HOST + "=" + getHostName();
        return beanName;
    }

    /** Get the name for the bean of a single job.  This bean does not exist
     * until after a job has been created using initialize().
     *
     * @return Bean name, to be passed into JMXUtils#getBeanName(String)
     */
    private String getCrawlJobBeanName() {
        final String beanName = "org.archive.crawler:"
                                + JmxUtils.NAME + "=" + jobName + ","
                                + JmxUtils.TYPE + "=CrawlService.Job,"
                                + JmxUtils.JMX_PORT + "=" + getJMXPort() + ","
                                + JmxUtils.MOTHER + "=Heritrix,"
                                + JmxUtils.HOST + "=" + getHostName();
        return beanName;
    }

    /** Get the JMX connector to Heritrix.
     *
     * @return A connector that connects to a local Heritrix instance.
     */
    private JMXConnector getHeritrixJMXConnector() {
        JMXConnector connector
                = JMXUtils.getJMXConnector(SystemUtils.LOCALHOST, getJMXPort(),
                                           getJMXAdminName(),
                                           getJMXAdminPassword());
        return connector;
    }
}
