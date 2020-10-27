package dk.netarkivet.harvester.scheduler.jobgen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobTest;
import dk.netarkivet.harvester.datamodel.TestInfo;

public abstract class AbstractJobGeneratorTest extends DataModelTestCase {
    /**
     * Tests that global crawler traps defined in the jobDAO are added to new jobs.
     */
    @Test
    @Ignore
    public void testCreateJobWithGlobalCrawlerTraps() throws FileNotFoundException {
        GlobalCrawlerTrapList list1 = new GlobalCrawlerTrapList(new FileInputStream(new File(TestInfo.TOPDATADIR,
                TestInfo.CRAWLER_TRAPS_01)), "list1", "A Description of list1", true);
        GlobalCrawlerTrapList list2 = new GlobalCrawlerTrapList(new FileInputStream(new File(TestInfo.TOPDATADIR,
                TestInfo.CRAWLER_TRAPS_02)), "list2", "A Description of list2", true);
        GlobalCrawlerTrapListDAO trapDao = GlobalCrawlerTrapListDAO.getInstance();
        trapDao.create(list1);
        trapDao.create(list2);
        HarvestDefinition harvestDefinition = mock(HarvestDefinition.class);
        Job job = createJobGenerator().getNewJob(harvestDefinition, TestInfo.getDRConfiguration());
        /*
         * FIXME only appropriate for H1
        Document doc = job.getOrderXMLdoc();
        String TRAPS_XPATH = "/crawl-order/controller/newObject" + "/newObject[@name='decide-rules']"
                             + "/map[@name='rules']/newObject[@name='" + Constants.GLOBAL_CRAWLER_TRAPS_ELEMENT_NAME + "']";
        Node trapsNode = doc.selectSingleNode(TRAPS_XPATH);
        assertNotNull("Should have added a node", trapsNode);

        Element stringList = (Element) ((Element) trapsNode).elements("stringList").get(0);
        assertTrue("Should be several crawler traps present", stringList.elements("string").size() > 2);
        */
    }


    @Test ()
    public void testAddConfigurationMinCountObjects() {
        // Note: The configurations have these expectations:
        // 500, 1400, 2400, 4000
        DomainConfiguration dc1 = TestInfo.createConfig("kb.dk", "fuld_dybde", 112);
        DomainConfiguration dc2 = TestInfo.createConfig("netarkivet.dk", "fuld_dybde", 1112);
        DomainConfiguration dc3 = TestInfo.createConfig("statsbiblioteket.dk", "fuld_dybde", 2223);
        // System.out.println("generatorclass: " + Settings.get(HarvesterSettings.JOBGEN_CLASS));
        // This line is needed for this test to pass if this test is run as part of the whole suite
        // If the default JOBGEN_CLASS is changed, this will not work.
        dk.netarkivet.harvester.scheduler.jobgen.DefaultJobGenerator.reset();
        Settings.set(HarvesterSettings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE, "3");
        Settings.set(HarvesterSettings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "0");
        Job job = JobTest.createDefaultJob(dc1);
        // should be okay, relative size difference below 3
        job.addConfiguration(dc2);
        assertFalse(createJobGenerator().canAccept(job, dc3, null));
    }

    protected abstract AbstractJobGenerator createJobGenerator();
}
