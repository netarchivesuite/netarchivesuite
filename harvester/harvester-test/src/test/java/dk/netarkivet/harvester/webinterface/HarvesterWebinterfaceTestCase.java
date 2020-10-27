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

import java.io.File;

import org.junit.After;
import org.junit.Before;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.WebinterfaceTestCase;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * A TestCase subclass specifically tailored to test webinterface classes, primarily the classes in
 * dk.netarkivet.harvester.webinterface: HarvestStatusTester, EventHarvestTester, DomainDefinitionTester,
 * ScheduleDefinitionTester, SnapshotHarvestDefinitionTester but also
 * dk.netarkivet.archive.webinterface.BitpreserveFileStatusTester
 */
public abstract class HarvesterWebinterfaceTestCase extends WebinterfaceTestCase {
    static final File HARVEST_DEFINITION_BASEDIR = new File(TestInfo.WORKING_DIR, "harvestdefinitionbasedir");
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        HarvestDAOUtils.resetDAOs();
        GlobalCrawlerTrapListDBDAO.reset();

        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:" + HARVEST_DEFINITION_BASEDIR.getCanonicalPath()
                + "/fullhddb");
        DatabaseTestUtils.createHDDB("./" + TestInfo.DBFILE + "/fullhddb.sql", "fullhddb", HARVEST_DEFINITION_BASEDIR);
        DBSpecifics.getInstance().updateTables();
    }

    @After
    public void tearDown() throws Exception {
        DatabaseTestUtils.dropHDDB();
        HarvestDAOUtils.resetDAOs();
        GlobalCrawlerTrapListDBDAO.reset();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
        super.tearDown();
    }

}
