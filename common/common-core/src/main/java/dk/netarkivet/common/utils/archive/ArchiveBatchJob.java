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
package dk.netarkivet.common.utils.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/**
 * Abstract class defining a batch job to run on a set of ARC/WARC files. Each implementation is required to define
 * initialize() , processRecord() and finish() methods. The bitarchive application then ensures that the batch job runs
 * initialize(), runs processRecord() on each record in each file in the archive, and then runs finish().
 */
@SuppressWarnings({"serial"})
public abstract class ArchiveBatchJob extends ArchiveBatchJobBase {

    private static final Logger log = LoggerFactory.getLogger(ArchiveBatchJob.class);

    /**
     * Exceptions should be handled with the handleException() method.
     *
     * @param os The OutputStream to which output data is written
     * @param record the object to be processed.
     */
    public abstract void processRecord(ArchiveRecordBase record, OutputStream os);

    /**
     * Returns an ArchiveBatchFilter object which restricts the set of records in the archive on which this batch-job is
     * performed. The default value is a neutral filter which allows all records.
     *
     * @return A filter telling which records should be given to processRecord().
     */
    public ArchiveBatchFilter getFilter() {
        return ArchiveBatchFilter.NO_FILTER;
    }

    /**
     * Accepts only arc(.gz) and warc(.gz) files. Runs through all records and calls processRecord() on every record
     * that is allowed by getFilter(). Does nothing on a non-(w)arc file.
     *
     * @param archiveFile The arc(.gz) or warc(.gz) file to be processed.
     * @param os the OutputStream to which output is to be written
     * @return true, if file processed successful, otherwise false
     * @throws ArgumentNotValid if either argument is null
     */
    public final boolean processFile(File archiveFile, OutputStream os) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(archiveFile, "archiveFile");
        ArgumentNotValid.checkNotNull(os, "os");
        long arcFileIndex = 0;
        boolean success = true;
        log.info("Processing archive file: {}", archiveFile.getName());

        try { // This outer try-catch block catches all unexpected exceptions
              // Create an ArchiveReader and retrieve its Iterator:
            ArchiveReader archiveReader = null;

            try {
                archiveReader = ArchiveReaderFactory.get(archiveFile);
            } catch (IOException e) { // Some IOException
                handleException(e, archiveFile, arcFileIndex);

                return false; // Can't process file after exception
            }

            try {
                Iterator<? extends ArchiveRecord> it = archiveReader.iterator();
                /* Process all records from this Iterator: */
                log.debug("Starting processing records in archive file '{}'.", archiveFile.getName());
                if (!it.hasNext()) {
                    log.debug("No records found in archive file '{}'.", archiveFile.getName());
                }
                ArchiveRecord archiveRecord = null;
                ArchiveRecordBase record;
                while (it.hasNext()) {
                    log.trace("At begin of processing-loop");
                    // Get a record from the file
                    archiveRecord = (ArchiveRecord) it.next();
                    record = ArchiveRecordBase.wrapArchiveRecord(archiveRecord);
                    // Process with the job
                    try {
                        if (!getFilter().accept(record)) {
                            continue;
                        }
                        log.debug("Processing record #{} in archive file '{}'.", noOfRecordsProcessed,
                                archiveFile.getName());
                        processRecord(record, os);
                        ++noOfRecordsProcessed;
                    } catch (NetarkivetException e) {
                        // Our exceptions don't stop us
                        success = false;

                        // With our exceptions, we assume that just the
                        // processing of this record got stopped, and we can
                        // easily find the next
                        handleOurException(e, archiveFile, arcFileIndex);
                    } catch (Exception e) {
                        success = false; // Strange exceptions do stop us

                        handleException(e, archiveFile, arcFileIndex);
                        // With strange exceptions, we don't know
                        // if we've skipped records
                        break;
                    }
                    // Close the record
                    try {
                        /*
                         * // FIXME: Don't know how to compute this for warc-files // computation for arc-files: long
                         * arcRecordOffset = // record.getBodyOffset() + record.getMetaData().getLength(); //
                         * computation for warc-files (experimental) long arcRecordOffset =
                         * record.getHeader().getOffset();
                         */
                        // TODO maybe this works, maybe not...
                        long arcRecordOffset = archiveRecord.getHeader().getContentBegin()
                                + archiveRecord.getHeader().getLength();
                        archiveRecord.close();
                        arcFileIndex = arcRecordOffset;
                    } catch (IOException ioe) { // Couldn't close an WARCRecord
                        success = false;

                        handleException(ioe, archiveFile, arcFileIndex);
                        // If close fails, we don't know if we've skipped
                        // records
                        break;
                    }
                    log.trace("At end of processing-loop");
                }
            } finally {
                try {
                    archiveReader.close();
                } catch (IOException e) { // Some IOException
                    // TODO Discuss whether exceptions on close cause
                    // filesFailed addition
                    handleException(e, archiveFile, arcFileIndex);
                }
            }
        } catch (Exception unexpectedException) {
            handleException(unexpectedException, archiveFile, arcFileIndex);
            return false;
        }
        return success;
    }

}
