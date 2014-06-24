
package dk.netarkivet.common.distribute.indexserver;

import java.io.File;

/**
 * Constants for the common indexserver tests
 *
 */
public class TestInfo {
    private static final File BASE_DIR =
            new File("tests/dk/netarkivet/common/distribute/indexserver/data");
    static final File WORKING_DIR = new File(BASE_DIR, "working");
    static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");

}
