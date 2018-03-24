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
package dk.netarkivet.systemtest.page;

import java.util.LinkedList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@SuppressWarnings({"unused"})
public class HarvestHistoryPageHelper {
    private static final String HARVEST_HISTORY_TABLE_CLASS = "selection_table";

    public static final String HARVEST_NAME_HEADER = "Harvest name";
    public static final String RUN_NUMBER_HEADER = "Run number";
    public static final String RUN_ID_HEADER = "Job ID";
    public static final String CONFIGURATION_HEADER = "Configuration";
    public static final String START_TIME_HEADER = "Start time";
    public static final String END_TIME_HEADER = "End time";
    public static final String BYTES_HARVESTED_HEADER = "Bytes Harvested";
    public static final String DOCUMENTS_HARVESTED_HEADER = "Documents Harvested";
    public static final String STOPPED_DUE_TO_HEADER = "Stopped due to";

    public static List<HarvestHistoryEntry> readHarvestHistory() {
        List<HarvestHistoryEntry> harvestHistory = new LinkedList<HarvestHistoryEntry>();
        List<WebElement> rows = PageHelper.getWebDriver().findElement(By.className("selection_table"))
                .findElements(By.tagName("tr"));
        rows.remove(0); // SKip headers
        for (WebElement rowElement : rows) {
            harvestHistory.add(new HarvestHistoryEntry(rowElement));
        }
        return harvestHistory;
    }

    public static class HarvestHistoryEntry {
        final String runNumber;
        final String runID;
        final String configuration;
        final String startTime;
        final String endTime;
        final String bytesHarvested;
        final String documentsHarvested;
        final String stoppedDueTo;

        public HarvestHistoryEntry(WebElement rowElement) {
            List<WebElement> cells = rowElement.findElements(By.xpath("td"));
            this.runNumber = cells.get(0).getText();
            this.runID = cells.get(1).getText();
            this.configuration = cells.get(2).getText();
            this.startTime = cells.get(3).getText();
            this.endTime = cells.get(4).getText();
            this.bytesHarvested = cells.get(5).getText();
            this.documentsHarvested = cells.get(6).getText();
            this.stoppedDueTo = cells.get(7).getText();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HarvestHistoryEntry)) {
                return false;
            }

            HarvestHistoryEntry that = (HarvestHistoryEntry) o;

            if (!bytesHarvested.equals(that.bytesHarvested)) {
                return false;
            }
            if (!configuration.equals(that.configuration)) {
                return false;
            }
            if (!documentsHarvested.equals(that.documentsHarvested)) {
                return false;
            }
            if (!endTime.equals(that.endTime)) {
                return false;
            }
            if (!runID.equals(that.runID)) {
                return false;
            }
            if (!runNumber.equals(that.runNumber)) {
                return false;
            }
            if (!startTime.equals(that.startTime)) {
                return false;
            }
            if (!stoppedDueTo.equals(that.stoppedDueTo)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = runNumber.hashCode();
            result = 31 * result + runID.hashCode();
            result = 31 * result + configuration.hashCode();
            result = 31 * result + startTime.hashCode();
            result = 31 * result + endTime.hashCode();
            result = 31 * result + bytesHarvested.hashCode();
            result = 31 * result + documentsHarvested.hashCode();
            result = 31 * result + stoppedDueTo.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "HarvestHistoryEntry{" + "runNumber='" + runNumber + '\'' + ", runID='" + runID + '\''
                    + ", configuration='" + configuration + '\'' + ", startTime='" + startTime + '\'' + ", endTime='"
                    + endTime + '\'' + ", bytesHarvested='" + bytesHarvested + '\'' + ", documentsHarvested='"
                    + documentsHarvested + '\'' + ", stoppedDueTo='" + stoppedDueTo + '\'' + '}';
        }
    }
}
