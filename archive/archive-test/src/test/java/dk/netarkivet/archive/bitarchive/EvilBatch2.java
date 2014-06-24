package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class attempts to do illegal actions.
 */
@SuppressWarnings({ "serial"})
public class EvilBatch2 extends FileBatchJob {
    public void initialize(OutputStream os) {
    }
    
    public boolean processFile(File file, OutputStream os) {
        try {
            file.delete();
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }
    
    public void finish(OutputStream os) {
    }
}
