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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DomainConfigurationPageHelper {
    public static final String NEW_CONFIGURATION = "New configuration";

    public static final String DEFAULT_DOMAIN_NAME = "defaultconfig";
    public static final String MAX_OBJECTS_FIELD = "maxObjects";
    public static final String MAX_BYTES_FIELD = "maxBytes";
    public static final String COMMENTS = "comments";
    public static final String MAX_HOPS = "MAX_HOPS";
    public static final String HONOR_ROBOTS_DOT_TXT = "HONOR_ROBOTS_DOT_TXT";
    public static final String EXTRACT_JAVASCRIPT = "EXTRACT_JAVASCRIPT";

    public static void createConfiguration(String domainName, String configurationName) {
        TestEventManager.getInstance().addStimuli("Creating configuration" + configurationName);
        WebDriver driver = PageHelper.getWebDriver();
        DomainWebTestHelper.editDomain(domainName);

        driver.findElement(By.linkText(NEW_CONFIGURATION)).click();
        driver.findElement(By.name("configName")).sendKeys(configurationName);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    /**
     * Goto the edit page for the indicated domains default configuration.
     */
    public static void gotoDefaultConfigurationPage(String domainName) {
        gotoConfigurationPage(domainName, DEFAULT_DOMAIN_NAME);
    }

    public static void gotoConfigurationPage(String domainName, String configurationName) {
        TestEventManager.getInstance().addStimuli("Updating configuration " + configurationName);
        DomainWebTestHelper.editDomain(domainName);
        PageHelper.getWebDriver().findElement(By.linkText("Show unused configurations")).click();
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement : tr_collection) {
            List<WebElement> rowCells = webElement.findElements(By.xpath("td"));
            if (rowCells.size() > 0 && // none header
                    rowCells.get(0).getText().contains(configurationName)) {
                webElement.findElement(By.linkText("Edit")).click();
                break;
            }
        }
        PageHelper.getWebDriver().findElement(By.name(MAX_OBJECTS_FIELD)); // Ensure page is loaded.
    }

    public static void setMaxObjects(int value) {
        WebElement maxObjectsField = PageHelper.getWebDriver().findElement(By.name(MAX_OBJECTS_FIELD));
        maxObjectsField.clear();
        maxObjectsField.sendKeys(String.valueOf(value));
    }

    public static void setMaxHops(int value) {
        WebElement maxHopsField = PageHelper.getWebDriver().findElement(By.name(MAX_HOPS));
        maxHopsField.clear();
        maxHopsField.sendKeys(String.valueOf(value));
    }

    public static void setHonorRobots(boolean value) {
        WebElement honorRobotsBox = PageHelper.getWebDriver().findElement(By.name(HONOR_ROBOTS_DOT_TXT));
        setCheckbox(honorRobotsBox, value);
    }

    public static void setExtractJavascript(boolean value) {
        WebElement extractJavascriptBox = PageHelper.getWebDriver().findElement(By.name(EXTRACT_JAVASCRIPT));
        setCheckbox(extractJavascriptBox, value);
    }

    private static void setCheckbox(WebElement checkbox, boolean value) {
        if ( (checkbox.isSelected() && !value) || (!checkbox.isSelected() && value)) {
            checkbox.click();
        }
    }

    public static void submitChanges() {
        PageHelper.getWebDriver().findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }
}
