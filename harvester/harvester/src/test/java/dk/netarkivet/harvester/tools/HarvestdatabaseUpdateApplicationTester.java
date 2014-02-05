/*$Id: CreateCDXMetadataFileTester.java 2284 2012-03-06 09:51:01Z mss $
* $Revision: 2284 $
* $Date: 2012-03-06 10:51:01 +0100 (Tue, 06 Mar 2012) $
* $Author: mss $
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
package dk.netarkivet.harvester.tools;
/**
 * Tests of the tool to create metadata files.
 */

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.harvester.datamodel.TestInfo;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.SetSystemProperty;


public class HarvestdatabaseUpdateApplicationTester extends TestCase {
    SetSystemProperty derbyLog
            = new SetSystemProperty(
            "derby.stream.error.file",
            new File(dk.netarkivet.harvester.datamodel.TestInfo.TEMPDIR, "derby.log")
                    .getAbsolutePath());
    ReloadSettings rs = new ReloadSettings();
    File commonTempdir = new File(TestInfo.TEMPDIR, "commontempdir");


    public HarvestdatabaseUpdateApplicationTester(String s) {
        super(s);
    }

    public void setUp()  throws Exception {

    }

    public void tearDown(){}

    /**
     * Primary use it to create a updated **hddb.jar after database changes.
     */
    public void testUpdateFull() throws Exception {
        setupDatabase("fullhddb");
    }

    /**
     * Primary use it to create a updated **hddb.jar after database changes.
     */
    public void testUpdateEmpty() throws Exception {
        setupDatabase("emptyhddb");
    }

    private void setupDatabase(String dataFile) throws IOException, SQLException, IllegalAccessException {
        String derbyDBUrl = "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath()
            + "/" + dataFile;
        Settings.set(CommonSettings.DB_BASE_URL, derbyDBUrl);
        Settings.set(CommonSettings.DB_MACHINE, "");
        Settings.set(CommonSettings.DB_PORT, "");
        Settings.set(CommonSettings.DB_DIR, "");
        commonTempdir.mkdir();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
                commonTempdir.getAbsolutePath());

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());
        HarvestDAOUtils.resetDAOs();

        Connection c = DatabaseTestUtils.getHDDB(
                new File(dk.netarkivet.harvester.datamodel.TestInfo.TOPDATADIR, dataFile + ".jar"),
                dataFile,
                dk.netarkivet.harvester.datamodel.TestInfo.TEMPDIR);
        if (c == null) {
            fail("No connection to Database: "
                    + dk.netarkivet.harvester.datamodel.TestInfo.DBFILE.getAbsolutePath());
        }
    }
}