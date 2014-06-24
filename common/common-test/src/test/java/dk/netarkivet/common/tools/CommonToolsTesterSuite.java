package dk.netarkivet.common.tools;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class CommonToolsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonToolsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArcMergeTester.class);
        suite.addTestSuite(ArcWrapTester.class);
        suite.addTestSuite(ExtractCDXTester.class);
        suite.addTestSuite(ToolRunnerTester.class);
        suite.addTestSuite(ChecksumCalculatorTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonToolsTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
