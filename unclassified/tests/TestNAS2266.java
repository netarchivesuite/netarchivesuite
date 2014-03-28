import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;

import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;
import dk.netarkivet.viewerproxy.webinterface.HarvestedUrlsForDomainBatchJob;


public class TestNAS2266 {

    /**
     * @param args
     * @throws UnsupportedEncodingException 
     */
    public static void main(String[] args) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
        File f = new File("/home/svc/files/970-33-20110226015923-00006-sb-test-har-001.arc");
        File f1 = new File("/home/svc/files/5147-163-20140110140517-00000-kb-test-har-004.kb.dk.warc");
        File f2a = new File("/home/svc/files/179636-metadata-1.warc");
        File f2b = new File("/home/svc/files/179732-metadata-1.warc");
        //BatchLocalFiles blf = new BatchLocalFiles(new File[]{f, f1});
        BatchLocalFiles blf = new BatchLocalFiles(new File[]{f2a});
        FileBatchJob fbj = new HarvestedUrlsForDomainBatchJob("360sundhed.dk");
        //FileBatchJob fbj = new HarvestedUrlsForDomainBatchJob("elektronikskader.dk");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        blf.run(fbj, os);
        System.out.println("#files failed: " + fbj.getFilesFailed().size());
        System.out.println("#files processed: " + fbj.getNoOfFilesProcessed());
        System.out.println("#exceptions:" + fbj.getExceptions().size());
        for (ExceptionOccurrence e: fbj.getExceptions()) {
            System.out.println(e.getException());
            System.out.println(e.getFileOffset());
            System.out.println(e.getFileName());
        }
        System.out.println(os.toString("UTF-8"));
    }

}
