package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.batch.FileBatchJob;

public abstract class ArchiveBatchJobBase extends FileBatchJob {

	/**
	 * UID.
	 */
	private static final long serialVersionUID = 6009508388703527028L;

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
     * @param archiveFile The ARCFile where the exception occurred.
     * @param index The offset in the ARCFile where the exception occurred.
     */
    protected void handleOurException(
            NetarkivetException e, File archiveFile, long index) {
        handleException(e, archiveFile, index);
    }

    /**
     * When the org.archive.io.arc classes throw IOExceptions while reading,
     * this is where they go. Subclasses are welcome to override the default
     * functionality which simply logs and records them in a list.
     * TODO Actually use the arcfile/index entries in the exception list
     *
     * @param e An Exception thrown by the org.archive.io.arc classes.
     * @param archiveFile The arcFile that was processed while the Exception
     * was thrown
     * @param index The index (in the ARC file) at which the Exception
     * was thrown
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
     * ARC batch job.
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
     * 
     * @return the number of records processed.
     */
    public int noOfRecordsProcessed() {
        return noOfRecordsProcessed;
    }

}
