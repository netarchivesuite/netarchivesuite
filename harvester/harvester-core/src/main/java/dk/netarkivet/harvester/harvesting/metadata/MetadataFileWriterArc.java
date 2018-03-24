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
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.jwat.arc.ArcFileNaming;
import org.jwat.arc.ArcFileNamingSingleFile;
import org.jwat.arc.ArcFileWriter;
import org.jwat.arc.ArcFileWriterConfig;
import org.jwat.arc.ArcHeader;
import org.jwat.arc.ArcRecord;
import org.jwat.arc.ArcRecordBase;
import org.jwat.arc.ArcVersion;
import org.jwat.arc.ArcVersionBlock;
import org.jwat.arc.ArcVersionHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * MetadataFileWriter that writes to ARC files.
 */
public class MetadataFileWriterArc extends MetadataFileWriter {

    private static final Logger log = LoggerFactory.getLogger(MetadataFileWriterArc.class);

    /** Writer to this jobs metadatafile. This is closed when the metadata is marked as ready. */
    private ArcFileWriter writer = null;

    /**
     * Create a <code>MetadataFileWriter</code> for ARC output.
     *
     * @param metadataARCFile The metadata ARC <code>File</code>
     * @return <code>MetadataFileWriter</code> for writing metadata files in ARC
     */
    public static MetadataFileWriter createWriter(File metadataARCFile) {
        MetadataFileWriterArc mtfw = new MetadataFileWriterArc();
    	ArcFileNaming naming = new ArcFileNamingSingleFile(metadataARCFile);
    	ArcFileWriterConfig config = new ArcFileWriterConfig(metadataARCFile.getParentFile(), compressRecords(), Long.MAX_VALUE, true);
        mtfw.writer = ArcFileWriter.getArcWriterInstance(naming, config);
        mtfw.open();
        return mtfw;
    }

    protected void open() {
        ArcVersionHeader versionHeader;
        ArcRecordBase record;
        byte[] versionHeaderBytes;
        try {
            writer.open();
            versionHeader = ArcVersionHeader.create(ArcVersion.VERSION_1, "InternetArchive");
            versionHeader.rebuild();
            versionHeaderBytes = versionHeader.getHeader();
            record = ArcVersionBlock.createRecord(writer.writer);
            record.header.recordFieldVersion = 1;
            record.header.urlStr = "filedesc://" + writer.getFile().getName();
            record.header.ipAddressStr = "0.0.0.0";
            record.header.archiveDate = new Date();
            record.header.contentTypeStr = "text/plain";
            record.header.archiveLength = new Long(versionHeaderBytes.length);
            writer.writer.writeHeader(record);
            writer.writer.writePayload(versionHeaderBytes);
            writer.writer.closeRecord();
        } catch (IOException e) {
            throw new IOFailure("Error opening MetadataFileWriterArc", e);
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new IOFailure("Error closing MetadataFileWriterArc", e);
            }
            writer = null;
        }
    }

    @Override
    public File getFile() {
        return writer.getFile();
    }

    @Override
    public void writeFileTo(File file, String uri, String mime) {
        writeTo(file, uri, mime);
    }

    /**
     * Writes a File to an ArcWriter, if available, otherwise logs the failure to the class-logger.
     *
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     */
    @Override
    public boolean writeTo(File fileToArchive, String URL, String mimetype) {
        if (!fileToArchive.isFile()) {
            throw new IOFailure("Not a file: " + fileToArchive.getPath());
        }
        log.info("Writing file '{}' to ARC file: {}", fileToArchive, fileToArchive.length());
        InputStream in = null;
        try {
            ArcRecordBase record = ArcRecord.createRecord(writer.writer);
            ArcHeader header = record.header;
            header.urlStr = URL;
            header.archiveDate = new Date(fileToArchive.lastModified());
            header.ipAddressStr = SystemUtils.getLocalIP();
            header.contentTypeStr = mimetype;
            header.archiveLength = fileToArchive.length();
            in = new FileInputStream(fileToArchive);
            writer.writer.writeHeader(record);
            writer.writer.streamPayload(in);
            writer.writer.closeRecord();
        } catch (FileNotFoundException e) {
            throw new IOFailure("Unable to open file: " + fileToArchive.getPath(), e);
        } catch (IOException e) {
            throw new IOFailure("Epic IO fail while writing to ARC file: " + fileToArchive.getPath(), e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return true;
    }

    /* Copied from the ArcWriter. (Before change to JWAT) */
    @Override
    public void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, byte[] payload)
            throws IOException {
        ByteArrayInputStream in = null;
        try {
            ArcRecordBase record = ArcRecord.createRecord(writer.writer);
            ArcHeader header = record.header;
            header.urlStr = uri;
            header.archiveDate = new Date(fetchBeginTimeStamp);
            header.ipAddressStr = hostIP;
            header.archiveLength = new Long(payload.length);
            header.contentTypeStr = contentType;
            in = new ByteArrayInputStream(payload);
            writer.writer.writeHeader(record);
            writer.writer.streamPayload(in);
            writer.writer.closeRecord();
        } catch (IOException e) {
            throw new IOFailure("Epic IO fail while writing payload to ARC file.", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
