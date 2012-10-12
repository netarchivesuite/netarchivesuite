package dk.netarkivet.common.utils.batch;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: csr
 * Date: 10/9/12
 * Time: 2:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatedFileListJobTester extends TestCase {
      MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);


     public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testProcess() throws IOException, InterruptedException {
        FileListJob job = new FileListJob();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        processOnDirectory(job, TestInfo.WORKING_DIR, os);
        int totalFiles = countLines(os);
        int filesToBeIgnored = 4;
        int filesTouched = 0;
        for (File file: TestInfo.WORKING_DIR.listFiles()) {
             if (filesTouched < filesToBeIgnored) {
                  file.setLastModified(new Date().getTime() - 3600000L);
                 filesTouched++;
             } else {
                 file.setLastModified(new Date().getTime());
             }
        }
        //Now there are four files with timestamp one hour ago and the rest with timestamp "now".
        DatedFileListJob job2 = new DatedFileListJob(new Date(new Date().getTime() - 1800000));
        os = new ByteArrayOutputStream();
        processOnDirectory(job2, TestInfo.WORKING_DIR, os);
        assertEquals("Expected the number of files found to exclude those files with timestamp set to one hour ago.",
                totalFiles-filesToBeIgnored, countLines(os));
    }

    private static void processOnDirectory(FileBatchJob job, File dir, OutputStream os) {
        for (File file: dir.listFiles()) {
            job.processFile(file, os);
        }
    }

    private static int countLines(ByteArrayOutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(os.toString()));
        int lines = 0;
        while (reader.readLine() != null) {
             lines++;
        }
        return lines;
    }

}
