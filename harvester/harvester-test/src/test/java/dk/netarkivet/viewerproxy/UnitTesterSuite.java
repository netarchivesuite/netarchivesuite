
package dk.netarkivet.viewerproxy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.viewerproxy.ViewerProxyTesterSuite;
import dk.netarkivet.viewerproxy.distribute.ViewerproxyDistributeTesterSuite;
import dk.netarkivet.viewerproxy.webinterface.ViewerproxyWebinterfaceTesterSuite;

/**
 * This class runs all the unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        /*
         * Testersuites for the viewerproxy module
         */
        ViewerproxyDistributeTesterSuite.addToSuite(suite);
        ViewerProxyTesterSuite.addToSuite(suite);
        ViewerproxyWebinterfaceTesterSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(UnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", UnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
