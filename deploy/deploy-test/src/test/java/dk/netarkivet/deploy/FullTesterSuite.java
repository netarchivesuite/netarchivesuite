package dk.netarkivet.deploy;


import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * The FullTesterSuite comprises all our test-suites.
 */
public class FullTesterSuite {
    public static void addToSuite(TestSuite suite) {
        FullUnitTesterSuite.addToSuite(suite);
        IntegrityTesterSuite.addToSuite(suite);
        HeritrixTesterSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("FullTesterSuite");

        addToSuite(suite);

        return suite;
    }

    public static void main(String args[]) {
        String[] args2 = {"-noloading", "dk.netarkivet.FullTesterSuite"};
        TestRunner.main(args2);
    }
}
