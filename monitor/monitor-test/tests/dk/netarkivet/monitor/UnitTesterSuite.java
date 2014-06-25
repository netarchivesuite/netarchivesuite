
package dk.netarkivet.monitor;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.common.utils.MonitorCommonUtilsTesterSuite;
import dk.netarkivet.monitor.jmx.MonitorJMXTesterSuite;
import dk.netarkivet.monitor.logging.MonitorLoggingTesterSuite;
import dk.netarkivet.monitor.registry.MonitorRegistryTesterSuite;
import dk.netarkivet.monitor.webinterface.MonitorWebinterfaceTesterSuite;

/**
 * This class runs all the monitor module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        MonitorTesterSuite.addToSuite(suite);
        MonitorLoggingTesterSuite.addToSuite(suite);
        MonitorJMXTesterSuite.addToSuite(suite);
        MonitorRegistryTesterSuite.addToSuite(suite);
        MonitorWebinterfaceTesterSuite.addToSuite(suite);
        MonitorCommonUtilsTesterSuite.addToSuite(suite);
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
