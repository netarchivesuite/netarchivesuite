/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.archive.util.Base32;
import org.jwat.common.ANVLRecord;
import org.jwat.common.ContentType;
import org.jwat.common.Uri;
import org.jwat.warc.WarcConstants;
import org.jwat.warc.WarcDigest;
import org.jwat.warc.WarcFileNaming;
import org.jwat.warc.WarcFileNamingSingleFile;
import org.jwat.warc.WarcFileWriter;
import org.jwat.warc.WarcFileWriterConfig;
import org.jwat.warc.WarcHeader;
import org.jwat.warc.WarcRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * MetadataFileWriter that writes to WARC files.
 */
public class MetadataFileWriterWarc extends MetadataFileWriter {

    private static final Logger log = LoggerFactory.getLogger(MetadataFileWriterWarc.class);

    /** Writer to this jobs metadatafile. This is closed when the metadata is marked as ready. */
    private WarcFileWriter writer = null;

    /** The ID of the Warcinfo record. Set when calling the insertInfoRecord method. */
    private Uri warcInfoUID = null;

    /**
     * Create a <code>MetadataFileWriter</code> for WARC output.
     *
     * @param metadataWarcFile The WARC output file
     * @return <code>MetadataFileWriter</code> for writing metadata files in WARC
     */
    public static MetadataFileWriter createWriter(File metadataWarcFile) {
        MetadataFileWriterWarc mtfw = new MetadataFileWriterWarc();
    	WarcFileNaming naming = new WarcFileNamingSingleFile(metadataWarcFile);
    	WarcFileWriterConfig config = new WarcFileWriterConfig(metadataWarcFile.getParentFile(), compressRecords(), Long.MAX_VALUE, true);
        mtfw.writer = WarcFileWriter.getWarcWriterInstance(naming, config);
        mtfw.open();
        return mtfw;
    }

    protected void open() {
        try {
            writer.open();
        } catch (IOException e) {
            throw new IOFailure("Error opening MetadataFileWriterWarc", e);
        }
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
     * Insert a warcInfoRecord in the WARC-file, if it doesn't already exists. saves the recordID of the written
     * info-record for future reference to be used for later in the
     *
     * @param payloadToInfoRecord the given payload for this record.
     */
    public void insertInfoRecord(ANVLRecord payloadToInfoRecord) {
        if (warcInfoUID != null) {
            throw new IllegalState("An WarcInfo record has already been inserted");
        }
        String filename = writer.getFile().getName();
        if (filename.endsWith(WarcFileWriter.ACTIVE_SUFFIX)) {
        	filename = filename.substring(0, filename.length() - WarcFileWriter.ACTIVE_SUFFIX.length());
        }
        Uri recordId;
        try {
            recordId = new Uri("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!", e);
        }
        warcInfoUID = recordId;
        try {
            byte[] payloadAsBytes = payloadToInfoRecord.getUTF8Bytes();
            byte[] blockDigestBytes = ChecksumCalculator.digestInputStream(new ByteArrayInputStream(payloadAsBytes), "SHA1");
            WarcDigest blockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encode(blockDigestBytes));
            WarcRecord record = WarcRecord.createRecord(writer.writer);
            WarcHeader header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_WARCINFO;
            header.addHeader(WarcConstants.FN_WARC_RECORD_ID, recordId, null);
            header.addHeader(WarcConstants.FN_WARC_DATE, new Date(), null);
            header.addHeader(WarcConstants.FN_WARC_FILENAME, filename);
            header.addHeader(WarcConstants.FN_CONTENT_TYPE, ContentType.parseContentType(WarcConstants.CT_APP_WARC_FIELDS), null);
            header.addHeader(WarcConstants.FN_CONTENT_LENGTH, new Long(payloadAsBytes.length), null);
            header.addHeader(WarcConstants.FN_WARC_BLOCK_DIGEST, blockDigest, null);
            writer.writer.writeHeader(record);
            ByteArrayInputStream bin = new ByteArrayInputStream(payloadAsBytes);
            writer.writer.streamPayload(bin);
            writer.writer.closeRecord();
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
        if (!fileToArchive.isFile()) {
            throw new IOFailure("Not a file: " + fileToArchive.getPath());
        }
        if (warcInfoUID == null) {
            throw new IllegalState("An WarcInfo record has not been inserted yet");
        }
        log.info("{} {}", fileToArchive, fileToArchive.length());
        byte[] blockDigestBytes = ChecksumCalculator.digestFile(fileToArchive, "SHA1");
        WarcDigest blockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encode(blockDigestBytes));
        Uri recordId;
        try {
            recordId = new Uri("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!", e);
        }
        InputStream in = null;
        try {
            WarcRecord record = WarcRecord.createRecord(writer.writer);
            WarcHeader header = record.header;
            header.warcTypeIdx = WarcConstants.RT_IDX_RESOURCE;
            header.addHeader(WarcConstants.FN_WARC_RECORD_ID, recordId, null);
            header.addHeader(WarcConstants.FN_WARC_DATE, new Date(), null);
            header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, warcInfoUID, null);
            header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, SystemUtils.getLocalIP());
            header.addHeader(WarcConstants.FN_WARC_TARGET_URI, URL);
            header.addHeader(WarcConstants.FN_WARC_BLOCK_DIGEST, blockDigest, null);
            header.addHeader(WarcConstants.FN_CONTENT_TYPE, ContentType.parseContentType(mimetype), null);
            header.addHeader(WarcConstants.FN_CONTENT_LENGTH, new Long(fileToArchive.length()), null);
            writer.writer.writeHeader(record);
            in = new FileInputStream(fileToArchive);
            writer.writer.streamPayload(in);
            writer.writer.closeRecord();
        } catch (FileNotFoundException e) {
            throw new IOFailure("Unable to open file: " + fileToArchive.getPath(), e);
        } catch (IOException e) {
            throw new IOFailure("Epic IO fail while writing to WARC file: " + fileToArchive.getPath(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }

    @Override
    public void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, byte[] payload)
            throws java.io.IOException {    	
        ByteArrayInputStream in = new ByteArrayInputStream(payload);
        byte[] blockDigestBytes = ChecksumCalculator.digestInputStream(in, "SHA1");
        WarcDigest blockDigest = WarcDigest.createWarcDigest("SHA1", blockDigestBytes, "base32", Base32.encode(blockDigestBytes));
        Uri recordId;
        try {
            recordId = new Uri("urn:uuid:" + UUID.randomUUID().toString());
        } catch (URISyntaxException e) {
            throw new IllegalState("Epic fail creating URI from UUID!", e);
        }
        WarcRecord record = WarcRecord.createRecord(writer.writer);
        WarcHeader header = record.header;
        header.warcTypeIdx = WarcConstants.RT_IDX_RESOURCE;
        header.addHeader(WarcConstants.FN_WARC_RECORD_ID, recordId, null);
        header.addHeader(WarcConstants.FN_WARC_DATE, new Date(fetchBeginTimeStamp), null);
        header.addHeader(WarcConstants.FN_WARC_WARCINFO_ID, warcInfoUID, null);
        header.addHeader(WarcConstants.FN_WARC_IP_ADDRESS, hostIP);
        header.addHeader(WarcConstants.FN_WARC_TARGET_URI, uri);
        header.addHeader(WarcConstants.FN_WARC_BLOCK_DIGEST, blockDigest, null);
        header.addHeader(WarcConstants.FN_CONTENT_TYPE, ContentType.parseContentType(contentType), null);
        header.addHeader(WarcConstants.FN_CONTENT_LENGTH, new Long(payload.length), null);
        writer.writer.writeHeader(record);
        in = new ByteArrayInputStream(payload); // A re-read is necessary here!
        writer.writer.streamPayload(in);
        writer.writer.closeRecord();
    }

}
