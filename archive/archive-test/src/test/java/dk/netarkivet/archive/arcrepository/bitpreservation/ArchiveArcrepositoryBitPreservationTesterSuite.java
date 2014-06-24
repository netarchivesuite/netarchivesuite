
package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.common.utils.batch.ChecksumJobTester;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Unit testersuite for the bitpreservation package.
 *
 */
public class ArchiveArcrepositoryBitPreservationTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                ArchiveArcrepositoryBitPreservationTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ChecksumJobTester.class);
        //suite.addTestSuite(DatabaseBasedActiveBitPreservationTester.class); Failing, see https://sbforge.org/jira/browse/NAS-2264
        suite.addTestSuite(DatabasePreservationStateTester.class);
        suite.addTestSuite(FileBasedActiveBitPreservationTester.class);
        suite.addTestSuite(FileListJobTester.class);
        suite.addTestSuite(FilePreservationStateTester.class);
        suite.addTestSuite(WorkFilesTester.class);
        suite.addTestSuite(UtilityTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading",
                ArchiveArcrepositoryBitPreservationTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
