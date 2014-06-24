package dk.netarkivet.common.tools;

import java.io.File;

public class TestInfo {
    static final File WORKING_DIR = new File("./tests/dk/netarkivet/common/tools/working/");
    static final File DATA_DIR = new File("./tests/dk/netarkivet/common/tools/data/originals");

    static final File METADATA_DIR = new File(DATA_DIR, "admindatadir");

    //Three ARC files with one record in each:
    static final File ARC1 = new File(WORKING_DIR,"test1.arc");
    static final File ARC2 = new File(WORKING_DIR,"test2.arc");
    static final File ARC3 = new File(WORKING_DIR,"test3.arc");
    //For each of the above, description of the contained record:
    static final String ARC1_CONTENT = "First test content.";
    static final String ARC2_CONTENT = "Second test content.";
    static final String ARC3_CONTENT = "Third test content.";
    static final String ARC1_MIME = "text/plain";
    static final String ARC2_MIME = "text/plain";
    static final String ARC3_MIME = "application/x-text";
    static final String ARC1_URI = "testdata://netarkivet.dk/test/foo/1";
    static final String ARC2_URI = "testdata://netarkivet.dk/test/foo/2";
    static final String ARC3_URI = "testdata://netarkivet.dk/test/bar/1";
    //A CDX file:
    static final File INDEX_FILE = new File(WORKING_DIR,"2-3-cache.zip");
}
