package dk.netarkivet.page;

import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.selenium.Selenium;

import dk.netarkivet.Application;
import dk.netarkivet.NASAssert;

/**
 * Provides utilities for accessing, manipulation and testing the 
 * 'Overview of the system state' page.
 */
public class SystemStatePageHelper {
    private Selenium selenium;
    private int numberOfRows = -1;

    public SystemStatePageHelper(Selenium selenium) {
        this.selenium = selenium;
    }

    /**
     * Loads (reloads) the page.
     */
    public void loadPage() {
        selenium.click("link=Systemstate");
        selenium.waitForPageToLoad("1000");
        selenium.click("link=Overview of the system state");
        selenium.waitForPageToLoad("3000");
    }

    /**
     * Verifies the the displayed applications correspond to the supplied set 
     * of applications.
     * @param expectedApplicationSet Set of <code>Application</code>s which 
     * should be displayed in the status table.
     */
    public void validateApplicationList(Set<Application> expectedApplicationSet) {
        // We need to click the 'Instance id' link to differentiate between 
        // instances of the same application running on the same machine
        selenium.click("link=Instance id");
        selenium.waitForPageToLoad("3000");

        Set<Application> displayedApplicationSet = new HashSet<Application>();

        for (int rowCounter = 1;rowCounter < getNumberOfRows(); rowCounter++) {
            System.out.println("Checking row "+rowCounter+", value is: " + selenium.getTable("system_state_table."+rowCounter+".0"));

            displayedApplicationSet.add(new Application(
                    selenium.getTable("system_state_table."+rowCounter+(".0")),
                    selenium.getTable("system_state_table."+rowCounter+(".1")),
                    selenium.getTable("system_state_table."+rowCounter+(".2")),
                    selenium.getTable("system_state_table."+rowCounter+(".3")),
                    selenium.getTable("system_state_table."+rowCounter+(".4"))));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }

    public void checkAllLocationAre(String string) {
        numberOfRows = -1;
        for (int rowCounter = 1;rowCounter < getNumberOfRows(); rowCounter++) {
            String log = selenium.getTable("system_state_table."+rowCounter+(".0"));
            NASAssert.assertTrue(log.equals(string), 
                    "Found localtion " + string + " in row " + rowCounter +
            " on system state page");
        }
    }

    public void checkStringsNotPresentInLog(String[] strings) {
        for (int rowCounter = 1;rowCounter < getNumberOfRows(); rowCounter++) {
            String log = selenium.getTable("system_state_table."+rowCounter+(".6"));
            for (String string: strings) {
                NASAssert.assertTrue(!log.contains(string), 
                        "Found string " + string + " in log on row " + 
                        rowCounter + " on system state page." +
                        "\nFull log message is: " + log);
            }       
        }
    }

    public void checkNoLogsAre(String string) {
        for (int rowCounter = 1;rowCounter < getNumberOfRows(); rowCounter++) {
            String log = selenium.getTable("system_state_table."+rowCounter+(".6"));
            NASAssert.assertTrue(!log.equals(string), 
                    "Found log " + string + " in log on row " + 
                    rowCounter + " on system state page");
        }
    }

    private int getNumberOfRows() {
        if (numberOfRows == -1) {
            numberOfRows = selenium.getXpathCount("//table[@id='system_state_table']/tbody/tr").intValue();
        }
        return numberOfRows;
    }
}
