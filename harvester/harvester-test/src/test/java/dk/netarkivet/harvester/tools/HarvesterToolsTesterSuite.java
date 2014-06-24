
package dk.netarkivet.harvester.tools;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvesterToolsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterToolsTesterSuite.class.getName());

        HarvesterToolsTesterSuite.addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CreateCDXMetadataFileTester.class);
    }


    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterToolsTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
    }


}
