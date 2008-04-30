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
import java.util.Arrays;
import java.util.List;

import dk.netarkivet.common.utils.SettingsStructure;
import dk.netarkivet.common.utils.StringTree;

/**
 * Provides access to monitor settings.
 * The settings are retrieved from an monitor_settings.xml file.
 */
public class Settings {
    /**
     * The property name specifying the file name of the settings file.
     * If the property is unset, uses DEFAULT_FILEPATH.
     */
    public static final String SETTINGS_FILE_NAME_PROPERTY
            = "dk.netarkivet.monitorsettings.file";

    /** The default place where the settings file can be found. */
    final static String DEFAULT_FILEPATH = "./conf/monitor_settings.xml";

    /** The singleton Settings object initialized at load time. */
    public static final SettingsStructure SETTINGS_STRUCTURE
            = new SettingsStructure(SETTINGS_FILE_NAME_PROPERTY,
                                    DEFAULT_FILEPATH);

    /**
     * The fields of this class that don't actually correspond to settings,
     * or are pluggable settings not always present.
     */
    public static final List<String> EXCLUDED_FIELDS = Arrays.asList(
            "DEFAULT_FILEPATH", "DEFAULT_XSD_FILEPATH");

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#get(String)
     */
    public static String get(String s) {
        return SETTINGS_STRUCTURE.get(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getInt(String)
     */
    public static int getInt(String s) {
        return SETTINGS_STRUCTURE.getInt(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getLong(String)
     */
    public static long getLong(String s) {
        return SETTINGS_STRUCTURE.getLong(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#get(String)
     */
    public static String[] getAll(String s) {
        return SETTINGS_STRUCTURE.getAll(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getSettingsFile()
     */
    public static File getSettingsFile() {
        return SETTINGS_STRUCTURE.getSettingsFile();
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getEdition()
     */
    public static int getEdition() {
        return SETTINGS_STRUCTURE.getEdition();
    }

    /** Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getTree(String)
     */
    public static StringTree<String> getTree(String path) {
        return SETTINGS_STRUCTURE.getTree(path);
    }

    /**
     * Utility method. Provides static access to conditionalReload in
     * settingsStructure.
     *
     * @see SettingsStructure#conditionalReload()
     */
    public static void conditionalReload() {
        SETTINGS_STRUCTURE.conditionalReload();
    }

    /**
     * Utility method. Provides static access to reload in settingsStructure.
     *
     * @see SettingsStructure#reload()
     */
    public static void reload() {
        SETTINGS_STRUCTURE.reload();
    }

    /**
     * Utility method. Provides static access to setter in settingsStructure.
     *
     * @see SettingsStructure#set(String,String...)
     */
    public static void set(String s, String... values) {
        SETTINGS_STRUCTURE.set(s, values);
    }

    /**
     * Utility method. Provides static access to create in settingsStructure.
     *
     * @see SettingsStructure#create(String,String...)
     */
    public static void create(String s, String... values) {
        SETTINGS_STRUCTURE.create(s, values);
    }

    /* The setting names used should be declared and documented here */

    /** The password needed to connect as 'monitorRole' to the MBeanservers. */
    public static final String JMX_MONITOR_ROLE_PASSWORD_SETTING
            = "settings.monitor.jmxMonitorRolePassword";
}
