package dk.netarkivet.archive.webinterface;

import java.io.File;

public class TestInfo {
    public static final File BASE_DIR
            = new File("tests/dk/netarkivet/archive/webinterface/data");    
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");

    public static final File BATCH_DIR = new File(WORKING_DIR, "batch");
    
    public static final String CONTEXT_CLASS_NAME = "batchjob";
    
}
