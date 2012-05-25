package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/**
 * Abstract class defining a batch job to run on a set of ARC/WARC files.
 * Each implementation is required to define initialize() , processRecord() and
 * finish() methods. The bitarchive application then ensures that the batch
 * job run initialize(), runs processRecord() on each record in each file in
 * the archive, and then runs finish().
 */
public abstract class ArchiveBatchJob extends ArchiveBatchJobBase {

	/**
	 * UID.
	 */
	private static final long serialVersionUID = -6776271561735259429L;

    /**
     * Exceptions should be handled with the handleException() method.
     * @param os The OutputStream to which output data is written
     * @param record the object to be processed.
     */
    public abstract void processRecord(ArchiveRecordBase record, OutputStream os);

    /**
     * returns a BatchFilter object which restricts the set of warc records
     * in the archive on which this batch-job is performed. The default value
     * is a neutral filter which allows all records.
     *
     * @return A filter telling which records should be given to
     * processRecord().
     */
    public ArchiveBatchFilter getFilter() {
        return ArchiveBatchFilter.NO_FILTER;
    }

    /**
     * Accepts only WARC and WARCGZ files. Runs through all records and calls
     * processRecord() on every record that is allowed by getFilter().
     * Does nothing on a non-arc file.
     *
     * @param archiveFile The WARC or WARCGZ file to be processed.
     * @param os the OutputStream to which output is to be written
     * @throws ArgumentNotValid if either argument is null
     * @return true, if file processed successful, otherwise false
     */
    public final boolean processFile(File archiveFile, OutputStream os) throws
            ArgumentNotValid{
        ArgumentNotValid.checkNotNull(archiveFile, "archiveFile");
        ArgumentNotValid.checkNotNull(os, "os");
        Log log = LogFactory.getLog(getClass().getName());
        long arcFileIndex = 0;
        boolean success = true;
        log.info("Processing WARCfile: " + archiveFile.getName());

        try { // This outer try-catch block catches all unexpected exceptions
            //Create an WARCReader and retrieve its Iterator:
            WARCReader warcReader = null;

            try {
                warcReader = WARCReaderFactory.get(archiveFile);
            } catch (IOException e) { //Some IOException
                handleException(e, archiveFile, arcFileIndex);

                return false; // Can't process file after exception
            }
            
            try {
                Iterator<? extends ArchiveRecord> it = warcReader.iterator();
                /* Process all records from this Iterator: */
                log.debug("Starting processing records in WARCfile '"
                        + archiveFile.getName() + "'.");
                if (!it.hasNext()) {
                    log.debug("No WARCRecords found in WARCfile '"
                            + archiveFile.getName() + "'.");
                }
                WARCRecord record = null;
                while (it.hasNext()) {
                    log.trace("At begin of processing-loop");
                    // Get a record from the file
                    record = (WARCRecord) it.next();
                    // Process with the job
                    try {
                    	// NICL do
                    	/*
                        if (!getFilter().accept(record)) {
                            continue;
                        }
                        */
                        log.debug(
                                "Processing WARCRecord #" + noOfRecordsProcessed
                                + " in WARCfile '" + archiveFile.getName()  + "'.");
                        // NICL do
                        //processRecord(record, os);
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
                    	// TODO maybe this works, maybe not...
                        long arcRecordOffset =
                                record.getHeader().getContentBegin() 
                                + record.getHeader().getLength();
                        record.close();
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
                    warcReader.close();
                } catch (IOException e) { //Some IOException
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
