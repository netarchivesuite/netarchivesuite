package dk.netarkivet.wayback.hadoop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.util.Progressable;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import org.archive.wayback.resourcestore.indexer.ARCRecordToSearchResultAdapter;
import org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;
import dk.netarkivet.common.utils.batch.WARCBatchFilter;
import dk.netarkivet.wayback.batch.UrlCanonicalizerFactory;

/**
 * Class for creating CDX indexes from archive files.
 */
public class CDXIndexer implements Indexer {
    /** The warc record searcher.*/
    protected final WARCRecordToSearchResultAdapter warcAdapter;
    protected final ARCRecordToSearchResultAdapter arcAdapter;
    /** The CDX line creator, which creates the cdx lines from the warc records.*/
    protected final SearchResultToCDXLineAdapter cdxLineCreator;
    protected final UrlCanonicalizer urlCanonicalizer;

    /** Constructor.*/
    public CDXIndexer() {
        warcAdapter = new WARCRecordToSearchResultAdapter();
        arcAdapter = new ARCRecordToSearchResultAdapter();
        cdxLineCreator = new SearchResultToCDXLineAdapter();
        urlCanonicalizer = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
    }

    /**
     * Index the given archive file.
     * @param archiveInputStream An inputstream to the given file.
     * @param archiveName The name of the given file.
     * @return The extracted CDX lines from the file.
     * @throws IOException
     */
    public List<String> index(InputStream archiveInputStream, String archiveName, Progressable progressable) throws IOException {
        try (ArchiveReader archiveReader = ArchiveReaderFactory.get(archiveName, archiveInputStream, false)) {
            boolean isMetadataFile = archiveName.matches("(.*)" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX));
            if (isMetadataFile) {
                return extractMetadataCDXLines(archiveReader, progressable);
            } else {
                return extractCDXLines(archiveReader, progressable);
            }
        }
    }

    /**
     * Create the CDX indexes from an archive file.
     * @param archiveFile The archive file.
     * @return The CDX lines for the records in the archive file.
     * @throws IOException If it fails to read the archive file.
     */
    public List<String> indexFile(File archiveFile, Progressable progressable) throws IOException {
        return index(new FileInputStream(archiveFile), archiveFile.getName(), progressable);
    }

    /**
     * Extracts CDX lines from an ArchiveReader specifically for metadata files.
     * @param archiveReader The reader used for reading the archive file.
     * @return A list of the CDX lines for the records in the archive file.
     */
    private List<String> extractMetadataCDXLines(ArchiveReader archiveReader, Progressable progressable) {
        final int HTTP_HEADER_BUFFER_SIZE = 1024 * 1024;
        String[] fields = {"A", "e", "b", "m", "n", "g", "v"};
        List<String> cdxIndexes = new ArrayList<>();

        for (ArchiveRecord archiveRecord : archiveReader) {
            progressable.progress();
            ArchiveRecordBase record = ArchiveRecordBase.wrapArchiveRecord(archiveRecord);
            boolean isResponseRecord = ArchiveBatchFilter.EXCLUDE_NON_WARCINFO_RECORDS.accept(record);
            if (!isResponseRecord) {
                continue;
            }
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
    }

    /**
     * Method for extracting the cdx lines from an ArchiveReader.
     * @param reader The ArchiveReader which is actively reading an archive file (e.g WARC).
     * @return The list of CDX index lines for the records of the archive in the reader.
     */
    protected List<String> extractCDXLines(ArchiveReader reader, Progressable progressable) {
        List<String> res = new ArrayList<>();

        for (ArchiveRecord archiveRecord: reader) {
            progressable.progress();
            // TODO: look at logging something here
           if (archiveRecord instanceof WARCRecord) {
               WARCRecord warcRecord = (WARCRecord) archiveRecord;
               boolean isResponseRecord = WARCBatchFilter.EXCLUDE_NON_RESPONSE_RECORDS.accept(warcRecord);
               if (!isResponseRecord) {
                   continue;

               }
               warcAdapter.setCanonicalizer(urlCanonicalizer);
               //TODO this returns null and prints stack trace on OutOfMemoryError. Bad code. //jolf & abr
               CaptureSearchResult captureSearchResult = warcAdapter.adapt(warcRecord);
               if (captureSearchResult != null) {
                   res.add(cdxLineCreator.adapt(captureSearchResult));
               }

           } else {
               ARCRecord arcRecord = (ARCRecord) archiveRecord;
               boolean isResponseRecord = ARCBatchFilter.EXCLUDE_FILE_HEADERS.accept(arcRecord);
               if (!isResponseRecord) {
                   continue;
               }
               arcAdapter.setCanonicalizer(urlCanonicalizer);
               final CaptureSearchResult captureSearchResult = arcAdapter.adapt(arcRecord);
               if (captureSearchResult != null) {
                   res.add(cdxLineCreator.adapt(captureSearchResult));
               }
           }
        }
        return res;
    }
}
