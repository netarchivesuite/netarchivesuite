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
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;

import org.netarchivesuite.heritrix3wrapper.CommandLauncher;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.LaunchResultHandlerAbstract;
import org.netarchivesuite.heritrix3wrapper.unzip.UnzipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ProcessUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;
import dk.netarkivet.harvester.heritrix3.Heritrix3Settings;

/**
 * Abstract base class for REST-based Heritrix controllers.
 */
public abstract class AbstractRestHeritrixController implements IHeritrixController {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(AbstractRestHeritrixController.class);

    /** The various files used by Heritrix. */
    protected final Heritrix3Files files;

    protected Heritrix3Wrapper h3wrapper;
    protected CommandLauncher h3launcher;
    protected LaunchResultHandlerAbstract h3handler;
    protected PrintWriter outputPrinter;
    protected PrintWriter errorPrinter; 
    protected File heritrixBaseDir;
    
    /** The host name for this machine that matches what Heritrix uses in its MBean names. */
    private final String hostName;

    /** The port to use for Heritrix GUI, as set in settings.xml. */
    private final int guiPort = Settings.getInt(Heritrix3Settings.HERITRIX_GUI_PORT);

    /**
     * The shutdownHook that takes care of killing our process. This is removed in cleanup() when the process is shut
     * down.
     */
    private Thread processKillerHook;
 
   /**
     * Create a BnfHeritrixController object.
     *
     * @param files Files that are used to set up Heritrix.
     */
    public AbstractRestHeritrixController(Heritrix3Files files) {
        ArgumentNotValid.checkNotNull(files, "Heritrix3Files files");
        this.files = files;
        SystemUtils.checkPortNotUsed(guiPort);
        
        hostName = SystemUtils.getLocalHostName();
        //hostName = SystemUtils.getLocalIP();
        try {
            log.info("Starting Heritrix for {} in crawldir {}", this, files.getCrawlDir());
            String zipFileStr = files.getHeritrixZip().getAbsolutePath();

            heritrixBaseDir = files.getHeritrixBaseDir();
            if (!heritrixBaseDir.isDirectory()) {
            	heritrixBaseDir.mkdirs();
            }
            if (!heritrixBaseDir.isDirectory()) {
            	throw new IOFailure("Unable to create heritrixbasedir: " + heritrixBaseDir.getAbsolutePath() );
            }

            log.debug("Unzipping heritrix into the crawldir");
            UnzipUtils.unzip(zipFileStr, 1, heritrixBaseDir.getAbsolutePath());

            if (files.getCertificateFile() != null) {
                log.debug("Copying override keystore into heritrix dir");
                Heritrix3Wrapper.copyFileAs(files.getCertificateFile(), heritrixBaseDir, "h3server.jks");
            }

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
            String[] cmd = {
                    "./bin/heritrix",
                    "-b",
                    //getHostName(),
                    hostName,
                    "-p ",
                    Integer.toString(guiPort),
                    "-a ",
                    getHeritrixAdminName() + ":" + getHeritrixAdminPassword(),
                    "-s",
                    "h3server.jks,h3server,h3server"
            };
            log.info("Starting Heritrix3 with the following arguments:{} ", 
            		StringUtils.conjoin(" ", cmd));
            h3launcher = CommandLauncher.getInstance();
            h3launcher.init(heritrixBaseDir, cmd);
            h3launcher.env.put("FOREGROUND", "true");
            log.info(".. and setting FOREGROUND to 'true'");
            String javaOpts = "";
            String jvmOptsStr = Settings.get(Heritrix3Settings.HERITRIX_JVM_OPTS);
            if ((jvmOptsStr != null) && (!jvmOptsStr.isEmpty())) {
            	javaOpts = " " + jvmOptsStr;
            }
            String javaOptsValue = "-Xmx" + Settings.get(Heritrix3Settings.HERITRIX_HEAP_SIZE) + javaOpts; 
            h3launcher.env.put("JAVA_OPTS", javaOptsValue);
            log.info(".. and setting JAVA_OPTS to '{}'", javaOptsValue);
            String heritrixOutValue = files.getHeritrixOutput().getAbsolutePath();
            h3launcher.env.put("HERITRIX_OUT", heritrixOutValue);
            log.info(".. and setting HERITRIX_OUT to '{}'", heritrixOutValue);
            // TODO NEED THIS?
            //h3launcher.env.put("HERITRIX_HOME", files.getCrawlDir().getAbsolutePath());
            // TODO NEED THIS?
            //h3launcher.env.put("JAVA_HOME", ....)	
            
            outputPrinter = new PrintWriter(files.getHeritrixStdoutLog(), "UTF-8");
            errorPrinter = new PrintWriter(files.getHeritrixStderrLog(), "UTF-8");
            h3handler = new LaunchResultHandler(outputPrinter, errorPrinter);
            log.info("..using the following environment settings: ");
            h3launcher.start(h3handler);
            Runtime.getRuntime().addShutdownHook(new HeritrixKiller());
            log.info("Heritrix3 launched successfully");
        } catch( Throwable e) {
        	log.debug("Unexpected error while launching H3: ", e);
        	throw new IOFailure("Unexpected error while launching H3: ", e);
        }
    }

    public static class LaunchResultHandler implements LaunchResultHandlerAbstract {
    	protected Semaphore semaphore = new Semaphore(-2);
        protected PrintWriter outputPrinter;
        protected PrintWriter errorPrinter;
    	public LaunchResultHandler(PrintWriter outputPrinter, PrintWriter errorPrinter) {
    		this.outputPrinter = outputPrinter;
    		this.errorPrinter = errorPrinter;
    	}
    	@Override
    	public void exitValue(int exitValue) {
    		semaphore.release();
        	log.info("Heritrix3 exitValue=: {}", exitValue);
   	    }
    	@Override
    	public void output(String line) {
    		outputPrinter.println(line);
    	}
    	@Override
    	public void closeOutput() {
    		outputPrinter.close();
    		semaphore.release();
    	}
    	@Override
    	public void error(String line) {
    		errorPrinter.println(line);
    	}
    	@Override
    	public void closeError() {
    		errorPrinter.close();
    		semaphore.release();
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
        return Settings.get(Heritrix3Settings.HERITRIX_ADMIN_NAME);
    }

    /**
     * Get the login password for accessing the Heritrix GUI. This password can be set in the settings.xml file.
     *
     * @return Password to use for accessing the Heritrix GUI
     */
    protected String getHeritrixAdminPassword() {
        return Settings.get(Heritrix3Settings.HERITRIX_ADMIN_PASSWORD);
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
            return "job " + files.getJobID() + " of harvest " + files.getHarvestID() 
            		+ " in " + files.getCrawlDir();
    }

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

    private class HeritrixKiller extends Thread {
        @Override
        public void run() {
            stopHeritrix();
        }
    }
}
