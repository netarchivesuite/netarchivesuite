/*
 * #%L
 * Bitrepository Integrity Service
 * %%
 * Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class HarvestWebTestHelper {
    public static void createSelectiveHarvest(
            String name, String comments, String[] domains) {
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
        for (String domain:domains) {
            driver.findElement(By.name("domainlist")).sendKeys(domain + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        driver.findElement(By.name("save")).click();
    }

    /**
     * Creates a selective harvest without comments for the default domain.
     */
    public static void createSelectiveHarvest(String name) {
        createSelectiveHarvest(name, null, new String[] {"netarkivet.dk"});
    }

    public static void activateHarvest(String name) {
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Activate")).click();
            }
        }
    }

    public static void deactivateHarvest(String name) {
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Deactivate")).click();
            }
        }
    }
}
