
package dk.netarkivet.deploy;

//import dk.netarkivet.archive.arcrepository.ARCLookupTester;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

//import dk.netarkivet.harvester.indexserver.CDXOriginCrawlLogIteratorTester;

/**
 * This class runs all tests of Deduplicator functionality.  Must be run as part
 * of deduplicator upgrades. 
 * FIXME This doesn't test the Heritrix modules.
 *
 */

public class DeduplicatorTesterSuite {
    public static void addToSuite(TestSuite suite) {
        // FIXME:  deploy cannot see archiver or harvester test classes.
//        suite.addTestSuite(CDXOriginCrawlLogIteratorTester.class);
//        suite.addTestSuite(ARCLookupTester.class);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("DeduplicatorTesterSuite");

        addToSuite(suite);

        return suite;
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", "dk.netarkivet.DeduplicatorTesterSuite"};
        TestRunner.main(args2);
    }}
