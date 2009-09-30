/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DBSpecifics;


/**
 * Logic to connect with the harvest definition database.
 * This also defines basic logic for checking versions of tables.
 *
 * The statements to create the tables are in
 * scripts/sql/createBitpreservationDB.sql
 */

public class DBConnect {

    private static Map<Thread, Connection> connectionPool
            = new WeakHashMap<Thread, Connection>();
    private static final Log log = LogFactory.getLog(DBConnect.class);

    /**
     * Get a connection to our database. If a connection is already registered 
     * to the current thread, checks that it is valid, and if not renews it. 
     * Assumes that AutoCommit is true.
     * @return a connection to our database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    public static Connection getDBConnection() {
        String dbUrl = Settings.get(
                ArchiveSettings.URL_ARCREPOSITORY_BITPRESERVATION_DATABASE);
        try {    		
            int validityCheckTimeout = Settings.getInt(
                    CommonSettings.DB_CONN_VALID_CHECK_TIMEOUT);

            Connection connection = connectionPool.get(Thread.currentThread());
            boolean renew = ((connection == null) 
                    || (! connection.isValid(validityCheckTimeout)));

            if (renew) {  
                Class.forName(DBSpecifics.getInstance().getDriverClassName());
                connection = DriverManager.getConnection(dbUrl);
                connectionPool.put(Thread.currentThread(), connection);
                log.info("Connected to database using DBurl '"
                        + dbUrl 
                        + "'  using driver '"
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
                + Settings.get(CommonSettings.DB_URL) + "' using driver '"
                + DBSpecifics.getInstance().getDriverClassName() + "'" +
                "\n" +
                ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /** Update a table by executing all the statements in
     *  the updates String array.
     *
     * @param table The table to update
     * @param newVersion The version that the table should end up at
     * @param updates The SQL update statements that makes the necessary
     * updates.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected static void updateTable(final String table,
            final int newVersion,
            final String... updates) {
        
        log.info("Updating table to version " + newVersion);

        String[] sqlStatements = new String[updates.length + 1];
        final String updateSchemaversionSql = 
            "UPDATE schemaversions SET version = "
            + newVersion + " WHERE tablename = '" + table + "'";
        System.arraycopy(updates, 0, sqlStatements, 0, updates.length);
        sqlStatements[updates.length] = updateSchemaversionSql;

        DBUtils.executeSQL(getDBConnection(), sqlStatements);
    }
}
