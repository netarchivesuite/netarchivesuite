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
	public void close() throws IOException {
    	if (writer != null) {
    		writer.close();
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
            long fetchBeginTimeStamp, long recordLength, InputStream in)
            											throws IOException {
    	writer.write(uri, contentType, hostIP, fetchBeginTimeStamp, recordLength, in);
    }

}
