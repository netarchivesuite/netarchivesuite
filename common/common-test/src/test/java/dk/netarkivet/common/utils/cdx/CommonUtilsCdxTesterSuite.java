package dk.netarkivet.common.utils.cdx;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/** 
 * Unit-tester suite for the package 
 * dk.netarkivet.common.utils.cdx .
 *
 */
public class CommonUtilsCdxTesterSuite {

    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsCdxTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ARCFilenameCDXRecordFilterTester.class);
        suite.addTestSuite(CDXReaderTester.class);
        suite.addTestSuite(ExtractCDXJobTester.class);
        suite.addTestSuite(BinSearchTester.class);
        suite.addTestSuite(CDXRecordTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", CommonUtilsCdxTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
