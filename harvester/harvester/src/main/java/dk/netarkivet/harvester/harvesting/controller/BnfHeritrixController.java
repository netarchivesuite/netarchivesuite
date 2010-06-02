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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.archive.crawler.Heritrix;
import org.archive.util.JmxUtils;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;
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
public class BnfHeritrixController implements HeritrixController {

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
		 * Returns the {@link CrawlServiceAttribute} enum value matching 
		 * the given name. Throws {@link UnknownID} if no match is found.
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
		 * Returns the {@link CrawlServiceJobAttribute} enum value matching 
		 * the given name. Throws {@link UnknownID} if no match is found.
		 * @param name the attribute name
		 * @return the corresponding {@link CrawlServiceJobAttribute} 
		 * enum value.
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
		/** Fetches the progress statistics legend string 
		 * from an Heritrix instance. 
		 */
		progressStatisticsLegend;
	}

	/**
	 * How long we're willing to wait for Heritrix to shutdown in a shutdown
	 * hook.
	 */
	private static final long SHUTDOWN_HOOK_MAX_WAIT = 1000L;
	
	private static final boolean ABORT_IF_CONN_LOST = 
		Settings.getBoolean(HarvesterSettings.ABORT_IF_CONNECTION_LOST);

	/**
	 * The part of the Job MBean name that designates the unique id. For some
	 * reason, this is not included in the normal Heritrix definitions in
	 * JmxUtils, otherwise we wouldn't have to define it. I have committed a
	 * feature request: http://webteam.archive.org/jira/browse/HER-1618
	 */
	private static final String UID_PROPERTY = "uid";

	/** File path Separator. Used to separate the jar-files in the classpath. */
	private static final String FILE_PATH_SEPARATOR = ":";

	/**
	 * The one-shot Heritrix process created in the constructor. It will only
	 * perform a single crawl before being shut down.
	 */
	private final Process heritrixProcess;

	/**
	 * The shutdownHook that takes care of killing our process. This is removed
	 * in cleanup() when the process is shut down.
	 */
	private Thread processKillerHook;

	/**
	 * The threads used to collect process output. Only one thread used
	 * presently.
	 */
	private Set<Thread> collectionThreads = new HashSet<Thread>(1);

	/**
	 * The name that Heritrix gives to the job we ask it to create. This is part
	 * of the name of the MBean for that job, but we can only retrieve the name
	 * after the MBean has been created.
	 */
	private String jobName;

	/** The various files used by Heritrix. */
	private final HeritrixFiles files;

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
	 * The host name for this machine that matches what Heritrix uses in its
	 * MBean names.
	 */
	private final String hostName;

	/**
	 * The name of the MBean for the submitted job.
	 */
	private String crawlServiceJobBeanName;

	/**
	 * The name of the main Heritrix MBean
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
				+ JmxUtils.JMX_PORT + "=" + jmxPort + "," + JmxUtils.GUI_PORT
				+ "=" + guiPort + "," + JmxUtils.HOST + "=" + hostName;

		// We want to be sure there are no jobs when starting, in case we got
		// an old Heritrix or somebody added jobs behind our back.
		TabularData doneJobs = (TabularData) executeMBeanOperation(CrawlServiceOperation.completedJobs);
		TabularData pendingJobs = (TabularData) executeMBeanOperation(CrawlServiceOperation.pendingJobs);
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
		executeMBeanOperation(CrawlServiceOperation.addJob, files
				.getOrderXmlFile().getAbsolutePath(), files.getArcFilePrefix(),
				getJobDescription(), files.getSeedsTxtFile().getAbsolutePath());

		jobName = getJobName();

		crawlServiceJobBeanName = "org.archive.crawler:" + JmxUtils.NAME + "="
				+ jobName + "," + JmxUtils.TYPE + "=CrawlService.Job,"
				+ JmxUtils.JMX_PORT + "=" + jmxPort + "," + JmxUtils.MOTHER
				+ "=Heritrix," + JmxUtils.HOST + "=" + hostName;

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
		return "http://" + SystemUtils.getLocalHostName() + ":" + guiPort;
	}
	
	/**
	 * Return true if the Heritrix process has exited, logging the exit value if
	 * so.
	 * 
	 * @return True if the process has exited.
	 */
	private boolean processHasExited() {
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
	 * Return the URL for monitoring this instance.
	 * 
	 * @return the URL for monitoring this instance.
	 */
	public String getAdminInterfaceUrl() {
		return "http://" + SystemUtils.getLocalHostName() + ":" + guiPort;
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
	 * Get a string that describes the current controller in terms of job ID,
	 * harvest ID, and crawldir.
	 * 
	 * @return A human-readable string describing this controller.
	 */
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
     * Gets a message that stores the information summarizing 
     * the crawl progress.
     * @return a message that stores the information summarizing 
     * the crawl progress.
     */
	public CrawlProgressMessage getCrawlProgress() {

		CrawlProgressMessage cpm = new CrawlProgressMessage(files
				.getHarvestID(), files.getJobID(), progressStatisticsLegend);

		cpm.setHostUrl(getHeritrixConsoleURL());
		
		// First, get CrawlService attributes
		
		List<Attribute> heritrixAtts = getMBeanAttributes(
				new CrawlServiceAttribute[] {
						CrawlServiceAttribute.AlertCount,
						CrawlServiceAttribute.IsCrawling,
						CrawlServiceAttribute.CurrentJob
				});
		
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
					hStatus.setCrawling(
							value != null ? (Boolean) value : false);
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
		jStatus.setProgressStatistics(
				progressStats != null ? progressStats : "?");

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
				jStatus.setDiscoveredFilesCount(
						value != null ? (Long) value : -1);
				break;
			case DocRate:
				jStatus.setProcessedDocsPerSec(
						value != null ? (Double) value : -1);
				break;
			case DownloadedCount:
				jStatus.setDownloadedFilesCount(
						value != null ? (Long) value : -1);
				break;
			case FrontierShortReport:
				jStatus.setFrontierShortReport(
						value != null ? (String) value : "?");
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
	 * Return a human-readable description of the job. This will only be visible
	 * in the Heritrix GUI.
	 * 
	 * @return String containing various information grabbed from HeritrixFiles.
	 */
	private String getJobDescription() {
		String dedupPart = (files.getIndexDir() != null) ? "with the deduplication index stored in '"
				+ files.getIndexDir().getAbsolutePath() + "'"
				: "with deduplication disabled";
		return "Job " + files.getJobID() + " for harvest "
				+ files.getHarvestID() + " performed in " + files.getCrawlDir()
				+ dedupPart + " and "
				+ FileUtils.countLines(files.getSeedsTxtFile()) + " seeds";
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
			pendingJobs = (TabularData) executeMBeanOperation(CrawlServiceOperation.pendingJobs);
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

	/** Peridically scans the crawl dir to see if Heritrix has finished 
	 * generating the crawl reports. The time to wait is bounded by
	 * {@link HarvesterSettings#WAIT_FOR_REPORT_GENERATION_TIMEOUT}.
	 * 
	 * @param crawlDir the crawl directory to scan.
	 */
	private void waitForReportGeneration(File crawlDir) {

		// Verify that crawlDir is present and can be read
		if (!crawlDir.isDirectory() || !crawlDir.canRead()) {
			String message = "'"
					+ crawlDir.getAbsolutePath()
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
	 * as defined by {@link MetadataFile#REPORT_FILE_PATTERN}
	 * @param crawlDir the directory to scan
	 * @return a map where key are the report filenames, and values their size
	 * in bytes.
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
	private Object executeMBeanOperation(
			CrawlServiceOperation operation,
			String... arguments) {
		return executeOperation(
				crawlServiceBeanName,
				operation.name(), 
				arguments);
	}

	/**
	 * Execute a single command
	 * 
	 * @param operation
	 *            the operation to execute
	 * @return Whatever the command returned.
	 */
	private Object executeMBeanOperation(
			CrawlServiceJobOperation operation,
			String... arguments) {
		return executeOperation(
				crawlServiceJobBeanName,
				operation.name(), 
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

		return getAttributes(
				crawlServiceJobBeanName, 
				attNames);
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

		return getAttributes(
				crawlServiceBeanName, 
				attNames);
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
	private Object executeOperation(
			String beanName, 
			String operation, 
			String... args) {
		return jmxCall( 
				beanName, 
				true, 
				new String[] {operation}, 
				args);
	}

	/**
	 * Get the value of several attributes from a bean.
	 * 
	 * @param beanName
	 *            Name of the bean to get an attribute for.
	 * @param attributes
	 *            Name of the attributes to get.
	 * @param connection
	 *            A connection to the JMX server for the bean.
	 * @return Value of the attribute. 
	 */
	@SuppressWarnings("unchecked")
	private List<Attribute> getAttributes(
			String beanName,
			String[] attributes) {
		return (List<Attribute>) jmxCall(
				beanName, 
				false, 
				attributes);
	}
	
	/**
	 * Executes a JMX call (attribute read or single operation) on a given bean.
	 * @param beanName the MBean name.
	 * @param isOperation true if the call is an operation, 
	 * false if it's an attribute read.
	 * @param names name of operation or name of attributes 
	 * @param args optional arguments for operations
	 * @return the object returned by the distant MBean
	 */
	private Object jmxCall(
			String beanName,
			boolean isOperation,
			String[] names,
			String... args) {
		
		MBeanServerConnection connection = getMBeanServeConnection();
		
		int tries = 0;
		Throwable lastException;
		do {
            tries++;
            try {
            	if (isOperation) {
            		final String[] signature = new String[args.length];
            		Arrays.fill(signature, String.class.getName());
            		return connection.invoke(
        					JMXUtils.getBeanName(beanName),
        					names[0], 
        					args, 
        					signature);
            	} else {
            		return connection.getAttributes(
        					JMXUtils.getBeanName(beanName),
        					names).asList();
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
			msg = "Failed to execute " + names[0]
			+ " with args " + Arrays.toString(args)
			+ " on " + beanName;			
		} else {
			msg = "Failed to read attributes " 
				+ Arrays.toString(names)
				+ " of " + beanName;
		}
		msg += (lastException != null ? 
				"last exception was " 
				+ lastException.getClass().getName()
				: "")
				+ " after " + tries + " attempts";
		
		throw new IOFailure(msg, lastException);		
	}

	/**
	 * Initializes the JMX connection.
	 */
	private void initJMXConnection() {
		
		// Initialize the connection to Heritrix' MBeanServer
		this.jmxConnector = JMXUtils.getJMXConnector(SystemUtils.LOCALHOST,
				jmxPort, Settings.get(HarvesterSettings.HERITRIX_JMX_USERNAME),
				Settings.get(HarvesterSettings.HERITRIX_JMX_PASSWORD));
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
				rte = new RuntimeException(
						"Failed to connect to MBeanServer", ioe);
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
