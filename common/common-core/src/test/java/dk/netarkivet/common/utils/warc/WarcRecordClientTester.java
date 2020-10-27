package dk.netarkivet.common.utils.warc;

import java.io.File;
// import java.io.FileDescriptor;
import java.io.FileInputStream;
// import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileResolver;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SettingsFactory;
import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
// import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
// import org.archive.io.warc.WARCRecord;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

// @Category(RequiresFileResolver.class)
public class WarcRecordClientTester {

    /**
     * Tests that basic data retrieval function by issuing a get request.
     * @throws Exception
     */

    FileResolver fileResolver;

    //     **************** Positive tests ***************

    @Test
    public void testGet() throws Exception {
        final URI  baseUri = new URI("http://localhost:8883/cgi-bin2/py1.cgi");
        WarcRecordClient warcRecordClient = new WarcRecordClient(baseUri);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.get("10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz", 3442L);
        assertNotNull("Should have non null BitarchiveRecord", bitarchiveRecord);
        assertTrue("Expect a non-zero length bitarchiveRecord",IOUtils.toByteArray(bitarchiveRecord.getData()).length > 100);
    }


    @Test
    public void testBuildingBitarchiveRecord() throws IOException {
        String filename = "thisisa.warc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        ArchiveReader archiveReader = WARCReaderFactory.get(filename, fileInputStream, true);
        ArchiveRecord archiveRecord = archiveReader.get();
        BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
        bitarchiveRecord.getData(System.out);
    }

    // Test reading .warc record from offset 3442L
    @Test
    public void testBuildingBitarchiveRecord1() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 3442L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
        }
        assertNotNull(warcRecord);
    }

    // Test reading .warc record from offset 0L
    @Test
    public void testBuildingBitarchiveRecord2() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 0L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
        }
        assertNotNull(warcRecord);
    }


    // 1.st Test reading .arc record, offfset 0L
    @Test
    public void testBuildingBitarchiveRecord3() throws Exception {
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 0L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
        }
        assertNotNull(warcRecord);
    }


    // 2.nd Test for .arc records, offset 0L
    @Test
    public void testPosBuildingBitarchiveRecord4() throws Exception {
        String filename = "91-7-20100212214140-00000-sb-test-har-001.statsbiblioteket.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 0L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
        }
        assertNotNull(warcRecord);
    }


    @Test
    public void testPosBuildingBitarchiveRecord5() throws Exception {
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 682L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNotNull(warcRecord);
    }

    // 3.rd test for .arc record at correct positive offset, but no body content

    @Test
    public void testPosBuildingBitarchiveRecord6() throws Exception {
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 789L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNotNull(warcRecord);
    }

// Testing .arc in a loop
@Test
public void testPosBuildingBitarchiveRecord12() throws Exception {
    String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
    URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
    URI test_uri = SAMPLE_HOST;
    long offset = 789L;


    BitarchiveRecord warcRecord = null;
    WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

    for (int i = 0; i<10; i++) {
        try {
            warcRecord = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNotNull(warcRecord);
    }
}


// Testing .warc in loop
@Test
public void testBuildingBitarchiveRecord13() throws Exception {
    String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
    URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
    URI test_uri = SAMPLE_HOST;
    long offset = 0L;

    BitarchiveRecord warcRecord = null;

    WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

    for (int i = 0; i<10; i++) {
        try {
            warcRecord = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
        }
        assertNotNull(warcRecord);
    }
}

    // ************** Negative tests ****************


    // test read non-existing .arc record from offset 0L
    @Test
    public void testBuildingBitarchiveRecord7() throws Exception {
        String filename = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 0L;

        BitarchiveRecord arcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            arcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            arcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNull(arcRecord);
    }

    // test reading .arc record from positive invalid offset 4000L
    @Test
    public void testBuildingBitarchiveRecord8() throws Exception {
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 4000L;

        BitarchiveRecord arcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            arcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            arcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNull(arcRecord);
    }

    // Test reading .warc record from invalid offset 5000L
    @Test
    public void testBuildingBitarchiveRecord9() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 5000L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNull(warcRecord);
    }


    @Test
    public void testBuildingBitarchiveRecord10() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 2L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by offset errror ");
        }
        assertNull(warcRecord);
    }

    
    // Testing for file that is not in our environment
    @Test
    public void testBuildingBitarchiveRecord11() throws Exception {
        String filename = "netarkivet-20081105135926-00001.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 3442L;

        BitarchiveRecord warcRecord = null;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);

        try {
            warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);
            warcRecord.getData(System.out);
        } catch (Exception e) {
            System.out.println("Nullpointer Exception caused by non-existing file ");
        }
        assertNull(warcRecord);
    }


    // Testting looping same test


    // From csr -adapted to this part
/*
    @Before
    public void testFactoryMethod() {
        Settings.set(CommonSettings.FILE_RESOLVER_CLASS, "dk.netarkivet.common.utils.warc.WarcRecordClient");
        Settings.set(CommonSettings.FILE_RESOLVER_BASE_URL, "http://localhost:8884/cgi-bin2/fileresolver.cgi/");
        fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        assertTrue(fileResolver instanceof WarcRecordClient);
    }

    @Test
    public void testFailOnBadUrl() {
        Settings.set(CommonSettings.FILE_RESOLVER_CLASS, "dk.netarkivet.common.utils.FileResolverRESTClient");
        Settings.set(CommonSettings.FILE_RESOLVER_BASE_URL, "localhost:8884/cgi-bin2/fileresolver.cgi/");
        try {
            SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
            fail("Should have thrown exception before getting here.");
        } catch (Exception e) {
            //expected
        }
    }

    @Test
    public void getPathsMultiple() {
        String byJobNumber = "1-*.warc*";
        List<Path> paths = fileResolver.getPaths(byJobNumber); // Needs special adds
        assertEquals("Expected two files for " + byJobNumber + " not " + paths, paths.size(), 2);
    }

    @Test
    public void getPath() {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        Path path = fileResolver.getPath(filename);
        assertTrue("Expected a valid path for the file, not " + path,
                path.toString().contains("/") && path.endsWith(filename));
    }


    @Test
    public void testManyRuns() {
        for (int i = 0; i<40; i++ ) {
            String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
            Path path = fileResolver.getPath(filename);
            assertTrue("Expected a valid path for the file, not " + path,
                    path.toString().contains("/") && path.endsWith(filename));
            System.out.println("Done: " + i);
        }
    }
*/

}