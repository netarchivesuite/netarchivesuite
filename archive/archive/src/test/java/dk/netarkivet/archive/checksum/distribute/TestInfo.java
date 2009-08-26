package dk.netarkivet.archive.checksum.distribute;

import java.io.File;

public class TestInfo {

    static final File UPLOADMESSAGE_TEMP_DIR = new File("tests/dk/netarkivet/archive/checksum/distribute/data/original");
    
    static final File UPLOADMESSAGE_TESTFILE_1 = new File(UPLOADMESSAGE_TEMP_DIR, "NetarchiveSuite-upload1.arc");
    
    static final File BASE_FILE_DIR = new File(TestInfo.UPLOADMESSAGE_TEMP_DIR, "basefiledir");

    static final File CORRECTMESSAGE_TESTFILE = new File(BASE_FILE_DIR, "NetarchiveSuite-upload1.arc");
    
    static final File CHECKSUM_FILE = new File(BASE_FILE_DIR, "checksum.md5");
    
    static final String TESTFILE_1_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1";
    static final String CORRECTFILE_1_CHECKSUM = "98ed62d461697085c802011f6fd7716d";
}
