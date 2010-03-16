/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.utils.Settings;

/**
 * Provides access to monitor settings. The settings are retrieved from an
 * settings.xml file under monitor dir.
 */
public class MonitorSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/monitor/settings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH);
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /* The setting names used should be declared and documented here */

    /**
     * <b>settings.monitor.jmxUsername</b>: <br> The username used to connect to
     * the all MBeanservers started by the application. The username must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String JMX_USERNAME_SETTING
            = "settings.monitor.jmxUsername";

    /**
     * <b>settings.monitor.jmxPassword</b>: <br> The password used to connect to
     * the all MBeanservers started by the application. The password must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String JMX_PASSWORD_SETTING
            = "settings.monitor.jmxPassword";

    /**
     * <b>settings.monitor.logging.historySize</b>: <br> The number of
     * logmessages from each application visible in the monitor.
     */
    public static String LOGGING_HISTORY_SIZE
            = "settings.monitor.logging.historySize";

    /**
     * <b>settings.monitor.jmxProxyTimeout</b>: <br> The number of milliseconds
     * we wait for a connection to other machines when we proxy all machines
     * MBeans to one place for monitoring, for instance in the Status GUI site
     * section.
     */
    public static String JMX_PROXY_TIMEOUT
            = "settings.monitor.jmxProxyTimeout";

    /** Delay between every reregistering in minutes. */
    public static String DEFAULT_REREGISTER_DELAY =
            "settings.monitor.reregisterDelay";
}
