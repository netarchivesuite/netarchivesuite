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
import java.io.PrintWriter;
import java.util.concurrent.Semaphore;

import org.netarchivesuite.heritrix3wrapper.CommandLauncher;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.LaunchResultHandlerAbstract;
import org.netarchivesuite.heritrix3wrapper.unzip.UnzipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
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
     * Create a AbstractRestHeritrixController  object.
     *
     * @param files Files that are used to set up Heritrix.
     */
    public AbstractRestHeritrixController(Heritrix3Files files) {
        ArgumentNotValid.checkNotNull(files, "Heritrix3Files files");
        this.files = files;
        SystemUtils.checkPortNotUsed(guiPort);
        
        hostName = SystemUtils.getLocalHostName();
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
            String javaOptsValue = "-Xmx" + Settings.get(Heritrix3Settings.HERITRIX_HEAP_SIZE) + " " + javaOpts + " " +  getSettingsProperty();
            h3launcher.env.put("JAVA_OPTS", javaOptsValue);
            log.info(".. and setting JAVA_OPTS to '{}'", javaOptsValue);
            String heritrixOutValue = files.getHeritrixOutput().getAbsolutePath();
            h3launcher.env.put("HERITRIX_OUT", heritrixOutValue);
            log.info(".. and setting HERITRIX_OUT to '{}'", heritrixOutValue);
            
            outputPrinter = new PrintWriter(files.getHeritrixStdoutLog(), "UTF-8");
            errorPrinter = new PrintWriter(files.getHeritrixStderrLog(), "UTF-8");
            log.info(".. and setting output from heritrix3 to '{}', and errors to '{}'", files.getHeritrixStdoutLog(),files.getHeritrixStderrLog() );
            h3handler = new LaunchResultHandler(outputPrinter, errorPrinter);
            h3launcher.start(h3handler);
            Runtime.getRuntime().addShutdownHook(new HeritrixKiller());
            log.info("Heritrix3 engine launched successfully");
        } catch( Throwable e) {
        	String errMsg = "Unexpected error while launching H3: ";
        	log.debug(errMsg, e);
        	throw new IOFailure(errMsg, e);
        }
    }
    
    /**
     * Implementation of a LaunchResultHandler for Heritrix3. 
     *
     */
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
        	if (exitValue != 0) {
        	    log.error("Heritrix3 engine shutdown failed. ExitValue =  {}", exitValue);
        	} else {
        	    log.info("Heritrix3 engine shutdown was successful. ExitValue =  {}", exitValue);
        	}
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
     * @return the Settingsproperty for heritrix3
     */
    private static String getSettingsProperty() {
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
    	return "-Ddk.netarkivet.settings.file=" + settingProperty;
    }
    
    /**
     * @return the HTTP port used by the Heritrix3 GUI.
     */
    protected int getGuiPort() {
        return guiPort;
    }

    /**
     * @return the Heritrix3 files wrapper.
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
     * Get the login name for accessing the Heritrix3 GUI. This name can be set in the settings.xml file.
     *
     * @return Name to use for accessing Heritrix3 web GUI
     */
    protected String getHeritrixAdminName() {
        return Settings.get(Heritrix3Settings.HERITRIX_ADMIN_NAME);
    }

    /**
     * Get the login password for accessing the Heritrix3 GUI. This password can be set in the settings.xml file.
     *
     * @return Password to use for accessing the Heritrix3 GUI
     */
    protected String getHeritrixAdminPassword() {
        return Settings.get(Heritrix3Settings.HERITRIX_ADMIN_PASSWORD);
    }

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
