/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils.cdx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/**
 * Batch job that extracts information to create a CDX file.
 * <p>
 * A CDX file contains sorted lines of metadata from the ARC/WARC files, with each line followed by the file and offset
 * the record was found at, and optionally a checksum. The timeout of this job is 7 days. See
 * http://www.archive.org/web/researcher/cdx_file_format.php
 */
@SuppressWarnings({"serial", "unused"})
public class ArchiveExtractCDXJob extends ArchiveBatchJob {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ArchiveExtractCDXJob.class);

    /** An encoding for the standard included metadata fields without checksum. */
    private static final String[] STD_FIELDS_EXCL_CHECKSUM = {"A", "e", "b", "m", "n", "g", "v"};

    /** An encoding for the standard included metadata fields with checksum. */
    private static final String[] STD_FIELDS_INCL_CHECKSUM = {"A", "e", "b", "m", "n", "g", "v", "c"};

    /** Buffer size used to read the http header. */
    private int HTTP_HEADER_BUFFER_SIZE = 1024 * 1024;

    /** The fields to be included in CDX output. */
    private String[] fields;

    /** True if we put an MD5 in each CDX line as well. */
    private boolean includeChecksum;

    /**
     * Constructs a new job for extracting CDX indexes.
     *
     * @param includeChecksum If true, an MD5 checksum is also written for each record. If false, it is not.
     */
    public ArchiveExtractCDXJob(boolean includeChecksum) {
        this.fields = includeChecksum ? STD_FIELDS_INCL_CHECKSUM : STD_FIELDS_EXCL_CHECKSUM;
        this.includeChecksum = includeChecksum;
        batchJobTimeout = 7 * Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Equivalent to ArchiveExtractCDXJob(true).
     */
    public ArchiveExtractCDXJob() {
        this(true);
    }

    /**
     * Filters out the NON-RESPONSE records.
     *
     * @return The filter that defines what ARC/WARC records are wanted in the output CDX file.
     * @see dk.netarkivet.common.utils.archive.ArchiveBatchJob#getFilter()
     */
    @Override
    public ArchiveBatchFilter getFilter() {
        return ArchiveBatchFilter.EXCLUDE_NON_RESPONSE_RECORDS;
    }

    /**
     * Initialize any data needed (none).
     *
     * @see dk.netarkivet.common.utils.archive.ArchiveBatchJob#initialize(OutputStream)
     */
    @Override
    public void initialize(OutputStream os) {
    }

    /**
     * Process this entry, reading metadata into the output stream.
     *
     * @throws IOFailure on trouble reading arc record data
     * @see dk.netarkivet.common.utils.archive.ArchiveBatchJob#processRecord(ArchiveRecordBase, OutputStream)
     */
    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) {
        log.trace("Processing Archive Record with offset: {}", record.getHeader().getOffset());
        /*
         * Fields are stored in a map so that it's easy to pull them out when looking at the fieldarray.
         */
        ArchiveHeaderBase header = record.getHeader();
        Map<String, String> fieldsread = new HashMap<String, String>();
        fieldsread.put("A", header.getUrl());
        fieldsread.put("e", header.getIp());
        fieldsread.put("b", header.getArcDateStr());
        fieldsread.put("n", Long.toString(header.getLength()));
        fieldsread.put("g", record.getHeader().getArchiveFile().getName());
        fieldsread.put("v", Long.toString(record.getHeader().getOffset()));

        String mimeType = header.getMimetype();
        String msgType;
        ContentType contentType = ContentType.parseContentType(mimeType);
        boolean bResponse = false;
        boolean bRequest = false;
        if (contentType != null) {
            if ("application".equals(contentType.contentType) && "http".equals(contentType.mediaType)) {
                msgType = contentType.getParameter("msgtype");
                if ("response".equals(msgType)) {
                    bResponse = true;
                } else if ("request".equals(msgType)) {
                    bRequest = true;
                }
            }
            mimeType = contentType.toStringShort();
        }
        ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(record.getInputStream(), HTTP_HEADER_BUFFER_SIZE);
        HttpHeader httpResponse = null;
        if (bResponse) {
            try {
                httpResponse = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, pbin, header.getLength(), null);
                if (httpResponse != null && httpResponse.contentType != null) {
                    contentType = ContentType.parseContentType(httpResponse.contentType);
                    if (contentType != null) {
                        mimeType = contentType.toStringShort();
                    }
                }
            } catch (IOException e) {
                throw new IOFailure("Error reading httpresponse header", e);
            }
        }
        fieldsread.put("m", mimeType);

        /* Only include checksum if necessary: */
        if (includeChecksum) {
            // InputStream instream = sar; //Note: ARCRecord extends InputStream
            // fieldsread.put("c", MD5.generateMD5(instream));
            fieldsread.put("c", ChecksumCalculator.calculateMd5(pbin));
        }

        if (httpResponse != null) {
            try {
                httpResponse.close();
            } catch (IOException e) {
                throw new IOFailure("Error closing httpresponse header", e);
            }
        }

        printFields(fieldsread, os);
    }

    /**
     * End of the batch job.
     *
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#finish(OutputStream)
     */
    @Override
    public void finish(OutputStream os) {
    }

    /**
     * Print the values found for a set of fields. Prints the '-' character for any null values.
     *
     * @param fieldsread A hashtable of values indexed by field letters
     * @param outstream The outputstream to write the values to
     */
    private void printFields(Map<String, String> fieldsread, OutputStream outstream) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < fields.length; i++) {
            Object o = fieldsread.get(fields[i]);
            sb.append((i > 0) ? " " : "");
            sb.append((o == null) ? "-" : o.toString());
        }
        sb.append("\n");
        try {
            outstream.write(sb.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            throw new IOFailure("Error writing CDX line '" + sb + "' to batch outstream", e);
        }
    }

    /**
     * @return Humanly readable description of this instance.
     */
    public String toString() {
        return getClass().getName() + ", with Filter: " + getFilter() + ", include checksum = " + includeChecksum;
    }

}
