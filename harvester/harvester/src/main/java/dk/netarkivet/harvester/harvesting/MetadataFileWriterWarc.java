package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.warc.WARCWriter;

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

	public void close() throws IOException {
    	if (writer != null) {
    		writer.close();
    		writer = null;
    	}
    }

	@Override
	public void insertMetadataFile(File metadataFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeFileTo(File file, String uri, String mime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean writeTo(File fileToArchive, String URL, String mimetype) {
		// TODO Auto-generated method stub
		return false;
	}

    /* Copied from the ARCWriter. */
    @Override
    public void write(String uri, String contentType, String hostIP,
            long fetchBeginTimeStamp, long recordLength, InputStream in)
            									throws java.io.IOException {
    	throw new UnsupportedOperationException();
    }

}
