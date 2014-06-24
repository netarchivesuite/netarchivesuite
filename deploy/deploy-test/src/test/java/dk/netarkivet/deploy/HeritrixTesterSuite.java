
package dk.netarkivet.deploy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * This class runs all tests of Heritrix functionality.  Must be run as part
 * of heritrix upgrades.
 */
public class HeritrixTesterSuite {
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(dk.netarkivet.externalsoftware.HeritrixTests.class);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HeritrixTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", HeritrixTesterSuite.class.getName()};
        TestRunner.main(args2);
    }}
