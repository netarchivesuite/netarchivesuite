package dk.netarkivet.common.distribute;

import java.io.File;

/**
 * Tests the ChannelID class that defines instances of message queue names.
 */
public class TestInfo{
    public static final File DATA_DIR = new File("tests/dk/netarkivet/common/distribute/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File FILE1 = new File(WORKING_DIR, "arc_record0.txt");
    public static final File FILE2 =
            new File(new File(WORKING_DIR, "local_files"), "Upload3.ARC");
    public static final File TMPDIR = new File(WORKING_DIR, "tmpdir");
    public static final long SHORT_TIME = 10;
}
