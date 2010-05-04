package dk.netarkivet.common.webinterface;

import java.io.File;

public class Constants {

    public static final File DATA_DIR = new File("tests/dk/netarkivet/common/webinterface/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    
    public static final File BATCH_DIR = new File(WORKING_DIR, "batch");
    
    public static final String CONTEXT_CLASS_NAME = "BatchJobName";
}
