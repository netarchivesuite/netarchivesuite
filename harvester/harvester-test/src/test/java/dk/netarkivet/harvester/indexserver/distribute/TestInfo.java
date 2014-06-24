package dk.netarkivet.harvester.indexserver.distribute;

import java.io.File;

class TestInfo {
    private static final File BASE_DIR = new File("tests/dk/netarkivet/harvester/indexserver/distribute");
    private static final File DATA_DIR = new File(BASE_DIR, "data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR,"originals");
    public static final File WORKING_DIR = new File(DATA_DIR,"working");
    public static final File DUMMY_INDEX_FILE = new File(ORIGINALS_DIR,"dummy_index_file.txt");
    public static final File DUMMY_CACHEDIR = new File(ORIGINALS_DIR,"dummy_cachedir");
    public static final File DUMMY_CACHEFILE = new File(DUMMY_CACHEDIR,"dummy_index_file.txt.gz");
}
