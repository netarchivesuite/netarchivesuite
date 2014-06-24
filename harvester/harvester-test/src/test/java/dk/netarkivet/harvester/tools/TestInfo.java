
package dk.netarkivet.harvester.tools;

import java.io.File;

public class TestInfo {
    static final File WORKING_DIR = new File("./tests/dk/netarkivet/common/tools/working/");
    static final File DATA_DIR = new File("./tests/dk/netarkivet/common/tools/data/originals");

    static final File METADATA_DIR = new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR, "admindatadir");
    static final File OLDJOBS_DIR = new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR, "oldjobs");
    static final File JOBID_HARVESTID_MAPPING_FILE =
                new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR, "jobid-harvestid.txt");

    static final String TEST_ENTRY_FILENAME = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
    static final long TEST_ENTRY_OFFSET = 8459;

    static final File TEXT_FILE_1 = new File(dk.netarkivet.harvester.tools.TestInfo.WORKING_DIR,"testfile01.txt");
    static final File TEXT_FILE_2 = new File(dk.netarkivet.harvester.tools.TestInfo.WORKING_DIR,"testfile02.txt");
    
    /** data files used by createIndex tests. */
    static final File CACHE_DIR = new File(WORKING_DIR, "cache");
    static final File CACHE_TEMP_DIR = new File(WORKING_DIR, "tempCache");
    static final File CACHE_1_FILE = new File(CACHE_TEMP_DIR, "cache_file_1");
    static final File CACHE_OUTPUT_DIR = new File(WORKING_DIR, "outCache");
    static final File CACHE_ZIP_FILE = new File(CACHE_OUTPUT_DIR, "cache_file_1.gz");
}
