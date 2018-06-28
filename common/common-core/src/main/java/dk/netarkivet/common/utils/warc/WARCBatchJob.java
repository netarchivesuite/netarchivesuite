/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.WARCBatchFilter;

/**
 * Abstract class defining a batch job to run on a set of WARC files. Each implementation is required to define
 * initialize() , processRecord() and finish() methods. The bitarchive application then ensures that the batch job run
 * initialize(), runs processRecord() on each record in each file in the archive, and then runs finish().
 */
@SuppressWarnings({"serial"})
public abstract class WARCBatchJob extends FileBatchJob {

    private static final Logger log = LoggerFactory.getLogger(WARCBatchJob.class);

    /** The total number of records processed. */
    protected int noOfRecordsProcessed = 0;

    /**
     * Initialize the job before running. This is called before the processRecord() calls start coming.
     *
     * @param os The OutputStream to which output data is written
     */
    public abstract void initialize(OutputStream os);

    /**
     * Exceptions should be handled with the handleException() method.
     *
     * @param os The OutputStream to which output data is written
     * @param record the object to be processed.
     */
    public abstract void processRecord(WARCRecord record, OutputStream os);

    /**
     * Finish up the job. This is called after the last processRecord() call.
     *
     * @param os The OutputStream to which output data is written
     */
    public abstract void finish(OutputStream os);

    /**
     * returns a BatchFilter object which restricts the set of warc records in the archive on which this batch-job is
     * performed. The default value is a neutral filter which allows all records.
     *
     * @return A filter telling which records should be given to processRecord().
     */
    public WARCBatchFilter getFilter() {
        return WARCBatchFilter.NO_FILTER;
    }

    /**
     * Accepts only WARC and WARCGZ files. Runs through all records and calls processRecord() on every record that is
     * allowed by getFilter(). Does nothing on a non-arc file.
     *
     * @param warcFile The WARC or WARCGZ file to be processed.
     * @param os the OutputStream to which output is to be written
     * @return true, if file processed successful, otherwise false
     * @throws ArgumentNotValid if either argument is null
     */
    public final boolean processFile(File warcFile, OutputStream os) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(warcFile, "warcFile");
        ArgumentNotValid.checkNotNull(os, "os");
        long arcFileIndex = 0;
        boolean success = true;
        log.info("Processing WARCfile: {}", warcFile.getName());

        try { // This outer try-catch block catches all unexpected exceptions
              // Create an WARCReader and retrieve its Iterator:
            WARCReader warcReader = null;

            try {
                warcReader = WARCReaderFactory.get(warcFile);
            } catch (IOException e) { // Some IOException
                handleException(e, warcFile, arcFileIndex);

                return false; // Can't process file after exception
            }

            try {
                Iterator<? extends ArchiveRecord> it = warcReader.iterator();
                /* Process all records from this Iterator: */
                log.debug("Starting processing records in WARCfile '{}'.", warcFile.getName());
                if (!it.hasNext()) {
                    log.debug("No WARCRecords found in WARCfile '{}'.", warcFile.getName());
                }
                WARCRecord record = null;
                while (it.hasNext()) {
                    log.trace("At begin of processing-loop");
                    // Get a record from the file
                    record = (WARCRecord) it.next();
                    // Process with the job
                    try {
                        if (!getFilter().accept(record)) {
                            continue;
                        }
                        log.debug("Processing WARCRecord #{} in WARCfile '{}'.", noOfRecordsProcessed,
                                warcFile.getName());
                        processRecord(record, os);
                        ++noOfRecordsProcessed;
                    } catch (NetarkivetException e) {
                        // Our exceptions don't stop us
                        success = false;

                        // With our exceptions, we assume that just the
                        // processing of this record got stopped, and we can
                        // easily find the next
                        handleOurException(e, warcFile, arcFileIndex);
                    } catch (Exception e) {
                        success = false; // Strange exceptions do stop us

                        handleException(e, warcFile, arcFileIndex);
                        // With strange exceptions, we don't know
                        // if we've skipped records
                        break;
                    }
                    // Close the record
                    try {
                        // TODO maybe this works, maybe not...
                        long arcRecordOffset = record.getHeader().getContentBegin() + record.getHeader().getLength();
                        record.close();
                        arcFileIndex = arcRecordOffset;
                    } catch (IOException ioe) { // Couldn't close an WARCRecord
                        success = false;

                        handleException(ioe, warcFile, arcFileIndex);
                        // If close fails, we don't know if we've skipped
                        // records
                        break;
                    }
                    log.trace("At end of processing-loop");
                }
            } finally {
                try {
                    warcReader.close();
                } catch (IOException e) { // Some IOException
                    // TODO Discuss whether exceptions on close cause
                    // filesFailed addition
                    handleException(e, warcFile, arcFileIndex);
                }
            }
        } catch (Exception unexpectedException) {
            handleException(unexpectedException, warcFile, arcFileIndex);
            return false;
        }
        return success;
    }

    /**
     * Private method that handles our exception.
     *
     * @param e the given exception
     * @param warcFile The WARC File where the exception occurred.
     * @param index The offset in the WARC File where the exception occurred.
     */
    private void handleOurException(NetarkivetException e, File warcFile, long index) {
        handleException(e, warcFile, index);
    }

    /**
     * When the org.archive.io.arc classes throw IOExceptions while reading, this is where they go. Subclasses are
     * welcome to override the default functionality which simply logs and records them in a list. TODO Actually use the
     * warcfile/index entries in the exception list
     *
     * @param e An Exception thrown by the org.archive.io.arc classes.
     * @param warcfile The arcFile that was processed while the Exception was thrown
     * @param index The index (in the WARC file) at which the Exception was thrown
     * @throws ArgumentNotValid if e is null
     */
    public void handleException(Exception e, File warcfile, long index) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(e, "e");

        log.debug("Caught exception while running batch job on file {}, position {}:\n{}", warcfile, index,
                e.getMessage(), e);
        addException(warcfile, index, ExceptionOccurrence.UNKNOWN_OFFSET, e);
    }

    /**
     * Returns a representation of the list of Exceptions recorded for this WARC batch job. If called by a subclass, a
     * method overriding handleException() should always call super.handleException().
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
     * @return the number of records processed.
     */
    public int noOfRecordsProcessed() {
        return noOfRecordsProcessed;
    }

}
