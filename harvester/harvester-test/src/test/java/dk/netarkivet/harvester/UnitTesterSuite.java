
package dk.netarkivet.harvester;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.common.webinterface.HarvesterCommonWebinterfaceTesterSuite;
import dk.netarkivet.harvester.datamodel.HarvesterDataModelTesterSuite;
import dk.netarkivet.harvester.datamodel.extendedfield.HarvesterDataModelExtendedfieldTesterSuite;
import dk.netarkivet.harvester.distribute.HarvesterDistributeTesterSuite;
import dk.netarkivet.harvester.harvesting.HarvestingTesterSuite;
import dk.netarkivet.harvester.harvesting.distribute.HarvestingDistributeTesterSuite;
import dk.netarkivet.harvester.harvesting.frontier.HarvesterHarvestingFrontierTesterSuite;
import dk.netarkivet.harvester.scheduler.HarvesterSchedulerTesterSuite;
import dk.netarkivet.harvester.webinterface.HarvesterWebinterfaceTesterSuite;

/**
 * This class runs all the harvester module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        HarvesterTesterSuite.addToSuite(suite);
        HarvestingTesterSuite.addToSuite(suite);
        HarvesterDataModelTesterSuite.addToSuite(suite);
        HarvesterDataModelExtendedfieldTesterSuite.addToSuite(suite);
        HarvesterDistributeTesterSuite.addToSuite(suite);
        HarvesterHarvestingFrontierTesterSuite.addToSuite(suite);
        HarvestingDistributeTesterSuite.addToSuite(suite);
        HarvesterSchedulerTesterSuite.addToSuite(suite);
        //HarvesterToolsTesterSuite.addToSuite(suite);
        HarvesterWebinterfaceTesterSuite.addToSuite(suite);

        HarvesterCommonWebinterfaceTesterSuite.addToSuite(suite);
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
