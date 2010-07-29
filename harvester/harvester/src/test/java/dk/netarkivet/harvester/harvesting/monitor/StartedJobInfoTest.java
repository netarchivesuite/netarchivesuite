/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.harvesting.monitor;

import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;

/** Unittests for the StartedJobInfo class. */
public class StartedJobInfoTest extends DataModelTestCase {

    public StartedJobInfoTest(String s) {
        super(s);
    }

    public void testStartedJobInfo() {
        StartedJobInfo sji = new StartedJobInfo(42, 450);
    }

    public void testGetJobId() {
        fail("Not yet implemented");
    }

    public void testGetHarvestName() {
        fail("Not yet implemented");
    }

    public void testGetHostName() {
        fail("Not yet implemented");
    }

    public void testGetHostUrl() {
        fail("Not yet implemented");
    }

    public void testGetProgress() {
        fail("Not yet implemented");
    }

    public void testGetQueuedFilesCount() {
        fail("Not yet implemented");
    }

    public void testGetTotalQueuesCount() {
        fail("Not yet implemented");
    }

    public void testGetActiveQueuesCount() {
        fail("Not yet implemented");
    }

    public void testGetExhaustedQueuesCount() {
        fail("Not yet implemented");
    }

    public void testGetElapsedTime() {
        fail("Not yet implemented");
    }

    public void testGetAlertsCount() {
        fail("Not yet implemented");
    }

    public void testGetDownloadedFilesCount() {
        fail("Not yet implemented");
    }

    public void testGetCurrentProcessedKBPerSec() {
        fail("Not yet implemented");
    }

    public void testGetProcessedKBPerSec() {
        fail("Not yet implemented");
    }

    public void testGetCurrentProcessedDocsPerSec() {
        fail("Not yet implemented");
    }

    public void testGetProcessedDocsPerSec() {
        fail("Not yet implemented");
    }

    public void testGetActiveToeCount() {
        fail("Not yet implemented");
    }

    public void testGetStatus() {
        fail("Not yet implemented");
    }

    public void testCompareTo() {
        fail("Not yet implemented");
    }

    public void testUpdate() {
        StartedJobInfo sji = new StartedJobInfo(42, 450);
        CrawlProgressMessage cpm = new CrawlProgressMessage(0, 0);
        
        sji.update(cpm);
    }

}
