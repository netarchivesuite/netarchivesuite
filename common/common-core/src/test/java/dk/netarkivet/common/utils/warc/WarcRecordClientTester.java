package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;

public class WarcRecordClientTester {

    // Her følger to kode-eksempler af hvordan man kan parse en InputStream til en BitarchiveRecord
    // Det er vigtigt at skrive koden som at den kan håndtere både Arc og Warc records. Før den begynder
    // at parse dats så kan den se om det er arc eller warc kun med at kigge på id'er dvs. filnavnet hvorfra
    // dataene blev hentet.
    
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
}