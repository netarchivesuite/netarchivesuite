
package dk.netarkivet.viewerproxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.viewerproxy.webinterface.CrawlLogLinesMatchingRegexp;

/** 
 * Tester for the class CrawlLogLinesMatchingRegexp used in 
 * Reporting.getCrawlLoglinesMatchingRegexp(jobid, regexp);
 *
 */
public class CrawlLogLinesMatchingRegexpTester extends TestCase {
    
    MoveTestFiles mtf;
    File metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
    
    @Override
    public void setUp() {
        TestInfo.WORKING_DIR.mkdir();
        File metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
        metadataDir.mkdir();
        mtf = new MoveTestFiles(TestInfo.METADATA_DIR,
                metadataDir);
        mtf.setUp();
    }
    
    @Override
    public void tearDown() {
        mtf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }
    
    public void testBatchJob() throws FileNotFoundException {
        File ZipOne = new File(metadataDir, "1-metadata-1.warc.zip");
        File ZipTwo = new File(metadataDir, "1-metadata-1.arc.zip");
        
        FileBatchJob cJob = new CrawlLogLinesMatchingRegexp(".*netarkivet\\.dk.*");
        ZipUtils.unzip(ZipOne, TestInfo.WORKING_DIR);
        ZipUtils.unzip(ZipTwo, TestInfo.WORKING_DIR);
        
        File f1 = new File(TestInfo.WORKING_DIR, "1-metadata-1.warc");
        File f = new File(TestInfo.WORKING_DIR, "1-metadata-1.arc");
        File[] files = new File[]{f1,f};
        BatchLocalFiles blf = new BatchLocalFiles(files);
        blf = new BatchLocalFiles(files);
        OutputStream os2 = new FileOutputStream("tmp1");
        blf.run(cJob, os2);
        //System.out.println(cJob.getNoOfFilesProcessed());
        assertEquals("Expected no files to fail, but " + cJob.getFilesFailed().size()
                    + " failed", 0, cJob.getFilesFailed().size());
        
        for (ExceptionOccurrence e: cJob.getExceptions()) {
            System.out.println(e.getException());
        }
    }
}
