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
package dk.netarkivet.harvester.tools;

/**
 * Tests of the tool to create metadata files.
 */

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.SetSystemProperty;

@Ignore("binary derby database not converted to scripts yet")
public class HarvestdatabaseUpdateApplicationTester {
    SetSystemProperty derbyLog = new SetSystemProperty("derby.stream.error.file", new File(
            dk.netarkivet.harvester.datamodel.TestInfo.TEMPDIR, "derby.log").getAbsolutePath());
    ReloadSettings rs = new ReloadSettings();
    File commonTempdir = new File(TestInfo.TEMPDIR, "commontempdir");

    /**
     * Primary use it to create a updated **hddb.jar after database changes.
     */
    @Test
    public void testUpdateFull() throws Exception {
        setupDatabase("fullhddb");
        DBSpecifics.getInstance().updateTables();
    }

    /**
     * Primary use it to create a updated **hddb.jar after database changes.
     */
    @Test
    public void testUpdateEmpty() throws Exception {
        setupDatabase("emptyhddb");
        DBSpecifics.getInstance().updateTables();
    }

    private void setupDatabase(String dataFile) throws Exception {
        String derbyDBUrl = "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/" + dataFile;
        Settings.set(CommonSettings.DB_BASE_URL, derbyDBUrl);
        Settings.set(CommonSettings.DB_MACHINE, "");
        Settings.set(CommonSettings.DB_PORT, "");
        Settings.set(CommonSettings.DB_DIR, "");
        commonTempdir.mkdir();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, commonTempdir.getAbsolutePath());

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        HarvestDAOUtils.resetDAOs();

        DatabaseTestUtils.createHDDB("/" + dk.netarkivet.harvester.datamodel.TestInfo.TOPDATADIR + "/"
                + dataFile + ".sql", dataFile, dk.netarkivet.harvester.datamodel.TestInfo.TEMPDIR);
    }
}
