
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit tests for FileListJob.
 * TODO Move unittest to common.utils.batch
 */
public class FileListJobTester extends TestCase {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                                 TestInfo.WORKING_DIR);

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    /**
     * Test that FileBatchJob outputs the right data.
     */
    public void testProcessFile() {
        File bitarchive = new File(TestInfo.WORKING_DIR, "bitarchive1");
        File arcfile = new File(bitarchive, "integrity1.ARC");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileBatchJob job = new FileListJob();
        job.processFile(arcfile, baos);
        assertEquals("Job should return filename + newline", arcfile.getName()
                +"\n", baos.toString());
    }

    /**
     * Tests serializability of this class, under the assumption that its
     * toString() method is dependent on its entire relevant state.
     * @throws Exception On any error
     */
    public void testSerializable() throws Exception {
        FileBatchJob job = new FileListJob();
        FileBatchJob job2 = (FileBatchJob) Serial.serial(job);
        assertEquals("Should have same toString()", job.toString(),
                job2.toString());
    }

}
