
package dk.netarkivet.harvester.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvesterDistributeTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterDistributeTesterSuite.class.getName());

        HarvesterDistributeTesterSuite.addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(HarvesterMessageHandlerTester.class);
        suite.addTestSuite(IndexReadyMessageTester.class);
        suite.addTestSuite(ChannelIDTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterDistributeTesterSuite.class.getName()};

        TestRunner.main(args2);
    }


}
