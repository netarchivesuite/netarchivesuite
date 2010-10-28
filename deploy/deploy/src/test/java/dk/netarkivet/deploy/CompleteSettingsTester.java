/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.deploy;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

public class CompleteSettingsTester extends TestCase{
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private PreventSystemExit pse = new PreventSystemExit();

    public void setUp() {
        pss.setUp();
        pse.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);
        // reset Settings to before
        pse.tearDown();
        pss.tearDown();
    }

    /**
     * Tests if the complete settings file is correctly created.
     * If this test fails, it is most likely because the file tests/dk/netarkivet/deploy/
     * data/originals/complete_settings/complete_settings.xml is obsolete.
     * You probably need to rebuild the file src/dk/netarkivet/deploy/complete_settings.xml
     * using the program dk.netarkivet.deploy.BuildCompleteSettings and replace the version
     * in ../originals/complete_settings/complete_settings.xml with the new version.
     */
    public void testCompleteSettings() {
        try {
            // the output directory is not automatically created,
            // hence create it before running.
            FileUtils.createDir(TestInfo.TMPDIR);

            String[] args = { TestInfo.FILE_COMPLETE_SETTINGS.getAbsolutePath() };
            BuildCompleteSettings.main(args);

            String differences = TestFileUtils.compareDirsText(
                    TestInfo.COMPLETE_SETTINGS_DIR, TestInfo.TMPDIR);

            if (differences.length() > 0) {
                pss.tearDown();
                System.out.println("testDeployTest");
                System.out.println(differences);
                pss.setUp();
            }

            assertEquals("No differences expected", 0, differences.length());
        } catch (Exception e) {
            // give error if exception caught.
            assertEquals(e.getMessage(), -1, 0);
        }
    }
    
    /**
     * Method for testing the constructor of the CompleteSettings class.
     */
    public void testConstructor() {
        ReflectUtils.testUtilityConstructor(BuildCompleteSettings.class);
    }
}
