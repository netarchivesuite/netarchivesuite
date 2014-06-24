package dk.netarkivet.harvester.datamodel;

import java.util.Set;

import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

@SuppressWarnings({ "unused"})
public class RunningJobsInfoDAOTester extends DataModelTestCase {
    public RunningJobsInfoDAOTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testGetInstance() {
    	StartedJobInfo sji = new StartedJobInfo("harvest", 42L);
        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        String[] types = dao.getFrontierReportFilterTypes();
        dao.deleteFrontierReports(42L);
        Set<Long> records = dao.getHistoryRecordIds();
        dao.getMostRecentByHarvestName();
        dao.store(sji);
        dao.getFullJobHistory(42l);
        dao.getMostRecentByJobId(42L);
        dao.removeInfoForJob(42L);
        //dao.storeFrontierReport(filterId, report);
    }
}
