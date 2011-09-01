/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.archive.indexserver.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.archive.indexserver.FileBasedCache;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;

/**
 * Index request server singleton.
 *
 * This class contains a singleton that handles requesting an index over JMS.
 *
 * It will ALWAYS reply to such messages, either with the index, a message
 * telling that only a subset is available, and which, or an error message,
 *
 */
public class IndexRequestServer extends ArchiveMessageHandler
        implements CleanupIF {
    /** The class logger. */
    private Log log = LogFactory.getLog(getClass().getName());
    /** The unique instance. */
    private static IndexRequestServer instance;
    /** The handlers for index request types. */
    private Map<RequestType, FileBasedCache<Set<Long>>> handlers;
    
    /** The connection to the JMSBroker. */
    private JMSConnection conn;
    /** A set with the current indexing jobs in progress. */
    private Set<IndexRequestMessage> currentJobs;
    /** The max number of concurrent jobs. */
    private long maxConcurrentJobs;
    /** Are we listening, now. */
    private AtomicBoolean isListening = new AtomicBoolean();
    /**
     * The directory to store backup copies of the currentJobs.
     * In case of the indexserver crashing. 
     */
    private File requestDir;
    /** Initialise index request server with no handlers, listening to the
     * index JMS channel.
     */
    private IndexRequestServer() {
        maxConcurrentJobs = Settings.getLong(
                ArchiveSettings.INDEXSERVER_INDEXING_MAXCLIENTS);
        requestDir = Settings.getFile(
                ArchiveSettings.INDEXSERVER_INDEXING_REQUESTDIR);
        currentJobs = new HashSet<IndexRequestMessage>();
        handlers = new EnumMap<RequestType, FileBasedCache<Set<Long>>>(
                RequestType.class);      
    }

    /**
     * Restore old requests from requestDir.
     */
    private void restoreRequestsfromRequestDir() {
        if (!requestDir.exists()) {
            log.info("requestdir not found: creating request dir");
            requestDir.mkdirs();
            return;
        }
        
        File[] requests = requestDir.listFiles();
        if (requests != null) {
            // Fill up the currentJobs 
            for (File request: requests){
                if (request.isFile()) {
                    final IndexRequestMessage msg = restoreMessage(request);
                    currentJobs.add(msg);
                    //Start a new thread to handle the actual request.
                    new Thread(){
                        public void run() {
                            doGenerateIndex(msg);
                        }
                    }.start();
                    log.info("Restarting indexjob w/ ID=" + msg.getID());
                } else {
                    log.debug("Ignoring directory in requestdir: " 
                            + request.getAbsolutePath());
                            
                }
            }
        }
        
        
    }

    /** Get the unique index request server instance.
     *
     * @return The index request server.
     */
    public static synchronized IndexRequestServer getInstance() {
        if (instance == null) {
            instance = new IndexRequestServer();
        }

        return instance;
    }

    /** Set handler for certain type of index request. If called more than once,
     * new handler overwrites old one.
     *
     * @param t The type of index requested
     * @param handler The handler that should handle this request.
     */
    public void setHandler(RequestType t,
                           FileBasedCache<Set<Long>> handler) {
        ArgumentNotValid.checkNotNull(t, "RequestType t");
        ArgumentNotValid.checkNotNull(handler,
                                      "FileBasedCache<Set<Long>> handler");
        log.info("Setting handler for RequestType: " + t);
        handlers.put(t, handler);
    }

    /** Given a request for an index over a set of job ids, use a cache to
     * try to create the index, Then reply result.
     *
     * If for any reason not all requested jobs can be indexed, return the
     * subset. The client can then retry with this subset, in order to get index
     * of that subset.
     *
     * Values read from the message in order to handle this:
     * - Type of index requested - will use the index cache of this type
     * - Set of job IDs - which jobs to generate index for
     *
     * Values written to message before replying:
     * - The subset indexed - may be the entire set. ALWAYS set unless reply !OK
     * - File with index - ONLY if subset is entire set, the index requested.
     *
     * This method should ALWAYS reply. May reply with not OK message if:
     * - Message received was not OK
     * - Request type is null or unknown in message
     * - Set of job ids is null in message
     * - Cache generation throws exception
     *
     * @param irMsg A message requesting an index.
     * @throws ArgumentNotValid on null parameter
     */
    public synchronized void visit(final IndexRequestMessage irMsg) 
    throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(irMsg, "IndexRequestMessage irMsg");
        // save new msg to requestDir
        try {
            saveMsg(irMsg);
            currentJobs.add(irMsg);

            // Limit the number of concurrently indexing job
            if (currentJobs.size() >= maxConcurrentJobs) {
                if (isListening.get()) {
                    conn.removeListener(Channels.getTheIndexServer(), this);
                    isListening.set(false);
                }
            }

            //Start a new thread to handle the actual request.
            new Thread(){
                public void run() {
                    doGenerateIndex(irMsg);
                }
            }.start();
            log.debug("Now " + currentJobs.size()
                    + " indexing jobs in progress");
        } catch (IOException e) {
            final String errMsg = "Unable to initiate indexing. Send failed "
                + "message back to sender: " + e; 
            log.warn(errMsg, e);
            irMsg.setNotOk(errMsg);
            JMSConnectionFactory.getInstance().reply(irMsg);
        }
    }

    /**
     * Save a IndexRequestMessage to disk.
     * @param irMsg A message to store to disk
     * @throws IOException Throws IOExecption, if unable to save message
     */
    private void saveMsg(IndexRequestMessage irMsg) throws IOException {
        File dest = new File(requestDir, irMsg.getID());
        log.debug("Storing message to " + dest.getAbsolutePath());
        // Writing message to file
        FileOutputStream fos = new FileOutputStream(dest);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(irMsg);
    }

    /**
     * Restore message from serialized state.
     * @param serializedObject the object stored as a file.
     * @return the restored message.
     */
    private IndexRequestMessage restoreMessage(File serializedObject) {        
        Object obj = null;
        try {
        // Read the message from disk.
        FileInputStream fis = new 
            FileInputStream(serializedObject);

        ObjectInputStream ois = 
            new ObjectInputStream(fis);

        obj = ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalState(
                    "Not possible to read the stored message from file '"
                    + serializedObject.getAbsolutePath() + "':", e);
        } catch (IOException e) {
            throw new IOFailure(
                    "Not possible to read the stored message from file '" 
                    + serializedObject.getAbsolutePath() + "':", e);
        }
        
        if (obj instanceof IndexRequestMessage){
            return (IndexRequestMessage) obj;
        } else {
            throw new IllegalState("The serialized message is not a " 
                    + IndexRequestMessage.class.getName() + " but a " 
                    + obj.getClass().getName());
        }
    }
    
    /**
     * Method that handles generating an index; supposed to be run in its own
     * thread, because it blocks while the index is generated.
     * @see #visit(IndexRequestMessage)
     * @param irMsg A message requesting an index
     */
    private void doGenerateIndex(final IndexRequestMessage irMsg) {
        final boolean mustReturnIndex = irMsg.mustReturnIndex();
        try {
            checkMessage(irMsg);
            RequestType type = irMsg.getRequestType();
            Set<Long> jobIDs = irMsg.getRequestedJobs();
            log.info("Generating an index of type '" + type
                     + "' for the jobs [" + StringUtils.conjoin(",", jobIDs)
                     + "]");
            FileBasedCache<Set<Long>> handler = handlers.get(type);
            Set<Long> foundIDs = handler.cache(jobIDs);
            irMsg.setFoundJobs(foundIDs);
            if (foundIDs.equals(jobIDs)) {
                log.info("Successfully generated index of type '" + type
                         + "' for the jobs [" + StringUtils.conjoin(",", jobIDs)
                         + "]");
                File cacheFile = handler.getCacheFile(jobIDs);
                if (mustReturnIndex) { // return index now! (default behaviour)
                    if (cacheFile.isDirectory()) {
                        // This cache uses multiple files stored in a directory,
                        // so transfer them all.
                        File[] cacheFiles = cacheFile.listFiles();
                        List<RemoteFile> resultFiles 
                            = new ArrayList<RemoteFile>(cacheFiles.length);
                        for (File f : cacheFiles) {
                            resultFiles.add(
                                    RemoteFileFactory.getCopyfileInstance(f));
                        }
                        irMsg.setResultFiles(resultFiles);
                    } else {
                        irMsg.setResultFile(
                                RemoteFileFactory.getCopyfileInstance(
                                cacheFile));
                    }
                }
            } else {
                Set<Long> missingJobIds = new HashSet<Long>(jobIDs);
                missingJobIds.removeAll(foundIDs);
                log.warn("Failed generating index of type '" + type
                         + "' for the jobs [" + StringUtils.conjoin(",", jobIDs)
                         + "]. Missing data for jobs ["
                         + StringUtils.conjoin(",", missingJobIds)
                         + "].");
                
            }
        } catch (Exception e) {
            log.warn(
                    "Unable to generate index for jobs ["
                    + StringUtils.conjoin(",", irMsg.getRequestedJobs()) + "]",
                    e);
            irMsg.setNotOk(e);
        } finally {
            // Remove job from currentJobs Set and reenable us as listener
            // if necessary.
            currentJobs.remove(irMsg);
            // delete stored message
            deleteStoredMessage(irMsg);
            if (!isListening.get()) {
                if (maxConcurrentJobs > currentJobs.size()) {
                    log.info("Re-enabling listening to the indexserver-queue");
                    conn.setListener(Channels.getTheIndexServer(), this);
                }
            }
            String state = "failed";
            if (irMsg.isOk()) {
                state = "successful";
            }
            if (mustReturnIndex) {
                log.info("Sending " + state 
                        + " reply for IndexRequestMessage"
                        + " back to sender '"
                        + irMsg.getReplyTo() + "'.");
                JMSConnectionFactory.getInstance().reply(irMsg);
            } else {
               log.info("Sending IndexReadyMessage to Scheduler");
               IndexReadyMessage irm = new IndexReadyMessage(
                       irMsg.getHarvestId(), 
                       irMsg.getReplyTo(),
                       Channels.getTheIndexServer());
               JMSConnectionFactory.getInstance().send(irm);
            }
        }
    }

    /**
     * Deleted stored file for given message.
     * @param irMsg a given IndexRequestMessage
     */
    private void deleteStoredMessage(IndexRequestMessage irMsg) {
        File expectedSerializedFile = new File(requestDir, irMsg.getID());
        log.debug("Trying to delete stored serialized message: "
                + expectedSerializedFile.getAbsolutePath());
        if (!expectedSerializedFile.exists()) {
            log.warn("The file does not exist any more.");
            return;
        }
        boolean deleted = FileUtils.remove(expectedSerializedFile);
        if (!deleted) {
            log.debug("The file '" + expectedSerializedFile 
                    + "' was not deleted");
        }
    }

    /**
     * Helper method to check message properties. Will throw exceptions on any
     * trouble.
     * @param irMsg The message to check.
     * @throws ArgumentNotValid If message is not OK, or if the list of jobs or
     * the index request type is null.
     * @throws UnknownID If the index request type is of a form that is unknown
     * to the server.
     */
    private void checkMessage(final IndexRequestMessage irMsg) 
            throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkTrue(irMsg.isOk(), "Message was not OK");
        ArgumentNotValid.checkNotNull(irMsg.getRequestType(),
                "RequestType type");
        ArgumentNotValid.checkNotNull(irMsg.getRequestedJobs(),
                "Set<Long> jobIDs");
        if (handlers.get(irMsg.getRequestType()) == null) {
            throw new UnknownID("No handler known for requesttype "
                                + irMsg.getRequestType());
        }
    }

    /** Releases the JMS-connection and resets the singleton. */
    public void close() {
        cleanup();
    }

    /** Releases the JMS-connection and resets the singleton. */
    public void cleanup() {
        conn = JMSConnectionFactory.getInstance();
        conn.removeListener(Channels.getTheIndexServer(), this);
        handlers.clear();

        if (instance != null) {
            instance = null;
        }
    }
    
    /**
     * Look for stored messages to be preprocessed, and start processing those.
     * And start listening for index-requests.
     */
    public void start() {
        restoreRequestsfromRequestDir();
        log.info("" + currentJobs.size()
                + " indexing jobs in progress that was stored in requestdir: " 
                + requestDir.getAbsolutePath() );
        conn = JMSConnectionFactory.getInstance();
        
        if (maxConcurrentJobs > currentJobs.size()) {
            log.info("Enabling listening to the indexserver-queue");
            conn.setListener(Channels.getTheIndexServer(), this);
            isListening.set(true);
            log.info("Index request server is listening for requests on "
                    + "channel '"
                    + Channels.getTheIndexServer() + "'");

        } else {
            log.info("Currently full occupied with indexjobs stored in the " 
                    + "requestdirectory");
            isListening.set(false);
        }
    }
}
