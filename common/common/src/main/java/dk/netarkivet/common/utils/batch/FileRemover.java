
package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.OutputStream;


/**
 * This class implements a batchjob that enables you to delete files
 * from an archive. Note that the default Java Security Policy distributed
 * with NetarchiveSuite does not allow this.
 * 
 * @author ngiraud
 * @author svc
 */
@SuppressWarnings({ "serial"})
public class FileRemover extends FileBatchJob {

    /**
     * The method to initialize the batchjob.
     * 
     * @param os
     *            The OutputStream to which output should be written
     * @see FileBatchJob#initialize(OutputStream)
     */
    @Override
    public void initialize(OutputStream os) {
    }

    /**
     * This method deletes the file in the argument list. Note that the default
     * Java Security Policy distributed with NetarchiveSuite does not allow
     * this.
     * 
     * @param file
     *            The file to be processed
     * @param os
     *            The OutputStream to which output should be written
     * @return true, if and only if the file is succesfully deleted.
     * @see FileBatchJob#processFile(File, OutputStream)
     */
    @Override
    public boolean processFile(File file, OutputStream os) {
        return file.delete();
    }

    /**
     * The method to finish the batchjob.
     * 
     * @param os
     *            The OutputStream to which output should be written
     * @see FileBatchJob#finish(OutputStream)
     */
    @Override
    public void finish(OutputStream os) {
    }
}
