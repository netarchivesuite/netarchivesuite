package dk.netarkivet.common.utils.warc;

import java.io.File;

/**
 * Static constants for utils.arc and also utils.batch testing.
 */
public class TestInfo {

    public static final File BASE_DIR
            = new File("tests/dk/netarkivet/common/utils/warc/data");
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");

    public static final int LINES_IN_FILEDESC = 4;
    public static final int NON_FILEDESC_LINES_IN_INPUT_1 = 12;
    public static final int NON_FILEDESC_LINES_IN_INPUT_2 = 0;
    public static final int NON_FILEDESC_LINES_IN_INPUT_3 = 42;
    public static final File INPUT_1 = new File(WORKING_DIR, "input-1.arc");
    public static final File INPUT_2 = new File(WORKING_DIR, "input-2.arc");
    public static final File INPUT_3 = new File(WORKING_DIR, "input-3.arc");
    public static final File FAIL_ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1_to_fail");

}
