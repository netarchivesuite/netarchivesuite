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

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.AbstractSystemTest;
import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.page.DomainWebTestHelper;
import dk.netarkivet.systemtest.page.PageHelper;

public class ExtendedFieldTest extends AbstractSystemTest {
    private String extendedIDForTest;
    private DateFormat dateFomatter = new SimpleDateFormat("HHmmss");

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) {
        Date startTime = new Date();
        extendedIDForTest = method.getName() + "-" + dateFomatter.format(startTime);
    }

    @Test(groups = {"guitest", "functest"})
    public void extendedDomainStringFieldTest() throws Exception {
        addDescription("Tests that String type extended fields works correctly on domains.");
        extendedDomainStringFieldTest(driver, extendedIDForTest);
    }

    public void extendedDomainStringFieldTest(WebDriver driver, String extendedIDForTest) throws Exception {
        addStep("Create a new String type field (name:" + extendedIDForTest + ") for domains", "");
        PageHelper.gotoPage(PageHelper.MenuPages.ExtendedFields);
        driver.findElement(By.linkText("create Extended Field")).click(); // Todo needs more specific find

        driver.findElement(By.name("extf_name")).clear();
        driver.findElement(By.name("extf_name")).sendKeys(extendedIDForTest);

        Select select = new Select(driver.findElement(By.name("extf_datatype")));
        select.selectByVisibleText("String");

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        addStep("Edit the default domain (" + HarvestUtils.DEFAULT_DOMAIN + ")",
                "The new extended field should be shown");
        DomainWebTestHelper.editDomain(HarvestUtils.DEFAULT_DOMAIN);
        NASAssert.assertTrue(driver.getPageSource().contains(extendedIDForTest));

        addStep("Fill out the new extended field with a value and save the " + "updated domain", "");

        addStep("Reopen the domain", "The new extended field should contain the newly defined value");
    }

}
