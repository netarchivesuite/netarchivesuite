
package dk.netarkivet.common.utils;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Sweet suite of utility tests.
 *
 */
public class CommonUtilsTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        // Moved to "monitor"
        // suite.addTestSuite(ApplicationUtilsTester.class);

        suite.addTestSuite(DiscardingOutputStreamTester.class);
        suite.addTestSuite(ExceptionUtilsTester.class);
        suite.addTestSuite(FileArrayIteratorTester.class);
        suite.addTestSuite(FileUtilsTester.class);
        suite.addTestSuite(FilterIteratorTester.class);
        suite.addTestSuite(JMXUtilsTester.class);
        suite.addTestSuite(KeyValuePairTester.class);
        suite.addTestSuite(LargeFileGZIPInputStreamTester.class);
        // Disabled, as it is platform specific
        // suite.addTestSuite(ProcessUtilsTester.class);
        suite.addTestSuite(ReadOnlyByteArrayTester.class);
        suite.addTestSuite(SettingsTester.class);
        suite.addTestSuite(SettingsFactoryTester.class);
        suite.addTestSuite(SimpleXmlTester.class);
        suite.addTestSuite(SparseBitSetTester.class);
        suite.addTestSuite(StreamUtilsTester.class);
        suite.addTestSuite(StringUtilsTester.class);
        suite.addTestSuite(SystemUtilsTester.class);
        suite.addTestSuite(TablesortTester.class);
        suite.addTestSuite(TimeUtilsTester.class);
        suite.addTestSuite(XmlTreeTester.class);
        suite.addTestSuite(XmlUtilsTester.class);
        suite.addTestSuite(ZipUtilsTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", CommonUtilsTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
