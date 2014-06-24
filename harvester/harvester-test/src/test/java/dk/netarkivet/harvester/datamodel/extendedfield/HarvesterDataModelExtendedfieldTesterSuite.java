package dk.netarkivet.harvester.datamodel.extendedfield;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * 
 * Unit-tester suite for the package dk.netarkivet.harvester.datamodel.extendedfield.
 * 
 */
public class HarvesterDataModelExtendedfieldTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterDataModelExtendedfieldTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ExtendedFieldDefaultValuesTester.class);
        suite.addTestSuite(ExtendedFieldTypeTester.class);
        suite.addTestSuite(ExtendedFieldOptionsTester.class);
        suite.addTestSuite(ExtendedFieldValueTester.class);
        suite.addTestSuite(ExtendedFieldTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", 
                HarvesterDataModelExtendedfieldTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
    }


}