/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.By;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;

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
        NASAssert.assertTrue(driver.getPageSource().contains(harvest1ID), harvest1ID
                + " not found in harvest list after creation");

        addStep("Create a second harvest and active it", "The second harvest also be listed in the HD list");
        String harvest2ID = createHarverstID();
        SelectiveHarvestPageHelper.createSelectiveHarvest(harvest2ID);
        SelectiveHarvestPageHelper.activateHarvest(harvest2ID);
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");

        addStep("Hide inactive harvests", "The harvest first harvest should disappear from the HD list, "
                + "the second should remain");
        driver.findElement(By.linkText("Hide inactive harvest definitions")).click();
        NASAssert.assertFalse(driver.getPageSource().contains(harvest1ID), "Inactive harvest " + harvest1ID
                + " show in harvest list after 'hide inactive harvests' was clicked");
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");

        addStep("Show inactive harvests", "The harvest first harvest should reappear from the HD list, "
                + "the second should remain");
        driver.findElement(By.linkText("Show inactive harvest definitions")).click();
        NASAssert.assertTrue(driver.getPageSource().contains(harvest1ID), "Inactive harvest " + harvest1ID
                + " show in harvest list after 'hide inactive harvests' was clicked");
        NASAssert.assertTrue(driver.getPageSource().contains(harvest2ID), harvest2ID
                + " not found in harvest list after creation");
    }

    private String createHarverstID() {
        return harvestIDForTest + "-" + harvestCounter++;
    }
}
