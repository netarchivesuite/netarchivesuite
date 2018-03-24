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
import java.util.regex.Pattern;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * Job to get cdx records out of metadata files.
 */
@SuppressWarnings({"serial"})
public class GetCDXRecordsBatchJob extends ARCBatchJob {

    /** The URL pattern used to retrieve the CDX-records. */
    private final Pattern URLMatcher;
    /** The MIME pattern used to retrieve the CDX-records. */
    private final Pattern mimeMatcher;

    /**
     * Constructor.
     */
    public GetCDXRecordsBatchJob() {
        URLMatcher = Pattern.compile(Constants.ALL_PATTERN);
        mimeMatcher = Pattern.compile(Constants.CDX_MIME_PATTERN);
        batchJobTimeout = 7 * Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Initialize job. Does nothing
     *
     * @param os The output stream (unused in this implementation)
     */
    public void initialize(OutputStream os) {
    }

    /**
     * Process a single ARCRecord if the record contains cdx.
     *
     * @param sar The record we want to process
     * @param os The output stream to write the result to
     */
    public void processRecord(ARCRecord sar, OutputStream os) {
        if (URLMatcher.matcher(sar.getMetaData().getUrl()).matches()
                && mimeMatcher.matcher(sar.getMetaData().getMimetype()).matches()) {
            try {
                try {
                    byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = sar.read(buf)) != -1) {
                        os.write(buf, 0, bytesRead);
                    }
                } finally {
                    // TODO Should we close ARCRecord here???
                    // if (is != null) {
                    // is.close();
                    // }
                }
            } catch (IOException e) {
                String message = "Error writing body of ARC entry '" + sar.getMetaData().getArcFile() + "' offset '"
                        + sar.getMetaData().getOffset() + "'";
                throw new IOFailure(message, e);
            }
        }
    }

    /**
     * Finish job. Does nothing
     *
     * @param os The Outputstream (unused in this implementation)
     */
    public void finish(OutputStream os) {
    }

}
