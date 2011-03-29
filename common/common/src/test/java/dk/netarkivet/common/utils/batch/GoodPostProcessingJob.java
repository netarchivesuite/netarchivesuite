package dk.netarkivet.common.utils.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class GoodPostProcessingJob extends FileBatchJob {

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
            os.write(new String(file.getName() + "\n").getBytes());
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean postProcess(InputStream input, OutputStream output) {
        Log log = LogFactory.getLog(this.getClass());
        try {
            // sort the input stream.
            List<String> filenames = new ArrayList<String>();

            log.info("Reading all the filenames.");
            // read all the filenames.
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String line;
            while((line = br.readLine()) != null) {
                filenames.add(line);
            }
            
            log.info("Sorting the filenames");
            // sort and print to output.
            Collections.sort(filenames);
            for(String file : filenames) {
                output.write(file.getBytes());
                output.write(new String("\n").getBytes());
            }
            
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }
}
