package dk.netarkivet.harvester.scheduler.jobgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainConfigurationTest;
import dk.netarkivet.harvester.datamodel.H1HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobTest;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.harvester.datamodel.eav.EAV;
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
                jobGen.canAccept(job, domainConfiguration, null));

        // Test with a config associated with another domain:
        assertTrue("Job should accept configuration associated with domain " + anotherConfig.getDomainName(),
                jobGen.canAccept(job, anotherConfig, null));

        // Test split according to byte limits

        // Make a job with limit of 2000000 defined by harvest definition
        domainConfiguration.setMaxBytes(5000000);
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        
        job = new Job(TestInfo.HARVESTID, domainConfiguration, ht, FOCUSED_CHANNEL, -1L, 2000000, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 0);

        anotherConfig.setMaxBytes(2000000);
        assertTrue("Should accept config with same limit", jobGen.canAccept(job, anotherConfig, null));

        anotherConfig.setMaxBytes(1000000);
        assertFalse("Should NOT accept config with lower limit", jobGen.canAccept(job, anotherConfig, null));

        anotherConfig.setMaxBytes(3000000);
        assertTrue("Should accept config with higher limit", jobGen.canAccept(job, anotherConfig, null));

        // Make a job with limit of 2000000 defined by harvest definition
        domainConfiguration.setMaxBytes(2000000);
        job = new Job(TestInfo.HARVESTID, domainConfiguration, ht, 
                SNAPSHOT_CHANNEL, -1L, 5000000, Constants.DEFAULT_MAX_JOB_RUNNING_TIME, 0);

        anotherConfig.setMaxBytes(2000000);
        assertTrue("Should accept config with same limit", jobGen.canAccept(job, anotherConfig, null));

        anotherConfig.setMaxBytes(1000000);
        assertFalse("Should NOT accept config with lower limit", jobGen.canAccept(job, anotherConfig, null));

        anotherConfig.setMaxBytes(3000000);
        assertFalse("Should NOT accept config with higher limit", jobGen.canAccept(job, anotherConfig, null));

        // TODO: Should also be tested that expected size associated with this configuration
        // is with limits (minCountObjects, maxCountObjects). This should be a separate
        // test case and should have been done in Iteration 4.
    }

    @Test (expected = ArgumentNotValid.class)
    public void testCanAcceptNullConfiguration() {
        Job job = JobTest.createDefaultJob();
        createJobGenerator().canAccept(job, null, null);
    }

    @Test
    public void testDefaultAttributes() {
        DomainConfiguration dc1 = DomainConfigurationTest.createDefaultDomainConfiguration("1.dk");
        DomainConfiguration dc2 = DomainConfigurationTest.createDefaultDomainConfiguration("2.dk");
        dc1.setAttributesAndTypes(DomainConfigurationTest.getAttributes(20, false, true));
        dc2.setAttributesAndTypes(new ArrayList<EAV.AttributeAndType>());
        assertEquals(0, EAV.compare(dc1.getAttributesAndTypes(), dc2.getAttributesAndTypes()));
    }

    @Test
    public void testSorting() {
        List<DomainConfiguration> dcs = new ArrayList<>();
        dcs.add(getDomainConfiguration("1.dk", 4000000L, 10, false, true));
        dcs.add(getDomainConfiguration("2.dk", 2000000L, 20, false, true));
        dcs.add(getDomainConfiguration("3.dk", 4000000L, 10, false, true));
        dcs.add(getDomainConfiguration("4.dk", 2000000L, 20, false, false));
        dcs.add(getDomainConfiguration("5.dk", 2000000L, 20, false, true));
        DomainConfiguration dc6 = DomainConfigurationTest.createDefaultDomainConfiguration("6.dk");
        dc6.setMaxBytes(2000000L);
        dcs.add(dc6); //default domain, should get values (20, false, true)
        Comparator<DomainConfiguration> comparator = new DefaultJobGenerator.CompareConfigsDesc(-1, 10000000L);
        Collections.sort(dcs, comparator);
        List<String> sortedNames = new ArrayList<>();
        for (DomainConfiguration dc:dcs) {
            sortedNames.add(dc.getDomainName());
        }
        List<String> expected1 = new ArrayList<>();
        expected1.add("2.dk");
        expected1.add("5.dk");
        expected1.add("6.dk");
        assertFalse(Collections.indexOfSubList(sortedNames, expected1) == -1);
        List<String> expected2 = new ArrayList<>();
        expected2.add("1.dk");
        expected2.add("3.dk");
        assertFalse(Collections.indexOfSubList(sortedNames, expected2) == -1);
    }

    public static DomainConfiguration getDomainConfiguration(String name, Long maxBytes, int maxHops,
            boolean obeyRobots, boolean extractJS) {
        DomainConfiguration dc1 = DomainConfigurationTest.createDefaultDomainConfiguration(name);
        dc1.setMaxBytes(maxBytes);
        dc1.setAttributesAndTypes(DomainConfigurationTest.getAttributes(maxHops, obeyRobots, extractJS));
        return dc1;
    }

}
