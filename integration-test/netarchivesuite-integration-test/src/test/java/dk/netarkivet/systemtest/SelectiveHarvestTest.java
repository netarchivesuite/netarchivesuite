/* File:            $Id: SystemOverviewTest.java 2382 2012-06-20 12:42:56Z mss $
 * Revision:        $Revision: 2382 $
 * Author:          $Author: mss $
 * Date:            $Date: 2012-06-20 14:42:56 +0200 (Ons, 20 Jun 2012) $
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
package dk.netarkivet.systemtest;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.netarkivet.systemtest.page.Harvests;
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
    @Test (groups = {"guitest","functest"})
    public void selectiveHarvestListingTest() throws Exception {
        addDescription("Verify the functionality of the harvest listings.");
        addStep("Create a selective harvest",
                "The harvest should be created successfully a be listed in the HD list");
        String harvest1ID = createHarverstID();
        Harvests.createSelectiveHarvest(driver, harvest1ID);
        NASAssert.assertTrue(driver.getPageSource().contains(harvest1ID),
                harvest1ID + " not found in harvest list after creation");

        addStep("Create a second harvest and active it",
                "The second harvest also be listed in the HD list");
        String harvest2ID = createHarverstID();
        Harvests.createSelectiveHarvest(driver, harvest2ID);
        Harvests.activateHarvest(driver, harvest2ID);
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
