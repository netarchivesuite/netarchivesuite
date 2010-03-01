/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.archive.indexserver.distribute;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.archive.indexserver.FileBasedCache;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.StringUtils;

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

    /** Initialise index request server with no handlers, listening to the
     * index JMS channel.
     */
    private IndexRequestServer() {
        JMSConnection conn = JMSConnectionFactory.getInstance();
        conn.setListener(Channels.getTheIndexServer(), this);
        log.info("Index request server is listening for requests on channel '"
                 + Channels.getTheIndexServer() + "'");

        handlers = new EnumMap<RequestType, FileBasedCache<Set<Long>>>(
                RequestType.class);
    }

    /** Get the unique index request server instance.
     *
     * @return The index request server.
     */
    public static IndexRequestServer getInstance() {
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
    public void visit(final IndexRequestMessage irMsg) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(irMsg, "IndexRequestMessage irMsg");

        //Start a new thread to handle the actual request.
        new Thread(){
            public void run() {
                doGenerateIndex(irMsg);
            }
        }.start();
    }

    /**
     * Method that handles generating an index; supposed to be run in its own
     * thread, because it blocks while the index is generated.
     * @see #visit(IndexRequestMessage)
     * @param irMsg A message requesting an index
     */
    private void doGenerateIndex(final IndexRequestMessage irMsg) {
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
                if (cacheFile.isDirectory()) {
                    // This cache uses multiple files stored in a directory,
                    // so transfer them all.
                    File[] cacheFiles = cacheFile.listFiles();
                    List<RemoteFile> resultFiles = new ArrayList<RemoteFile>(
                            cacheFiles.length);
                    for (File f : cacheFiles) {
                        resultFiles.add(
                                RemoteFileFactory.getCopyfileInstance(f));
                    }
                    irMsg.setResultFiles(resultFiles);
                } else {
                    irMsg.setResultFile(RemoteFileFactory.getCopyfileInstance(
                            cacheFile));
                }
            } else {
                log.warn("Failed generating index of type '" + type
                         + "' for the jobs [" + StringUtils.conjoin(",", jobIDs)
                         + "], only the jobs ["
                         + StringUtils.conjoin(",", foundIDs)
                         + "] are available.");
            }
        } catch (Exception e) {
            log.warn(
                    "Unable to generate index for jobs ["
                    + StringUtils.conjoin(",", irMsg.getRequestedJobs()) + "]",
                    e);
            irMsg.setNotOk(e);
        } finally {
            String state = "failed";
            if (irMsg.isOk()) {
                state = "successful";
            }
            log.info("Sending " + state 
                    + " reply for IndexRequestMessage"
                    //"with ID='" + irMsg.getReplyOfId()
                    + " back to sender '"
                    + irMsg.getReplyTo() + "'."); 
            JMSConnectionFactory.getInstance().reply(irMsg);
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
        JMSConnection conn = JMSConnectionFactory.getInstance();
        conn.removeListener(Channels.getTheIndexServer(), this);
        handlers.clear();

        if (instance != null) {
            instance = null;
        }
    }
}
