package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import dk.netarkivet.common.utils.batch.FileBatchJob;

@SuppressWarnings({ "serial"})
public class TimeoutBatch extends FileBatchJob {
    Date start;

    public TimeoutBatch() {
        batchJobTimeout = 1000;
    }

    public void initialize(OutputStream os) {
        // one second in milisecond.
        start = new Date();

        String msg = "timeout: " + getBatchJobTimeout() + "\n";
        System.out.println(msg);
        try {
            os.write(msg.getBytes());
        } catch (IOException e) {
        }
    }

    

    public boolean processFile(File file, OutputStream os) {
        String name = file.getName() + "\n";
        try {
            os.write(name.getBytes());
            Thread.sleep(1000);
            System.out.println("te;");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void finish(OutputStream os) {
        System.out.println("time: " + ((new Date()).getTime() - start.getTime()) );
    }
}
