
package dk.netarkivet.archive.arcrepository;

import java.io.File;

/**
 * Defines test data and directories for the package
 * dk.netarkivet.archive.arcrepository.
 */
class TestInfo {
    static final File DATA_DIR 
        = new File("tests/dk/netarkivet/archive/arcrepository/data");
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR,
            "correct/originals/");
    static final File CORRECT_WORKING_DIR = new File(DATA_DIR,
            "correct/working/");
    static final File TMP_FILE = new File(WORKING_DIR, "temp");
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final long SHORT_TIMEOUT = 1000;
    
    
    static final File ORIGINAL_DATABASE_DIR = new File(DATA_DIR, "database");
    static final File DATABASE_DIR = new File(WORKING_DIR, "database");
    static final File DATABASE_FILE = new File("archivedatabasedir", "archivedb.jar");
}

