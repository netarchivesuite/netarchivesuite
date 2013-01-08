/*$Id: ArcRepositoryDatabaseTester.java 2588 2012-12-20 12:56:29Z svc $
* $Revision: 2588 $
* $Date: 2012-12-20 13:56:29 +0100 (Thu, 20 Dec 2012) $
* $Author: svc $
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.indexserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;

/** A batch job that extracts metadata. */
public class GetMetadataArchiveBatchJob extends ArchiveBatchJob {
    /** The logger for this class. */
    private final Log log = LogFactory.getLog(getClass());
    /** The pattern for matching the urls.*/
    private final Pattern urlMatcher;
    /** The pattern for the mimetype matcher.*/
    private final Pattern mimeMatcher;
    
    /**
     * Constructor.
     * 
     * @param urlMatcher A pattern for matching URLs of the desired entries.
     * If null, a .* pattern will be used.
     * @param mimeMatcher A pattern for matching mime-types of the desired
     * entries.  If null, a .* pattern will be used.
     * 
     * The batchJobTimeout is set to one day. 
     */
    public GetMetadataArchiveBatchJob(Pattern urlMatcher, Pattern mimeMatcher) {
        this.urlMatcher = urlMatcher;
        this.mimeMatcher = mimeMatcher;
        
        batchJobTimeout = Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Initialize method. Run before the arc-records are being processed.
     * Currently does nothing.
     * 
     * @param os The output stream to print any pre-processing data.
     */
    @Override
    public void initialize(OutputStream os) { }

    /**
     * The method for processing the arc-records.
     * 
     * @param record The arc-record to process.
     * @param os The output stream to write the results of the processing.
     * @throws IOFailure In an IOException is caught during handling of 
     * the arc record.
     */
    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) 
            throws IOFailure {
        ArchiveHeaderBase header = record.getHeader();
        InputStream in = record.getInputStream();
        
        if (header.getUrl() == null) {
            return;
        }
        log.info(header.getUrl() + " - " + header.getMimetype());
        if (urlMatcher.matcher(header.getUrl()).matches()
                && mimeMatcher.matcher(
                        header.getMimetype()).matches()) {
            try {
                byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    os.write(buf, 0, bytesRead);
                }
            } catch (IOException e) {
                // TODO is getOffset() correct using the IA archiveReader?
                String message = "Error writing body of Archive entry '"
                        + header.getArchiveFile() + "' offset '"
                        + header.getOffset() + "'";
                throw new IOFailure(message, e);
            }
        }

        try {
            in.close();
        } catch (IOException e) {
            String message = "Error closing Archive input stream";
            throw new IOFailure(message, e);
        }
    }

    /**
     * Method for post-processing the data.
     * Currently does nothing.
     * 
     * @param os The output stream to write the results of the 
     * post-processing data.
     */
    @Override
    public void finish(OutputStream os) { }
    
    /**
     * Humanly readable description of this instance.
     * 
     * @return The human readable description of this instance.
     */
    @Override
    public String toString() {
        return getClass().getName() + ", with arguments: URLMatcher = " 
        + urlMatcher + ", mimeMatcher = " + mimeMatcher;
    }
}