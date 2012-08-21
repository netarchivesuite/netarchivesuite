/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.deploy;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

public class CompleteSettingsTester extends TestCase {
	
    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
    }

    /**
     * Tests if the complete settings file is correctly created.
     * If this test fails, it is most likely because the file 
     * tests/dk/netarkivet/deploy/
     * data/originals/complete_settings/complete_settings.xml is obsolete.
     * You probably need to rebuild the file src/dk/netarkivet/deploy/complete_settings.xml
     * using the program dk.netarkivet.deploy.BuildCompleteSettings and replace the version
     * in ../originals/complete_settings/complete_settings.xml with the new version.
     */
    public void testCompleteSettings() throws Exception {
            // the output directory is not automatically created,
            // hence create it before running.
            FileUtils.createDir(TestInfo.TMPDIR);

            String[] args = { TestInfo.FILE_COMPLETE_SETTINGS.getAbsolutePath() };
            BuildCompleteSettings.main(args);

            String differences = TestFileUtils.compareDirsText(
                    TestInfo.COMPLETE_SETTINGS_DIR, TestInfo.TMPDIR);

            assertEquals("No differences expected", "", differences);
    }
}
