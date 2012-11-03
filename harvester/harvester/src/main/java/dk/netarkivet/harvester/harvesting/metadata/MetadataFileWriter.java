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
 */
package dk.netarkivet.harvester.harvesting.metadata;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.FileUtils.FilenameParser;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;

/**
 * Abstract base class for Metadata file writer.
 * Implementations must extend this class.
 *
 * @author nicl
 */
public abstract class MetadataFileWriter {

    /** Logging output place. */
    private static final Log log = LogFactory.getLog(MetadataFileWriter.class);
    /** Constant representing the ARC format. */
    protected static final int MDF_ARC = 1;
    /** Constant representing the WARC format. */
    protected static final int MDF_WARC = 2;
    /** Constant representing the metadata Format. Recognized formats are either MDF_ARC
     * or MDF_WARC */
    protected static int metadataFormat = 0;

    /**
     * Initialize the used metadata format from settings.  
     */
    protected static synchronized void initializeMetadataFormat() {
        String metadataFormatSetting = Settings.get(HarvesterSettings.METADATA_FORMAT);
        if ("arc".equalsIgnoreCase(metadataFormatSetting)) {
            metadataFormat = MDF_ARC;
        } else if ("warc".equalsIgnoreCase(metadataFormatSetting)) {
            metadataFormat = MDF_WARC;
        } else {
            throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.METADATA_FORMAT 
                    + "' is invalid! Unrecognized format '"
                    + metadataFormatSetting + "'.");
        }
    }

    /**
     * Generates a name for an archive(ARC/WARC) file containing "preharvest" metadata
     * regarding a given job (e.g. excluded aliases).
     *
     * @param jobID the number of the harvester job
     * @return The file name to use for the preharvest metadata, as a String.
     * @throws ArgumentNotValid If jobId is negative
     */
    public static String getPreharvestMetadataArchiveFileName(long jobID)
        throws ArgumentNotValid {
        ArgumentNotValid.checkNotNegative(jobID, "jobID");
        if (metadataFormat == 0) {
            initializeMetadataFormat();
        }
        switch (metadataFormat) {
        case MDF_ARC:
            return jobID + "-preharvest-metadata-" + 1 + ".arc";
        case MDF_WARC:
            return jobID + "-preharvest-metadata-" + 1 + ".warc";
        default:
        	throw new ArgumentNotValid("Configuration of '" 
        	        + HarvesterSettings.METADATA_FORMAT + "' is invalid!");
        }
    }

    /**
     * Generates a name for an archive(ARC/WARC) file containing metadata regarding
     * a given job.
     *
     * @param jobID The number of the job that generated the archive file.
     * @return A "flat" file name (i.e. no path) containing the jobID parameter
     * and ending on "-metadata-N.(w)arc", where N is the serial number of the
     * metadata files for this job, e.g. "42-metadata-1.(w)arc".  Currently,
     * only one file is ever made.
     * @throws ArgumentNotValid if any parameter was null.
     */
    public static String getMetadataArchiveFileName(String jobID)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        if (metadataFormat == 0) {
            initializeMetadataFormat();
        }
        switch (metadataFormat) {
        case MDF_ARC:
            return jobID + "-metadata-" + 1 + ".arc";
        case MDF_WARC:
            return jobID + "-metadata-" + 1 + ".warc";
        default:
            throw new ArgumentNotValid("Configuration of '"
                    + HarvesterSettings.METADATA_FORMAT + "' is invalid!");
        }
    }
    
    /**
     * Create a writer that writes data to the given archive file.
     * @param metadataArchiveFile The archive file to write to.
     * @return a writer that writes data to the given archive file.
     */
    public static MetadataFileWriter createWriter(File metadataArchiveFile) {
        if (metadataFormat == 0) {
            initializeMetadataFormat();
        }
        switch (metadataFormat) {
        case MDF_ARC:
        	return MetadataFileWriterArc.createWriter(metadataArchiveFile);
        case MDF_WARC:
        	return MetadataFileWriterWarc.createWriter(metadataArchiveFile);
        default:
        	throw new ArgumentNotValid("Configuration of '" 
        	        + HarvesterSettings.METADATA_FORMAT + "' is invalid!");
        }
    }
    
    /**
     * Close the metadatafile Writer.
     */
    public abstract void close();
    
    /**
     * @return the finished metadataFile
     */
    public abstract File getFile();
    
    /**
     * Insert given metadata file into the destination archive file.
     * @param metadataFile A given metadata file to insert 
     */
    public abstract void insertMetadataFile(File metadataFile);
    
    /**
     * Write the given file to the metadata file.
     * @param file A given file with metadata to write to the metadata archive file.
     * @param uri The uri associated with the piece of metadata
     * @param mime The mimetype associated with the piece of metadata
     */
    public abstract void writeFileTo(File file, String uri, String mime);

    /** Writes a File to an ARCWriter, if available,
     * otherwise logs the failure to the class-logger.
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     */
    public abstract boolean writeTo(File fileToArchive, String URL, String mimetype);

    /** 
     * Write a record to the archive file.
     * @param uri record URI
     * @param contentType  content-type of record
     * @param hostIP resource ip-address
     * @param fetchBeginTimeStamp record datetime
     * @param recordLength record length
     * @param in input stream of data to be written as record payload
     * @see ARCWriter#write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in
     */
    public abstract void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in) throws java.io.IOException;

    /**
     * Append the files contained in the directory to the metadata file, but
     * only if the filename matches the supplied filter.
     * @param parentDir directory containing files to append to metadata
     * @param filter filter describing which files to ignore
     * @param mimetype content-type write along with the files in the metadata output
     */
    public void insertFiles(File parentDir, FilenameFilter filter, String mimetype) {
        //For each CDX file...
        File[] cdxFiles
                = parentDir.listFiles(filter);
        for (File cdxFile : cdxFiles) {
            //...write its content to the MetadataFileWriter
        	writeFileTo(cdxFile,
                    getURIforFileName(cdxFile).toASCIIString(),
                    mimetype);
            //...and delete it afterwards
            try {
                FileUtils.remove(cdxFile);
            } catch (IOFailure e) {
                log.warn("Couldn't delete file '"
                         + cdxFile.getAbsolutePath()
                         + "' after adding in metadata file, ignoring.",
                         e);
            }
        }
    }

    /**
     * Parses the name of the given file
     * and generates a URI representation of it.
     * @param cdx A CDX file.
     * @return A URI appropriate for identifying the
     * file's content in Netarkivet.
     * @throws UnknownID if something goes terribly wrong in the CDX URI
     * construction.
     */
    private static URI getURIforFileName(File cdx)
        throws UnknownID {
        FilenameParser parser = new FilenameParser(cdx);
        return HarvestDocumentation.getCDXURI(
                parser.getHarvestID(),
                parser.getJobID(),
                parser.getTimeStamp(),
                parser.getSerialNo());
    }

    /**
     * Reset the metadata format. Should only be used by a unittest.
     */
    public static void resetMetadataFormat() {
        metadataFormat = 0;
    }
    
}
