
package dk.netarkivet.harvester.webinterface;

import java.io.File;

/**
 * Various constants used in testing the web interface support classes.
 */

public class TestInfo {
    public static final File BASE_DIR
           = new File("tests/dk/netarkivet/harvester/webinterface/data");    
    public static final File FUTURE_BASE_DIR
        = new File("tests/dk/netarkivet/harvester/data");  
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");

    public static final File DBFILE = new File(BASE_DIR, "fullhddb.jar");
}
