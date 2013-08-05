/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.systemtest.page;

import java.util.List;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DomainConfigurationPageHelper {
    public static final String NEW_CONFIGURATION = "New configuration";

    public static final String DEFAULT_DOMAIN_NAME = "defaultconfig";
    public static final String MAX_OBJECTS_FIELD = "maxObjects";
    public static final String MAX_BYTES_FIELD = "maxBytes";
    public static final String COMMENTS = "comments";

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
        TestEventManager.getInstance().addStimuli("Updating configuration" + configurationName);
        DomainWebTestHelper.editDomain(domainName);

        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            List<WebElement> rowCells = webElement.findElements(By.xpath("td"));
            if (rowCells.size() > 0 && //none header
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

    public static void submitChanges() {
        PageHelper.getWebDriver().findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }
}
