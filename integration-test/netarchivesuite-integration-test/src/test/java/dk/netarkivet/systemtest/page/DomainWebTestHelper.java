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

import java.util.Arrays;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DomainWebTestHelper {
    public static final String SHOW_UNUSED_CONFIGURATIONS_LINK = "Show unused configurations";
    public static final String HIDE_UNUSED_CONFIGURATIONS_LINK = "Hide unused configurations";
    public static final String NEW_CONFIGURATION = "New configuration";
    public static final String SHOW_UNUSED_SEED_LISTS_LINK = "Show unused seed lists";
    public static final String HIDE_UNUSED_SEED_LISTS_LINK = "Hide unused seed lists";
    public static final String NEW_SEED_LIST = "New seed list";

    public static void createDomain(String[] domains) {
        TestEventManager.getInstance().addStimuli("Creating domains" + Arrays.asList(domains));
        WebDriver driver = PageHelper.getWebDriver();
        PageHelper.gotoPage(PageHelper.MenuPages.CreateDomain);

        driver.findElement(By.name("domainlist")).clear();
        for (String domain:domains) {
            driver.findElement(By.name("domainlist")).sendKeys(domain + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    public static void editDomain(String domainName) {
        PageHelper.gotoSubPage("HarvestDefinition/Definitions-edit-domain.jsp?name=" + domainName);
    }

    public static void createConfiguration(String domainName, String configurationName) {
        TestEventManager.getInstance().addStimuli("Creating configuration" + configurationName);
        WebDriver driver = PageHelper.getWebDriver();
        editDomain(domainName);

        driver.findElement(By.linkText(NEW_CONFIGURATION)).click();
        driver.findElement(By.name("configName")).sendKeys(configurationName);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    public static void createSeedList(String domainName, String seedListName, String[] seeds) {
        TestEventManager.getInstance().addStimuli("Creating configuration" + seedListName +
                "(" + seeds + ")");
        WebDriver driver = PageHelper.getWebDriver();
        editDomain(domainName);

        driver.findElement(By.linkText(NEW_SEED_LIST)).click();
        driver.findElement(By.name("urlListName")).sendKeys(seedListName);

        driver.findElement(By.name("seedList")).clear();
        for (String seed:seeds) {
            driver.findElement(By.name("seedList")).sendKeys(seed + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }
}
