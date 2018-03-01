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
package dk.netarkivet.harvester.indexserver.distribute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.RemoteFileSettings;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;
import dk.netarkivet.harvester.indexserver.FileBasedCache;
import dk.netarkivet.harvester.indexserver.IndexRequestServerInterface;

/**
 * Index request server singleton.
 * <p>
 * This class contains a singleton that handles requesting an index over JMS.
 * <p>
 * It will ALWAYS reply to such messages, either with the index, a message telling that only a subset is available, and
 * which, or an error message,
 */
public final class IndexRequestServer extends HarvesterMessageHandler implements CleanupIF, IndexRequestServerInterface {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(IndexRequestServer.class);

    /** The unique instance. */
    private static IndexRequestServer instance;
    /** The handlers for index request types. */
    private Map<RequestType, FileBasedCache<Set<Long>>> handlers;

    /** The connection to the JMSBroker. */
    private static JMSConnection conn;
    /** A set with the current indexing jobs in progress. */
    private static Map<String, IndexRequestMessage> currentJobs;

    /** The max number of concurrent jobs. */
    private static long maxConcurrentJobs;
    /** Are we listening, now. */
    private static AtomicBoolean isListening = new AtomicBoolean();

    /** Interval in milliseconds between listening checks. */
    private static long listeningInterval;
    /** The timer that initiates the checkIflisteningTask. */
    private Timer checkIflisteningTimer = new Timer();

    /** satisfactoryThreshold percentage as an integer. */
    private int satisfactoryThresholdPercentage;

    /**
     * The directory to store backup copies of the currentJobs. In case of the indexserver crashing.
     */
    private File requestDir;

    /**
     * Initialise index request server with no handlers, listening to the index JMS channel.
     */
    private IndexRequestServer() {
        maxConcurrentJobs = Settings.getLong(HarvesterSettings.INDEXSERVER_INDEXING_MAXCLIENTS);
        requestDir = Settings.getFile(HarvesterSettings.INDEXSERVER_INDEXING_REQUESTDIR);
        listeningInterval = Settings.getLong(HarvesterSettings.INDEXSERVER_INDEXING_LISTENING_INTERVAL);
        satisfactoryThresholdPercentage = Settings
                .getInt(HarvesterSettings.INDEXSERVER_INDEXING_SATISFACTORYTHRESHOLD_PERCENTAGE);

        currentJobs = new HashMap<String, IndexRequestMessage>();
        handlers = new EnumMap<RequestType, FileBasedCache<Set<Long>>>(RequestType.class);
        conn = JMSConnectionFactory.getInstance();
        checkIflisteningTimer = new Timer();
    }

    /**
     * Restore old requests from requestDir.
     */
    private void restoreRequestsfromRequestDir() {
        if (!requestDir.exists()) {
            log.info("requestdir not found: creating request dir");
            if (!requestDir.mkdirs()) {
                throw new IOFailure("Unable to create requestdir '" + requestDir.getAbsolutePath() + "'");
            } else {
                return; // requestdir was just created, so nothing to do
            }
        }

        File[] requests = requestDir.listFiles();
        // Fill up the currentJobs
        for (File request : requests) {
            if (request.isFile()) {
                final IndexRequestMessage msg = restoreMessage(request);
                synchronized (currentJobs) {
                    if (!currentJobs.containsKey(msg.getID())) {
                        currentJobs.put(msg.getID(), msg);
                    } else {
                        log.debug("Skipped message w/id='{}'. Already among current jobs", msg.getID());
                        continue;
                    }

                }
                // Start a new thread to handle the actual request.
                new Thread() {
                    public void run() {
                        doProcessIndexRequestMessage(msg);
                    }
                }.start();
                log.info("Restarting indexjob w/ ID={}", msg.getID());
            } else {
                log.debug("Ignoring directory in requestdir: " + request.getAbsolutePath());
            }
        }
    }

    /**
     * Get the unique index request server instance.
     *
     * @return The index request server.
     */
    public static synchronized IndexRequestServer getInstance() {
        if (instance == null) {
            instance = new IndexRequestServer();
        }

        return instance;
    }

    /**
     * Set handler for certain type of index request. If called more than once, new handler overwrites old one.
     *
     * @param t The type of index requested
     * @param handler The handler that should handle this request.
     */
    public void setHandler(RequestType t, FileBasedCache<Set<Long>> handler) {
        ArgumentNotValid.checkNotNull(t, "RequestType t");
        ArgumentNotValid.checkNotNull(handler, "FileBasedCache<Set<Long>> handler");
        log.info("Setting handler for RequestType: {}", t);
        handlers.put(t, handler);
    }

    /**
     * Given a request for an index over a set of job ids, use a cache to try to create the index, Then reply result.
     * <p>
     * If for any reason not all requested jobs can be indexed, return the subset. The client can then retry with this
     * subset, in order to get index of that subset.
     * <p>
     * Values read from the message in order to handle this: - Type of index requested - will use the index cache of
     * this type - Set of job IDs - which jobs to generate index for
     * <p>
     * Values written to message before replying: - The subset indexed - may be the entire set. ALWAYS set unless reply
     * !OK - File with index - ONLY if subset is entire set, the index requested.
     * <p>
     * This method should ALWAYS reply. May reply with not OK message if: - Message received was not OK - Request type
     * is null or unknown in message - Set of job ids is null in message - Cache generation throws exception
     *
     * @param irMsg A message requesting an index.
     * @throws ArgumentNotValid on null parameter
     */
    public synchronized void visit(final IndexRequestMessage irMsg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(irMsg, "IndexRequestMessage irMsg");
        // save new msg to requestDir
        try {
            saveMsg(irMsg);
            synchronized (currentJobs) {
                if (!currentJobs.containsKey(irMsg.getID())) {
                    currentJobs.put(irMsg.getID(), irMsg);
                } else {
                    final String errMsg = "Should not happen. Skipping msg w/ id= '" + irMsg.getID()
                            + "' because already among current jobs. "
                            + "Unable to initiate indexing. Sending failed message back to sender";
                    log.warn(errMsg);
                    irMsg.setNotOk(errMsg);
                    JMSConnectionFactory.getInstance().reply(irMsg);
                    return;
                }
            }
            // Limit the number of concurrently indexing job
            if (currentJobs.size() >= maxConcurrentJobs) {
                if (isListening.get()) {
                    conn.removeListener(Channels.getTheIndexServer(), this);
                    isListening.set(false);
                }
            }

            // Start a new thread to handle the actual request.
            new Thread() {
                public void run() {
                    doProcessIndexRequestMessage(irMsg);
                }
            }.start();
            log.debug("Now {} indexing jobs in progress", currentJobs.size());
        } catch (IOException e) {
            final String errMsg = "Unable to initiate indexing. Send failed message back to sender: " + e;
            log.warn(errMsg, e);
            irMsg.setNotOk(errMsg);
            JMSConnectionFactory.getInstance().reply(irMsg);
        }
    }

    /**
     * Save a IndexRequestMessage to disk.
     *
     * @param irMsg A message to store to disk
     * @throws IOException Throws IOExecption, if unable to save message
     */
    private void saveMsg(IndexRequestMessage irMsg) throws IOException {
        File dest = new File(requestDir, irMsg.getID());
        log.debug("Storing message to {}", dest.getAbsolutePath());
        // Writing message to file
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fos = new FileOutputStream(dest);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(irMsg);
        } finally {
            IOUtils.closeQuietly(oos);
        }

    }

    /**
     * Restore message from serialized state.
     *
     * @param serializedObject the object stored as a file.
     * @return the restored message.
     */
    private IndexRequestMessage restoreMessage(File serializedObject) {
        Object obj = null;
        ObjectInputStream ois = null;
        try {
            // Read the message from disk.
            FileInputStream fis = new FileInputStream(serializedObject);
            ois = new ObjectInputStream(fis);

            obj = ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalState("Not possible to read the stored message from file '"
                    + serializedObject.getAbsolutePath() + "':", e);
        } catch (IOException e) {
            throw new IOFailure("Not possible to read the stored message from file '"
                    + serializedObject.getAbsolutePath() + "':", e);
        } finally {
            IOUtils.closeQuietly(ois);
        }

        if (obj instanceof IndexRequestMessage) {
            return (IndexRequestMessage) obj;
        } else {
            throw new IllegalState("The serialized message is not a " + IndexRequestMessage.class.getName() + " but a "
                    + obj.getClass().getName());
        }
    }

    /**
     * Method that handles the processing of an indexRequestMessage. Returns the requested index immediately, if already
     * available, otherwise proceeds with the index generation of the requested index. Must be run in its own thread,
     * because it blocks while the index is generated.
     *
     * @param irMsg A message requesting an index
     * @see #visit(IndexRequestMessage)
     */
    private void doProcessIndexRequestMessage(final IndexRequestMessage irMsg) {
        final boolean mustReturnIndex = irMsg.mustReturnIndex();
        try {
            checkMessage(irMsg);
            RequestType type = irMsg.getRequestType();
            Set<Long> jobIDs = irMsg.getRequestedJobs();

            if (log.isInfoEnabled()) {
                log.info("Request received for an index of type '{}' for the {} jobs [{}]", type, jobIDs.size(),
                        StringUtils.conjoin(",", jobIDs));
            }
            FileBasedCache<Set<Long>> handler = handlers.get(type);

            // Here we need to make sure that we don't accidentally process more than
            // one message at the time before the whole process is over
            List<Long> sortedList = new ArrayList<Long>(jobIDs);
            String allIDsString = StringUtils.conjoin("-", sortedList);
            String checksum = ChecksumCalculator.calculateMd5(allIDsString.getBytes());
            log.debug("Waiting to enter the synchronization zone for the indexing job of size {} with checksum '{}'",
                    jobIDs.size(), checksum);
            // Begin synchronization
            synchronized (checksum.intern()) {
                log.debug("The indexing job of size {} with checksum '{}' is now in the synchronization zone",
                        jobIDs.size(), checksum);
                Set<Long> foundIDs = handler.cache(jobIDs);
                irMsg.setFoundJobs(foundIDs);
                if (foundIDs.equals(jobIDs)) {
                    if (log.isInfoEnabled()) {
                        log.info("Retrieved successfully index of type '{}' for the {} jobs [{}]", type, jobIDs.size(),
                                StringUtils.conjoin(",", jobIDs));
                    }
                    File cacheFile = handler.getCacheFile(jobIDs);
                    if (mustReturnIndex) { // return index now!
                        packageResultFiles(irMsg, cacheFile);
                    }
                } else if (satisfactoryTresholdReached(foundIDs, jobIDs)) {
                    log.info("Data for full index w/ {} jobs not available. Only found data for {} jobs - "
                            + "but satisfactoryTreshold reached, so assuming presence of all data", jobIDs.size(),
                            foundIDs.size());
                    // Make sure that the index of the data available is generated
                    Set<Long> theFoundIDs = handler.cache(foundIDs);
                    // TheFoundIDS should be identical to foundIDs
                    // Lets make sure of that
                    Set<Long> diffSet = new HashSet<Long>(foundIDs);
                    diffSet.removeAll(theFoundIDs);

                    // Make a copy of the index available, and give it the name of
                    // the index cache file wanted.
                    File cacheFileWanted = handler.getCacheFile(jobIDs);
                    File cacheFileCreated = handler.getCacheFile(foundIDs);

                    log.info("Satisfactory threshold reached - copying index {} '{}' to full index: {}",
                            (cacheFileCreated.isDirectory() ? "dir" : "file"), cacheFileCreated.getAbsolutePath(),
                            cacheFileWanted.getAbsolutePath());
                    if (cacheFileCreated.isDirectory()) {
                        // create destination cacheFileWanted, and
                        // copy all files in cacheFileCreated to cacheFileWanted.
                        cacheFileWanted.mkdirs();
                        FileUtils.copyDirectory(cacheFileCreated, cacheFileWanted);
                    } else {
                        FileUtils.copyFile(cacheFileCreated, cacheFileWanted);
                    }

                    // TODO This delete-operation commented out, because it is deemed too dangerous,
                    // as the cachedir represented by cacheFileCreated may still be used

                    // log.info("Deleting the temporary index "
                    // + cacheFileCreated.getAbsolutePath());
                    // FileUtils.removeRecursively(cacheFileCreated);
                    log.info("We keep the index '{}', as we don't know if anybody is using it",
                            cacheFileCreated.getAbsolutePath());

                    // Information needed by recipient to store index in local cache
                    irMsg.setFoundJobs(jobIDs);
                    if (mustReturnIndex) { // return index now.
                        packageResultFiles(irMsg, cacheFileWanted);
                    }
                } else {
                    Set<Long> missingJobIds = new HashSet<Long>(jobIDs);
                    missingJobIds.removeAll(foundIDs);
                    log.warn("Failed generating index of type '{}' for the jobs [{}]. Missing data for jobs [{}].",
                            type, StringUtils.conjoin(",", jobIDs), StringUtils.conjoin(",", missingJobIds));
                }

            } // End of synchronization block
        } catch (Throwable t) {
            log.warn("Unable to generate index for jobs [" + StringUtils.conjoin(",", irMsg.getRequestedJobs()) + "]",
                    t);
            irMsg.setNotOk(t);
        } finally {
            // Remove job from currentJobs Set
            synchronized (currentJobs) {
                currentJobs.remove(irMsg.getID());
            }
            // delete stored message
            deleteStoredMessage(irMsg);
            String state = "failed";
            if (irMsg.isOk()) {
                state = "successful";
            }
            if (mustReturnIndex) {
                log.info("Sending {} reply for IndexRequestMessage back to sender '{}'.", state, irMsg.getReplyTo());
                JMSConnectionFactory.getInstance().reply(irMsg);
            } else {
                log.info("Sending {} IndexReadyMessage to Scheduler for harvest {}", state, irMsg.getHarvestId());
                boolean isindexready = true;
                if (state.equalsIgnoreCase("failed")) {
                    isindexready = false;
                }
                IndexReadyMessage irm = new IndexReadyMessage(irMsg.getHarvestId(), isindexready, irMsg.getReplyTo(),
                        Channels.getTheIndexServer());
                JMSConnectionFactory.getInstance().send(irm);
            }
        }
    }

    /**
     * Package the result files with the message reply.
     *
     * @param irMsg the message being answered
     * @param cacheFile The location of the result on disk.
     */
    private void packageResultFiles(IndexRequestMessage irMsg, File cacheFile) {
        RemoteFileSettings connectionParams = irMsg.getRemoteFileSettings();

        if (connectionParams != null) {
            log.debug("Trying to use client supplied RemoteFileServer: {}", connectionParams.getServerName());
        }
        if (cacheFile.isDirectory()) {
            // This cache uses multiple files stored in a directory,
            // so transfer them all.
            File[] cacheFiles = cacheFile.listFiles();
            List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(cacheFiles.length);
            for (File f : cacheFiles) {
                resultFiles.add(RemoteFileFactory.getCopyfileInstance(f, irMsg.getRemoteFileSettings()));
            }
            irMsg.setResultFiles(resultFiles);
        } else {
            irMsg.setResultFile(RemoteFileFactory.getCopyfileInstance(cacheFile, irMsg.getRemoteFileSettings()));
        }
    }

    /**
     * Threshold for when the created index contains enough data to be considered a satisfactory index. Uses the
     * {@link IndexRequestServer#satisfactoryThresholdPercentage}.
     *
     * @param foundIDs The list of IDs contained in the index
     * @param requestedIDs The list of IDs requested in the index.
     * @return true, if the ratio foundIDs/requestedIDs is above the
     * {@link IndexRequestServer#satisfactoryThresholdPercentage}.
     */
    private boolean satisfactoryTresholdReached(Set<Long> foundIDs, Set<Long> requestedIDs) {
        int jobsRequested = requestedIDs.size();
        int jobsFound = foundIDs.size();
        int percentage = (jobsFound * 100) / jobsRequested;
        if (percentage > satisfactoryThresholdPercentage) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Deleted stored file for given message.
     *
     * @param irMsg a given IndexRequestMessage
     */
    private void deleteStoredMessage(IndexRequestMessage irMsg) {
        File expectedSerializedFile = new File(requestDir, irMsg.getID());
        log.debug("Trying to delete stored serialized message: {}", expectedSerializedFile.getAbsolutePath());
        if (!expectedSerializedFile.exists()) {
            log.warn("The file does not exist any more.");
            return;
        }
        boolean deleted = FileUtils.remove(expectedSerializedFile);
        if (!deleted) {
            log.debug("The file '{}' was not deleted", expectedSerializedFile);
        }
    }

    /**
     * Helper method to check message properties. Will throw exceptions on any trouble.
     *
     * @param irMsg The message to check.
     * @throws ArgumentNotValid If message is not OK, or if the list of jobs or the index request type is null.
     * @throws UnknownID If the index request type is of a form that is unknown to the server.
     */
    private void checkMessage(final IndexRequestMessage irMsg) throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkTrue(irMsg.isOk(), "Message was not OK");
        ArgumentNotValid.checkNotNull(irMsg.getRequestType(), "RequestType type");
        ArgumentNotValid.checkNotNull(irMsg.getRequestedJobs(), "Set<Long> jobIDs");
        if (handlers.get(irMsg.getRequestType()) == null) {
            throw new UnknownID("No handler known for requesttype " + irMsg.getRequestType());
        }
    }

    /** Releases the JMS-connection and resets the singleton. */
    public void close() {
        cleanup();
    }

    /** Releases the JMS-connection and resets the singleton. */
    public void cleanup() {
        // shutdown listening timer.
        checkIflisteningTimer.cancel();
        conn.removeListener(Channels.getTheIndexServer(), this);
        handlers.clear();

        if (instance != null) {
            instance = null;
        }
    }

    /**
     * Look for stored messages to be preprocessed, and start processing those. And start the separate thread that
     * decides if we should listen for index-requests.
     */
    public void start() {
        restoreRequestsfromRequestDir();
        log.info("{} indexing jobs in progress that was stored in requestdir: {}", currentJobs.size(),
                requestDir.getAbsolutePath());

        // Define and start thread to observe current jobs:
        // Only job is to look at the isListening atomicBoolean.
        // If not listening, check if we are ready to listen again.
        TimerTask checkIfListening = new ListeningTask(this);
        isListening.set(false);
        checkIflisteningTimer.schedule(checkIfListening, 0L, listeningInterval);
    }

    /**
     * Defines the task to repeatedly check the listening status. And begin listening again, if we are ready for more
     * tasks.
     */
    private static class ListeningTask extends TimerTask {
        /** The indexrequestserver this task is associated with. */
        private IndexRequestServer thisIrs;

        /**
         * Constructor for the ListeningTask.
         *
         * @param irs The indexrequestserver this task should be associated with
         */
        ListeningTask(IndexRequestServer irs) {
            thisIrs = irs;
        }

        @Override
        public void run() {
            log.trace("Checking if we should be listening again");
            if (!isListening.get()) {
                if (maxConcurrentJobs > currentJobs.size()) {
                    log.info("Enabling listening to the indexserver channel '{}'", Channels.getTheIndexServer());
                    conn.setListener(Channels.getTheIndexServer(), thisIrs);
                    isListening.set(true);
                }
            }
        }

    }

}
