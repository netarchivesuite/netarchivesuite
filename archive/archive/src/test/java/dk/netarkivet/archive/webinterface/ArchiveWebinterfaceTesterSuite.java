package dk.netarkivet.archive.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ArchiveWebinterfaceTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(BitpreserveFileStatusTester.class);
        suite.addTestSuite(BatchGUITester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveWebinterfaceTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
