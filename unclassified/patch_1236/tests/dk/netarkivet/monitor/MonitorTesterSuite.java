package dk.netarkivet.monitor;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class MonitorTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(MonitorTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", MonitorTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
