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
package dk.netarkivet.systemtest;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jaccept.TestEventManager;
import org.jaccept.structure.ExtendedTestCase;
import org.jaccept.testreport.ReportGenerator;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.environment.TestGUIController;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;

/**
 * The super class for all Selenium based system tests.
 */
@SuppressWarnings({"unused"})
public abstract class SeleniumTest extends ExtendedTestCase {
    protected TestEnvironmentController testController;
    protected static TestGUIController TestGUIController;
    private static ReportGenerator reportGenerator;
    protected final TestLogger log = new TestLogger(getClass());
    protected static WebDriver driver;
    protected static String baseUrl;

    public SeleniumTest(TestEnvironmentController testController) {
        this.testController = testController;
    }

    @BeforeSuite(alwaysRun = true)
    public void setupTest() {
        TestGUIController = new TestGUIController(testController);
        deployTestSystem();
        initialiseSelenium();
        setupFixture();
    }

    /**
     * Start the test system, either the full system including resetting of settings/DB, or just reploy of individual
     * component code.
     */
    private void deployTestSystem() {
        if (System.getProperty("systemtest.deploy", "false").equals("true")) {
            try {
                testController.runCommandWithoutQuotes(getStartupScript());
            } catch (Exception e) {
                throw new RuntimeException("Failed to start test system", e);
            }
        } else {
            if (System.getProperty("systemtest.redeploy.gui", "false").equals("true")) {
                TestGUIController.redeployGUI();
            }
        }
    }

    /**
     * Defines the default test system startup script to run. May be overridden by subclasses classes.
     *
     * @return The startup script to run.
     */
    protected String getStartupScript() {
        return "all_test.sh";
    }

    private void initialiseSelenium(){
        FirefoxProfile fxProfile = new FirefoxProfile();
            fxProfile.setPreference("browser.download.folderList",2);
            fxProfile.setPreference("browser.download.manager.showWhenStarting",false);
        try {
            fxProfile.setPreference("browser.download.dir",(File.createTempFile("aaaa","bbbb")).getParentFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","text/csv");
        fxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","text/xml");
        fxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","binary/octet-stream");



        driver = new FirefoxDriver(fxProfile);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        baseUrl = testController.ENV.getGuiHost() + ":" + testController.ENV.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        TestGUIController.waitForGUIToStart(60);
        TestEventManager.getInstance().addFixture("Selecting English as language");
        driver.findElement(By.linkText("English")).click();
    }

    private void setupFixture() {
        HarvestUtils.minimizeDefaultHarvest();
    }

    @AfterSuite(alwaysRun = true)
    public void shutdown() {
        if (driver != null) {
            try {
                SelectiveHarvestPageHelper.deactivateAllHarvests();
                driver.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    @AfterMethod
    /**
     * Takes care of failure situations. This includes: <ol>
     * <li> Generate a a screen dump of the page failing the test.
     * <ol>
     *
     * This method is called by TestNG.
     *
     * @param result The result which TestNG will inject
     */
    public void onFailure(ITestResult result) {
        if (!result.isSuccess()) {
            log.info("Test failure, dumping screenshot as " + "target/failurescreendumps/" + result.getMethod()
                    + ".png");
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                FileUtils.forceMkdir(new File("target"));
                        FileUtils.copyFile(scrFile, new File("target/failurescreendumps/" + result.getMethod() + ".png"));
            } catch (IOException e) {
                log.error("Failed to save screendump on error");
            }
        }
    }

    public TestEnvironmentController getTestController() {
        return testController;
    }
}
