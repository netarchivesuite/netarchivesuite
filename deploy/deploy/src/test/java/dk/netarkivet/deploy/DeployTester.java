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
package dk.netarkivet.deploy;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class DeployTester extends TestCase {

    String oldSettingsFileName;
    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // reset Settings to before
        rs.tearDown();
    }

    /**
     * This test invokes the deploy application and verifies that all files
     * (settings.xml, scripts, bats) created by the script matches the target
     * files stored in CVS Any change to the output files, will break this test.
     * When the test is broken: Verify that all differences reported by this
     * test are intended and correct, when all output files are verified
     * correct, replace the target files in CVS with the new set of
     * output files.
     */
    public void testDeploy() {
        if (!TestUtils.runningAs("SVC")) {
            return;
        }
        String it_conf_xml_name = TestInfo.IT_CONF_FILE.getPath();
        //String it_conf_xml_name = TestInfo.IT_CONF_TEST_FILE.getPath();
        String settings_xml_name = TestInfo.SETTINGS_FILE.getPath();
        String environmentName = "UNITTEST";
        String output_dir = TestInfo.TMPDIR.getPath();
        String[] args =
                {it_conf_xml_name,
                        settings_xml_name,
                        environmentName,
                        output_dir};
        DeployApplication.main(args);
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.TARGETDIR,
                                              TestInfo.TMPDIR);
        if (differences.length() > 0) {
            System.out.println(differences);
        }
        assertEquals("No differences expected", 0, differences.length());

        // find all settings*.xml files
        List<File> settingsFiles = TestFileUtils.findFiles(TestInfo.TMPDIR,
                new FileFilter() {
                    Pattern settingsPattern = Pattern.compile(
                            "^settings.*\\.xml$");
                    public boolean accept(File pathname) {
                        return settingsPattern.matcher(
                                pathname.getName()).matches();
                    }
                });
        // XSD-test them
        for (File f : settingsFiles) {
            System.setProperty("dk.netarkivet.settings.file",
                    f.getAbsolutePath());
            Settings.reload();
            Settings.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
        }
    }

    /** Test that we can deploy with a single location.
     *
     */
    public void testDeploySingle() {
        if (!TestUtils.runningAs("SVC")) {
            return;
        }
        String it_conf_xml_name = TestInfo.IT_CONF_SINGLE_FILE.getPath();
        //String it_conf_xml_name = TestInfo.IT_CONF_TEST_FILE.getPath();
        String settings_xml_name = TestInfo.SETTINGS_FILE.getPath();
        String environmentName = "UNITTEST";
        String output_dir = TestInfo.TMPDIR.getPath();
        String[] args =
                {it_conf_xml_name,
                 settings_xml_name,
                 environmentName,
                 output_dir};
        DeployApplication.main(args);
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.SINGLE_TARGET_DIR,
                                              TestInfo.TMPDIR);
        if (differences.length() > 0) {
            System.out.println(differences);
        }
        assertEquals("No differences expected", 0, differences.length());

        // find all settings*.xml files
        List<File> settingsFiles = TestFileUtils.findFiles(
                TestInfo.TMPDIR,
                new FileFilter() {
                    Pattern settingsPattern = Pattern.compile(
                            "^settings.*\\.xml$");
                    public boolean accept(File pathname) {
                        return settingsPattern.matcher(
                                pathname.getName()).matches();
                    }
                 }
                );
        // XSD-test them
        for (File f : settingsFiles) {
            System.setProperty("dk.netarkivet.settings.file",
                               f.getAbsolutePath());
            //TODO test here
        }

    }
}
