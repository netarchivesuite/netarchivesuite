/*$Id$
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

package dk.netarkivet.wayback;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the wayback module of NetarchiveSuite.
 */
public class WaybackSettings {
      /** The default place in classpath where the settings
       * file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/wayback/settings.xml";

     /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /**
     * Setting specifying the name of the class used to canonicalize urls. This
     * class must implement the interface org.archive.wayback.UrlCanonicalizer .
     */
    public static String URL_CANONICALIZER_CLASSNAME = 
            "settings.wayback.urlcanonicalizer.classname";


    /**
     * There now follows a list of hibernate-related properties.
     */

    public static String C3P0_ACQUIRE_INCREMENT =
            "settings.wayback.hibernate.c3p0.acquire_increment";
    public static String C3P0_IDLE_PERIOD =
            "settings.wayback.hibernate.c3p0.idle_test_period";
    public static String  C3P0_MAX_SIZE =
            "settings.wayback.hibernate.c3p0.max_size";
    public static String C3P0_MAX_STATEMENTS =
             "settings.wayback.hibernate.c3p0.max_statements";
    public static String C3P0_MIN_SIZE =
             "settings.wayback.hibernate.c3p0.min_size";
    public static String C3P0_TIMEOUT =
             "settings.wayback.hibernate.c3p0.timeout";
    public static String HIBERNATE_DB_URL =
            "settings.wayback.hibernate.connection_url";
    public static String HIBERNATE_DB_DRIVER =
            "settings.wayback.hibernate.db_driver_class";
    public static String HIBERNATE_REFLECTION_OPTIMIZER =
            "settings.wayback.hibernate.use_reflection_optimizer";
    public static String HIBERNATE_TRANSACTION_FACTORY =
            "settings.wayback.hibernate.transaction_factory";
    public static String HIBERNATE_DIALECT =
            "settings.wayback.hibernate.dialect";
    public static String HIBERNATE_SHOW_SQL =
            "settings.wayback.hibernate.show_sql";
    public static String HIBERNATE_FORMAT_SQL =
            "settings.wayback.hibernate.format_sql";
    public static String HIBERNATE_HBM2DDL_AUTO =
            "settings.wayback.hibernate.hbm2ddl_auto";
    public static String HIBERNATE_USERNAME =
            "settings.wayback.hibernate.user";
    public static String HIBERNATE_PASSWORD =
            "settings.wayback.hibernate.password";
    public static String WAYBACK_REPLICA =
            "settings.wayback.indexer.replicaId";

}
