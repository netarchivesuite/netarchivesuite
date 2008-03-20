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
package dk.netarkivet.archive.arcrepositoryadmin;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the class ReadOnlyAdminData.
 */
public class ReadOnlyAdminDataTester extends TestCase {
    public ReadOnlyAdminDataTester(String s) {
        super(s);
    }

    public void setUp() {
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.TEST_DIR);
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                TestInfo.TEST_DIR.getAbsolutePath());
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEST_DIR);
        Settings.reload();
    }

    public void testSynchronize() throws Exception {
        ReadOnlyAdminData ad = AdminData.getReadOnlyInstance();

        try {
            ad.synchronize();
            fail("Should have thrown IllegalState if no file found.");
        } catch (IllegalState e) {
            // Should break if no file.
        }

        UpdateableAdminData updad = AdminData.getUpdateableInstance();
        ad.synchronize();
        assertEquals("Should start out with no files",
                0, ad.getAllFileNames().size());

        Thread.sleep(1000);
        updad.addEntry("foo", null, "bar"
        );
        assertEquals("Should not have noticed change without synch",
                0, ad.getAllFileNames().size());

        // Not a test of the method, but if the file does not look modifed,
        // synchronize will fail.  The sleep before update should ensure we
        // get beyond the granularity of file system datestamps.
        assertTrue("File should look modified",
                ad.lastModified < ad.adminDataFile.lastModified());

        ad.synchronize();
        assertEquals("Should have noticed new entry now",
                1, ad.getAllFileNames().size());

        updad.close();
        Thread.sleep(1000);

        // Also check that we clear the entries at update
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.NON_EMPTY_ADMIN_DATA_DIR_ORIG,
                TestInfo.TEST_DIR);
        ad.synchronize();
        // Now the "foo" file should be gone.
        assertEquals("Should see only new entries",
                5, ad.getAllFileNames().size());
    }
}
