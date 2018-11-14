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
package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Alternate unit test class for the TemplateDAO. FIXME Merge with TemplateDAOTester
 */
public class TemplateDAOTesterAlternate {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/emptyhddb");
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        HarvestDAOUtils.resetDAOs();

        DatabaseTestUtils.createHDDB(TestInfo.EMPTYDBFILE, "emptyhddb", TestInfo.TEMPDIR);

        assertEquals("DBUrl wrong", Settings.get(CommonSettings.DB_BASE_URL),
                "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/emptyhddb");
        TemplateDAO.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        DatabaseTestUtils.dropHDDB();
        Field f = ReflectUtils.getPrivateField(DBSpecifics.class, "instance");
        f.set(null, null);
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        HarvestDAOUtils.resetDAOs();
        rs.tearDown();
    }

    /**
     * Test that it's possible to get access to an empty templates table. This tests that Bug 916 is fixed. FIXME merge
     * with TemplateDAOTester
     */
    @Category(SlowTest.class)
    @Test
    public void testGetinstanceOnEmptyDatabase() {
        TemplateDAO dao = null;
        try {
            dao = TemplateDAO.getInstance();
        } catch (Exception e) {
            fail("Should not throw an exception with an templates table without"
                    + "the default template, but threw exception: " + e);
        }
        // verify that templates table is indeed derived of default template
        assertFalse("Should not contain default template",
                dao.exists(Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML)));
    }
}
