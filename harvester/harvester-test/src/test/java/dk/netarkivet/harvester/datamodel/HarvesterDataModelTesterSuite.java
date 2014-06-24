package dk.netarkivet.harvester.datamodel;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.harvester.webinterface.HarvestDefinitionGUITester;

/**
 * 
 * Unit-tester suite for the package dk.netarkivet.harvester.datamodel.
 * 
 */
public class HarvesterDataModelTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(HarvesterDataModelTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        // Sorted in alphabetical order.
        suite.addTestSuite(AliasInfoTester.class);
        suite.addTestSuite(ConstantsTester.class);
        suite.addTestSuite(DailyFrequencyTester.class);
        suite.addTestSuite(HarvestDBConnectionTester.class);
        suite.addTestSuite(DerbySpecificsTester.class);
        suite.addTestSuite(DomainConfigurationTester.class);
        suite.addTestSuite(DomainDAOTester.class);
        suite.addTestSuite(DomainDBDAOTester.class);
        suite.addTestSuite(DomainHistoryTester.class);
        suite.addTestSuite(DomainOwnerInfoTester.class);
        suite.addTestSuite(DomainTester.class);
        suite.addTestSuite(FrequencyTester.class);
        suite.addTestSuite(FullHarvestTester.class);
        suite.addTestSuite(GlobalCrawlerTrapListTester.class);
        suite.addTestSuite(GlobalCrawlerTrapListDBDAOTester.class  );
        suite.addTestSuite(HarvestDefinitionDAOTester.class);
        suite.addTestSuite(HarvestDefinitionGUITester.class);
        suite.addTestSuite(HarvestDefinitionTester.class);
        suite.addTestSuite(HarvestTemplateApplicationTester.class);
        suite.addTestSuite(HeritrixTemplateTester.class);
        suite.addTestSuite(HourlyFrequencyTester.class);
        suite.addTestSuite(JobDAOTester.class);
        suite.addTestSuite(JobStatusTester.class);
        suite.addTestSuite(JobTester.class);
        suite.addTestSuite(MonthlyFrequencyTester.class);
        suite.addTestSuite(MySQLSpecificsTester.class);
        suite.addTestSuite(NumberUtilsTester.class);
        suite.addTestSuite(PartialHarvestTester.class);
        suite.addTestSuite(RepeatingScheduleTester.class);
        suite.addTestSuite(RunningJobsInfoDAOTester.class);
        suite.addTestSuite(ScheduleDAOTester.class);
        suite.addTestSuite(ScheduleDBDAOTester.class);
        suite.addTestSuite(ScheduleTester.class);
        suite.addTestSuite(SparsePartialHarvestTester.class);
        suite.addTestSuite(StopReasonTester.class);
        suite.addTestSuite(TemplateDAOTester.class);
        suite.addTestSuite(TemplateDAOTesterAlternate.class);
        suite.addTestSuite(TimedScheduleTester.class);
        suite.addTestSuite(TimeUnitTester.class);
        suite.addTestSuite(TLDInfoTester.class);
        suite.addTestSuite(WeeklyFrequencyTester.class);
        suite.addTestSuite(SeedListTester.class);
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", HarvesterDataModelTesterSuite.class.getName()};

        TestRunner.main(args2);
        //junit.swingui.TestRunner.main(args2);
    }


}