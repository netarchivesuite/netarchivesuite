/* File:        $Id$
*  Date:        $Date$
*  Revision:    $Revision$
*  Author:      $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.common.utils.cdx;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCRecord;


/** Batch job that extracts information to create a CDX file.
 *
 * A CDX file contains sorted lines of metadata from the ARC files, with
 * each line followed by the file and offset the record was found at, and
 * optionally a checksum.
 * See http://www.archive.org/web/researcher/cdx_file_format.php
 */
public class ExtractCDXJob extends ARCBatchJob {

    /** An encoding for the standard included metadata fields without
     * checksum.*/
    private static final String[] STD_FIELDS_EXCL_CHECKSUM = {
            "A", "e", "b", "m", "n", "g", "v"
        };

    /** An encoding for the standard included metadata fields with checksum. */
    private static final String[] STD_FIELDS_INCL_CHECKSUM = {
            "A", "e", "b", "m", "n", "g", "v", "c"
        };

    /** The fields to be included in CDX output. */
    private String[] fields;

    /** True if we put an MD5 in each CDX line as well. */
    private boolean includeChecksum;

    /**
     * Logger for this class.
     */
    private final Log log = LogFactory.getLog(getClass().getName());
    
    /**
     * Constructs a new job for extracting CDX indexes.
     * @param includeChecksum If true, an MD5 checksum is also
     * written for each record. If false, it is not.
     */
    public ExtractCDXJob(boolean includeChecksum) {
        this.fields = includeChecksum ? STD_FIELDS_INCL_CHECKSUM
                                      : STD_FIELDS_EXCL_CHECKSUM;
        this.includeChecksum = includeChecksum;
    }

    /**
     * Equivalent to ExtractCDXJob(true).
     */
    public ExtractCDXJob() {
        this(true);
    }

    /** Filter out the filedesc: headers.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#getFilter()
     * @return The filter that defines what ARC records are wanted
     * in the output CDX file.
     */
    public ARCBatchFilter getFilter() {
        //Per default we want to index all records except ARC file headers:
        return ARCBatchFilter.EXCLUDE_FILE_HEADERS;
    }

    /**
     * Initialize any data needed (none).
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#initialize(OutputStream)
     */
    public void initialize(OutputStream os) {
    }

    /** Process this entry, reading metadata into the output stream.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#processRecord(
     * ARCRecord, OutputStream)
     * @throws IOFailure on trouble reading arc record data
     */
    public void processRecord(ARCRecord sar, OutputStream os) {
        log.trace("Processing ARCRecord with offset: " + sar.getMetaData().getOffset());
        /*
        * Fields are stored in a map so that it's easy
        * to pull them out when looking at the
        * fieldarray.
        */
        Map<String,String> fieldsread = new HashMap<String,String>();
        fieldsread.put("A", sar.getMetaData().getUrl());
        fieldsread.put("e", sar.getMetaData().getIp());
        fieldsread.put("b", sar.getMetaData().getDate());
        fieldsread.put("m", sar.getMetaData().getMimetype());
        fieldsread.put("n", Long.toString(sar.getMetaData().getLength()));

        /* Note about offset:
        * The original dk.netarkivet.ArcUtils.ExtractCDX
        * yields offsets that are consistently 1 lower
        * than this version, which pulls the offset value
        * from the org.archive.io.arc-classes.
        * This difference is that the former classes
        * count the preceeding newline as part of the
        * ARC header.
        */
        fieldsread.put("v", Long.toString(sar.getMetaData().getOffset())); 
        fieldsread.put("g", sar.getMetaData().getArcFile().getName());

        /* Only include checksum if necessary: */
        if (includeChecksum) {
            // To avoid taking all of the record into an array, we
            // slurp it directly from the ARCRecord.  This leaves the
            // sar in an inconsistent state, so it must not be used
            // afterwards.
            InputStream instream = sar; //Note: ARCRecord extends InputStream
            fieldsread.put("c", MD5.generateMD5(instream));
        }

        printFields(fieldsread, os);
    }

    /** End of the batch job.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#finish(OutputStream)
     */
    public void finish(OutputStream os) {
    }

    /** Print the values found for a set of fields.  Prints the '-'
     * character for any null values.
     *
     * @param fieldsread A hashtable of values indexed by field letters
     * @param outstream
     */
    private void printFields(Map fieldsread, OutputStream outstream) {
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
            throw new IOFailure("Error writing CDX line '"
                    + sb + "' to batch outstream", e);
        }
    }
    
    /**
     * Humanly readable description of this instance.
     */
    public String toString() {
	return getClass().getName() + ", with Filter: " + getFilter()
	+ ", include checksum = " + includeChecksum;
    }
}
