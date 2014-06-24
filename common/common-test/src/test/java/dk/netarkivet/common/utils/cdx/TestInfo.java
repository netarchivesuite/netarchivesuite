
package dk.netarkivet.common.utils.cdx;

import java.io.File;

/**
 * Constants for the tests in this package.
 */
public class TestInfo {
    //Reference to test input file:
    static final File ARC_DIR = new File("tests/dk/netarkivet/common/utils/cdx/data/input/");
    static final File WARC_DIR = new File("tests/dk/netarkivet/common/utils/cdx/data/input/warcs");
    
    static final File ARC_FILE1 = new File(ARC_DIR, "fyensdk.arc");
    static final File ARC_FILE2 = new File(ARC_DIR, "input-2.arc");
    static final File ARC_FILE3 = new File(ARC_DIR, "input-3.arc");
   
    static final File WARC_FILE1 = new File(WARC_DIR, "netarkivet-20081105135926-00000.warc");
    static final File WARC_FILE2 = new File(WARC_DIR, "netarkivet-20081105135926-00001.warc");
    static final File WARC_FILE3 = new File(WARC_DIR, "netarkivet-20081105140044-00002.warc");        
    
    // This file may or may not reflect the ARC_FILE
    static final File SORTED_CDX_FILE = new File(ARC_DIR, "fyensdk.cdx");
    //Reference to test output file:
    static final File CDX_DIR = new File("tests/dk/netarkivet/common/utils/cdx/data/cdxoutput/");
    static final File CDX_FILE = new File(CDX_DIR, "fyensdk.cdx");
    //Output Stream for batch jobs
    static final int NUM_RECORDS = 11;
    static final File CDX_FILE1 = new File(ARC_DIR, "Reader1.cdx");
    static final File CDX_FILE2 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader2.cdx");
    static final File CDX_FILE3 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader3.cdx");
    static final File CDX_FILE4 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader4.cdx");
    static final File MISSING_FILE = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Missing.cdx");
    public static final File TEMP_FILE = new File(CDX_DIR, "tmp");
    public static final File CORRECT_CDX_FILE = new File(ARC_DIR, "fyensdk.cdx-correct");

    private TestInfo() {}
}
