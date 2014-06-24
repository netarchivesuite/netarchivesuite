package dk.netarkivet.common.utils.batch;

import java.io.File;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

@SuppressWarnings({ "unused", "unchecked"})
public class ByteClassLoaderTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    public ByteClassLoaderTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testDefineClass() throws Exception {
        ByteClassLoader loader
                = new ByteClassLoader(new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        Class<LoadableTestJob> c = loader.defineClass();
        assertEquals("Class name should be correct",
                     "dk.netarkivet.common.utils.batch.LoadableTestJob",
                     c.getName());
        // Note that we can't cast it to a LoadableTestJob, as we've already
        // loaded that class through a different classloader, so they aren't
        // quite the same.
        FileBatchJob job = c.newInstance();

        try {
            loader = new ByteClassLoader(TestInfo.INPUT_1);
            c = loader.defineClass();
            fail("Should have died trying to load illegal class");
        } catch (ClassFormatError e) {
            // expected
        }
    }
}
