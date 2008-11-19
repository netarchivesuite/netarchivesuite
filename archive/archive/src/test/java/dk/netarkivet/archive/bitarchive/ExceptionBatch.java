package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class throws an exception. */
public class ExceptionBatch extends FileBatchJob {
    public void initialize(OutputStream os) {
    }
    
    public boolean processFile(File file, OutputStream os) {
        String name = file.getName();
        if (name.indexOf("metadata")>0) { 
            throw new SecurityException("Security exception");
        } else {
            String s = "non-metadata file: " + name + "\n"; 
            try {
                os.write(s.getBytes());
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
    
    public void finish(OutputStream os) {
    }
}
