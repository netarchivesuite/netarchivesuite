/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
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
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jaccept.structure.ExtendedTestCase;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import com.thoughtworks.selenium.Selenium;

/**
 * The super class for all Selenium based system tests.
 */
public abstract class SeleniumTest extends ExtendedTestCase {
    protected TestEnvironmentManager environmentManager;
    protected final TestLogger log = new TestLogger(getClass());
    protected static FirefoxDriver driver;
    protected static Selenium selenium;
    protected String baseUrl;

    @BeforeTest (alwaysRun=true)
    public void setupTest() {
        environmentManager = new TestEnvironmentManager(getTestX(), 8071);
        startTestSystem();
        initialiseSelenium();
    }
    
    /**
     * Start the full test system. May be used to start 
     */
    private void startTestSystem() {
        if (System.getProperty("systemtest.deploy", "false").equals("true")) {
            try {
                environmentManager.runCommandWithoutQuotes(getStartupScript());
            } catch (Exception e) {
                throw new RuntimeException("Failed to start test system", e);
            }
        }
    }

    /**
     * Defines the default test system startup script to run. May be overridden by 
     * subclasses classes.
     * @return The startup script to run.
     */
    protected String getStartupScript() {
        return "all_test_db.sh";
    }
    
    private void initialiseSelenium() {
        driver = new FirefoxDriver();
        baseUrl = "http://kb-test-adm-001.kb.dk:" + environmentManager.getPort();
        selenium = new WebDriverBackedSelenium(driver, baseUrl);
        
        int numberOfSecondsToWaiting = 0;
        int maxNumberOfSecondsToWait = 60;
        System.out.print("Waiting for GUI to start");
        while (numberOfSecondsToWaiting++ < maxNumberOfSecondsToWait) {
            driver.get(baseUrl + "/HarvestDefinition/");
            if (selenium.isTextPresent("Definitions")) {
                System.out.println();
                return;
            } else {
                System.out.print(".");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        } 
        throw new RuntimeException("Failed to load GUI");
    }
    
    @AfterTest (alwaysRun=true)
    public void stopSelenium() {
        selenium.stop();
    }

    /**
     * Identifies the test on the test system. More concrete this value will be
     * used for the test environment variable.
     * 
     * @return
     */
    protected String getTestX() {
        return "SystemTest";
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
            log.info("Test failure, dumping screenshot as " + "target/failurescreendumps/" + 
                    result.getMethod() + ".png");
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            try {
                FileUtils.copyFile(scrFile, new File("failurescreendumps/" + result.getMethod() + ".png"));
            } catch (IOException e) {
                log.error("Failed to save screendump on error");
            }
        }
    }
}
