/* $Id$
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

package dk.netarkivet.monitor;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.TestInfo;
import dk.netarkivet.testutils.TestFileUtils;

public class SettingsTester extends TestCase {

    String oldMonitorSettingsFilename;

    public SettingsTester(String sTestName) {
        super(sTestName);
    }
    
    public void setUp()  {
        try {
            if (!dk.netarkivet.common.utils.TestInfo.TEMPDIR.exists()) {
                TestInfo.TEMPDIR.mkdir();
            }
            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        }
        catch (Exception e) {
            fail("Could not setup configuration");
        }

        oldMonitorSettingsFilename = System.getProperty(Settings.SETTINGS_FILE_NAME_PROPERTY);
    }
    

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        if (oldMonitorSettingsFilename != null) {
            System.setProperty(Settings.SETTINGS_FILE_NAME_PROPERTY,
                               oldMonitorSettingsFilename);
        } else {
            System.clearProperty(Settings.SETTINGS_FILE_NAME_PROPERTY);
        }
        Settings.reload();
    }

    public void testLoadJmxData() {    
        System.setProperty(Settings.SETTINGS_FILE_NAME_PROPERTY,
                new File(TestInfo.TEMPDIR, "monitor_settings.xml").getAbsolutePath());
        Settings.reload();
        Settings.SETTINGS_STRUCTURE.validateStrings(Settings.class,
                                                Settings.EXCLUDED_FIELDS);

        assertEquals("Failed to get setting settings.monitor.jmxMonitorRolePassword",
                     Settings.get(
                             Settings.JMX_MONITOR_ROLE_PASSWORD_SETTING),
                "DetErIkkeVoresSkyld");
        String jmxHostNumberSetting = "settings.monitor.numberOfHosts";
        assertEquals("Failed to get setting '" +  jmxHostNumberSetting + "'",
                     Settings.getInt(jmxHostNumberSetting), 13);

        // try to load list of hosts from settings-fil.
        int numberofhosts = Settings.getInt(jmxHostNumberSetting);
        for (int i=1; i <= numberofhosts; i++) {
            // get name
            Settings.get("settings.monitor.host" + i + ".name");
            Settings.getAll("settings.monitor.host" + i + ".jmxport");
        }
    }
    
}
