/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 *
 */
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.WARCWriter;
import org.archive.util.anvl.ANVLRecord;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.ChecksumCalculator;
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
        
        String blockDigest = ChecksumCalculator.sha1(fileToArchive);
        
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
            namedFields.addLabelValue("WARC-Concurrent-To", warcInfoUID.toString());
            namedFields.addLabelValue("WARC-Warcinfo-ID", warcInfoUID.toString());
            
            writer.writeMetadataRecord(URL, create14DigitDate,
            		mimetype, recordId, namedFields, in,
                    fileToArchive.length());
        } catch (FileNotFoundException e) {
            throw new IOFailure("Unable to open file: "
                    + fileToArchive.getPath(), e);
        } catch (IOException e) {
            throw new IOFailure("Epic IO fail while writing to WARC file: "
                    + fileToArchive.getPath(), e);
        }
        return true;
    }

    @Override
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, byte[] payload)
                    throws java.io.IOException {
        // hostIP?
        String create14DigitDate = ArchiveDateConverter.getWarcDateFormat()
                .format(new Date(fetchBeginTimeStamp));
        ByteArrayInputStream in = new ByteArrayInputStream(payload);
        String blockDigest = ChecksumCalculator.sha1(in);
        in = new ByteArrayInputStream(payload); // A re-read is necessary here!
        ANVLRecord namedFields = new ANVLRecord(3);
        namedFields.addLabelValue(
        WARCConstants.HEADER_KEY_BLOCK_DIGEST, "sha1:" + blockDigest);
        namedFields.addLabelValue("WARC-Concurrent-To", warcInfoUID.toString());
        namedFields.addLabelValue("WARC-Warcinfo-ID", warcInfoUID.toString());
        URI recordId;
        try {
            recordId = new URI("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!");
        }
        writer.writeMetadataRecord(uri, create14DigitDate, contentType,
                recordId, namedFields, in, payload.length);
    }

}
