/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import java.util.HashSet;
import java.util.Set;

import dk.netarkivet.systemtest.page.DomainConfigurationPageHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;
import org.jaccept.TestEventManager;
import org.openqa.selenium.By;

public class HarvestUtils {
    public static String DEFAULT_DOMAIN = "pligtaflevering.dk";
    public static final int MAX_MINUTES_TO_WAIT_FOR_HARVEST = 60;

    /**
     * Ensures that the number of selelective harvests have run for the
     * default domain. Existingharvests - requiredNumberOfHarvests are
     * started as part of this method, and the method returns when the
     * harvests are finished.
     * @param requiredNumberOfHarvests The number of harvests which must have
     * run when this method returns.
     */
    public static void ensureNumberOfHarvestsForDefaultDomain(
            int requiredNumberOfHarvests) {
        int numberOfExtraHarvestsToRun = requiredNumberOfHarvests - getNumberOfHarvestsRun(DEFAULT_DOMAIN);
        if (numberOfExtraHarvestsToRun > 0) {
            runHarvests("EnsureNumberOfHarvests-" + System.currentTimeMillis(), numberOfExtraHarvestsToRun);
        }
    }

    /**
     * Returns number of harvests run for the indicated domain.
     */
    public static int getNumberOfHarvestsRun(String domainName) {
        gotoHarvestHistoryForDomain(domainName);
        return PageHelper.getWebDriver().findElements(By.xpath(
                "//table[@class='selection_table']/tbody/tr[position()>1]")).size();
    }

    public static void gotoHarvestHistoryForDomain(String domainName) {
        PageHelper.gotoSubPage(
                "History/Harveststatus-perdomain.jsp?domainName=" +
                        domainName);
    }

    public static void runHarvests(String name, int count) {
        Set<String> unfinishedHarvests = new HashSet<String>();
        for (int i=0;i<count;i++) {
            String nameWithCounter = name + i;
            unfinishedHarvests.add(nameWithCounter);
            SelectiveHarvestPageHelper.createSelectiveHarvest(nameWithCounter);
            SelectiveHarvestPageHelper.activateHarvest(nameWithCounter);
        }

        long starttime = System.currentTimeMillis();
        TestEventManager.getInstance().addResult("Waiting for the following harvests to finish: " + unfinishedHarvests);
        int minutesWaitingForHarvest = 0;
        while (!unfinishedHarvests.isEmpty()) {
            System.out.print(".");
            PageHelper.reloadSubPage(
                    "History/Harveststatus-perdomain.jsp?domainName=" + DEFAULT_DOMAIN);
            for (String harvest:unfinishedHarvests.toArray(new String [unfinishedHarvests.size()])) {
                if (PageHelper.getWebDriver().getPageSource().contains(harvest)) {
                    System.out.println(harvest + " finished");
                    unfinishedHarvests.remove(harvest);
                }
            }
            try { Thread.sleep(60000); } catch (InterruptedException e) {}
            if (++minutesWaitingForHarvest > MAX_MINUTES_TO_WAIT_FOR_HARVEST) {
                throw new RuntimeException("The harvests " + unfinishedHarvests + " took to long (more that " +
                        MAX_MINUTES_TO_WAIT_FOR_HARVEST + ") to finish, " + "aborting");
            }
        }
        System.out.println("All harvests finished in " + (System.currentTimeMillis() - starttime)/1000 +" seconds");
    }


    public static void minimizeDefaultHarvest() {
        DomainConfigurationPageHelper.gotoDefaultConfigurationPage(DEFAULT_DOMAIN);
        DomainConfigurationPageHelper.setMaxObjects(10);
        DomainConfigurationPageHelper.submitChanges();
    }
}
