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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * Logic to connect with the harvest definition database.
 * This also defines basic logic for checking versions of tables.
 *
 * The statements to create the tables are in
 * scripts/sql/createBitpreservationDB.sql
 */
public final class DBConnect {
    /** The pool of connections.*/
    private static Map<Thread, Connection> connectionPool
            = new WeakHashMap<Thread, Connection>();
    /** The log.*/
    private static Log log = LogFactory.getLog(DBConnect.class);

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private DBConnect() {}
    
    /**
     * Get a connection to our database. If a connection is already registered 
     * to the current thread, checks that it is valid, and if not renews it. 
     * This sets AutoCommit to false as part of getting a fresh connection.
     * @param dbUrl The url to the database.
     * @return a connection to our database.
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     * @throws ArgumentNotValid If the dbUrl is either null or the empty string.
     */
    public static Connection getDBConnection(String dbUrl) throws IOFailure, 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(dbUrl, "String dbUrl");
        
        try {
            int validityCheckTimeout = Settings.getInt(
                    CommonSettings.DB_CONN_VALID_CHECK_TIMEOUT);

            Connection connection = connectionPool.get(Thread.currentThread());
            boolean renew = ((connection == null) 
                    || (!connection.isValid(validityCheckTimeout)));

            if (renew) {  
                Class.forName(DBSpecifics.getInstance().getDriverClassName());
                connection = DriverManager.getConnection(dbUrl);
                connection.setAutoCommit(false);
                connectionPool.put(Thread.currentThread(), connection);
                log.info("Connected to database using DBurl '"
                        + dbUrl + "'  using driver '"
                        + DBSpecifics.getInstance().getDriverClassName() + "'");
            }

            return connection;
        } catch (ClassNotFoundException e) {
            final String message = "Can't find driver '"
                + DBSpecifics.getInstance().getDriverClassName() + "'";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } catch (SQLException e) {
            final String message = "Can't connect to database with DBurl: '"
                + dbUrl + "' using driver '"
                + DBSpecifics.getInstance().getDriverClassName() + "'" 
                + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }
    
    /**
     * Method for retrieving the url for the archive database.
     * This url will be constructed from the base-url, the machine, 
     * the port and the directory.
     * 
     * @return The url for the archive database.
     */
    public static String getArchiveUrl() {
        StringBuilder res = new StringBuilder();
        res.append(Settings.get(
                ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE));

        // append the machine part of the url, if it exists.
        String tmp = Settings.get(
                ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE);
        if(!tmp.isEmpty()) {
            res.append("://");
            res.append(tmp);
        }
        
        // append the machine part of the url, if it exists.
        tmp = Settings.get(
                ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE);
        if(!tmp.isEmpty()) {
            res.append(":");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(
                ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE);
        if(!tmp.isEmpty()) {
            res.append("/");
            res.append(tmp);
        }

        return res.toString();
    }
    
    /**
     * Clears the pool of connections.
     */
    public static void cleanup() {
        connectionPool.clear();
    }
}
