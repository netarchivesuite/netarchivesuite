package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.util.FileUtils;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;

/**
 * A simple test of the WARCREADER that is bundled with Heritrix 1.14.4. 
 */
public class WARCReaderTester extends TestCase {

    public static final String ARCHIVE_DIR =
            "tests/dk/netarkivet/common/utils/warc/data/input/";
    public static final String testFileName = "working.warc";
    
    public void testARCReaderClose() {
        try {
            final File testfile = new File(ARCHIVE_DIR + testFileName);
            FileUtils.copyFile(new File(ARCHIVE_DIR + "fyensdk.warc"),
                    testfile);
            
            WARCReader reader = WARCReaderFactory.get(testfile);
            WARCRecord record = (WARCRecord) reader.get(0);
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
