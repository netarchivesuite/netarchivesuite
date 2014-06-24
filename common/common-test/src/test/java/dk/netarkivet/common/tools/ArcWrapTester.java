package dk.netarkivet.common.tools;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import junit.framework.TestCase;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * Unit test for the ArcWrap tool.
 */
public class ArcWrapTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(
            TestInfo.DATA_DIR, TestInfo.WORKING_DIR);
    
    public void setUp(){
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }
    public void tearDown(){
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
    }
    public void testMain() throws IOException {
        String pathname = "tests/dk/netarkivet/common/tools/ArcWrapTester.txt";
        File storeFile = new File(pathname);
        assertTrue(pathname + " exists()?", storeFile.exists());
        String mime = "text/plain";
        String arcUri = "testdata://netarkivet.dk/test/code/ArcWrapTester.java";
        File arcFile = new File(TestInfo.WORKING_DIR, "test.arc");
        OutputStream myOut = new FileOutputStream(arcFile);
        System.setOut(new PrintStream(myOut));
        try {
            ArcWrap.main(new String[]{storeFile.getAbsolutePath(),
                    arcUri, mime});
        } catch (SecurityException e) {
            assertEquals("Should have exited normally",
                         0, pse.getExitValue());
        }
        myOut.close();
        //Put an ARCReader on top of the file.
        ARCReader r = ARCReaderFactory.get(arcFile);
        Iterator<ArchiveRecord> it = r.iterator();
        it.next(); //Skip ARC file header
        //Read the record, checking mime-type, uri and content.
        ARCRecord record = (ARCRecord) it.next();
        ARCRecordMetaData meta = record.getMetaData();
        assertEquals("Should record the object under the given URI",
                arcUri, meta.getUrl());
        assertEquals("Should indicate the intended MIME type",
                mime, meta.getMimetype());
        String foundContent = ARCTestUtils.readARCRecord(record);
        assertEquals("Should store content unchanged",
                FileUtils.readFile(storeFile), foundContent);
    }
}
