package dk.netarkivet.harvester;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvesterTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(HarvesterSettingsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
