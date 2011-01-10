package dk.netarkivet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import dk.netarkivet.page.SystemStatePageHelper;

/**
 * Test specification: http://netarchive.dk/suite/TEST1
 */
public class Test1 {

	private Selenium selenium;

	@BeforeClass
	@Parameters({"selenium.host","selenium.port","selenium.browser","selenium.url"})
	public void startSelenium (
			@Optional("localhost") String host, 
			@Optional("4444")String port, 
			@Optional("*firefox") String browser, 
			@Optional("http://kb-test-adm-001.kb.dk:8079/") String url) {

		
		selenium = new DefaultSelenium(host, Integer.parseInt(port), browser, url);
		selenium.start();
	    selenium.open("/HarvestDefinition/");
	}

	@AfterClass(alwaysRun=true)
	public void stopSelenium() {
		this.selenium.stop();
	}

	/** 
	 * See http://netarchive.dk/suite/It23JMXMailCheck
	 * 
	 * @throws Exception
	 */
	@Test
	public void step1() throws Exception {
		// Click 'Systemstate'->'Overview of the system state' 
		SystemStatePageHelper systemStatePage = new SystemStatePageHelper(selenium);		
		systemStatePage.loadPage();

	    // Check that all internally developed applications are up and running
		Set<Application> expectedApplicationSet = new HashSet<Application>(
				Arrays.asList(NASSystemUtil.getApplications()));
		systemStatePage.validateApplicationList(expectedApplicationSet);
		
		// Check that last status message for each application do not contain errors or warnings");		
		systemStatePage.checkStringsNotPresentInLog( new String[] {"Error", "Warn"});
		
		// Check that there are no empty log messages		
		systemStatePage.checkNoLogsAre("");
		
		// Click on an physical location in the 'Location' column e.g. "K"
        selenium.click("link=Location");
        selenium.waitForPageToLoad("3000");
        selenium.click("//table[@id='system_state_table']/tbody/tr[2]/td[1]/a");
        selenium.waitForPageToLoad("3000");
        
        // Check that you now only see relevant SW applications for the chosen organisation
        systemStatePage.checkAllLocationAre("K");
	}

	/**
	 * Test specification: http://netarchive.dk/suite/It10DefSelHarv
	 */
	@Test(dependsOnMethods={"step1"})
	public void step2() throws Exception {
		
	}
}
