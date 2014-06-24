
package dk.netarkivet.common.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvesterCommonWebinterfaceTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterCommonWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(HTMLUtilsTester.class);
        suite.addTestSuite(SiteSectionTester.class);
        suite.addTestSuite(GUIWebServerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", HarvesterCommonWebinterfaceTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
