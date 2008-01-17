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
import java.util.logging.Logger;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.monitor.Settings;
import dk.netarkivet.testutils.TestFileUtils;

public class DeployTester extends TestCase {

    protected final Logger log = Logger.getLogger(getClass().getName());
    String oldSettingsFileName;
    String oldMonitorSettingsFileName;
    public void setUp() {
        // Save previous settingsfile before setting it to the new one.
        oldSettingsFileName = System.getProperty(
                "dk.netarkivet.settings.file");
        oldMonitorSettingsFileName =  System.getProperty(
            "dk.netarkivet.monitorsettings.file");
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // reset Settings to before
        if (oldSettingsFileName != null) {
            System.setProperty("dk.netarkivet.settings.file",
                               oldSettingsFileName);
        } else {
            System.clearProperty("dk.netarkivet.settings.file");
        }
        if (oldMonitorSettingsFileName != null) {
            System.setProperty("dk.netarkivet.monitorsettings.file",
                               oldMonitorSettingsFileName);
        } else {
            System.clearProperty("dk.netarkivet.monitorsettings.file");
        }

        dk.netarkivet.common.Settings.reload();
        Settings.reload();

    }

    /**
     * This test invokes the deploy application and verifies that all files
     * (settings.xml, scripts, bats) created by the script matches the target files
     * stored in CVS Any change to the output files, will break this test. When the
     * test is broken: Verify that all differences reported by this test are
     * intended and correct, when all output files are verified correct, replace
     * the target files in CVS with the new set of output files.
     */
    public void testDeploy() {
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
                    Pattern settingsPattern = Pattern.compile("^settings.*\\.xml$");
                    public boolean accept(File pathname) {
                        return settingsPattern.matcher(pathname.getName()).matches();
                    }
                });
        // XSD-test them
        for (File f : settingsFiles) {
            System.setProperty("dk.netarkivet.settings.file",
                    f.getAbsolutePath());
            dk.netarkivet.common.Settings.reload();
            dk.netarkivet.common.Settings.SETTINGS_STRUCTURE.validateWithXSD(new File(
                    "./lib/data-definitions/settings.xsd"));
        }
    }

    /** Test that we can deploy with a single location.
     *
     */
    public void testDeploySingle() {
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
        List<File> settingsFiles = TestFileUtils.findFiles(TestInfo.TMPDIR,
                                                           new FileFilter() {
                                                               Pattern settingsPattern = Pattern.compile("^settings.*\\.xml$");
                                                               public boolean accept(File pathname) {
                                                                   return settingsPattern.matcher(pathname.getName()).matches();
                                                               }
                                                           });
        // XSD-test them
        for (File f : settingsFiles) {
            final String fileName = f.getName();
            System.setProperty("dk.netarkivet.settings.file",
                               f.getAbsolutePath());
            Settings.reload();
            Settings.SETTINGS_STRUCTURE.validateWithXSD(new File(
                    "./lib/data-definitions/monitor_settings.xsd"));
        }

    }

    /**
     * Try and read the JMX information in settings.xml for the admin-server.
     * This information is written to the monitor_settings.xml file.
     */
    public void testJmxDeploy() {
        String it_conf_xml_name = TestInfo.IT_CONF_FILE.getPath();
        String settings_xml_name = TestInfo.SETTINGS_FILE.getPath();
        String environmentName = "UNITTEST";
        String output_dir = TestInfo.TMPDIR.getPath();
        String[] args =
                {it_conf_xml_name,
                        settings_xml_name,
                        environmentName,
                        output_dir};
        DeployApplication.main(args);

        // examine the resulting settings.xml for the admin machine
        File adminHostDir = new File(TestInfo.TMPDIR,
                                     TestInfo.TEST_SYSTEM_ADMIN_HOSTNAME);
        System.setProperty("dk.netarkivet.monitorsettings.file",
                           new File(adminHostDir,
                                    "monitor_settings.xml").getAbsolutePath());
        Settings.reload();
        Settings.SETTINGS_STRUCTURE.validateStrings(Settings.class,
                                                Settings.EXCLUDED_FIELDS);

        assertEquals(
                "Failed to get setting settings.monitor.jmxMonitorRolePassword",
                TestInfo.TEST_JMX_PASSWORD,
                Settings.get(
                        "settings.monitor.jmxMonitorRolePassword")
                );
        String jmxHostNumberSetting = "settings.monitor.numberOfHosts";
        assertEquals("Failed to get setting '" + jmxHostNumberSetting + "'",
                     TestInfo.TEST_JMX_HOSTS,
                     Settings.getInt(jmxHostNumberSetting)
                     );

        // try to load list of hosts from settings-fil.
        int numberofhosts = Settings.getInt(jmxHostNumberSetting);
        for (int i = 1; i <= numberofhosts; i++) {
            // get name
            String name = Settings.SETTINGS_STRUCTURE
                    .get("settings.monitor.host" + i + ".name");
            String[] jmxports = Settings.SETTINGS_STRUCTURE
                    .getAll("settings.monitor.host" + i + ".jmxport");
        }
    }
}
