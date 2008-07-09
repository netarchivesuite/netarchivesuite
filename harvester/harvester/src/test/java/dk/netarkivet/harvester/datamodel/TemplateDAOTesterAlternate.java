/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * lc forgot to comment this!
 */
public class TemplateDAOTesterAlternate extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public TemplateDAOTesterAlternate(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        Settings.set(CommonSettings.DB_URL,
                     "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/emptyhddb");
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        TestUtils.resetDAOs();

        Connection c = DatabaseTestUtils.getHDDB(TestInfo.EMPTYDBFILE, TestInfo.TEMPDIR);
        if (c == null) {
            fail("No connection to Database: " + TestInfo.EMPTYDBFILE.getAbsolutePath());
        }

        assertEquals("DBUrl wrong", Settings.get(CommonSettings.DB_URL), "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/emptyhddb");
        TemplateDAO.getInstance();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        DatabaseTestUtils.dropHDDB();
        Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
        f.set(null, null);
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestUtils.resetDAOs();
        rs.tearDown();
    }

    /**
     * Test that it's possible to get access to an empty templates table.
     * This tests that Bug 916 is fixed.
     * TODO merge with TemplateDAOTester
     * @throws IllegalAccessException
     * @throws IOException
     * @throws SQLException
     */
    public void testGetinstanceOnEmptyDatabase() throws IOException, SQLException, IllegalAccessException {
        TemplateDAO dao = null;
        try {
            dao = TemplateDAO.getInstance();
        } catch (Exception e) {
            fail("Should not throw an exception with an templates table without" +
                    "the default template");
        }
        // verify that templates table is indeed derived of default template
        assertFalse("Should not contain default template", dao.exists(
                Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML)));
    }
}