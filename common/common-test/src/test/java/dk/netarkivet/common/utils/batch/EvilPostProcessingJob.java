package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.utils.FileUtils;

@SuppressWarnings({ "serial"})
public class EvilPostProcessingJob extends FileBatchJob {

    @Override
    public void finish(OutputStream os) {
        // TODO Auto-generated method stub
    }

    @Override
    public void initialize(OutputStream os) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean processFile(File file, OutputStream os) {
        try {
            os.write((file.getName() + "\n").getBytes());
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean postProcess(InputStream input, OutputStream output) {
        Log log = LogFactory.getLog(this.getClass());
        try {
            File[] files = FileUtils.getTempDir().listFiles();
            
            log.info("directory batch contains " + files.length + " files.");
            
            for(File fil : files) {
                log.warn("deleting: " + fil.getName());
                fil.delete();
            }
            
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }
}
