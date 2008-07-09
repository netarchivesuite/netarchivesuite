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

import java.util.Arrays;
import java.util.List;

import dk.netarkivet.common.utils.Settings;

/**
 * Provides access to monitor settings.
 * The settings are retrieved from an monitor_settings.xml file.
 */
public class MonitorSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/monitor/settings.xml";

    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH);
    }

    /**
     * The fields of this class that don't actually correspond to settings.
     */
    public static List<String> EXCLUDED_FIELDS = Arrays.asList(
            "DEFAULT_SETTINGS_CLASSPATH");

    /* The setting names used should be declared and documented here */

    /** The password needed to connect as 'monitorRole' to the MBeanservers. */
    public static String JMX_MONITOR_ROLE_PASSWORD_SETTING
            = "settings.monitor.jmxMonitorRolePassword";
    /**
     * The number of logmessages from each application visible in the
     * monitor.
     */
    public static String LOGGING_HISTORY_SIZE
            = "settings.monitor.logging.historySize";
}
