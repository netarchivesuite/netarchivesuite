
package dk.netarkivet.common;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import dk.netarkivet.common.distribute.CommonDistributeTesterSuite;
import dk.netarkivet.common.distribute.indexserver.CommonDistributeIndexserverTesterSuite;
import dk.netarkivet.common.exceptions.CommonExceptionsTesterSuite;
import dk.netarkivet.common.management.CommonManagementTesterSuite;
import dk.netarkivet.common.tools.CommonToolsTesterSuite;
import dk.netarkivet.common.utils.CommonUtilsTesterSuite;
import dk.netarkivet.common.utils.arc.CommonUtilsArcTesterSuite;
import dk.netarkivet.common.utils.batch.CommonUtilsBatchTesterSuite;
import dk.netarkivet.common.utils.cdx.CommonUtilsCdxTesterSuite;
import dk.netarkivet.common.utils.warc.CommonUtilsWarcTesterSuite;
import dk.netarkivet.common.webinterface.CommonWebinterfaceTesterSuite;

/**
 * This class runs all the common module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        CommonUtilsArcTesterSuite.addToSuite(suite);
        CommonUtilsWarcTesterSuite.addToSuite(suite);
        CommonUtilsBatchTesterSuite.addToSuite(suite);
        CommonUtilsCdxTesterSuite.addToSuite(suite);
        // CommonsTesterSuite.addToSuite(suite);
        // CommonLifecycleTesterSuite.addToSuite(suite);
        CommonUtilsTesterSuite.addToSuite(suite);
        CommonDistributeIndexserverTesterSuite.addToSuite(suite);
        CommonDistributeTesterSuite.addToSuite(suite);
        CommonExceptionsTesterSuite.addToSuite(suite);
        CommonManagementTesterSuite.addToSuite(suite);
        CommonToolsTesterSuite.addToSuite(suite);
        CommonWebinterfaceTesterSuite.addToSuite(suite);
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
