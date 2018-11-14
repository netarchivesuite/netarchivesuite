/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit-test for the HistorySiteSection class. FIXME Does not currently test HistorySiteSection functionality.
 */
public class HistorySiteSectionTester {

    /**
     * Tests the HarvestStatus.makeHarvestRunLink() method.
     */
    @Test
    public void testMakeHarvestRunLink() {
        long harvestID = 10L;
        int harvestRun = 5;
        assertEquals("Should get correctly formatted link for run",
                "<a href=\"/History/Harveststatus-perharvestrun.jsp?" + "HARVEST_ID=" + harvestID + "&amp;HARVEST_RUN="
                        + harvestRun + "&amp;JOB_STATUS=" + HarvestStatusQuery.JOBSTATUS_ALL + "\">" + harvestRun
                        + "</a>", HarvestStatus.makeHarvestRunLink(harvestID, harvestRun));
        try {
            harvestID = -1L;
            harvestRun = 3;
            HarvestStatus.makeHarvestRunLink(harvestID, harvestRun);
            fail("Should die on negative harvestID");
        } catch (ArgumentNotValid e) {
            // expected
        }
        try {
            harvestID = 1L;
            harvestRun = -3;
            HarvestStatus.makeHarvestRunLink(1, -3);
            fail("Should die on negative harvestRun");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }
}
