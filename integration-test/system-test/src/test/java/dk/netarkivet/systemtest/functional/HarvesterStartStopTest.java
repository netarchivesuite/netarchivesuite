package dk.netarkivet.systemtest.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.page.DomainConfigurationPageHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;

public class HarvesterStartStopTest extends AbstractSystemTest {
    public static String DEFAULT_LARGE_DOMAIN = "kb.dk";

    //@Test(groups = {"guitest", "functest"})
    public void testHeritrixStopOnShutdown() throws Exception {
        addDescription("Tests that a running heritrix harvest is stopped correctly, when the HarvestController is "
                + "stopped with the kill script.");
        addFixture("Configure a domain to be harvested limitless");
        maximizeDefaultLargerDomain();

        addStep("Start a harvest for the large domain", "Wait for the harvest to marked as statred on the harvest "
                + "status page");
        String harvestName = "testHeritrixStopOnShutdown-" + SimpleDateFormat.getTimeInstance().format(new Date());
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvestName, null, DEFAULT_LARGE_DOMAIN);
        SelectiveHarvestPageHelper.activateHarvest(harvestName);
        HarvestUtils.waitForJobStatus(harvestName, HarvestUtils.JobStatus.STARTED);

        addStep("Goto the Running jobs to finde the harvestController running the job",
                "The crawler host should be listed here, with status 'crawler is running");
        String heritrixHost = HarvestUtils.findHarvestingHost();

        addStep("Goto the crawler host and find the crawler process",
                "A process for the active TESTX for the h3server should be running");
        String crawlerProcessString = getTestController().runCommand(heritrixHost,
                "ps -e u|grep " + getTestX() + "|grep h3server");
        assertThat(crawlerProcessString, containsString(""));
    }

    public static void maximizeDefaultLargerDomain() {
        DomainConfigurationPageHelper.gotoDefaultConfigurationPage(DEFAULT_LARGE_DOMAIN);
        DomainConfigurationPageHelper.setMaxObjects(-1);
        DomainConfigurationPageHelper.submitChanges();
    }
}
