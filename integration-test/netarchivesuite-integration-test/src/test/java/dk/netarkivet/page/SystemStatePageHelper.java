/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
           
            displayedApplicationSet.add(new Application(
                    selenium.getTable("system_state_table."+rowCounter+".0"),
                    selenium.getTable("system_state_table."+rowCounter+".1"),
                    selenium.getTable("system_state_table."+rowCounter+".2"),
                    selenium.getTable("system_state_table."+rowCounter+".3"),
                    selenium.getTable("system_state_table."+rowCounter+".4") 
                    ));
        }

        NASAssert.assertEquals(expectedApplicationSet, displayedApplicationSet);
    }

    /**
     * Check that all entries in the allocations column in the current system overview table corresponds to the 
     * indicated string.
     */
    public void checkAllLocationAre(String string) {
        numberOfRows = -1;
        for (int rowCounter = 1;rowCounter < getNumberOfRows(); rowCounter++) {
            String log = selenium.getTable("system_state_table."+rowCounter+(".0"));
            NASAssert.assertTrue(log.equals(string), 
                    "Found localtion " + string + " in row " + rowCounter +
            " on system state page");
        }
    }

    /**
     * Checks that none of the indicated string are present in the logs column.
     */
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

    /**
     * Finds the number of rows in the system overview table.
     */
    private int getNumberOfRows() {
        if (numberOfRows == -1) {
            numberOfRows = selenium.getXpathCount("//table[@id='system_state_table']/tbody/tr").intValue();
        }
        return numberOfRows;
    }
}
