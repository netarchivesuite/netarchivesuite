package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.util.Date;

import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData.HarvestDefinitionInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Test class PersistentJobData.
 */
public class PersistentJobDataTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    private File crawldir;

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
        crawldir = new File(TestInfo.WORKING_DIR, "my-crawldir");
        assertTrue("Unable to create crawldir '" + crawldir.getAbsolutePath() + "'",
                crawldir.mkdir());
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    /**
     * Test constructor for PersistentJobData.
     * 1. Throws ArgumentNotValid, if file argument null or file does not exist.
     * 2. accepts existing directory as argument
     */
    public void testConstructor() {
        try {
            new PersistentJobData(null);
            fail("PersistentJobData should have thrown an exception when given null-argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new PersistentJobData(new File("nonExistingDir"));
            fail("PersistentJobData should have thrown an exception when given "
                    + " non existingdir as argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Check that an existing dir doesn't throw an exception.
        new PersistentJobData(TestInfo.TEST_CRAWL_DIR);
    }

    /**
     * Test that the write(Job) method persists all necessary information
     * about the current harvest-job.
     * @throws Exception If failure to persist the information
     *                   or unable to access DB
     */
    public void testWrite() throws Exception {
        PersistentJobData pjd = new PersistentJobData(crawldir);
        Job testJob = TestInfo.getJob();
        testJob.setJobID(42L);
        testJob.setSubmittedDate(new Date());
        testJob.setHarvestAudience("Default Audience");
        pjd.write(testJob, new HarvestDefinitionInfo("test", "test", "test"));

        PersistentJobData pjdNew = new PersistentJobData(crawldir);
        
        assertEquals("retrieved jobID is not the same as original jobID",
                testJob.getJobID(), pjdNew.getJobID());
        assertEquals("retrieved jobpriority is not the same as original job priority",
                testJob.getChannel(), pjdNew.getChannel());
        assertEquals("retrived maxBytesPerDomain is not the same as original job maxBytesPerDomain",
                testJob.getMaxBytesPerDomain(), pjdNew.getMaxBytesPerDomain());
        assertEquals("retrived maxObjectsPerDomain is not the same as original job maxObjectsPerDomain",
                testJob.getMaxObjectsPerDomain(), pjdNew.getMaxObjectsPerDomain());
        assertEquals("retrived harvestNum is not the same as original job harvestNum",
                testJob.getHarvestNum(), pjdNew.getJobHarvestNum());
        assertEquals("retrived orderXMlName is not the same as original job orderXMLName",
                testJob.getOrderXMLName(), pjdNew.getOrderXMLName());
        assertEquals("retrived origHarvestDefinitionID is not the same as original ID",
                testJob.getOrigHarvestDefinitionID(),
                pjdNew.getOrigHarvestDefinitionID());
        assertEquals("The value of the performer should be set", pjdNew.getPerformer(), null);
        
        // cleanup after this unit-test.
        FileUtils.removeRecursively(crawldir);
    }
        
    /** Test reading the version 0.5 harvestInfo.xml. The newest */
    public void testReadVersion0_5() {
        File hiVersion03 = new File(TestInfo.DATA_DIR, "harvestInfo-0.5.xml");
        FileUtils.copyFile(hiVersion03, new File(crawldir, TestInfo.HarvestInfofilename));
        PersistentJobData pjd = new PersistentJobData(crawldir);
        pjd.getVersion();
    }
    
    /** Test reading the 0.4 harvestInfo.xml. */
    public void testReadVersion0_4() {
        File hiVersion03 = new File(TestInfo.DATA_DIR, "harvestInfo-0.4.xml");
        FileUtils.copyFile(hiVersion03, new File(crawldir, TestInfo.HarvestInfofilename));
        PersistentJobData pjd = new PersistentJobData(crawldir);
        pjd.getVersion();
    }
    
}
