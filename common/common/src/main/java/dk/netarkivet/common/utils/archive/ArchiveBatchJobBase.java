/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.batch.FileBatchJob;

public abstract class ArchiveBatchJobBase extends FileBatchJob {

	/** The total number of records processed. */
    protected int noOfRecordsProcessed = 0;

    /**
     * Initialize the job before running.
     * This is called before the processRecord() calls start coming.
     * @param os The OutputStream to which output data is written
     */
    public abstract void initialize(OutputStream os);

    /**
     * Finish up the job.
     * This is called after the last processRecord() call.
     * @param os The OutputStream to which output data is written
     */
    public abstract void finish(OutputStream os);

    /**
     * Private method that handles our exception.
     * @param e the given exception
     * @param archiveFile The archive file where the exception occurred.
     * @param index The offset in the archive file where the exception occurred.
     */
    protected void handleOurException(
            NetarkivetException e, File archiveFile, long index) {
        handleException(e, archiveFile, index);
    }

    /**
     * When the org.archive.io.arc classes throw IOExceptions while reading,
     * this is where they go. Subclasses are welcome to override the default
     * functionality which simply logs and records them in a list.
     * TODO: Actually use the archive file/index entries in the exception list
     *
     * @param e An Exception thrown by the org.archive.io.arc classes.
     * @param archiveFile The archive file that was processed while the Exception was thrown
     * @param index The index (in the archive file) at which the Exception was thrown
     * @throws ArgumentNotValid if e is null
     */
    public void handleException(Exception e, File archiveFile, long index)
      throws ArgumentNotValid{
        ArgumentNotValid.checkNotNull(e, "e");
        
        Log log = LogFactory.getLog(getClass().getName());
        log.debug("Caught exception while running batch job " + "on file "
                + archiveFile + ", position " + index + ":\n" + e.getMessage(), e);
        addException(archiveFile, index, ExceptionOccurrence.UNKNOWN_OFFSET, e);
    }

    /**
     * Returns a representation of the list of Exceptions recorded for this
     * archive batch job.
     * If called by a subclass, a method overriding handleException()
     * should always call super.handleException().
     *
     * @return All Exceptions passed to handleException so far.
     */
    public Exception[] getExceptionArray() {
        List<ExceptionOccurrence> exceptions = getExceptions();
        Exception[] exceptionList = new Exception[exceptions.size()];
        int i = 0;
        for (ExceptionOccurrence e : exceptions) {
            exceptionList[i++] = e.getException();
        }
        return exceptionList;
    }

    /**
     * Returns the number of records processed.
     * @return the number of records processed.
     */
    public int noOfRecordsProcessed() {
        return noOfRecordsProcessed;
    }

}
