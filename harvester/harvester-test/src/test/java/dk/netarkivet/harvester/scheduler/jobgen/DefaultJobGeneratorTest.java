package dk.netarkivet.harvester.scheduler.jobgen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainConfigurationTest;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobTest;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;

public class DefaultJobGeneratorTest extends AbstractJobGeneratorTest {
    private static final HarvestChannel FOCUSED_CHANNEL = new HarvestChannel("FOCUSED", false, true, "");
    private static final HarvestChannel SNAPSHOT_CHANNEL = new HarvestChannel("SNAPSHOT", true, true, "");

    @Override
    protected AbstractJobGenerator createJobGenerator() {
        return new DefaultJobGenerator();
    }

    /**
     * Test if a configuration is checked with respect to expected number of objects, and that the domain the domain in
     * this configuration is not already in job.
     */
    @Test
    public void testCanAcceptByteLimit() {
        DomainConfiguration domainConfiguration = DomainConfigurationTest.createDefaultDomainConfiguration();
        DomainConfiguration anotherConfig = DomainConfigurationTest.createDefaultDomainConfiguration("nondefault.org");
        Job job = JobTest.createDefaultJob();
        DefaultJobGenerator jobGen = new DefaultJobGenerator();
        assertFalse("Job should not accept configuration associated with domain " +
                        domainConfiguration.getDomainName(),
                jobGen.canAccept(job, domainConfiguration));

        // Test with a config associated with another domain:
        assertTrue("Job should accept configuration associated with domain " + anotherConfig.getDomainName(),
                jobGen.canAccept(job, anotherConfig));

        // Test split according to byte limits

        // Make a job with limit of 2000000 defined by harvest definition
        domainConfiguration.setMaxBytes(5000000);
        job = new Job(TestInfo.HARVESTID, domainConfiguration, OrderXmlBuilder.createDefault().getDoc(),
                FOCUSED_CHANNEL, -1L, 2000000, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 0);

        anotherConfig.setMaxBytes(2000000);
        assertTrue("Should accept config with same limit", jobGen.canAccept(job, anotherConfig));

        anotherConfig.setMaxBytes(1000000);
        assertFalse("Should NOT accept config with lower limit", jobGen.canAccept(job, anotherConfig));

        anotherConfig.setMaxBytes(3000000);
        assertTrue("Should accept config with higher limit", jobGen.canAccept(job, anotherConfig));

        // Make a job with limit of 2000000 defined by harvest definition
        domainConfiguration.setMaxBytes(2000000);
        job = new Job(TestInfo.HARVESTID, domainConfiguration, OrderXmlBuilder.createDefault().getDoc(),
                SNAPSHOT_CHANNEL, -1L, 5000000, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 0);

        anotherConfig.setMaxBytes(2000000);
        assertTrue("Should accept config with same limit", jobGen.canAccept(job, anotherConfig));

        anotherConfig.setMaxBytes(1000000);
        assertFalse("Should NOT accept config with lower limit", jobGen.canAccept(job, anotherConfig));

        anotherConfig.setMaxBytes(3000000);
        assertFalse("Should NOT accept config with higher limit", jobGen.canAccept(job, anotherConfig));

        // TODO: Should also be tested that expected size associated with this configuration
        // is with limits (minCountObjects, maxCountObjects). This should be a separate
        // test case and should have been done in Iteration 4.
    }

    @Test (expected = ArgumentNotValid.class)
    public void testCanAcceptNullConfiguration() {
        Job job = JobTest.createDefaultJob();
        createJobGenerator().canAccept(job, null);
    }
}
