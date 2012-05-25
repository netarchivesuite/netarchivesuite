package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class MetadataFileWriter {

    public static MetadataFileWriter createWriter(File metadataFile) {
    	return MetadataFileWriterArc.createWriter(metadataFile);
    }

	public abstract void close() throws IOException;

	public abstract void insertMetadataFile(File metadataFile);

    public abstract void writeFileTo(File file, String uri, String mime);

    /** Writes a File to an ARCWriter, if available,
     * otherwise logs the failure to the class-logger.
     * @param writer the given ARCWriter
     * @param fileToArchive the File to archive
     * @param URL the URL with which it is stored in the arcfile
     * @param mimetype The mimetype of the File-contents
     * @return true, if file exists, and is written to the arcfile.
     */
    public abstract boolean writeTo(File fileToArchive, String URL, String mimetype);

    /* Copied from the ARCWriter. */
    public abstract void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in)
            									throws java.io.IOException;

}
