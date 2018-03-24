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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class TemplatePageTest extends AbstractSystemTest {

    @BeforeMethod(alwaysRun = true)
    public void setup(Method method) {
    }

    @Test(priority=-1, groups = {"guitest", "functest"})
    public void updateTemplateTest() throws Exception {
        addDescription("Tests that harvest tempplates can be updated.");
        addStep("Goto the template page", "The template page should load");
        PageHelper.gotoPage(PageHelper.MenuPages.EditHarvestTemplates);
        assertTrue(driver.getPageSource().contains("Edit Harvest Templates"),
                "Template page not loaded correctly");

        addStep("Deactivate default_obeyrobots template", "");
        WebElement flipForm = driver.findElement(By.id("default_obeyrobotsflip"));
        flipForm.findElement(By.tagName("a")).click();
        flipForm = driver.findElement(By.id("default_obeyrobotsflip"));
        assertFalse(flipForm.findElement(By.xpath("parent::*")).findElement(By.xpath("parent::*")).isDisplayed(),
                "Ancestral row should be hidden.");
        addStep("Show inactive templates", "");
        driver.findElement(By.id("show")).click();
        flipForm = driver.findElement(By.id("default_obeyrobotsflip"));
        assertTrue(flipForm.findElement(By.xpath("parent::*")).findElement(By.xpath("parent::*")).isDisplayed(),
                "Ancestral row should be visible.");
        addStep("Reactivate default_obeyrobots", "");
        flipForm = driver.findElement(By.id("default_obeyrobotsflip"));
        flipForm.findElement(By.tagName("a")).click();
        flipForm = driver.findElement(By.id("default_obeyrobotsflip"));
        assertTrue(flipForm.findElement(By.xpath("parent::*")).findElement(By.xpath("parent::*")).isDisplayed(),
                        "Ancestral row should be visible.");

        addStep("Replace 'default_orderxml' ", "");

        WebElement defaultOrderRow =  driver.findElement(By.id("default_orderxmlflip")).findElement(
                        By.xpath("parent::*")).findElement(By.xpath("parent::*"));
        new Select(defaultOrderRow.findElement(By.name("requestedContentType"))).selectByValue("binary/octet-stream");
        defaultOrderRow.findElement(By.name("download")).click();

        File tempDir = File.createTempFile("ssss","dkdkdkd").getParentFile();
        File downloadedFile = new File(tempDir, "default_orderxml.xml");
        assertTrue(downloadedFile.exists(), downloadedFile.getAbsolutePath() + " should exist.");

        PageHelper.gotoPage(PageHelper.MenuPages.EditHarvestTemplates);
        defaultOrderRow =  driver.findElement(By.id("default_orderxmlflip")).findElement(
                               By.xpath("parent::*")).findElement(By.xpath("parent::*"));
        String fileToUpload = "src/test/resources/default_orderxml.cxml";
        File file = new File(fileToUpload);
        assertTrue(file.exists(), "No such file: "  + file.getAbsolutePath());
        defaultOrderRow.findElement(By.name("upload_file")).sendKeys(file.getAbsolutePath());
        addStep("Click the upload button.",
                "The text 'The harvest template 'default_orderxml' has been updated' should be displayed");
        defaultOrderRow.findElement(By.name("upload")).click();
        NASAssert.assertTrue(driver.getPageSource().contains("The harvest template 'default_orderxml' has been updated"),
                "Template not updated correctly");
    }


}
