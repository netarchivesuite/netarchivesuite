package dk.netarkivet.wayback;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public interface DeduplicateToCDXAdapterInterface {

    /**
     * Takes a deduplicate line from a crawl log and converts it to a line in a
     * cdx file suitable for searching in wayback. The target url in the line is
     * canonicalized by this method. If the input String is not a crawl-log
     * duplicate line, null is returned.
     * @param line a line from a crawl log
     * @return a line for a cdx file or null if the input is not a duplicate
     * line
     */
    String adaptLine(String line);

    /**
     * Scans an input stream from a crawl log and converts all dedup lines
     * to cdx records which it outputs to an output stream
     * @param is the input stream
     * @param os the output stream
     */
    void adaptStream(InputStream is, OutputStream os);

}
