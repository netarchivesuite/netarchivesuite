package dk.netarkivet.common.utils.arc;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import junit.framework.TestCase;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * A simple test of the ARCREADER that is bundled with Heritrix 1.14.4. 
 */
@SuppressWarnings({ "unused"})
public class ARCReaderTester extends TestCase {
    public static final String ARCHIVE_DIR =
            "tests/dk/netarkivet/common/utils/arc/data/input/";
    public static final String testFileName = "working.arc";
    
    public void testARCReaderClose() {
        try {
            final File testfile = new File(ARCHIVE_DIR + testFileName);
            FileUtils.copyFile(new File(ARCHIVE_DIR + "fyensdk.arc"),
                    testfile);
            
            ARCReader reader = ARCReaderFactory.get(testfile);
            ARCRecord record = (ARCRecord) reader.get(0);
            BitarchiveRecord rec =
                    new BitarchiveRecord(record, testFileName);
            record.close();
            reader.close();
            testfile.delete();
        } catch (IOException e) {
            fail("Should not throw IOException " + e);
        }

    }
}
