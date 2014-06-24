package dk.netarkivet.systemtest.functional;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
public class SelectiveHarvestTest extends SeleniumTest {
    private String harvestIDForTest;
    private int harvestCounter = 0;
    private DateFormat dateFomatter = new SimpleDateFormat("HHmmss");

    @BeforeMethod(alwaysRun=true)
    public void setup(Method method) {
        Date startTime = new Date();
        harvestIDForTest = getClass().getSimpleName() + "-" +
                method.getName() + "-" + dateFomatter.format(startTime);
        harvestCounter = 1;
    }

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test(groups = {"guitest","functest"})
    public void selectiveHarvestListingTest() throws Exception {
        addDescription("Verify the functionality of the harvest listings.");
        addStep("Create a selective harvest",
                "The harvest should be created successfully a be listed in the HD list");
        String harvest1ID = createHarverstID();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvest1ID);
        NASAssert.assertTrue(driver.getPageSource().contains(harvest1ID),
                harvest1ID + " not found in harvest list after creation");

        addStep("Create a second harvest and active it",
                "The second harvest also be listed in the HD list");
        String harvest2ID = createHarverstID();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvest2ID);
        SelectiveHarvestPageHelper.activateHarvest(harvest2ID);
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID),
                harvest2ID + " not found in harvest list after creation");

        addStep("Hide inactive harvests",
                "The harvest first harvest should disappear from the HD list, " +
                        "the second should remain");
        driver.findElement(By.linkText("Hide inactive harvest definitions")).click();
        NASAssert.assertFalse(driver.getPageSource().contains(harvest1ID),
                "Inactive harvest " + harvest1ID + " show in harvest list after 'hide inactive harvests' was clicked");
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID),
                harvest2ID + " not found in harvest list after creation");

        addStep("Show inactive harvests",
                "The harvest first harvest should reappear from the HD list, " +
                        "the second should remain");
        driver.findElement(By.linkText("Show inactive harvest definitions")).click();
        NASAssert.assertTrue(driver.getPageSource().contains(harvest1ID),
                "Inactive harvest " + harvest1ID + " show in harvest list after 'hide inactive harvests' was clicked");
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID),
                harvest2ID + " not found in harvest list after creation");
    }

    private String createHarverstID() {
        return harvestIDForTest + "-" + harvestCounter++;
    }
}
