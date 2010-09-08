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

import java.io.File;

import dk.netarkivet.TestUtils;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.WebinterfaceTestCase;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * A TestCase subclass specifically tailored to test webinterface classes,
 * primarily the classes in dk.netarkivet.harvester.webinterface:
 * HarvestStatusTester, EventHarvestTester, DomainDefinitionTester,
 * ScheduleDefinitionTester, SnapshotHarvestDefinitionTester but also
 * dk.netarkivet.archive.webinterface.BitpreserveFileStatusTester
 */
public class HarvesterWebinterfaceTestCase extends WebinterfaceTestCase {
    static final File HARVEST_DEFINITION_BASEDIR
            = new File(TestInfo.WORKING_DIR, "harvestdefinitionbasedir");
    ReloadSettings rs = new ReloadSettings();

    public HarvesterWebinterfaceTestCase(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        Settings.set(CommonSettings.DB_URL, "jdbc:derby:"
                                            + HARVEST_DEFINITION_BASEDIR.getCanonicalPath()
                                            + "/fullhddb");
        DatabaseTestUtils.getHDDB(TestInfo.DBFILE, "fullhddb",
                                  HARVEST_DEFINITION_BASEDIR);
    }

    public void tearDown() throws Exception {
        DatabaseTestUtils.dropHDDB();
        TestUtils.resetDAOs();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
        super.tearDown();
    }

}
   