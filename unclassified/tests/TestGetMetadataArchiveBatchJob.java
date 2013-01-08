import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.harvester.indexserver.GetMetadataArchiveBatchJob;

public class TestGetMetadataArchiveBatchJob {

    public static void main(String[] args) throws FileNotFoundException {
        //String domain = "netarkivet.dk";
        //String regexp = ".*" + domain.replaceAll("\\.", "\\\\.") + ".*";
        //System.out.println(regexp);
        
        Pattern CrawlLogUrlpattern = Pattern.compile(MetadataFile.CRAWL_LOG_PATTERN);
        Pattern textPlainMimepattern = Pattern.compile("text/plain");
        
        Pattern CdxUrlpattern = Pattern.compile(MetadataFile.CDX_PATTERN);
        Pattern xCDXMimepattern = Pattern.compile("application/x-cdx");
        
        
        GetMetadataArchiveBatchJob job = new GetMetadataArchiveBatchJob(
                CrawlLogUrlpattern, textPlainMimepattern);
        
        File f1 = new File("/home/svc/TESTFILES/1-metadata-1.warc");
        File f = new File("/home/svc/TESTFILES/1-metadata-1.arc");
        File[] files = new File[]{f1,f};
        BatchLocalFiles blf = new BatchLocalFiles(files);
        OutputStream os = new FileOutputStream("tmp");
        blf.run(job, os);
        System.out.println(job.getNoOfFilesProcessed());
        if (job.getFilesFailed().size() > 0) {
            System.out.println(job.getFilesFailed().size() + " failed");
        }
        for (Exception e: job.getExceptionArray()) {
            System.out.println(e);
            e.printStackTrace();
        }
        
        job = new GetMetadataArchiveBatchJob(
                CdxUrlpattern, xCDXMimepattern);
        
        blf = new BatchLocalFiles(files);
        OutputStream os1 = new FileOutputStream("tmp");
        blf.run(job, os1);
        System.out.println(job.getNoOfFilesProcessed());
        if (job.getFilesFailed().size() > 0) {
            System.out.println(job.getFilesFailed().size() + " failed");
        }
        for (Exception e: job.getExceptionArray()) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
