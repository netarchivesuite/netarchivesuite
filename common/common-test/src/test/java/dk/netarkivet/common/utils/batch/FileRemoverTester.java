package dk.netarkivet.common.utils.batch;


import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileRemover;

/**
 * Unit tests for the {@link FileRemover} class.
 */
public class FileRemoverTester extends TestCase {
    public FileRemoverTester(String s) {
        super(s);
    }
    
    public void testRemoverJob() throws IOException {
        FileBatchJob job = new FileRemover();
        job.initialize(null);
        File tmp = null;
        tmp = File.createTempFile("test", "fileremover");
        job.processFile(tmp, null);
        job.finish(null);
        assertFalse(tmp.exists());
    }
    
}