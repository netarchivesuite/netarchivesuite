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
import java.io.IOException;
import java.io.PrintWriter;

import org.netarchivesuite.heritrix3wrapper.CommandLauncher;
import org.netarchivesuite.heritrix3wrapper.EngineResult;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.LaunchResultHandlerAbstract;
import org.netarchivesuite.heritrix3wrapper.ResultStatus;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper.CrawlControllerState;
import org.netarchivesuite.heritrix3wrapper.unzip.UnzipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.Heritrix3Files;

/**
 * Abstract base class for REST-based Heritrix controllers.
 */
@SuppressWarnings({"rawtypes"})
public abstract class AbstractRestHeritrixController implements HeritrixController {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(AbstractRestHeritrixController.class);

    /** The various files used by Heritrix. */
    private final Heritrix3Files files;

    protected Heritrix3Wrapper h3wrapper;
    protected CommandLauncher h3launcher;
    protected PrintWriter outputPrinter;
    protected PrintWriter errorPrinter; 
    
    
    /** The host name for this machine that matches what Heritrix uses in its MBean names. */
    private final String hostName;

    /** The port to use for Heritrix GUI, as set in settings.xml. */
    private final int guiPort = Settings.getInt(HarvesterSettings.HERITRIX_GUI_PORT);
 
   /**
     * Create a BnfHeritrixController object.
     *
     * @param files Files that are used to set up Heritrix.
     */
    public AbstractRestHeritrixController(Heritrix3Files files) {
        ArgumentNotValid.checkNotNull(files, "HeritrixFile files");
        this.files = files;
        SystemUtils.checkPortNotUsed(guiPort);
        
        hostName = SystemUtils.getLocalHostName();
        try {
            log.info("Starting Heritrix for {} in crawldir {}", this, files.getCrawlDir());
            String zipFileStr = files.getHeritrixZip().getAbsolutePath();
            //public static String HERITRIX3_CERTIFICATE = "settings.harvester.harvesting.heritrix.certificate";
            String cerficatePath = files.getCertificateFile().getAbsolutePath();
            
            String unpackDirStr = files.getCrawlDir().getAbsolutePath();
            String basedirStr = unpackDirStr + "heritrix-3.2.0/";
            String[] cmd = {
            "./bin/heritrix",
            //  "-b 192.168.1.101",
            "-p " + guiPort,
            "-a " + getHeritrixAdminName() + ":" + getHeritrixAdminPassword(),
            
            //String cerficatePath = files.getCertificateFile().getAbsolutePath();
            //  "-s h3server.jks,h3server,h3server"
            };

            log.debug("Unzipping heritrix into the crawldir");
         
            UnzipUtils.unzip(zipFileStr, unpackDirStr);
            File basedir = new File(basedirStr);
            
            h3launcher = CommandLauncher.getInstance();
        	
            outputPrinter = new PrintWriter(files.getHeritrixStdoutLog(), "UTF-8");
            errorPrinter = new PrintWriter(files.getHeritrixStderrLog(), "UTF-8");
            h3launcher.init(basedir, cmd);
            
            /** The bin/heritrix script should read the following environment-variables:
             * 
             * JAVA_HOME Point at a JDK install to use  
             * 
             * HERITRIX_HOME    Pointer to your heritrix install.  If not present, we 
             *                  make an educated guess based of position relative to this
             *                  script.
             *
             * HERITRIX_OUT     Pathname to the Heritrix log file written when run in
             *                  daemon mode.
             *                  Default setting is $HERITRIX_HOME/heritrix_out.log
             *
             * JAVA_OPTS        Java runtime options.  Default setting is '-Xmx256m'.
             *
             * FOREGROUND      
             */
            h3launcher.env.put("FOREGROUND", "true");
            String javaOpts = "";
            String jvmOptsStr = Settings.get(HarvesterSettings.HERITRIX_JVM_OPTS);
            if ((jvmOptsStr != null) && (!jvmOptsStr.isEmpty())) {
            	javaOpts = " " + jvmOptsStr;
            }
            h3launcher.env.put("JAVA_OPTS", 
            		"-Xmx" + Settings.get(HarvesterSettings.HERITRIX_HEAP_SIZE)
            		+ javaOpts);
            h3launcher.env.put("HERITRIX_OUT", files.getHeritrixOutput().getAbsolutePath());
            // TODO NEED THIS?
            //h3launcher.env.put("HERITRIX_HOME", files.getCrawlDir().getAbsolutePath());
            // TODO NEED THIS?
            //h3launcher.env.put("JAVA_HOME", ....)	
            	
            
            h3launcher.start(new LaunchResultHandlerAbstract() {
            	@Override
            	public void exitValue(int exitValue) {
            		// debug
            		System.out.println("exitValue=" + exitValue);
           	}
            	@Override
            	public void output(String line) {
            		outputPrinter.println(line);
            	}
            	@Override
            	public void closeOutput() {
            		outputPrinter.close();
            	}
            	@Override
            	public void error(String line) {
            		errorPrinter.println(line);
            	}
            	@Override
            	public void closeError() {
            		errorPrinter.close();
            	}
            });
        } catch( Throwable e) {
        	log.debug("Unexpected error while launching H3: ", e);
        	throw new IOFailure("Unexpected error while launching H3: ", e);
        }

    }

    /**
     * @return the HTTP port used by the Heritrix GUI.
     */
    protected int getGuiPort() {
        return guiPort;
    }

    /**
     * @return the Heritrix files wrapper.
     */
    protected Heritrix3Files getHeritrixFiles() {
        return files;
    }

    /**
     * @return the host name
     */
    protected String getHostName() {
        return hostName;
    }

    /**
     * Get the login name for accessing the Heritrix GUI. This name can be set in the settings.xml file.
     *
     * @return Name to use for accessing Heritrix web GUI
     */
    protected String getHeritrixAdminName() {
        return Settings.get(HarvesterSettings.HERITRIX_ADMIN_NAME);
    }

    /**
     * Get the login password for accessing the Heritrix GUI. This password can be set in the settings.xml file.
     *
     * @return Password to use for accessing the Heritrix GUI
     */
    protected String getHeritrixAdminPassword() {
        return Settings.get(HarvesterSettings.HERITRIX_ADMIN_PASSWORD);
    }


    /**
     * Write various info on the system we're using into the given file. This info will later get put into metadata for
     * the crawl.
     *
     * @param outputFile A file to write to.
     * @param builder The ProcessBuilder being used to start the Heritrix process
     */
    /*
    @SuppressWarnings("unchecked")
    private void writeSystemInfo(File outputFile, ProcessBuilder builder) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(outputFile));
            writer.println("The Heritrix process is started in the following"
                    + " environment\n (note that some entries will be" + " changed by the starting JVM):");
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
    }*/


    /**
     * Get a string that describes the current controller in terms of job ID, harvest ID, and crawldir.
     *
     * @return A human-readable string describing this controller.
     */
    @Override
    public String toString() {
        //if (heritrixProcess != null) {
        //    return "job " + files.getJobID() + " of harvest " + files.getHarvestID() + " in " + files.getCrawlDir()
        //            + " running process " + heritrixProcess;
        //} else {
            return "job " + files.getJobID() + " of harvest " + files.getHarvestID() 
            		+ " in " + files.getCrawlDir();
        //}
    }

    /**
     * Return true if the Heritrix process has exited, logging the exit value if so.
     *
     * @return True if the process has exited.
     */
    /*
    protected boolean processHasExited() {
        // First check if the process has exited already
        try {
            int exitValue = heritrixProcess.exitValue();
            log.info("Process of {} returned exit code {}", this, exitValue);
            return true;
        } catch (IllegalThreadStateException e) {
            // Not exited yet, that's fine
        }
        return false;
    }
    */

    /**
     * Waits for the Heritrix process to exit.
     */
    /*
    protected void waitForHeritrixProcessExit() {
        final long maxWait = Settings.getLong(CommonSettings.PROCESS_TIMEOUT);
        final int maxJmxRetries = JMXUtils.getMaxTries();
        Integer exitValue = ProcessUtils.waitFor(heritrixProcess, maxWait);
        if (exitValue != null) {
            log.info("Heritrix process of {} exited with exit code {}", this, exitValue);
        } else {
            log.warn("Heritrix process of {} not dead after {} millis, killing it", this, maxWait);
            heritrixProcess.destroy();
            exitValue = ProcessUtils.waitFor(heritrixProcess, maxWait);
            if (exitValue != null) {
                log.info("Heritrix process of {} exited with exit code {}", this, exitValue);
            } else {
                // If it's not dead now, there's little we can do.
                log.error("Heritrix process of {} not dead after destroy. Exiting harvest controller. "
                        + "Make sure you kill the runaway Heritrix before you restart.", this);
                NotificationsFactory
                        .getInstance()
                        .notify("Heritrix process of "
                                + this
                                + " not dead after destroy. "
                                + "Exiting harvest controller. Make sure you kill the runaway Heritrix before you restart.",
                                NotificationType.ERROR);
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
        } while (attempt++ < maxJmxRetries);
    }
    */

    /**
     * Return a human-readable description of the job. This will only be visible in the Heritrix GUI.
     *
     * @return String containing various information grabbed from HeritrixFiles.
     */
    protected String getJobDescription() {
        String dedupPart = (files.getIndexDir() != null) ? "with the deduplication index stored in '"
                + files.getIndexDir().getAbsolutePath() + "'" : "with deduplication disabled";
        return "Job " + files.getJobID() + " for harvest " + files.getHarvestID() + " performed in "
                + files.getCrawlDir() + dedupPart + " and " + FileUtils.countLines(files.getSeedsTxtFile()) + " seeds";
    }

    public Heritrix3Files getFiles() {
        return this.files;
    }

}
