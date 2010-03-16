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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * Message for requesting and index from the index server, and for giving back
 * the reply.
 */
public class IndexRequestMessage extends ArchiveMessage {
    /** The log.*/
    private transient Log log
            = LogFactory.getLog(IndexRequestMessage.class.getName());
    /**
     * List of jobs for which an index is requested. Should always be set.
     */
    private Set<Long> requestedJobs;
    /**
     * Type of index is requested. Should always be set.
     */
    private RequestType requestType;
    /**
     * List of jobs for which an index _can_ be generated. Should only be set on
     * reply. Should always be a subset of requestedJobs. If This set is equal
     * to the requested set, resultFile should also be set.
     */
    private Set<Long> foundJobs;

    /** The list of files that make up the generated index.
     * Should only be set on reply, and only if index was generated for all
     * files
     *
     * if indexIsStoredInDirectory is false, this list must contain exactly one
     * file (or not have been set yet).
     */
    private List<RemoteFile> resultFiles;

    /** If true, the underlying cache uses a directory to store its files
     * (which may be zero or more files), otherwise just a single file is
     * used.
     */
    private boolean indexIsStoredInDirectory;

    /**
     * Generate an index request message. Receiver is always the index server
     * channel, replyto is always this index client.
     *
     * @param requestType Type of index requested.
     * @param jobSet Jobs for which the index is requested.
     * @throws ArgumentNotValid if wither argument is null.
     */
    public IndexRequestMessage(RequestType requestType, Set<Long> jobSet) 
            throws ArgumentNotValid {
        super(Channels.getTheIndexServer(), Channels.getThisIndexClient());
        ArgumentNotValid.checkNotNull(requestType, "RequestType requestType");
        ArgumentNotValid.checkNotNull(jobSet, "Set<Long> jobSet");
        //Note: Copy the set, since the received set may not be serializable.
        this.requestedJobs = new HashSet<Long>(jobSet);
        this.requestType = requestType;
    }

    /**
     * Calls visit on the visitor.
     * @param v The visitor of this message.
     * @see ArchiveMessageVisitor
     */
    public void accept(ArchiveMessageVisitor v) {
        v.visit(this);
    }

    /**
     * Get list of requested jobs. Should never return null.
     *
     * @return Set of jobs for which an index is requested.
     */
    public Set<Long> getRequestedJobs() {
        return requestedJobs;
    }

    /**
     * Get the request type. Should never be null.
     *
     * @return Type of index requested.
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * Get the set of jobs for which the index is found. This should always be
     * set on replies, and should always be a subset of the jobs requested. If
     * set of jobs found jobs is the same as the set of requested jobs, the
     * index file should also be present.
     *
     * @return Set of jobs for which the index is found.
     */
    public Set<Long> getFoundJobs() {
        return foundJobs;
    }

    /**
     * On reply, set the set of jobs for which an index is found. This should
     * always be set on replies, and should always be a subset of the jobs
     * requested. If set of jobs found jobs is the same as the set of requested
     * jobs, the index file should also be set.
     *
     * @param foundJobs The set of jobs for which the index is found
     * @throws ArgumentNotValid on null argument
     */
    public void setFoundJobs(Set<Long> foundJobs) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(foundJobs, "Set<Long> foundJobs");
        //Note: Copy the set, since the received set may not be serializable.
        this.foundJobs = new HashSet<Long>(foundJobs);
    }
    /**
     * The index over the requested jobs. Only set on replies, and only if
     * foundJobs is the same set as requestedJobs.
     *
     * @return index of requested jobs.
     * @throws IllegalState if this message is a multiFile message.
     */
    public RemoteFile getResultFile() throws IllegalState {
        if (resultFiles != null) {
            if (isIndexIsStoredInDirectory()) {
                throw new IllegalState("This message carries multiple result "
                        + "files: " + resultFiles);
            }
            return resultFiles.get(0);
        } else {
            return null;
        }
    }

    /** Returns the list of result files for the requested jobs.
     *
     * @return index of requested jobs in the form of several possibly
     * co-dependent files.
     * @throws IllegalState if this message is not a multiFile message.
     */
    public List<RemoteFile> getResultFiles() throws IllegalState {
        if (resultFiles != null) {
            if (!isIndexIsStoredInDirectory()) {
                throw new IllegalState("This message carries a single result "
                        + "file: '" + resultFiles.get(0) + "'");
            }
            return resultFiles;
        } else {
            return null;
        }
    }

    /**
     * On reply, set remote file containing index of requested jobs. Should
     * _only_ be set when an index over ALL requested jobs is present.
     *
     * @param resultFile RemoteFile containing index over requested jobs.
     * @throws ArgumentNotValid on null argument.
     * @throws IllegalState if the result file has already been set.
     */
    public void setResultFile(RemoteFile resultFile) throws IllegalState, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(resultFile, "RemoteFile resultFile");
        if (this.resultFiles != null) {
            throw new IllegalState(this + " already has result files "
                    + this.resultFiles + " set.");
        }
        resultFiles = new ArrayList<RemoteFile>(1);
        resultFiles.add(resultFile);
        indexIsStoredInDirectory = false;
    }

    /** Set several result files making up an index of requested jobs. Should
     * _only_ be set when an index over ALL requested jobs is present.
     *
     * @param resultFiles RemoteFiles containing index over requested jobs.
     * @throws ArgumentNotValid on null argument or null element in list.
     * @throws IllegalState if the result files have already been set.
     * */
    public void setResultFiles(List<RemoteFile> resultFiles) 
            throws IllegalState, ArgumentNotValid {
        ArgumentNotValid.checkNotNull(resultFiles,
                "List<RemoteFile> resultFiles");
        for (RemoteFile rf : resultFiles) {
            if (rf == null) {
                throw new ArgumentNotValid("List of result files contains"
                        + " a null element: " + resultFiles);
            }
        }
                if (this.resultFiles != null) {
            throw new IllegalState(this + " already has result files "
                    + this.resultFiles + " set.");
        }
        log.debug("Sending result containing "
                + resultFiles.size() + " files");
        this.resultFiles = resultFiles;
        indexIsStoredInDirectory = true;
    }

    /** If true, this message may carry multiple files that should be
     * stored in a directory.
     *
     * @return True if more than one file may be transferred with this message.
     */
    public boolean isIndexIsStoredInDirectory() {
        return indexIsStoredInDirectory;
    }

    /**
     * Invoke default method for deserializing object, and reinitialise the
     * logger.
     *
     * @param s The stream the object is read from.
     */
    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during deserialization", e);
        }
        log = LogFactory.getLog(getClass().getName());
    }

    /**
     * Invoke default method for serializing object.
     *
     * @param s The stream the object is written to.
     */
    private void writeObject(ObjectOutputStream s) {
        try {
            s.defaultWriteObject();
        } catch (Exception e) {
            throw new IOFailure("Unexpected error during serialization", e);
        }
    }

}
