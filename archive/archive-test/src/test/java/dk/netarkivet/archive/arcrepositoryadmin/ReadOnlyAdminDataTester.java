/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.arcrepositoryadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the class ReadOnlyAdminData.
 */
@SuppressWarnings({"deprecation"})
public class ReadOnlyAdminDataTester {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.TEST_DIR);
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.TEST_DIR.getAbsolutePath());
    }

    @After
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEST_DIR);
        rs.tearDown();
    }

    @Test
    public void testSynchronize() throws Exception {
        ReadOnlyAdminData ad = ReadOnlyAdminData.getInstance();

        try {
            ad.synchronize();
            fail("Should have thrown IllegalState if no file found.");
        } catch (IllegalState e) {
            // Should break if no file.
        }

        UpdateableAdminData updad = AdminData.getUpdateableInstance();
        ad.synchronize();
        assertEquals("Should start out with no files", 0, ad.getAllFileNames().size());

        Thread.sleep(1000);
        updad.addEntry("foo", null, "bar");
        assertEquals("Should not have noticed change without synch", 0, ad.getAllFileNames().size());

        // Not a test of the method, but if the file does not look modifed,
        // synchronize will fail. The sleep before update should ensure we
        // get beyond the granularity of file system datestamps.
        assertTrue("File should look modified", ad.lastModified < ad.adminDataFile.lastModified());

        ad.synchronize();
        assertEquals("Should have noticed new entry now", 1, ad.getAllFileNames().size());

        updad.close();
        Thread.sleep(1000);

        // Also check that we clear the entries at update
        TestFileUtils.copyDirectoryNonCVS(TestInfo.NON_EMPTY_ADMIN_DATA_DIR_ORIG, TestInfo.TEST_DIR);
        ad.synchronize();
        // Now the "foo" file should be gone.
        assertEquals("Should see only new entries", 5, ad.getAllFileNames().size());
    }
}
