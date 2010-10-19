/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.webinterface;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;

/**
 * Class for execution of a batchjob in a separate thread.
 */
public class BatchExecuter extends Thread {
    /** Whether the results should be appended to the file. */
    private static final boolean APPEND = true;
    
    /** The log.*/
    private Log log = LogFactory.getLog(BatchExecuter.class);
    
    /** The batchjob to execute.*/
    private FileBatchJob batchJob;
    
    /** The regular expression for the execution.*/
    private String regex;
    
    /** The replica where the batchjob should be sent.*/
    private Replica rep;
    
    
    /** 
     * Map for containing the ids for the running batchjobs.
     * Map between the name of the batchjob and the ID of the batch message.
     * */
    private static Map<String, String> batchjobs = 
        Collections.synchronizedMap(new HashMap<String, String>());
    
    /**
     * The constructor.
     * 
     * @param job The batchjob to execute.
     * @param pattern The regular expression pattern.
     * @param replica The replica where the batchjob should be executed.
     * @throws ArgumentNotValid If any of the arguments are null.
     */
    public BatchExecuter(FileBatchJob job, String pattern, Replica replica) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(job, "FileBatchJob job");
        ArgumentNotValid.checkNotNull(pattern, "String pattern");
        ArgumentNotValid.checkNotNull(replica, "Replica replica");
        
        batchJob = job;
        rep = replica;
        regex = pattern;
    }
    
    /**
     * Execution of the batchjob in its own thread (use start() instead).
     * 
     * @throws IOFailure If an IOException is caught while writing the results. 
     */
    public void run() throws IOFailure {
        ViewerArcRepositoryClient arcrep 
                = ArcRepositoryClientFactory.getViewerInstance();
        // get the timestamp in milliseconds
        String timestamp = new Long(new Date().getTime()).toString();
        // get the batchjob name without the classpath.
        String jobName = BatchGUI.getJobName(batchJob.getClass().getName());

        // handle if loaded batchjob.
        if(batchJob instanceof LoadableJarBatchJob) {
            LoadableJarBatchJob ljbj = (LoadableJarBatchJob) batchJob;
            jobName = BatchGUI.getJobName(ljbj.getLoadedJobClass());
            log.debug("LoadableJarBatchJob is actually the batchjob '"
                    + jobName + "'.");
        }
        
        try {
            // create output and error files. 
            File outputFile = new File(BatchGUI.getBatchDir(), jobName 
                    + Constants.NAME_TIMSTAMP_SEPARATOR + timestamp 
                    + Constants.OUTPUT_FILE_EXTENSION);
            outputFile.createNewFile();
            File eventLogFile = new File(BatchGUI.getBatchDir(), jobName 
                    + Constants.NAME_TIMSTAMP_SEPARATOR + timestamp 
                    + Constants.ERROR_FILE_EXTENSION);
            eventLogFile.createNewFile();

            // set pattern
            batchJob.processOnlyFilesMatching(regex);
            
            // write the output to the log file.
            FileWriter fw = new FileWriter(eventLogFile, APPEND);

            // execute the batchjob.
            String processInfo = "Starting batchjob '" + jobName 
                    + "' at time '" + timestamp + "' on replica '" 
                    + rep.getId() + "' with pattern '" + regex + "'.";
            log.info(processInfo);
            fw.write(processInfo + "\n");
            
            BatchStatus status = arcrep.batch(batchJob, rep.getId());
            final Collection<File> failedFiles = status.getFilesFailed();
            Collection<ExceptionOccurrence> exceptions = status.getExceptions();

            // log results.
            processInfo = "Successfully finished BatchJob '" + jobName 
                    + "' on " + status.getNoOfFilesProcessed() + " files.";
            log.info(processInfo);
            fw.write(processInfo + "\n");
            // log status.
            processInfo = "BatchJob '" + jobName + "' has failed on '" 
                    + failedFiles.size() + "' files and has gotten '" 
                    + exceptions.size() + "' exceptions.";
            log.info(processInfo);
            fw.write(processInfo + "\n");

            // copy results to outputfile, or log if problems with outputfile.
            if(outputFile != null && outputFile.exists()) {
                status.copyResults(outputFile);
            } else {
                log.warn("Could not print output to file. Logging it instead: "
                        + "'\n:" + StreamUtils.getInputStreamAsString(
                                status.getResultFile().getInputStream()));
            }

            // print failed files to errorfile
            if(!failedFiles.isEmpty()) {
                fw.write("File failed: " + failedFiles.size() + "\n");
                for(File f : failedFiles) {
                    fw.write(f.getPath() + "\n");
                }
            }
            
            // print exceptions
            log.info("BatchJob '" + jobName + "' encountered "
                    + exceptions.size() + " exceptions.");
            fw.write("Number of exceptions: " + exceptions.size() + "\n");
            if(!exceptions.isEmpty()) {
                // print filename and exception, with empty line between. 
                for(ExceptionOccurrence e : exceptions) {
                    fw.write(e.getFileName() + "\n");
                    fw.write(e.getException() + "\n \n");
                }
            }
            
            fw.flush();
            fw.close();
        } catch (IOException e) {
            String errMsg = "Could not handle batchjob '" + jobName 
                    + "' with timestamp '" + timestamp + "'.";
            log.error(errMsg, e);
            throw new IOFailure(errMsg, e);
        } catch (Throwable e) {
            log.error("Fatal error", e);
            throw new IOFailure("Fatal error", e);
        }
    }
    
    /**
     * Method for retrieving the data for the running batchjobs.
     * 
     * @return The set of entries in the map.
     */
    public static Set<Map.Entry<String, String>> getRunningBatchjobs() {
        return batchjobs.entrySet();
    }
}
