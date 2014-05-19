package dk.netarkivet.common.utils.warc;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Suite of unittests for the classes in the 
 * dk.netarkivet.commons.utils.warc package.
 */
public class CommonUtilsWarcTesterSuite {

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsWarcTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(WARCBatchJobTester.class);
        suite.addTestSuite(WARCReaderTester.class);
        //suite.addTestSuite(WARCUtilsTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                CommonUtilsWarcTesterSuite.class.getName() };
        TestRunner.main(args2);
    }
}
