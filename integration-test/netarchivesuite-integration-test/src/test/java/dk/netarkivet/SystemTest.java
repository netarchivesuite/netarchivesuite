package dk.netarkivet;

import org.apache.log4j.Logger;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public class SystemTest extends ExtendedTestCase {
	
	protected Selenium selenium;
	protected final Logger log = TestLogger.getLogger(getClass());

	@BeforeClass
	@Parameters({"selenium.host","selenium.port","selenium.browser","selenium.url"})
	public void startSelenium (
			@Optional("localhost") String host, 
			@Optional("4444")String port, 
			@Optional("*firefox") String browser, 
			@Optional("http://kb-test-adm-001.kb.dk") String url) {

		
		selenium = new DefaultSelenium(host, Integer.parseInt(port), browser, url + ":" + getPort() + "/");
		selenium.start();
	    selenium.open("/HarvestDefinition/");
	}

	@AfterClass(alwaysRun=true)
	public void stopSelenium() {
		this.selenium.stop();
	}

	
	public String getPort() {
		return System.getProperty("systemtest.port", "8071");
	}
}
