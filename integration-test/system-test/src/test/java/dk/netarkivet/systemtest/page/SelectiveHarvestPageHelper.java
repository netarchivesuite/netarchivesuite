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
package dk.netarkivet.systemtest.page;

import java.util.List;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.HarvestUtils;

/**
 * Provides functionality for commonly used test access to domain web content.
 * <p>
 * Will also log webpage interactions.
 */
public class SelectiveHarvestPageHelper {
    public static void createSelectiveHarvest(String name, String comments, String... domains) {
        TestEventManager.getInstance().addStimuli("Creating harvest " + name);
        WebDriver driver = PageHelper.getWebDriver();
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        driver.findElement(By.linkText("Create new selective harvest definition")).click();

        driver.findElement(By.name("harvestname")).clear();
        driver.findElement(By.name("harvestname")).sendKeys(name);

        driver.findElement(By.name("comments")).clear();
        if (comments != null) {
            driver.findElement(By.name("comments")).sendKeys(comments);
        }

        driver.findElement(By.name("domainlist")).clear();
        for (String domain : domains) {
            driver.findElement(By.name("domainlist")).sendKeys(domain + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        driver.findElement(By.name("save")).click();
    }

    /**
     * Creates a selective harvest without comments for the default domain.
     */
    public static void createSelectiveHarvest(String name) {
        createSelectiveHarvest(name, null, new String[] {HarvestUtils.DEFAULT_DOMAIN});
    }

    public static void activateHarvest(String name) {
        TestEventManager.getInstance().addStimuli("Activating harvest " + name);
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        if (PageHelper.getWebDriver().getPageSource().contains("Show inactive harvest definitions")) {
            PageHelper.getWebDriver().findElement(By.linkText("Show inactive harvest definitions")).click();
        }
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement : tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Activate")).click();
                break;
            }
        }
    }

    public static void editHarvest(String name) {
        TestEventManager.getInstance().addStimuli("Activating harvest " + name);
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        if (PageHelper.getWebDriver().getPageSource().contains("Show inactive harvest definitions")) {
            PageHelper.getWebDriver().findElement(By.linkText("Show inactive harvest definitions")).click();
        }
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement : tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Edit")).click();
                break;
            }
        }
    }

    public static void deactivateHarvest(String name) {
        TestEventManager.getInstance().addStimuli("Deactivating harvest " + name);
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement : tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Deactivate")).click();
            }
        }
    }

    public static void deactivateAllHarvests() {
        TestEventManager.getInstance().addStimuli("Deactivating all harvests");
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        if (PageHelper.getWebDriver().getPageSource().contains("Hide inactive harvest definitions")) {
            PageHelper.getWebDriver().findElement(By.linkText("Hide inactive harvest definitions")).click();
        }
        while (!PageHelper.getWebDriver().getPageSource().contains("No selective harvests defined")) {
            // Ensure page is loaded
            PageHelper.getWebDriver().findElement(By.linkText("Create new selective harvest definition"));
            WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
            List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
            TestEventManager.getInstance().addStimuli(
                    "Deactivating harvest: " + tr_collection.get(1).findElements(By.xpath("td")).get(0).getText());
            tr_collection.get(1).findElement(By.linkText("Deactivate")).click();
        }
    }
}
