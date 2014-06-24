package dk.netarkivet.harvester.harvesting.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCWriter;
import org.archive.util.anvl.ANVLRecord;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.common.utils.warc.WARCUtils;

/**
 * MetadataFileWriter that writes to WARC files.
 * 
 */
public class MetadataFileWriterWarc extends MetadataFileWriter {

    private static final Log log = LogFactory.getLog(MetadataFileWriterWarc.class);

    /** Writer to this jobs metadatafile.
     * This is closed when the metadata is marked as ready.
     */
    private WARCWriter writer = null;
    
    /** The ID of the Warcinfo record. Set when calling the 
     * insertInfoRecord method.
     */
    private URI warcInfoUID = null;
    
    /**
     * Create a <code>MetadataFileWriter</code> for WARC output.
     * @param metadataWarcFile The WARC output file
     * @return <code>MetadataFileWriter</code> for writing metadata files in WARC
     */
    public static MetadataFileWriter createWriter(File metadataWarcFile) {
        MetadataFileWriterWarc mtfw = new MetadataFileWriterWarc();
        mtfw.writer = WARCUtils.createWARCWriter(metadataWarcFile);
        return mtfw;
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new IOFailure("Error closing MetadataFileWriterWarc", e);
            }
        }
        writer = null;
    }

    @Override
    public File getFile() {
        return writer.getFile();
    }
    
    /**
     * Insert a warcInfoRecord in the WARC-file, if it doesn't already exists.
     * saves the recordID of the written info-record for future reference 
     * to be used for later in the 
     * 
     * @param payloadToInfoRecord the given payload for this record.
     */
    public void insertInfoRecord(ANVLRecord payloadToInfoRecord){ 
        if (warcInfoUID != null) {
            throw new IllegalState("An WarcInfo record has already been inserted");
        }
        
        String filename = writer.getFile().getName();
        String datestring = ArchiveDateConverter.getWarcDateFormat()
                .format(new Date());
        URI recordId;
        try {
            recordId = new URI("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!");
        }
        warcInfoUID = recordId;
        ANVLRecord namedFields = new ANVLRecord(1);
        namedFields.addLabelValue("WARC-Filename", filename);
        
        try {
            byte[] payloadAsBytes = payloadToInfoRecord.getUTF8Bytes();

            String blockDigest = ChecksumCalculator.calculateSha1(
                    new ByteArrayInputStream(payloadAsBytes));
            namedFields.addLabelValue("WARC-Block-Digest", "sha1:" + blockDigest);
            
            writer.writeWarcinfoRecord(datestring, "application/warc-fields", recordId, 
                namedFields, (InputStream) new ByteArrayInputStream(payloadAsBytes), payloadAsBytes.length);
        } catch (IOException e) {
            throw new IllegalState("Error inserting warcinfo record", e);
        }
    }

    @Override
    public void writeFileTo(File file, String uri, String mime) {
        writeTo(file, uri, mime);
    }

    @Override
    public boolean writeTo(File fileToArchive, String URL, String mimetype) {
        if (warcInfoUID == null) {
            throw new IllegalState("An WarcInfo record has not been inserted yet");
        }
        log.info(fileToArchive + " " + fileToArchive.length());
        
        String blockDigest = ChecksumCalculator.calculateSha1(fileToArchive);
        
        String create14DigitDate = ArchiveDateConverter.getWarcDateFormat()
                .format(new Date());
        URI recordId;
        try {
            recordId = new URI("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!");
        }
        InputStream in = null;
        try {
            in = new FileInputStream(fileToArchive);
            ANVLRecord namedFields = new ANVLRecord(3);

            namedFields.addLabelValue(
                    WARCConstants.HEADER_KEY_BLOCK_DIGEST, "sha1:" + blockDigest);
            namedFields.addLabelValue("WARC-Warcinfo-ID", 
                    generateEncapsulatedRecordID(warcInfoUID));
            namedFields.addLabelValue("WARC-IP-Address", SystemUtils.getLocalIP());
            
            writer.writeResourceRecord(URL, create14DigitDate,
                    mimetype, recordId, namedFields, in,
                    fileToArchive.length());
        } catch (FileNotFoundException e) {
            throw new IOFailure("Unable to open file: "
                    + fileToArchive.getPath(), e);
        } catch (IOException e) {
            throw new IOFailure("Epic IO fail while writing to WARC file: "
                    + fileToArchive.getPath(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }
    
    /** 
     * Generate encapsulated recordID.
     * @param recordID A given recordID
     * @return An encapsulated recordID.
     */
    private String generateEncapsulatedRecordID(URI recordID) {
        return "<" + recordID + ">";
    }
    
    
    @Override
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, byte[] payload)
                    throws java.io.IOException {
        
        String create14DigitDate = ArchiveDateConverter.getWarcDateFormat()
                .format(new Date(fetchBeginTimeStamp));
        ByteArrayInputStream in = new ByteArrayInputStream(payload);
        String blockDigest = ChecksumCalculator.calculateSha1(in);
        in = new ByteArrayInputStream(payload); // A re-read is necessary here!
        ANVLRecord namedFields = new ANVLRecord(3);
        namedFields.addLabelValue(
        WARCConstants.HEADER_KEY_BLOCK_DIGEST, "sha1:" + blockDigest);
        namedFields.addLabelValue("WARC-Warcinfo-ID", 
                generateEncapsulatedRecordID(warcInfoUID));
        namedFields.addLabelValue("WARC-IP-Address", SystemUtils.getLocalIP());
        URI recordId;
        try {
            recordId = new URI("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!");
        }
        writer.writeResourceRecord(uri, create14DigitDate, contentType,
                recordId, namedFields, in, payload.length);
    }

}
