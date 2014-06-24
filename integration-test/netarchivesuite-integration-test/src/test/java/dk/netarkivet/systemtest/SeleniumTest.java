package dk.netarkivet.systemtest;

import dk.netarkivet.systemtest.environment.ApplicationManager;
import dk.netarkivet.systemtest.environment.TestEnvironmentManager;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.jaccept.structure.ExtendedTestCase;
import org.jaccept.testreport.ReportGenerator;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * The super class for all Selenium based system tests.
 */
@SuppressWarnings({ "unused"})
public abstract class SeleniumTest extends ExtendedTestCase {
    protected static TestEnvironmentManager environmentManager;
    protected static ApplicationManager applicationManager;
    private static ReportGenerator reportGenerator;
    protected final TestLogger log = new TestLogger(getClass());
    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeSuite(alwaysRun=true)
    public void setupTest() {
        environmentManager = new TestEnvironmentManager(getTestX(), "http://kb-test-adm-001.kb.dk", 8071);
        applicationManager = new ApplicationManager(environmentManager);

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
                environmentManager.runCommandWithoutQuotes(getStartupScript());
            } catch (Exception e) {
                throw new RuntimeException("Failed to start test system", e);
            }
        } else {
            if (System.getProperty("systemtest.redeploy.gui", "false").equals("true")) {
                applicationManager.redeployGUI();
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
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        baseUrl = environmentManager.getGuiHost() + ":" + environmentManager.getGuiPort();
        PageHelper.initialize(driver, baseUrl);
        applicationManager.waitForGUIToStart(60);

    }

    private void setupFixture() {
        HarvestUtils.minimizeDefaultHarvest();
    }
    
    @AfterSuite(alwaysRun=true)
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

    /**
     * Identifies the test on the test system. More concrete this value will be
     * used for the test environment variable.
     */
    protected String getTestX() {
        return System.getProperty("deployable.postfix","SystemTest");
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
