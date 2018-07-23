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

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Abstract base class for Metadata file writer. Implementations must extend this class.
 *
 * @author nicl
 */
public abstract class MetadataFileWriter {

    /** Logging output place. */
    private static final Logger log = LoggerFactory.getLogger(MetadataFileWriter.class);

    /** Constant representing the ARC format. */
    public static final int MDF_ARC = 1;
    /** Constant representing the WARC format. */
    public static final int MDF_WARC = 2;
    /** Constant representing the metadata Format. Recognized formats are either MDF_ARC or MDF_WARC */
    protected static int metadataFormat = 0;

    /** Constants used in constructing URI for CDX content. */
    protected static final String CDX_URI_SCHEME = "metadata";
    private static final String CDX_URI_AUTHORITY_HOST = Settings.get(CommonSettings.ORGANIZATION);
    private static final String CDX_URI_PATH = "/crawl/index/cdx";
    private static final String CDX_URI_VERSION_PARAMETERS = "majorversion=2&minorversion=0";
    private static final String ALTERNATE_CDX_URI_VERSION_PARAMETERS = "majorversion=3&minorversion=0";

    private static final String CDX_URI_HARVEST_ID_PARAMETER_NAME = "harvestid";
    private static final String CDX_URI_JOB_ID_PARAMETER_NAME = "jobid";
    private static final String CDX_URI_FILENAME_PARAMETER_NAME = "filename";

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
            throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.METADATA_FORMAT + "' is invalid! "
                    + "Unrecognized format '" + metadataFormatSetting + "'.");
        }
    }

    /**
     * Generates a name for an archive(ARC/WARC) file containing metadata regarding a given job.
     *
     * @param jobID The number of the job that generated the archive file.
     * @return A "flat" file name (i.e. no path) containing the jobID parameter and ending on "-metadata-N.(w)arc",
     * where N is the serial number of the metadata files for this job, e.g. "42-metadata-1.(w)arc". Currently, only one
     * file is ever made.
     * @throws ArgumentNotValid if any parameter was null.
     */
    public static String getMetadataArchiveFileName(String jobID, Long harvestID) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        //retrieving the collectionName
        String collectionName = "";
        boolean isPrefix = false;
        //try to retrieve settings for prefixing or not metadata files
        String metadataFilenameFormat = "";
        try {
        	metadataFilenameFormat = Settings.get(HarvesterSettings.METADATA_FILENAME_FORMAT);
        } catch (UnknownID e) {
        	//nothing
        }
        if("prefix".equals(metadataFilenameFormat)) {
            try {
                //try to retrieve in both <heritrix> and <heritrix3> tags
                collectionName = Settings.get(HarvesterSettings.HERITRIX_PREFIX_COLLECTION_NAME);
                isPrefix = true;
            } catch(UnknownID e) {
                //nothing
            }
		}
        if (metadataFormat == 0) {
            initializeMetadataFormat();
        }
        boolean compressionOn = compressRecords();
        String possibleGzSuffix = "";
        if (compressionOn) {
            possibleGzSuffix = ".gz";
        }
        int versionNumber = Settings.getInt(HarvesterSettings.METADATA_FILE_VERSION_NUMBER);
        switch (metadataFormat) {
        case MDF_ARC:
            if(isPrefix) {
                return collectionName + "-" + jobID + "-" + harvestID + "-metadata-" + versionNumber + ".arc" + possibleGzSuffix;
            } else {
                return jobID + "-metadata-" + versionNumber + ".arc" + possibleGzSuffix;
            }
        case MDF_WARC:
            if(isPrefix) {
                return collectionName + "-" + jobID + "-" + harvestID + "-metadata-" + versionNumber + ".warc" + possibleGzSuffix;
            } else {
                return jobID + "-metadata-" + versionNumber + ".warc" + possibleGzSuffix;
            }
        default:
            throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.METADATA_FORMAT + "' is invalid!");
        }
    }

    /**
     * Create a writer that writes data to the given archive file.
     *
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
            throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.METADATA_FORMAT + "' is invalid!");
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
     * Write the given file to the metadata file.
     *
     * @param file A given file with metadata to write to the metadata archive file.
     * @param uri The uri associated with the piece of metadata
     * @param mime The mimetype associated with the piece of metadata
     */
    public abstract void writeFileTo(File file, String uri, String mime);

    /**
     * Writes a File to an ARCWriter, if available, otherwise logs the failure to the class-logger.
     *
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     */
    public abstract boolean writeTo(File fileToArchive, String URL, String mimetype);

    /**
     * Write a record to the archive file.
     *
     * @param uri record URI
     * @param contentType content-type of record
     * @param hostIP resource ip-address
     * @param fetchBeginTimeStamp record datetime
     * @param payload A byte array containing the payload
     * @see org.archive.io.arc.ARCWriter#write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp,
     * long recordLength, InputStream in)
     */
    public abstract void write(String uri, String contentType, String hostIP, long fetchBeginTimeStamp, byte[] payload)
            throws java.io.IOException;

    /**
     * Append the files contained in the directory to the metadata archive file, but only if the filename matches the
     * supplied filter.
     *
     * @param parentDir directory containing the files to append to metadata
     * @param filter filter describing which files to accept and which to ignore
     * @param mimetype The content-type to write along with the files in the metadata output
     * @param harvestId The harvestId of the harvest
     * @param jobId The jobId of the harvest 
     */
    public void insertFiles(File parentDir, FilenameFilter filter, String mimetype, long harvestId, long jobId) {
        // For each metadata source file in the parentDir that matches the filter ..
        File[] metadataSourceFiles = parentDir.listFiles(filter);
        log.debug("Now inserting " + metadataSourceFiles.length + " files from " + parentDir.getAbsolutePath() + "'.");
        for (File metadataSourceFile : metadataSourceFiles) {
            // ...write its content to the MetadataFileWriter
            log.debug("Inserting the file '{}'", metadataSourceFile.getAbsolutePath());
            writeFileTo(metadataSourceFile, getURIforFileName(metadataSourceFile, harvestId, jobId).toASCIIString(), mimetype);
            // ...and delete it afterwards
            try {
                FileUtils.remove(metadataSourceFile);
            } catch (IOFailure e) {
                log.warn("Couldn't delete file '{}' after adding to metadata archive file, ignoring.",
                        metadataSourceFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Parses the name of the given file and generates a URI representation of it.
     *
     * @param cdx A CDX file.
     * @param harvestID The harvestId of the harvest
     * @param jobId	The jobId of the harvest
     * @return A URI appropriate for identifying the file's content in Netarkivet
     * @throws UnknownID if something goes terribly wrong in the CDX URI construction
     */
    private static URI getURIforFileName(File cdx, long harvestId, long jobId) throws UnknownID {
        String extensionToRemove = FileUtils.CDX_EXTENSION;
        String filename = cdx.getName();
        if (!filename.endsWith(extensionToRemove)) {
            throw new IllegalState("Filename '" + cdx.getAbsolutePath() + "' has unexpected extension");
        }
        int suffix_index = cdx.getName().indexOf(extensionToRemove);
        filename = filename.substring(0, suffix_index);
        return getCDXURI("" + harvestId, "" + jobId, filename);
    }

    /**
     * Reset the metadata format. Should only be used by a unittest.
     */
    public static void resetMetadataFormat() {
        metadataFormat = 0;
    }
    
    
    /**
     * Generates a URI identifying CDX info for one harvested (W)ARC file. In Netarkivet, all of the parameters below
     * are in the (W)ARC file's name.
     *
     * @param harvestID The number of the harvest that generated the (W)ARC file.
     * @param jobID The number of the job that generated the (W)ARC file.
     * @param filename The name of the ARC or WARC file behind the cdx-data
     * @return A URI in the proprietary schema "metadata".
     * @throws ArgumentNotValid if any parameter is null.
     * @throws UnknownID if something goes terribly wrong in our URI construction.
     */
    public static URI getCDXURI(String harvestID, String jobID, String filename) throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(harvestID, "harvestID");
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        ArgumentNotValid.checkNotNull(filename, "filename");
        URI result;
        try {
            result = new URI(CDX_URI_SCHEME, null, // Don't include user info (e.g. "foo@")
                    CDX_URI_AUTHORITY_HOST, -1, // Don't include port no. (e.g. ":8080")
                    CDX_URI_PATH, getCDXURIQuery(harvestID, jobID, filename), null); // Don't include fragment (e.g.
            // "#foo")
        } catch (URISyntaxException e) {
            throw new UnknownID("Failed to generate URI for " + harvestID + "," + jobID + "," + filename + ",", e);
        }
        return result;
    }
    
    /**
     * Generates a URI identifying CDX info for one harvested ARC file.
     *
     * @param jobID The number of the job that generated the ARC file.
     * @param filename the filename.
     * @return A URI in the proprietary schema "metadata".
     * @throws ArgumentNotValid if any parameter is null.
     * @throws UnknownID if something goes terribly wrong in our URI construction.
     */
    public static URI getAlternateCDXURI(long jobID, String filename) throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(jobID, "jobID");
        ArgumentNotValid.checkNotNull(filename, "filename");
        URI result;
        try {
            result = new URI(CDX_URI_SCHEME, null, // Don't include user info (e.g. "foo@")
                    CDX_URI_AUTHORITY_HOST, -1, // Don't include port no. (e.g. ":8080")
                    CDX_URI_PATH, getAlternateCDXURIQuery(jobID, filename), null); // Don't include fragment (e.g.
            // "#foo")
        } catch (URISyntaxException e) {
            throw new UnknownID("Failed to generate URI for " + jobID + "," + filename + ",", e);
        }
        return result;
    }

    /**
     * Generate the query part of a CDX URI.
     *
     * @param harvestID The number of the harvest that generated the ARC file.
     * @param jobID The number of the job that generated the ARC file.
     * @param filename The name of the ARC file.
     * @return An appropriate list of assigned parameters, separated by the "&" character.
     */
    private static String getCDXURIQuery(String harvestID, String jobID, String filename) {
        String result = CDX_URI_VERSION_PARAMETERS;
        result += "&" + CDX_URI_HARVEST_ID_PARAMETER_NAME + "=" + harvestID;
        result += "&" + CDX_URI_JOB_ID_PARAMETER_NAME + "=" + jobID;
        result += "&" + CDX_URI_FILENAME_PARAMETER_NAME + "=" + filename;

        return result;
    }

    /**
     * Generate the query part of a CDX URI. Alternate version
     *
     * @param jobID The number of the job that generated the (W)ARC file.
     * @param filename the filename of the archive file
     * @return An appropriate list of assigned parameters, separated by the "&" character.
     */
    private static String getAlternateCDXURIQuery(long jobID, String filename) {
        String result = ALTERNATE_CDX_URI_VERSION_PARAMETERS;
        result += "&" + CDX_URI_JOB_ID_PARAMETER_NAME + "=" + jobID;
        result += "&" + CDX_URI_FILENAME_PARAMETER_NAME + "=" + filename;
        return result;
    }
    
    /**
     * @return true, if we want to compress out metadata records, false, if not
     */
    public static boolean compressRecords() {
        return Settings.getBoolean(HarvesterSettings.METADATA_COMPRESSION);
    }
    
}
