/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.webinterface;

import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit-test for the HistorySiteSection class.
 * FIXME Does not currently test HistorySiteSection functionality.
 */
public class HistorySiteSectionTester extends TestCase {
    public HistorySiteSectionTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    
    /**
     * Tests the HarvestStatus.makeHarvestRunLink() method.
     */
    public void testMakeHarvestRunLink() {
        long harvestID = 10L;
        int harvestRun = 5;
        assertEquals("Should get correctly formatted link for run",
                "<a href=\"/History/Harveststatus-perharvestrun.jsp?"
                        + "harvestID=" + harvestID 
                        + "&amp;harvestNum=" + harvestRun
                        + "&amp;jobstatusname=" 
                        + HarvestStatusQuery.JOBSTATUS_ALL
                        + "\">" + harvestRun + "</a>",
                HarvestStatus.makeHarvestRunLink(harvestID, harvestRun));
        try {
            harvestID = -1L;
            harvestRun = 3;
            HarvestStatus.makeHarvestRunLink(harvestID, harvestRun);
            fail("Should die on negative harvestID");
        } catch (ArgumentNotValid e) {
            //expected
        }
        try {
            harvestID = 1L;
            harvestRun = -3;
            HarvestStatus.makeHarvestRunLink(1, -3);
            fail("Should die on negative harvestRun");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
}