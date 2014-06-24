package dk.netarkivet.harvester.scheduler;

import java.io.File;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;

/**
 * Contains test information about all scheduler test data.
 *
 */
public class TestInfo {

    /**
     * The properties-file containing properties for logging in unit-tests.
     */
    static final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");

    static final File BASEDIR = new File(
            "tests/dk/netarkivet/harvester/scheduler/data");
    static final File ORIGINALS_DIR = new File(BASEDIR, "originals");
    static final File WORKING_DIR = new File(BASEDIR, "working");

    static File ORDER_FILE = new File(TestInfo.WORKING_DIR, "order.xml");
    public static final File LOG_FILE = new File(new File("tests/testlogs"), 
            "netarkivtest.log");
    
    static final File orderTemplatesOriginalsDir 
        = new File(
                "tests/dk/netarkivet/"
                + "/harvester/data/originals/order_templates/");
    
    public TestInfo() {
    }

    /**
     * Get a simple job.
     * @return Job
     */
    static Job getJob() {
        // This job doesn't get an ID here, because we want to see what happens
        // with an ID-less job, too.
        try {
            DataModelTestCase.addHarvestDefinitionToDatabaseWithId(0L);
        } catch (SQLException e) {
            throw new IOFailure(e.getMessage());
        }
        return Job.createJob(0L, new HarvestChannel("test", false, true, ""),
                DomainDAO.getInstance()
                .read("netarkivet.dk")
                .getConfiguration("Engelsk_netarkiv_et_niveau"), 0);
    }
    
}


