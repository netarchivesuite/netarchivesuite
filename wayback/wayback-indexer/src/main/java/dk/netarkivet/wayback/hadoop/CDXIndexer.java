package dk.netarkivet.wayback.hadoop;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCRecord;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import org.archive.wayback.resourcestore.indexer.WARCRecordToSearchResultAdapter;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.utils.batch.WARCBatchFilter;

/**
 * Class for creating CDX indexed from WARC files.
 */
public class CDXIndexer {
    /** The warc record searcher.*/
    protected final WARCRecordToSearchResultAdapter warcSearcher;
    /** The CDX line creator, which creates the cdx lines from the warc records.*/
    protected final SearchResultToCDXLineAdapter cdxLineCreater;

    /** Constructor.*/
    public CDXIndexer() {
        warcSearcher = new WARCRecordToSearchResultAdapter();
        cdxLineCreater = new SearchResultToCDXLineAdapter();
    }

    /**
     * Index the given WARC file.
     * @param warcInputStream An inputstream to the given file.
     * @param warcName The name of the given file.
     * @return The extracted CDX lines from the file.
     * @throws IOException
     */
    public List<String> index(InputStream warcInputStream, String warcName) throws IOException {
        ArchiveReader archiveReader = ArchiveReaderFactory.get(warcName, warcInputStream, false);
        return extractCdxLines(archiveReader);
    }


    /**
     * Create the CDX indexes from an WARC file.
     * @param warcFile The WARC file.
     * @return The CDX lines for the records in the WARC file.
     * @throws IOException If it fails to read the WARC file.
     */
    public List<String> indexFile(File warcFile) throws IOException {
        return index(new FileInputStream(warcFile), warcFile.getName());
    }


    /**
     * Filter for filtering out the NON-RESPONSE records.
     *
     * @return The filter that defines what WARC records are wanted in the output CDX file.
     */
    public WARCBatchFilter getFilter() {
        return WARCBatchFilter.EXCLUDE_NON_RESPONSE_RECORDS;
    }

    /**
     * Method for extracting the cdx lines from an ArchiveReader.
     * @param reader The ArchiveReader which is actively reading an archive file (e.g WARC).
     * @return The list of CDX index lines for the records of the archive in the reader.
     */
    protected List<String> extractCdxLines(ArchiveReader reader) {
        List<String> res = new ArrayList<>();

        for (ArchiveRecord archiveRecord: reader) {
            // TODO: look at logging something here instead of the below stuff
            //recordNum++;
            //System.out.println("Processing record #" + recordNum);
            WARCRecord warcRecord = (WARCRecord) archiveRecord;
            if (!getFilter().accept(warcRecord)) {
                //System.out.println("Skipping non response record #" + recordNum);
                continue;
            }
            warcSearcher.setCanonicalizer(new IdentityUrlCanonicalizer());
            //TODO this returns null and prints stack trace on OutOfMemoryError. Bad code. //jolf & abr
            CaptureSearchResult captureSearchResult = warcSearcher.adapt(warcRecord);
            if (captureSearchResult != null) {
                //actualLinesWritten++;
                //System.out.println("Actual cdx lines written: " + actualLinesWritten);
                res.add(cdxLineCreater.adapt(captureSearchResult));
            }
        }
        return res;
    }
}
