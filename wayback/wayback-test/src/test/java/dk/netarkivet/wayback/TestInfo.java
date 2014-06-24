package dk.netarkivet.wayback;

import java.io.File;

/**
 * Defines test data and directories for the package
 * dk.netarkivet.archive.arcrepository.
 */
public class TestInfo {
    public static final File DATA_DIR
        = new File("tests/dk/netarkivet/wayback/data/");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File FILE_DIR = new File(WORKING_DIR, "filedir");
    /**static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR,
            "correct/originals/");
    static final File CORRECT_WORKING_DIR = new File(DATA_DIR,
            "correct/working/");
    static final File TMP_FILE = new File(WORKING_DIR, "temp");*/
    public static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final long SHORT_TIMEOUT = 1000;
}
