/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.systemtest.functional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.page.DomainConfigurationPageHelper;
import dk.netarkivet.systemtest.page.DomainWebTestHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;
import dk.netarkivet.systemtest.performance.AbstractStressTest;

/**
 * Test specification: http://netarchive.dk/suite/TEST1 .
 */
public class SelectiveHarvestTest extends AbstractSystemTest {
    private String harvestIDForTest;
    private int harvestCounter = 0;
    private DateFormat dateFomatter = new SimpleDateFormat("HHmmss");

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) {
        Date startTime = new Date();
        harvestIDForTest = getClass().getSimpleName() + "-" + method.getName() + "-" + dateFomatter.format(startTime);
        harvestCounter = 1;
    }

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test(groups = {"guitest", "functest"})
    public void selectiveHarvestListingTest() throws Exception {
        addDescription("Verify the functionality of the harvest listings.");
        addReference("http://netarchive.dk/suite/TEST1");
        addStep("Create a selective harvest", "The harvest should be created successfully a be listed in the HD list");
        String harvest1ID = createHarverstID();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvest1ID);
        assertTrue(driver.getPageSource().contains(harvest1ID), harvest1ID
                + " not found in harvest list after creation");

        addStep("Create a second harvest and active it", "The second harvest also be listed in the HD list");
        String harvest2ID = createHarverstID();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvest2ID);
        SelectiveHarvestPageHelper.activateHarvest(harvest2ID);
        assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");

        addStep("Hide inactive harvests", "The harvest first harvest should disappear from the HD list, "
                + "the second should remain");
        driver.findElement(By.linkText("Hide inactive harvest definitions")).click();
        NASAssert.assertFalse(driver.getPageSource().contains(harvest1ID), "Inactive harvest " + harvest1ID
                + " show in harvest list after 'hide inactive harvests' was clicked");
        assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");

        addStep("Show inactive harvests", "The harvest first harvest should reappear from the HD list, "
                + "the second should remain");
        driver.findElement(By.linkText("Show inactive harvest definitions")).click();
        assertTrue(driver.getPageSource().contains(harvest1ID), "Inactive harvest " + harvest1ID
                + " show in harvest list after 'hide inactive harvests' was clicked");
        assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");
    }

    private String createHarverstID() {
        return harvestIDForTest + "-" + harvestCounter++;
    }

    /**
     * Test creates 8 distinct harvest configurations corresponding to two different values for each of 3
     * parameters defined in the EAV model (2³=8). In addition, there are created 3 configurations using the
     * same cxml and attribute values as 3 of the previous 8 jobs. So there should be
     * eight jobs created.
     */
    @Test(groups = {"guitest", "functest"})
    public void jobSplittingTest() {
        final String domainRandomString = RandomStringUtils.random(6, true, true);
        final String configRandomString = RandomStringUtils.random(6, true, true);
        final String configName = "newconf_" + configRandomString;
        List<String> domainList = new ArrayList<String>();
        for (int i=0; i<=10; i++ ) {
            domainList.add("d"+i+"-"+domainRandomString+".dk");
        }
        createDomainAndConfiguration(domainList.get(0), configName, 10, false, false);
        createDomainAndConfiguration(domainList.get(1), configName, 10, false, true);
        createDomainAndConfiguration(domainList.get(2), configName, 10, true, false);
        createDomainAndConfiguration(domainList.get(3), configName, 10, true, true);
        createDomainAndConfiguration(domainList.get(4), configName, 20, false, false);
        createDomainAndConfiguration(domainList.get(5), configName, 20, false, true);
        createDomainAndConfiguration(domainList.get(6), configName, 20, true, false);
        createDomainAndConfiguration(domainList.get(7), configName, 20, true, true);
        //The next three configs are identical to three of the above so they should
        //not generate new jobs - so only 8 jobs expected.
        createDomainAndConfiguration(domainList.get(8), configName, 20, true, true);
        createDomainAndConfiguration(domainList.get(9), configName, 20, true, false);
        createDomainAndConfiguration(domainList.get(10), configName, 20, false, true);
        final String harvestName = "splitharvest_" + RandomStringUtils.random(6, true, true);
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvestName, "",
                (String[]) domainList.toArray(new String[] {}));
        SelectiveHarvestPageHelper.editHarvest(harvestName);
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> selects = table.findElements(By.tagName("select"));
        for (WebElement select: selects) {
            Select dropdown = new Select(select);
            dropdown.selectByVisibleText(configName);
        }
        PageHelper.getWebDriver().findElement(By.name("save")).click();
        SelectiveHarvestPageHelper.activateHarvest(harvestName);
        HarvestUtils.waitForJobGeneration(harvestName);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
        }
        List<WebElement> links = PageHelper.getWebDriver().findElements(By.partialLinkText(harvestName));
        assertEquals(links.size(), 8, "Expected to generate one job per distinct configuration for " + harvestName + ".");
    }

    /**
     * In this test we first set the cfg for every known domain to be identical. Then we make changes in
     * a number of them and check that the right number of jobs is generated.
     */
    @Test(groups = {"guitest", "functest"})
    public void snapshotTest() {
        List<String> editDomainLinks = DomainWebTestHelper.getAllEditDomainLinks();
        for (String link: editDomainLinks) {
            driver.get(link);
            WebElement editAnchor = driver.findElement(By.id("configuration")).findElement(By.linkText("Edit"));
            driver.get(editAnchor.getAttribute("href"));
            Select order_xml = new Select(driver.findElement(By.name("order_xml")));
            order_xml.selectByVisibleText("default_orderxml");
            driver.findElement(By.name("maxObjects")).clear();
            driver.findElement(By.name("maxObjects")).sendKeys("10");
            driver.findElement(By.name("maxBytes")).clear();
            driver.findElement(By.name("maxBytes")).sendKeys("100000");
            DomainConfigurationPageHelper.setMaxHops(10);
            DomainConfigurationPageHelper.setHonorRobots(true);
            DomainConfigurationPageHelper.setExtractJavascript(true);
            DomainConfigurationPageHelper.submitChanges();
        }
        driver.get(editDomainLinks.get(0));
        WebElement editAnchor = driver.findElement(By.id("configuration")).findElement(By.linkText("Edit"));
        driver.get(editAnchor.getAttribute("href"));
        DomainConfigurationPageHelper.setMaxHops(20);
        DomainConfigurationPageHelper.submitChanges();

        driver.get(editDomainLinks.get(1));
        editAnchor = driver.findElement(By.id("configuration")).findElement(By.linkText("Edit"));
        driver.get(editAnchor.getAttribute("href"));
        DomainConfigurationPageHelper.setHonorRobots(false);
        DomainConfigurationPageHelper.submitChanges();

        driver.get(editDomainLinks.get(2));
        editAnchor = driver.findElement(By.id("configuration")).findElement(By.linkText("Edit"));
        driver.get(editAnchor.getAttribute("href"));
        DomainConfigurationPageHelper.setExtractJavascript(false);
        DomainConfigurationPageHelper.submitChanges();

        //So now a snapshot harvest should create four jobs
        PageHelper.gotoPage(PageHelper.MenuPages.SnapshotHarvests);
        final String harvestName = "snapshot_" + RandomStringUtils.random(3, true, true);
        driver.findElement(By.partialLinkText("Create new snapshot harvest definition")).click();
        driver.findElement(By.name("harvestname")).sendKeys(harvestName);
        driver.findElement(By.name("snapshot_byte_limit")).clear();
        driver.findElement(By.name("snapshot_byte_limit")).sendKeys("1000000");
        driver.findElement(By.name("snapshot_byte_limit")).submit();
        driver.findElement(By.cssSelector("input[value=\""+harvestName+"\"]")).submit();
        //HarvestUtils.waitForJobGeneration(harvestName);
        HarvestUtils.waitForJobGeneration(harvestName, this.getTestController());
        PageHelper.gotoPage(PageHelper.MenuPages.AllJobs);
        List<WebElement> links = PageHelper.getWebDriver().findElements(By.partialLinkText(harvestName));
        assertEquals(links.size(), 4, "Expected to generate one job per distinct configuration.");

    }


    private static void createDomainAndConfiguration(String domainName, String configurationName, int maxHops,
            boolean obeyRobots, boolean extractJavascript) {
        DomainWebTestHelper.createDomain(new String[] {domainName});
        DomainConfigurationPageHelper.createConfiguration(domainName, configurationName);
        DomainConfigurationPageHelper.gotoConfigurationPage(domainName, configurationName);
        DomainConfigurationPageHelper.setMaxHops(maxHops);
        DomainConfigurationPageHelper.setHonorRobots(obeyRobots);
        DomainConfigurationPageHelper.setExtractJavascript(extractJavascript);
        DomainConfigurationPageHelper.submitChanges();
    }



}
