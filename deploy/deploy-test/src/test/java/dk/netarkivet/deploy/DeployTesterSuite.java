package dk.netarkivet.deploy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class DeployTesterSuite {
    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite(DeployTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(DeployTester.class);
        suite.addTestSuite(CompleteSettingsTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", DeployTesterSuite.class.getName()};
        TestRunner.main(args2);
    }

}
