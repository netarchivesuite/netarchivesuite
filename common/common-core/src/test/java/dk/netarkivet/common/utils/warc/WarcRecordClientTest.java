package dk.netarkivet.common.utils.warc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.utils.service.WarcRecordClient;
import org.junit.experimental.categories.Category;

public class WarcRecordClientTest {
    final String WRS_URL = "https://localhost:10443/cgi-bin/warcrecordservice.cgi";

    @Test
    public void testBuildingBitarchiveRecord() throws IOException {
        String fileName = "thisisa.warc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        // String fileName = inputFile.getName();
        System.out.println("Name: " + fileName);
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        ArchiveReader archiveReader = WARCReaderFactory.get(fileName, fileInputStream, true);
        ArchiveRecord archiveRecord = archiveReader.get();
        BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(archiveRecord, fileName);
        bitarchiveRecord.getData(System.out);
    }

    @Test
    public void testBuildingBitarchiveRecord2() throws IOException {
        String filename = "thisisa.warc";
        File inputFile = new File("src/test/java/data.txt");
        System.out.println(inputFile.getAbsolutePath());
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        WARCRecord warcRecord = new WARCRecord(fileInputStream, filename, 0);
        BitarchiveRecord bitarchiveRecord = new BitarchiveRecord(warcRecord, filename);
        bitarchiveRecord.getData(System.out);
    }
    
    @Test
    public void getHttpClient() {
    }

    @Test
    public void getInstance() {
    }


    @Test
    public void getWarc() {
    }

    @Test
    public void get() throws IOException, URISyntaxException {
        URI baseUri = new URI(WRS_URL);

        //setting to NetarchiveSuite.
        WarcRecordClient warcRecordClient = new WarcRecordClient(baseUri);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord("10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz", 3442L);
        assertNotNull("Should have non null BitarchiveRecord", bitarchiveRecord);
        assertTrue("Expect a non-zero length bitarchiveRecord", IOUtils.toByteArray(bitarchiveRecord.getData()).length > 100);
        System.out.println("\n\n" + IOUtils.toString(bitarchiveRecord.getData()));
    }

   @Category(FailsOnJenkins.class)
    @Test
    public void getWithArc() throws IOException, URISyntaxException {
        URI baseUri = new URI(WRS_URL);

        //setting to NetarchiveSuite.
        WarcRecordClient warcRecordClient = new WarcRecordClient(baseUri);
        String filename = "42-23-20060726143926-00000-udvikling.kb.dk.arc.gz";
        BitarchiveRecord bitarchiveRecord = warcRecordClient.getBitarchiveRecord(filename, 682L);
        assertNotNull("Should have non null BitarchiveRecord", bitarchiveRecord);
        assertTrue("Expect a non-zero length bitarchiveRecord", IOUtils.toByteArray(bitarchiveRecord.getData()).length > 10);
        System.out.println("\n\n" + IOUtils.toString(bitarchiveRecord.getData()));
    }

    @Test
    public void getFile() {
    }
}