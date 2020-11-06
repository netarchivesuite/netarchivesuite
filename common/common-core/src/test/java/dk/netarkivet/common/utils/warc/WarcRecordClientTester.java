package dk.netarkivet.common.utils.warc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;

import static org.junit.Assert.*;

public class WarcRecordClientTester {

    /**
     * Tests that basic data retrieval function by issuing a get request.
     * @throws Exception
     */

    //     **************** Positive tests ***************

    @Test
    public void testGet() throws Exception {
        final URI  baseUri = new URI("http://localhost:8883/cgi-bin2/py1.cgi");
        WarcRecordClient warcRecordClient = new WarcRecordClient(baseUri);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord("10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz", 3442L);
        assertNotNull("Should have non null BitarchiveRecord", bitarchiveRecord);
        assertTrue("Expect a non-zero length bitarchiveRecord",IOUtils.toByteArray(bitarchiveRecord.getData()).length > 100);

    }

    // test read first existing warc record from file thisisa.warc
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

    // test read existing warc record from offset 0 of file thisisa.warc
    @Test
    public void testBuildingBitarchiveRecord2() throws IOException {
        String filename = "thisisa.warc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        ArchiveReader archiveReader = WARCReaderFactory.get(filename, fileInputStream, true);
        ArchiveRecord archiveRecord = archiveReader.get();
        BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
        bitarchiveRecord.getData(System.out);
    }

    /*
    @Test
    public void testBuildingBitarchiveRecord3() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        long offset = 3442L;
        URI test_uri = SAMPLE_HOST;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getWarc(SAMPLE_HOST, offset);

        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        ArchiveReader archiveReader = WARCReaderFactory.get(filename, fileInputStream, true);
        ArchiveRecord archiveRecord = archiveReader.get();
        BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
        bitarchiveRecord.getData(System.out);
    }
*/

    @Test
    public void testBuildingBitarchiveRecord5() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 3442L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord(filename, offset);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        warcRecord.getData(baos);
        assertTrue("Should have significant data", baos.size() > 10);
    }

    @Test
    public void testMultipleCalls() throws Exception {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        long offset = 3442L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(HOST);
        for (int i = 0; i < 40; i++) {
            BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord(filename, offset);
            assertNotNull("Expect non null record", bitarchiveRecord);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitarchiveRecord.getData(byteArrayOutputStream);
            assertTrue("Expect significantly sized output", byteArrayOutputStream.toByteArray().length > 10);
            System.out.println(i);
        }
    }

    //@Test
    public void testPosBuildingBitarchiveRecord6() throws Exception {
        String filename = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 3442L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord(filename, offset);
        Boolean fail = false;

        try {
            BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord(filename, offset);
            bitarchiveRecord.getData(System.out);
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception caused by offset errror");
            fail = true;
        }
        assertFalse("Exception", fail);
    }

    // ************** Negative tests ****************

    // test read existing arc record from offset 0  -Server Service Not Yet Implemented
    @Test
    public void testBuildingBitarchiveRecord7() throws IOException {
        String filename = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        Boolean fail = false;
        try {
            ARCRecord archiveRecord = new ARCRecord(fileInputStream, filename, 0L, false, false, true);
            BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
            bitarchiveRecord.getData(System.out);
        }   catch (Exception e) {
                System.out.println(("Error in reading arc record"));
                fail = true;
            }
        assertTrue("Exception", fail);
    }

    // Test for offset not atFirstRecord     OK
    // test read existing arc record from offset 5000
    @Test
    public void testBuildingBitarchiveRecord001() throws Exception {
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc");
        URI test_uri = SAMPLE_HOST;
        long offset = 5000L;  // 3442L; //5000L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord("10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc", offset);
        assertNull(warcRecord);
    }

    @Test
    public void testBuildingBitarchiveRecord002() throws IOException {
        String filename = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        Boolean fail = false;
        try {
            ARCRecord archiveRecord = new ARCRecord(fileInputStream, filename, 4000L, false, false, true);
            BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
            bitarchiveRecord.getData(System.out);
        }   catch (Exception e) {
            System.out.println(("Error in reading arc record"));
            fail = true;
        }
        assertTrue("Exception", fail);
    }

    // Testing for invalid offset
    @Test
    public void testFailInBuildingBitarchiveRecord3() throws IOException, URISyntaxException {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 4000L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord(filename, offset);
        assertNull(warcRecord);
    }

    @Test
    public void testFailInBuildingBitarchiveRecord4() throws IOException, URISyntaxException {
        String filename = "10-4-20161218234349999-00000-kb-test-har-003.kb.dk.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 2L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord(filename, offset);
        assertNull(warcRecord);
    }

    // Test for file not found
    // Testing for file that is not in our environment
    @Test
    public void testBuildingBitarchiveRecord4() throws Exception {
        String filename = "netarkivet-20081105135926-00001.warc.gz";
        URI SAMPLE_HOST = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        URI test_uri = SAMPLE_HOST;
        long offset = 3442L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(test_uri);
        BitarchiveRecord warcRecord  = warcRecordClient.getBitarchiveRecord(filename, offset);
        assertNull(warcRecord);
    }

    // Test using the wrong reader for .arc files
    @Test
    public void testFailInBuildingBitarchiveRecord1() throws IOException {
        String filename = "91-7-20100212214140-00000-sb-test-har-001.statsbiblioteket.dk.arc.gz";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);

        Boolean fail = false;
        try {
            ArchiveReader archiveReader = WARCReaderFactory.get(filename, fileInputStream, true);
            ArchiveRecord archiveRecord = archiveReader.get();
            BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, filename);
            bitarchiveRecord.getData(System.out);
            throw new Exception();
        }
        catch (Exception e) {
            fail = true;
            System.out.println(Boolean.parseBoolean("Expect IOException: " + e.getMessage()));
        }
        assertTrue("Exception: ", fail);
    }

    @Test
    public void testPosBuildingBitarchiveRecord13() throws Exception {
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        URI uri = new URI("http://localhost:8883/cgi-bin2/py1.cgi/" + filename);
        long offset = 789L;
        WarcRecordClient warcRecordClient = new WarcRecordClient(uri);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord(filename, offset);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitarchiveRecord.getData(baos);
        assertTrue("Should have non-zero length output", baos.size() > 10);
    }

}