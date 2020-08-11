package dk.netarkivet.common.utils.warc;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLOutput;

import static org.junit.Assert.*;

public class WarcRecordClientTest {
    // Hvordan man parser en InputStream til en BitarchiveRecord
    // Vigtigt:  Kode skal kunne håndtere både Arc og Warc records. Før den begynder
    // at parse dats så kan den se om det er arc eller warc kun med at kigge på id'er dvs. filnavnet hvorfra
    // dataene blev hentet.

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
        // String baseUri = "http://localhost:8883/cgi-bin2/py1.cgi";
        URI baseUri = new URI("http://localhost:8883/cgi-bin2/py1.cgi");

        //setting to NetarchiveSuite.
        WarcRecordClient warcRecordClient = new WarcRecordClient(baseUri);
        BitarchiveRecord bitarchiveRecord = warcRecordClient.get("10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz", 3442L);
        assertNotNull("Should have non null BitarchiveRecord", bitarchiveRecord);
        assertTrue("Expect a non-zero length bitarchiveRecord", IOUtils.toByteArray(bitarchiveRecord.getData()).length > 100);
    }

    @Test
    public void getFile() {
    }
}