package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;

/**
 * Job which returns the names of all files in the archive modified after a specific date.
 */
@SuppressWarnings({ "serial"})
public class DatedFileListJob extends FileListJob {

    private Date since;

    /**
     * Constructor for this class.
     * @param since The date after which we require files to be listed.
     */
    public DatedFileListJob(Date since) {
        super();
        this.since = since;
    }

    /**
     * Writes the name of the arcfile to the OutputStream if its lastModified date is more recent than "since".
     * @param file an arcfile
     * @param os the OutputStream to which data is to be written
     * @return false If listing of this arcfile fails because of an error; true if the name is listed or if it is not
     * listed because the file is too old.
     */
    @Override
    public boolean processFile(File file, OutputStream os) {
        if (file.lastModified() > since.getTime()) {
            return super.processFile(file, os);
        } else {
            return true;
        }
    }
}
