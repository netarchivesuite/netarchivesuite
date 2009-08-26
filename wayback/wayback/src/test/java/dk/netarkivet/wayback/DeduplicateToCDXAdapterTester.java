package dk.netarkivet.wayback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:40:23 AM To
 * change this template use File | Settings | File Templates.
 */
public class DeduplicateToCDXAdapterTester extends TestCase {

    public static final String DEDUP_CRAWL_LOG = "dedup_crawk_log.txt";
    public static final String DEDUP_CRAWL_STRING ="2009-05-25T13:00:00.992Z   200       9717 https://wiki.statsbiblioteket.dk/wiki/summa/img/draft.png LLEE https://wiki.statsbiblioteket.dk/wiki/summa/css/screen.css image/png #016 20090525130000915+76 sha1:AXH2IFNXC4MUT26SRHRJZHGR3FDAJDNR - duplicate:\"1-1-20090513141823-00008-sb-test-har-001.statsbiblioteket.dk.arc,22962264\",content-size:9969";

      public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
      }

    
    public void tearDown() {

        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
    }

    public void testCtor() {
                if (!TestUtils.runningAs("CSR")) {
            return;
        }
       DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter(); 
    }

    public void testAdaptLine() {
                if (!TestUtils.runningAs("CSR")) {
            return;
        }
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        String cdx_line = adapter.adaptLine(DEDUP_CRAWL_STRING);
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        CaptureSearchResult result = adapter2.adapt(cdx_line);
        assertEquals("Should get the arcfilename back out of the cdx line","1-1-20090513141823-00008-sb-test-har-001.statsbiblioteket.dk.arc",result.getFile());
        assertEquals("Should get the right http code out of the cdx line","200",result.getHttpCode());
    }

    public void testAdaptStream() throws FileNotFoundException {
                if (!TestUtils.runningAs("CSR")) {
            return;
        }
        InputStream is = new FileInputStream(new File(TestInfo.WORKING_DIR, DEDUP_CRAWL_LOG));
        OutputStream os = new ByteArrayOutputStream();
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        adapter.adaptStream(is, os);
        String output = os.toString();
        String[] lines = output.split("\n");
        assertTrue("expect at least 3 lines of output, got " + lines.length, lines.length > 2);
    }

}
