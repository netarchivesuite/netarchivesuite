/* File:        $Id: CDXUtils.java 2420 2012-07-31 14:42:21Z nicl@kb.dk $
 * Revision:    $Revision: 2420 $
 * Author:      $Author: nicl@kb.dk $
 * Date:        $Date: 2012-07-31 16:42:21 +0200 (Tue, 31 Jul 2012) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCWriter;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCUtils;

public class MetadataFileWriterArc extends MetadataFileWriter {

    private static final Log log = LogFactory.getLog(MetadataFileWriterArc.class);

    /** Writer to this jobs metadatafile.
     * This is closed when the metadata is marked as ready.
     */
    private ARCWriter writer = null;

    public static MetadataFileWriter createWriter(File metadataFile) {
    	MetadataFileWriterArc mtfw = new MetadataFileWriterArc();
    	mtfw.writer = ARCUtils.createARCWriter(metadataFile);
    	return mtfw;
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
	public void insertMetadataFile(File metadataFile) {
	    ARCUtils.insertARCFile(metadataFile, writer);
	}

    @Override
    public void writeFileTo(File file, String uri, String mime) {
    	ARCUtils.writeFileToARC(writer, file, uri, mime);
	}

    /** Writes a File to an ARCWriter, if available,
     * otherwise logs the failure to the class-logger.
     * @param writer the given ARCWriter
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     *
     * TODO I wonder if this is a clone of the ARCUtils method. (nicl)
     */
    @Override
    public boolean writeTo(File fileToArchive, String URL, String mimetype) {
        if (fileToArchive.isFile()) {
            try {
                ARCUtils.writeFileToARC(writer, fileToArchive,
                        URL, mimetype);
            } catch (IOFailure e) {
                log.warn("Error writing file '"
                        + fileToArchive.getAbsolutePath()
                        + "' to metadata file: ", e);
                return false;
            }
            log.debug("Wrote '" + fileToArchive.getAbsolutePath() + "' to '"
                      + writer.getFile().getAbsolutePath() + "'.");
            return true;
        } else {
            log.debug("No '" + fileToArchive.getName()
                      + "' found in dir: " + fileToArchive.getParent());
            return false;
        }
    }

    /* Copied from the ARCWriter. */
    @Override
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in) throws IOException {
    	writer.write(uri, contentType, hostIP, fetchBeginTimeStamp, recordLength, in);
    }

}
