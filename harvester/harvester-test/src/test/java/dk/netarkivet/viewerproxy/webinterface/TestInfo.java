package dk.netarkivet.viewerproxy.webinterface;

import java.io.File;

/**
 * Constants for use in viewerproxy webinterface tests.
 */
public class TestInfo {
    static final File DATA_DIR
            = new File("tests/dk/netarkivet/viewerproxy/webinterface/data");
    static final File ORIGINALS_DIR
            = new File(DATA_DIR, "originals");
    static final File WARC_ORIGINALS_DIR
    = new File(DATA_DIR, "warc-originals");

    
}
