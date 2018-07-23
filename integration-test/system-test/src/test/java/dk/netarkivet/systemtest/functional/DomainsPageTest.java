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

import static dk.netarkivet.systemtest.page.DomainWebTestHelper.HIDE_UNUSED_CONFIGURATIONS_LINK;
import static dk.netarkivet.systemtest.page.DomainWebTestHelper.HIDE_UNUSED_SEED_LISTS_LINK;
import static dk.netarkivet.systemtest.page.DomainWebTestHelper.SHOW_UNUSED_CONFIGURATIONS_LINK;
import static dk.netarkivet.systemtest.page.DomainWebTestHelper.SHOW_UNUSED_SEED_LISTS_LINK;


import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.page.DomainConfigurationPageHelper;
import dk.netarkivet.systemtest.page.DomainWebTestHelper;
import dk.netarkivet.systemtest.page.PageHelper;

public class  DomainsPageTest extends AbstractSystemTest {
    private String domainIDForTest;
    private int domainCounter = 0;
    private DateFormat dateFomatter = new SimpleDateFormat("HHmmss");

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) {
        Date startTime = new Date();
        domainIDForTest = getClass().getSimpleName() + "-" + method.getName() + "-" + dateFomatter.format(startTime);
        domainCounter = 1;
    }

    @Test(groups = {"guitest", "functest"})
    public void domainCreationTest() throws Exception {
        addDescription("Tests that domains can be created correctly.");
        addStep("Click the 'Create domain' link in the left menu under the " + "'Definitions' section",
                "The domain creation page should load");
        driver.findElement(By.linkText("Definitions")).click();
        driver.findElement(By.linkText("Create Domain")).click();
        driver.findElement(By.cssSelector("input[type=\"submit\"]"));
        NASAssert.assertTrue(driver.getPageSource().contains("Enter the domain or list of domains to be created"),
                "Domain creation page not loaded correctly");

        addStep("Add a domain to the list of domains to be created and click create.",
                "The created domain should now be displayed");
        String domain1ID = createDomainID();
        driver.findElement(By.name("domainlist")).sendKeys(domain1ID + "\n");
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        NASAssert.assertTrue(driver.getPageSource().contains("Edit domain"), "Domain page not loaded correctly");

        addStep("Goto the 'Find domain' page and search for the newly added domain", "The domain should be found");
        PageHelper.gotoPage(PageHelper.MenuPages.FindDomains);
        driver.findElement(By.name("DOMAIN_QUERY_STRING")).sendKeys(domain1ID);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        NASAssert.assertTrue(driver.getPageSource().contains("Searching for '" + domain1ID + "' returned 1 hits:"),
                "New domain not found.");
    }

    @Test(groups = {"guitest", "functest"})
    public void usedConfigurationFilteringTest() {
        addDescription("Tests configurations are filtered correctly when "
                + "using the 'Show/Hide unused configuration' filter.");
        usedConfigurationsTest(driver, createDomainID());
    }

    public void usedConfigurationsTest(WebDriver driver, String domain1ID) {
        addStep("Create a new domain", "The edit domain page should be loaded with only the default "
                + "config show. The configurations filter state should be that "
                + "unused configurations are hidden, eg. the filter link " + "text should 'Show unused configurations.");
        DomainWebTestHelper.createDomain(new String[] {domain1ID});
        List<WebElement> configurationRows = readConfigurationTableRows(driver);
        NASAssert.assertEquals(1, configurationRows.size(), "More than one configuration listed in the new domain");
        NASAssert.assertTrue(configurationRows.get(0).getText().contains("defaultconfig"),
                "Didn't find the defaultconfig as the only configuration");

        addStep("Create a new configuration",
                "The configuration should not be listed initially as the configuration is hidden by the unused filter");
        String configuration1ID = createConfigurationID();
        DomainConfigurationPageHelper.createConfiguration(domain1ID, configuration1ID);
        NASAssert.assertEquals(1, configurationRows.size(), "More than one configuration listed after second "
                + "configuration was created. Should have been hidden by unused filter");

        addStep("Click the 'Show unused configurations' link",
                "The new configuration should appear in the configuration list as the second element.");
        PageHelper.clickLink(SHOW_UNUSED_CONFIGURATIONS_LINK);
        configurationRows = readConfigurationTableRows(driver);
        NASAssert.assertEquals(2, configurationRows.size(), "The second configuration didn't appear in the list after "
                + "clicking the 'Show unused configurations' link.");
        NASAssert.assertTrue(configurationRows.get(1).getText().contains(configuration1ID),
                "Didn't find the new configuration in the full configuration list.");

        addStep("Click the 'Hide unused configurations' link",
                "The new configuration should disappear from the configuration list.");
        PageHelper.clickLink(HIDE_UNUSED_CONFIGURATIONS_LINK);
        configurationRows = readConfigurationTableRows(driver);
        NASAssert.assertEquals(1, configurationRows.size(), "More than one configuration listed after second "
                + "configuration was created. Should have been hidden by unused filter");
        NASAssert.assertTrue(configurationRows.get(0).getText().contains("defaultconfig"),
                "Didn't find the defaultconfig as the only configuration");

        addStep("Click the 'Show unused configurations' link again, set the second configuration as "
                + "default configuration and click save. ",
                "Only the second configuration should be, as the 'Hide unused configurations' filter has been reset "
                        + "after the save.");
        PageHelper.clickLink(SHOW_UNUSED_CONFIGURATIONS_LINK);
        configurationRows = readConfigurationTableRows(driver);
        configurationRows.get(1).findElement(By.name("default")).click();
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        configurationRows = readConfigurationTableRows(driver);
        NASAssert.assertEquals(1, configurationRows.size(),
                "More than one configuration shown after switching default.");
        NASAssert.assertTrue(configurationRows.get(0).getText().contains(configuration1ID),
                "Didn't find the new configuration in the full configuration list.");
        NASAssert.assertTrue(configurationRows.get(0).findElement(By.name("default")).isSelected());

        addStep("Click the 'Show unused configurations' link again.", "Both configurations should now be listed again.");
        PageHelper.clickLink(SHOW_UNUSED_CONFIGURATIONS_LINK);
        configurationRows = driver.findElement(By.className("selection_table")).findElements(By.className("row0"));
        NASAssert.assertEquals(2, configurationRows.size(), "Only one configuration listed after switching default "
                + "configuration and showing all configuration.");

        addStep("Create a new active selective harvest for the test domain and select the "
                + "'Hide unused configurations' list on the test domain.", "Both configurations should now be listed");
    }

    @Test(groups = {"guitest", "functest"})
    public void usedSeedListsFilteringTest() {
        addDescription("Tests seed lists are filtered correctly when "
                + "using the 'Show/Hide unused seed list' filter.");
        addStep("Create a new domain", "The edit domain page should be loaded with only the default "
                + "seed list show. The seed list filter state should be that "
                + "unused seed lists are hidden, eg. the filter link " + "text should 'Show unused seed lists.");
        String domain1ID = createDomainID();
        DomainWebTestHelper.createDomain(new String[] {domain1ID});
        List<WebElement> seedListRows = readSeedListTableRows(driver);
        NASAssert.assertEquals(1, seedListRows.size(), "More than one seed list listed in the new domain");
        NASAssert.assertTrue(seedListRows.get(0).getText().contains("defaultseeds"),
                "Didn't find the defaultseeds as the only seed list");

        addStep("Create a new seed list",
                "The seed list should not be listed initially as the seed list is hidden by the unused filter");
        String seedList1ID = createSeedListID();
        DomainWebTestHelper.createSeedList(domain1ID, seedList1ID, new String[] {domain1ID});
        NASAssert.assertEquals(1, seedListRows.size(), "More than one seed list listed after second "
                + "seed list was created. Should have been hidden by unused filter");

        addStep("Click the 'Show unused seed list' link",
                "The new seed list should appear in the seed list list as the second element.");
        PageHelper.clickLink(SHOW_UNUSED_SEED_LISTS_LINK);
        seedListRows = readSeedListTableRows(driver);
        NASAssert.assertEquals(2, seedListRows.size(), "The second seed list didn't appear in the list after "
                + "clicking the 'Show unused seed lists' link.");
        NASAssert.assertTrue(seedListRows.get(1).getText().contains(seedList1ID),
                "Didn't find the new seed list in the full seed list list.");

        addStep("Click the 'Hide unused seed lists' link",
                "The new seed list should disappear from the seed list list.");
        PageHelper.clickLink(HIDE_UNUSED_SEED_LISTS_LINK);
        seedListRows = readSeedListTableRows(driver);
        NASAssert.assertEquals(1, seedListRows.size(), "More than one seed list listed after second "
                + "seed list was created. Should have been hidden by unused filter");
        NASAssert.assertTrue(seedListRows.get(0).getText().contains("defaultseeds"),
                "Didn't find the defaultconfig as the only seed list");

        addStep("Add the new seed list to the default config (which is a used config)",
                "The seed list should now be listed");
        List<WebElement> configurationRows = readConfigurationTableRows(driver);
        configurationRows.get(0).findElement(By.linkText("Edit")).click();
        Select seedListSelect = new Select(driver.findElement(By.name("seedListList")));
        seedListSelect.selectByVisibleText(seedList1ID);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        seedListRows = readSeedListTableRows(driver);
        NASAssert.assertEquals(2, seedListRows.size(), "The second seed list didn't appear in the list after "
                + "it was added to the defaultconfig.");

        addStep("Remove the default seed list from the default configuration",
                "Only the new seed list should now be listed");
        configurationRows = readConfigurationTableRows(driver);
        configurationRows.get(0).findElement(By.linkText("Edit")).click();
        seedListSelect = new Select(driver.findElement(By.name("seedListList")));
        seedListSelect.deselectByVisibleText("defaultseeds");
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        seedListRows = readSeedListTableRows(driver);
        NASAssert.assertEquals(1, seedListRows.size(), "The default seed list still showing  in the list after "
                + "it was removed from the defaultconfig.");
        NASAssert.assertFalse(seedListRows.get(0).getText().contains("defaultseeds"),
                "Found the defaultseeds in the used seed list list after " + "it was removed from the defaultconfig.");
    }

    private String createDomainID() {
        return domainIDForTest + "-" + domainCounter++ + ".dk";
    }

    private String createConfigurationID() {
        return domainIDForTest + "-configuration-" + domainCounter++ + ".dk";
    }

    private String createSeedListID() {
        return domainIDForTest + "-seedlist-" + domainCounter++ + ".dk";
    }

    private List<WebElement> readConfigurationTableRows(WebDriver driver) {
        List<WebElement> seedListTableRows = driver.findElements(By.className("selection_table")).get(0)
                .findElements(By.className("row0"));
        return seedListTableRows;
    }

    private List<WebElement> readSeedListTableRows(WebDriver driver) {
        List<WebElement> seedListTableRows = driver.findElements(By.className("selection_table")).get(1)
                .findElements(By.className("row0"));
        return seedListTableRows;
    }
}
