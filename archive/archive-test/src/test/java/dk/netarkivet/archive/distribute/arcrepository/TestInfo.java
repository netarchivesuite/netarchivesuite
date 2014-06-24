package dk.netarkivet.archive.distribute.arcrepository;

import java.io.File;
import java.net.URI;

import dk.netarkivet.common.utils.arc.ARCKey;

/**
 * Constants for shared use by viewerproxy unit tests.
 *
 */
public class TestInfo {
    private static final File BASE_DIR = new File("tests/dk/netarkivet/archive/distribute/arcrepository/data");
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");
    public static final File LOG_FILE = new File("target/testlogs/netarkivtest.log");
    /**
     * An archive directory to work on.
     */
    public static final File ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    public static final File INDEX_DIR_2_3
            = new File(TestInfo.WORKING_DIR, "2-3-cache");
    public static final File INDEX_DIR_2_4_3_5
            = new File(TestInfo.WORKING_DIR, "2-4-3-5-cache");
    public static URI GIF_URL;
    public static final File LOG_PATH = new File(WORKING_DIR, "tmp");
    /**The key listed for GIF_URL. */
    public static final ARCKey GIF_URL_KEY =
            new ARCKey("2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc",
                    73269);
    public static final File SAMPLE_FILE = new File(WORKING_DIR, "testFile.txt");
    public static final File SAMPLE_FILE_COPY = new File(WORKING_DIR, "testCopy.txt");
    public static final File EMPTY_FILE = new File(WORKING_DIR, "zeroByteFile");
}