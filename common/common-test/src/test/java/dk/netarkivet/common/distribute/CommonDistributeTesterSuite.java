
package dk.netarkivet.common.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.common.utils.SettingsFactoryTester;

/**
 *  Collection of unittests of classes in the
 *  dk.netarkivet.common.distribute package.
 */
public class CommonDistributeTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonDistributeTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ChannelIDTester.class);
        suite.addTestSuite(ChannelsTester.class);
        suite.addTestSuite(FTPRemoteFileTester.class);
        suite.addTestSuite(HTTPRemoteFileTester.class);
        suite.addTestSuite(HTTPSRemoteFileTester.class);
        suite.addTestSuite(JMSConnectionTester.class);
        suite.addTestSuite(NetarkivetMessageTester.class);
        suite.addTestSuite(NullRemoteFileTester.class);
        suite.addTestSuite(SettingsFactoryTester.class);
        suite.addTestSuite(SynchronizerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonDistributeTesterSuite.class.getName()};

        TestRunner.main(args2);
    }
}
