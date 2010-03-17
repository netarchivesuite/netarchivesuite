/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.testutils.preconfigured;

import java.io.File;

import dk.netarkivet.common.utils.Settings;

public class ReloadSettings implements TestConfigurationIF {
    private File f;
    private String oldSettingsFilenames;
    private String settingsFileProperty = Settings.SETTINGS_FILE_PROPERTY;

    public ReloadSettings() {

    }

    public ReloadSettings(File f) {
        this.f = f;
    }

    public void setUp() {
        oldSettingsFilenames = System.getProperty(settingsFileProperty);
        if (f != null) {
            System.setProperty(settingsFileProperty, f.getAbsolutePath());
        }
        Settings.reload();
    }

    public void tearDown() {
        if (oldSettingsFilenames != null) {
            System.setProperty(settingsFileProperty, oldSettingsFilenames);
        } else {
            System.clearProperty(settingsFileProperty);
        }
        Settings.reload();
    }

}
