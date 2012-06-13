package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.warc.WARCWriter;
import org.archive.util.anvl.ANVLRecord;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.common.utils.warc.WARCUtils;

public class MetadataFileWriterWarc extends MetadataFileWriter {

    private static final Log log = LogFactory.getLog(MetadataFileWriterWarc.class);

    /** Writer to this jobs metadatafile.
     * This is closed when the metadata is marked as ready.
     */
    private WARCWriter writer = null;

    public static MetadataFileWriter createWriter(File metadataFile) {
    	MetadataFileWriterWarc mtfw = new MetadataFileWriterWarc();
    	mtfw.writer = WARCUtils.createWARCWriter(metadataFile);
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
		WARCUtils.insertWARCFile(metadataFile, writer);
	}

	@Override
	public void writeFileTo(File file, String uri, String mime) {
		writeTo(file, uri, mime);
	}

	@Override
	public boolean writeTo(File fileToArchive, String URL, String mimetype) {
		log.info(fileToArchive + " " + fileToArchive.length());

		String create14DigitDate = ArchiveDateConverter.getWarcDateFormat().format(new Date());
		URI recordId;
		try {
			recordId = new URI(UUID.randomUUID().toString());
		} catch (URISyntaxException e) {
			throw new IllegalState("Epic fail creating URI from UUID!");
		}
		InputStream in = null;
		try {
			in = new FileInputStream(fileToArchive);
			ANVLRecord  namedFields = new ANVLRecord();
			namedFields.addLabelValue("X-Metadata-Version", "1");
	    	writer.writeMetadataRecord(URL, create14DigitDate, "x-nas/metadata", recordId, namedFields, in, fileToArchive.length());
		} catch (FileNotFoundException e) {
			throw new IOFailure("Unable to open file: " + fileToArchive.getPath(), e);
		} catch (IOException e) {
			throw new IOFailure("Epic IO fail while writing to WARC file: " + fileToArchive.getPath(), e);
		}
		return true;
	}

    @Override
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in)
            									throws java.io.IOException {
    	// hostIP?
    	String create14DigitDate = ArchiveDateConverter.getWarcDateFormat().format(new Date(fetchBeginTimeStamp));
		URI recordId;
		try {
			recordId = new URI(UUID.randomUUID().toString());
		} catch (URISyntaxException e) {
			throw new IllegalState("Epic fail creating URI from UUID!");
		}
    	writer.writeMetadataRecord(uri, create14DigitDate, contentType, recordId, null, in, recordLength);
    }

}
