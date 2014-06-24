package dk.netarkivet.common.utils.arc;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Suite of unittests for the classes in the 
 * dk.netarkivet.commons.utils.arc package.
 */
public class CommonUtilsArcTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsArcTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ARCBatchJobTester.class);
        suite.addTestSuite(ARCKeyTester.class);
        suite.addTestSuite(ARCReaderTester.class);
        suite.addTestSuite(ARCUtilsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                CommonUtilsArcTesterSuite.class.getName() };
        TestRunner.main(args2);
    }
}
