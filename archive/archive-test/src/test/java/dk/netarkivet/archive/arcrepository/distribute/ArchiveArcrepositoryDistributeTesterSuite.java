
package dk.netarkivet.archive.arcrepository.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * TesterSuite for the classes in package 
 * dk.netarkivet.archive.arcrepository.distribute.
 *
 */
public class ArchiveArcrepositoryDistributeTesterSuite {
    
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                ArchiveArcrepositoryDistributeTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(JMSArcRepositoryClientTester.class);
        suite.addTestSuite(ArcRepositoryServerTester.class);
        suite.addTestSuite(StoreMessageTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading",
                ArchiveArcrepositoryDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
