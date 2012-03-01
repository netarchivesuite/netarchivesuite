/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.datamodel;

import java.util.Set;

import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

public class RunningJobsInfoDAOTester extends DataModelTestCase {
    public RunningJobsInfoDAOTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testGetInstance() {
    	StartedJobInfo sji = new StartedJobInfo("harvest", 42L);
        RunningJobsInfoDAO dao = RunningJobsInfoDAO.getInstance();
        String[] types = dao.getFrontierReportFilterTypes();
        dao.deleteFrontierReports(42L);
        Set<Long> records = dao.getHistoryRecordIds();
        dao.getMostRecentByHarvestName();
        dao.store(sji);
        dao.getFullJobHistory(42l);
        dao.getMostRecentByJobId(42L);
        dao.removeInfoForJob(42L);
        //dao.storeFrontierReport(filterId, report);
    }
}
