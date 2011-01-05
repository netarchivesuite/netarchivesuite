package dk.netarkivet;

import com.thoughtworks.selenium.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class Test1 extends SeleneseTestNgHelper {
	  @BeforeTest
	  @Override
	  @Parameters({"selenium.url", "selenium.browser"})
	  public void setUp(@Optional String url, @Optional String browserString)
	      throws Exception {
	    super.setUp("http://kb-test-adm-001.kb.dk:8079/", "*firefox");
	    setCaptureScreenShotOnFailure(true);
	    selenium.setSpeed("1000");
	  }
	
	@Test public void test1() throws Exception {
		step1();
	}
	
	public void step1() throws Exception {
		selenium.open("/HarvestDefinition/");
		selenium.click("link=Systemstate");
		selenium.waitForPageToLoad("3000");
		selenium.click("link=Overview of the system state");
		selenium.waitForPageToLoad("3000");
		selenium.getXpathCount("//span[@id='ctl00']");
		HashSet<Application> expectedApplicationSet = new HashSet<Application>(
				Arrays.asList(NASSystemUtil.getApplications()));
		HashSet<Application> displayedApplicationSet = new HashSet<Application>(
						Arrays.asList(NASSystemUtil.getApplications()));
		boolean moreTableRows = true;
		int rowCounter = 1;
		while (moreTableRows) {
			if (selenium.getTable("."+rowCounter+".0") == null) {
				moreTableRows = false;				
			} else {
				displayedApplicationSet.add(new Application(
						selenium.getTable("."+rowCounter+(".0")),
						selenium.getTable("."+rowCounter+(".1")),
						selenium.getTable("."+rowCounter+(".2"))));
			}
		}
		assertEquals(displayedApplicationSet, expectedApplicationSet);
	}
}
