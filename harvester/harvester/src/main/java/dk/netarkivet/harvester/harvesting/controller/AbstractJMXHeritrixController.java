/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.Heritrix;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.JMXUtils;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.ProcessUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

/**
 * Abstract base class for JMX-based Heritrix controllers.
 */
public abstract class AbstractJMXHeritrixController 
implements HeritrixController {

    /** The logger for this class. */
    private static final Log log = LogFactory
            .getLog(AbstractJMXHeritrixController.class);

    /** File path Separator. Used to separate the jar-files in the classpath. */
    private static final String FILE_PATH_SEPARATOR = ":";

    /**
     * How long we're willing to wait for Heritrix to shutdown in a shutdown
     * hook.
     */
    private static final long SHUTDOWN_HOOK_MAX_WAIT = 1000L;

    /** The various files used by Heritrix. */
    private final HeritrixFiles files;

    /**
     * The threads used to collect process output. Only one thread used
     * presently.
     */
    private Set<Thread> collectionThreads = new HashSet<Thread>(1);

    /**
     * The host name for this machine that matches what Heritrix uses in its
     * MBean names.
     */
    private final String hostName;

    /**
     * The port to use for Heritrix JMX, as set in settings.xml.
     */
    private final int jmxPort = Settings
            .getInt(HarvesterSettings.HERITRIX_JMX_PORT);

    /**
     * The port to use for Heritrix GUI, as set in settings.xml.
     */
    private final int guiPort = Settings
            .getInt(HarvesterSettings.HERITRIX_GUI_PORT);

    /**
     * The shutdownHook that takes care of killing our process. This is removed
     * in cleanup() when the process is shut down.
     */
    private Thread processKillerHook;

    /**
     * The one-shot Heritrix process created in the constructor. It will only
     * perform a single crawl before being shut down.
     */
    private final Process heritrixProcess;

    /**
     * Create a BnfHeritrixController object.
     * 
     * @param files
     *            Files that are used to set up Heritrix.
     */
    public AbstractJMXHeritrixController(HeritrixFiles files) {
        ArgumentNotValid.checkNotNull(files, "HeritrixFile files");
        this.files = files;

        SystemUtils.checkPortNotUsed(guiPort);
        SystemUtils.checkPortNotUsed(jmxPort);

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IOFailure("Failed to find name of localhost", e);
        }

        try {
            log.info("Starting Heritrix for " + this);
            /*
             * To start Heritrix, we need to do the following (taken from the
             * Heritrix startup shell script): - set heritrix.home to base dir
             * of Heritrix stuff - set com.sun.management.jmxremote.port to JMX
             * port - set com.sun.management.jmxremote.ssl to false - set
             * com.sun.management.jmxremote.password.file to JMX password file -
             * set heritrix.out to heritrix_out.log - set
             * java.protocol.handler.pkgs=org.archive.net - send processOutput &
             * stderr into heritrix.out - let the Heritrix GUI-webserver listen
             * on all available network interfaces: This is done with argument
             * "--bind /" (default is 127.0.0.1) - listen on a specific port
             * using the port argument: --port <GUI port>
             * 
             * We also need to output something like the following to
             * heritrix.out: `date Starting heritrix uname -a java -version
             * JAVA_OPTS ulimit -a
             */
            File heritrixOutputFile = files.getHeritrixOutput();
            StringBuilder settingProperty = new StringBuilder();
            for (File file : Settings.getSettingsFiles()) {
                settingProperty.append(File.pathSeparator);

                String absolutePath = file.getAbsolutePath();
                // check that the settings files not only exist but
                // are readable
                boolean readable = new File(absolutePath).canRead();
                if (!readable) {
                    final String errMsg = "The file '" + absolutePath
                            + "' is missing. ";
                    log.warn(errMsg);
                    throw new IOFailure("Failed to read file '" + absolutePath
                            + "'");
                }
                settingProperty.append(absolutePath);
            }
            if (settingProperty.length() > 0) {
                // delete last path-separator
                settingProperty.deleteCharAt(0);
            }

            List<String> allOpts = new LinkedList<String>();
            allOpts.add(new File(new File(System.getProperty("java.home"),
                    "bin"), "java").getAbsolutePath());
            allOpts.add("-Xmx"
                    + Settings.get(HarvesterSettings.HERITRIX_HEAP_SIZE));
            allOpts.add("-Dheritrix.home="
                    + files.getCrawlDir().getAbsolutePath());

            String jvmOptsStr = Settings
                    .get(HarvesterSettings.HERITRIX_JVM_OPTS);
            if ((jvmOptsStr != null) && (!jvmOptsStr.isEmpty())) {
                String[] add = jvmOptsStr.split(" ");
                allOpts.addAll(Arrays.asList(add));
            }

            allOpts.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
            allOpts.add("-Dcom.sun.management.jmxremote.ssl=false");
            // check that JMX password and access files are readable.
            // TODO This should probably be extracted to a method?
            File passwordFile = files.getJmxPasswordFile();
            String pwAbsolutePath = passwordFile.getAbsolutePath();
            if (!passwordFile.canRead()) {
                final String errMsg = "Failed to read the password file '"
                        + pwAbsolutePath + "'. It is possibly missing.";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }
            File accessFile = files.getJmxAccessFile();
            String acAbsolutePath = accessFile.getAbsolutePath();
            if (!accessFile.canRead()) {
                final String errMsg = "Failed to read the access file '"
                        + acAbsolutePath + "'. It is possibly missing.";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }
            allOpts.add("-Dcom.sun.management.jmxremote.password.file="
                    + new File(pwAbsolutePath));
            allOpts.add("-Dcom.sun.management.jmxremote.access.file="
                    + new File(acAbsolutePath));
            allOpts.add("-Dheritrix.out="
                    + heritrixOutputFile.getAbsolutePath());
            allOpts.add("-Djava.protocol.handler.pkgs=org.archive.net");
            allOpts.add("-Ddk.netarkivet.settings.file=" + settingProperty);
            allOpts.add(Heritrix.class.getName());
            allOpts.add("--bind");
            allOpts.add("/");
            allOpts.add("--port=" + guiPort);
            allOpts.add("--admin=" + getHeritrixAdminName() + ":"
                    + getHeritrixAdminPassword());

            String[] args = allOpts.toArray(new String[allOpts.size()]);
            log.info("Starting Heritrix process with args"
                    + Arrays.toString(args));
            ProcessBuilder builder = new ProcessBuilder(args);

            updateEnvironment(builder.environment());
            FileUtils.copyDirectory(new File("lib/heritrix"), files
                    .getCrawlDir());
            builder.directory(files.getCrawlDir());
            builder.redirectErrorStream(true);
            writeSystemInfo(heritrixOutputFile, builder);
            FileUtils.appendToFile(heritrixOutputFile, "Working directory: "
                    + files.getCrawlDir());
            addProcessKillerHook();
            heritrixProcess = builder.start();
            ProcessUtils.writeProcessOutput(heritrixProcess.getInputStream(),
                    heritrixOutputFile, collectionThreads);
        } catch (IOException e) {
            throw new IOFailure("Error starting Heritrix process", e);
        }
    }

    protected int getJmxPort() {
        return jmxPort;
    }

    protected int getGuiPort() {
        return guiPort;
    }

    /**
     * @return the Heritrix files wrapper.
     */
    protected HeritrixFiles getHeritrixFiles() {
        return files;
    }

    /**
     * @return the host name
     */
    protected String getHostName() {
        return hostName;
    }

    /**
     * Get the login name for accessing the Heritrix GUI. This name can be set
     * in the settings.xml file.
     * 
     * @return Name to use for accessing Heritrix web GUI
     */
    private String getHeritrixAdminName() {
        return Settings.get(HarvesterSettings.HERITRIX_ADMIN_NAME);
    }

    /**
     * Get the login password for accessing the Heritrix GUI. This password can
     * be set in the settings.xml file.
     * 
     * @return Password to use for accessing the Heritrix GUI
     */
    private String getHeritrixAdminPassword() {
        return Settings.get(HarvesterSettings.HERITRIX_ADMIN_PASSWORD);
    }

    /**
     * Change an environment to be suitable for running Heritrix.
     * 
     * At the moment, this involves the following:
     * 
     * Prepend the Jar files from the lib/heritrix/lib dir to the classpath.
     * Make sure the Heritrix jar file is at the front.
     * 
     * @param environment
     *            The environment from a process builder
     * @throws IOFailure
     *             If a Heritrix jarfile is not found.
     */
    private static void updateEnvironment(Map<String, String> environment) {
        List<String> classPathParts = SystemUtils.getCurrentClasspath();
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
                // of the functions in its dependencies (!). Thus, we have to
                // save it for later insertion at the head.
                heritixJar = jarPath;
            } else {
                classPathParts.add(0, jarPath);
            }
        }
        if (heritixJar != null) {
            classPathParts.add(0, heritixJar);
        } else {
            throw new IOFailure("Heritrix jar file not found");
        }
        environment.put("CLASSPATH", StringUtils.conjoin(FILE_PATH_SEPARATOR,
                classPathParts));
    }

    /**
     * Write various info on the system we're using into the given file. This
     * info will later get put into metadata for the crawl.
     * 
     * @param outputFile
     *            A file to write to.
     * @param builder
     *            The ProcessBuilder being used to start the Heritrix process
     */
    @SuppressWarnings("unchecked")
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
            log.warn("Error writing basic properties to output file.", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Add a shutdown hook that kills the process we've created. Since this hook
     * will be run only in case of JVM shutdown, it cannot expect that the
     * standard logging framework is still usable, and therefore writes to
     * stdout instead.
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
                    final Integer exitValue = ProcessUtils.waitFor(
                            heritrixProcess, SHUTDOWN_HOOK_MAX_WAIT);
                    if (exitValue != null) {
                        System.out.println("Process of " + this
                                + " returned exit code " + exitValue);
                    } else {
                        System.out.println("Process of " + this
                                + " never exited!");
                    }
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(processKillerHook);
    }

    /**
     * Get a string that describes the current controller in terms of job ID,
     * harvest ID, and crawldir.
     * 
     * @return A human-readable string describing this controller.
     */
    @Override
    public String toString() {
        if (heritrixProcess != null) {
            return "job " + files.getJobID() + " of harvest "
                    + files.getHarvestID() + " in " + files.getCrawlDir()
                    + " running process " + heritrixProcess;
        } else {
            return "job " + files.getJobID() + " of harvest "
                    + files.getHarvestID() + " in " + files.getCrawlDir();
        }
    }

    /**
     * Return true if the Heritrix process has exited, logging the exit value if
     * so.
     * 
     * @return True if the process has exited.
     */
    protected boolean processHasExited() {
        // First check if the process has exited already
        try {
            int exitValue = heritrixProcess.exitValue();
            log.info("Process of " + this + " returned exit code " + exitValue);
            return true;
        } catch (IllegalThreadStateException e) {
            // Not exited yet, that's fine
        }
        return false;
    }

    /**
     * Waits for the Heritrix process to exit.
     */
    protected void waitForHeritrixProcessExit() {
        final long maxWait = Settings.getLong(CommonSettings.PROCESS_TIMEOUT);
        Integer exitValue = ProcessUtils.waitFor(heritrixProcess, maxWait);
        if (exitValue != null) {
            log.info("Heritrix process of " + this + " exited with exit code "
                    + exitValue);
        } else {
            log.warn("Heritrix process of " + this + " not dead after "
                    + maxWait + " millis, killing it");
            heritrixProcess.destroy();
            exitValue = ProcessUtils.waitFor(heritrixProcess, maxWait);
            if (exitValue != null) {
                log.info("Heritrix process of " + this
                        + " exited with exit code " + exitValue);
            } else {
                // If it's not dead now, there's little we can do.
                log.fatal("Heritrix process of " + this
                        + " not dead after destroy. "
                        + "Exiting harvest controller. "
                        + "Make sure you kill the runaway Heritrix "
                        + "before you restart.");
                NotificationsFactory.getInstance().errorEvent(
                        "Heritrix process of " + this
                                + " not dead after destroy. "
                                + "Exiting harvest controller. "
                                + "Make sure you kill the runaway Heritrix "
                                + "before you restart.");
                System.exit(1);
            }
        }
        Runtime.getRuntime().removeShutdownHook(processKillerHook);
        // Wait until all collection threads are dead or until we have
        // tried JMXUtils.MAX_TRIES times.
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
            TimeUtils.exponentialBackoffSleep(attempt);
        } while (attempt++ < JMXUtils.getMaxTries());
    }

    /**
     * Return a human-readable description of the job. This will only be visible
     * in the Heritrix GUI.
     * 
     * @return String containing various information grabbed from HeritrixFiles.
     */
    protected String getJobDescription() {
        String dedupPart = (files.getIndexDir() != null) 
            ? "with the deduplication index stored in '"
                + files.getIndexDir().getAbsolutePath() + "'"
                : "with deduplication disabled";
        return "Job " + files.getJobID() + " for harvest "
                + files.getHarvestID() + " performed in " + files.getCrawlDir()
                + dedupPart + " and "
                + FileUtils.countLines(files.getSeedsTxtFile()) + " seeds";
    }
}
