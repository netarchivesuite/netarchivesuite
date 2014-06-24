
package dk.netarkivet.viewerproxy;

import java.io.File;

/**
 * Constants for shared use by viewerproxy unit tests.
 *
 */
public class TestInfo {
    private static final File BASE_DIR = new File("tests/dk/netarkivet/viewerproxy/data");
    
    static final File WORKING_DIR = new File(BASE_DIR, "working");
    static final File ORIGINALS_DIR = new File(BASE_DIR, "input");
    static final File METADATA_DIR = new File(BASE_DIR, "metadata");
    
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    /**
     * An archive directory to work on.
     */
    static final File ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    public static final File ZIPPED_INDEX_DIR
            = new File(WORKING_DIR, "2-3-cache");
    public static final File ZIPPED_INDEX_DIR2
            = new File(WORKING_DIR, "2-4-3-5-cache");
}
