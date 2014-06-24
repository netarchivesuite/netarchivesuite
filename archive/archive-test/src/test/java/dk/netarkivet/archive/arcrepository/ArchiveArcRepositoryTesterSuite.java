
package dk.netarkivet.archive.arcrepository;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Test suite for the classes in 
 * dk.netarkivet.archive.arcrepository.
 */
public class ArchiveArcRepositoryTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveArcRepositoryTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArcRepositoryTester.class);
        suite.addTestSuite(ArcRepositoryTesterStore.class);
        suite.addTestSuite(ArcRepositoryTesterStoreChecksum.class);
        suite.addTestSuite(ArcRepositoryTesterGet.class);
        suite.addTestSuite(ArcRepositoryTesterLog.class);
        suite.addTestSuite(ArcRepositoryTesterBatch.class);
        suite.addTestSuite(ArcRepositoryDatabaseTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading",
                ArchiveArcRepositoryTesterSuite.class.getName()};
        TestRunner.main(args2);
    }

}
