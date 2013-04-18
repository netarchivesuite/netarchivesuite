/*
 * #%L
 * Bitrepository Integrity Service
 * %%
 * Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
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

package dk.netarkivet.systemtest.page;

import java.util.HashMap;
import java.util.Map;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PageHelper {
    private static WebDriver driver;
    private static String baseUrl;
    private static Map<MenuPages, String> pageMapping = new HashMap<MenuPages, String>();

    public static enum MenuPages {
        SelectiveHarvests,
        SnapshotHarvests,
        Schedules,
        FindDomains,
        CreateDomain,
        DomainStatistics,
        AliasSummary,
        EditHarvestTemplates,
        GlobalCrawlerTraps,
        ExtendedFields,
        AllJobs,
        AllJobsPerDomain,
        RunningJobs,
        Filestatus,
        BatchjobOverview,
        ViewerproxyStatus,
        OverviewOfTheSystemState
    }

    static {
        pageMapping.put(MenuPages.SelectiveHarvests,
                "HarvestDefinition/Definitions-selective-harvests.jsp");
        pageMapping.put(MenuPages.SnapshotHarvests,
                "HarvestDefinition/Definitions-snapshot-harvests.jsp");
        pageMapping.put(MenuPages.Schedules,
                "HarvestDefinition/Definitions-schedules.jsp");
        pageMapping.put(MenuPages.FindDomains,
                "HarvestDefinition/Definitions-find-domains.jsp");
        pageMapping.put(MenuPages.CreateDomain,
                "HarvestDefinition/Definitions-create-domain.jsp");
        pageMapping.put(MenuPages.DomainStatistics,
                "HarvestDefinition/Definitions-domain-statistics.jsp");
        pageMapping.put(MenuPages.AliasSummary,
                "HarvestDefinition/Definitions-alias-summary.jsp");
        pageMapping.put(MenuPages.EditHarvestTemplates,
                "HarvestDefinition/Definitions-edit-harvest-templates.jsp");
        pageMapping.put(MenuPages.GlobalCrawlerTraps,
                "HarvestDefinition/Definitions-edit-global-crawler-traps.jsp");
        pageMapping.put(MenuPages.ExtendedFields,
                "HarvestDefinition/Definitions-list-extendedfields.jsp");
        pageMapping.put(MenuPages.AllJobs,
                "History/Harveststatus-alljobs.jsp");
        pageMapping.put(MenuPages.AllJobsPerDomain,
                "History/Harveststatus-perdomain.jsp");
        pageMapping.put(MenuPages.RunningJobs,
                "History/Harveststatus-running.jsp");
        pageMapping.put(MenuPages.Filestatus,
                "HarvestDefinition/Definitions-selective-harvests.jsp");
        pageMapping.put(MenuPages.BatchjobOverview,
                "BitPreservation/Bitpreservation-filestatus.jsp");
        pageMapping.put(MenuPages.ViewerproxyStatus,
                "QA/QA-status.jsp");
        pageMapping.put(MenuPages.OverviewOfTheSystemState,
                "Status/Monitor-JMXsummary.jsp");
    }

    public static void gotoPage(MenuPages page) {
        if (driver == null || baseUrl == null) {
            throw new IllegalStateException("Failed to goto page, webdriver " +
                    "and baseurl hasn't been set.");
        }
        String pageUrl = pageMapping.get(page);
        TestEventManager.getInstance().addStimuli("Loading " + pageUrl);
        driver.get(baseUrl + "/" + pageUrl);
    }

    /**
     * Load the page relative to the base ur.
     * @param subURL
     */
    public static void gotoSubPage(String subURL) {
        TestEventManager.getInstance().addStimuli("Loading " + subURL);
        driver.get(baseUrl + "/" + subURL);
    }

    public static void clickLink(String linkText) {
        TestEventManager.getInstance().addStimuli("Clicking '" + linkText + "' link.");
        driver.findElement(By.linkText(linkText)).click();
    }

    public static void initialize(WebDriver theDriver, String theBaseUrl) {
        driver = theDriver;
        baseUrl = theBaseUrl;
    }

    public static WebDriver getWebDriver() {
        return driver;
    }
}
