package dk.netarkivet.viewerproxy.webinterface.hadoop;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;
import dk.netarkivet.common.utils.hadoop.HadoopFileUtils;

/**
 * Hadoop Mapper for creating CDX indexes for metadata files through the GUI application's QA pages.
 *
 * The input is a key (not used) and a Text line, which should be the path to the archive file.
 * The output is an exit code (not used), and the generated CDX lines.
 */
public class MetadataCDXMapper extends Mapper<LongWritable, Text, NullWritable, Text> {

    private static final Logger log = LoggerFactory.getLogger(MetadataCDXMapper.class);

    /**
     * Mapping method.
     *
     * @param linenumber The linenumber. Is ignored.
     * @param archiveFilePath The path to the archive file.
     * @param context Context used for writing output.
     * @throws IOException If it fails to generate the CDX indexes.
     */
    @Override
    protected void map(LongWritable linenumber, Text archiveFilePath, Context context) throws IOException,
            InterruptedException {
        // reject empty or null warc paths.
        if (archiveFilePath == null || archiveFilePath.toString().trim().isEmpty()) {
            log.warn("Encountered empty path in job {}", context.getJobID().toString());
            return;
        }

        Path path = new Path(archiveFilePath.toString());

        try (FileSystem hdfsFileSystem =
                FileSystem.newInstance(context.getConfiguration())) {
            path = HadoopFileUtils.replaceWithCachedPathIfEnabled(context, path);
            List<String> cdxIndexes;
            try (InputStream in = new BufferedInputStream(path.getFileSystem(context.getConfiguration()).open(path))) {
                log.info("CDX-indexing archive file '{}'", path);
                cdxIndexes = index(in, archiveFilePath.toString(), context);
            }
            for (String cdxIndex : cdxIndexes) {
                context.write(NullWritable.get(), new Text(cdxIndex));
            }
        }
    }

    /**
     * Extracts CDX lines from an inputstream representing a metadata archive file
     * @param archiveInputStream The inputstream the archive file is read from
     * @return A list of the CDX lines for the records in the archive file.
     */
    public List<String> index(InputStream archiveInputStream, String archiveName, Context context) throws IOException {
        try (ArchiveReader archiveReader = ArchiveReaderFactory.get(archiveName, archiveInputStream, false)) {
            //TODO if this is to be configurable then put it in the hadoop config. As is it always uses the default compiled-in version.
            boolean isMetadataFile = archiveName
                    .matches("(.*)" + context.getConfiguration().get(CommonSettings.METADATAFILE_REGEX_SUFFIX) );
            if (isMetadataFile) {
                final int HTTP_HEADER_BUFFER_SIZE = 1024 * 1024;
                String[] fields = {"A", "e", "b", "m", "n", "g", "v"};
                List<String> cdxIndexes = new ArrayList<>();

                for (ArchiveRecord archiveRecord : archiveReader) {
                    ArchiveRecordBase record = ArchiveRecordBase.wrapArchiveRecord(archiveRecord);
                    boolean isResponseRecord = ArchiveBatchFilter.EXCLUDE_NON_WARCINFO_RECORDS.accept(record);
                    if (!isResponseRecord) {
                        continue;
                    }
                    log.trace("Processing archive record in '{}' with offset: {}", archiveName, record.getHeader().getOffset());
                    ArchiveHeaderBase header = record.getHeader();
                    Map<String, String> fieldsRead = new HashMap<>();
                    fieldsRead.put("A", header.getUrl());
                    fieldsRead.put("e", header.getIp());
                    fieldsRead.put("b", header.getArcDateStr());
                    fieldsRead.put("n", Long.toString(header.getLength()));
                    fieldsRead.put("g", record.getHeader().getArchiveFile().getName());
                    fieldsRead.put("v", Long.toString(record.getHeader().getOffset()));

                    String mimeType = header.getMimetype();
                    String msgType;
                    ContentType contentType = ContentType.parseContentType(mimeType);
                    boolean bResponse = false;
                    if (contentType != null) {
                        if (contentType.contentType.equals("application") && contentType.mediaType.equals("http")) {
                            msgType = contentType.getParameter("msgtype");
                            if (msgType.equals("response")) {
                                bResponse = true;
                            }
                        }
                        mimeType = contentType.toStringShort();
                    }
                    ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(record.getInputStream(),
                            HTTP_HEADER_BUFFER_SIZE);
                    HttpHeader httpResponse = null;
                    if (bResponse) {
                        try {
                            httpResponse = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, pbin, header.getLength(), null);
                            if (httpResponse.contentType != null) {
                                contentType = ContentType.parseContentType(httpResponse.contentType);
                                if (contentType != null) {
                                    mimeType = contentType.toStringShort();
                                }
                            }
                        } catch (IOException e) {
                            throw new IOFailure("Error reading httpresponse header", e);
                        }
                    }
                    fieldsRead.put("m", mimeType);

                    if (httpResponse != null) {
                        try {
                            httpResponse.close();
                        } catch (IOException e) {
                            throw new IOFailure("Error closing httpresponse header", e);
                        }
                    }

                    // Build the cdx line
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < fields.length; i++) {
                        Object o = fieldsRead.get(fields[i]);
                        sb.append((i > 0) ? " " : "");
                        sb.append((o == null) ? "-" : o.toString());
                    }
                    cdxIndexes.add(sb.toString());
                }
                return cdxIndexes;
            } else {
                return new ArrayList<>();
            }
        }
    }
}
