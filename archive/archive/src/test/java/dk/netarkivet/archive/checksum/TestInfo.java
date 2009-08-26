package dk.netarkivet.archive.checksum;

import java.io.File;

public class TestInfo {

    static final File DATA_DIR = new File("tests/dk/netarkivet/archive/checksum/data");
    static final File WORKING_DIR = new File(DATA_DIR, "working"); 
    static final File TMP_DIR = new File(DATA_DIR, "tmp");
    static final File ORIGINAL_DIR = new File(DATA_DIR, "original");
    
    static final File UPLOAD_FILE_1 = new File(WORKING_DIR, "settings.xml");
    static final File UPLOAD_FILE_2 = new File(WORKING_DIR, "NetarchiveSuite-upload1.arc");

    static final File CHECKSUM_FILE = new File(WORKING_DIR, "checksum.md5");
    
    static final String TEST1_CHECKSUM = "71e82bb05fb7da1248ca96015fb06585"; 
    static final String TEST2_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1"; 

}
