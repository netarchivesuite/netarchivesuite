package dk.netarkivet.harvester.indexserver.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ArchiveIndexserverDistributeTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(
                ArchiveIndexserverDistributeTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(IndexRequestClientTester.class);
        suite.addTestSuite(IndexRequestServerTester.class);
        suite.addTestSuite(IndexRequestMessageTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", 
                ArchiveIndexserverDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }

}
