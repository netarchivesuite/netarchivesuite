package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class throws an exception. */
public class ExceptionBatchFinish extends FileBatchJob {
    public void initialize(OutputStream os) {
    }
    
    public boolean processFile(File file, OutputStream os) {
        String name = file.getName() + "\n";
        try {
            os.write(name.getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public void finish(OutputStream os) {
        throw new SecurityException("Security exception in finish");
    }
}
