/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.ExceptionUtils;


/**
 * Logic to connect with the harvest definition database.
 * This also defines basic logic for checking versions of tables.
 *
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */

public class DBConnect {

    private static Map<Thread, Connection> connectionPool
            = new WeakHashMap<Thread, Connection>();
    private static final Log log = LogFactory.getLog(DBConnect.class);

    /**
     * Get a connection to our database.
     * Assumes that AutoCommit is true.
     * @return a connection to our database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    public static Connection getDBConnection() {
        if (connectionPool.get(Thread.currentThread()) == null) {
            try {
                Class.forName(DBSpecifics.getInstance().getDriverClassName());
                Connection connection = DriverManager.getConnection(
                        Settings.get(CommonSettings.DB_URL));
                connectionPool.put(Thread.currentThread(), connection);
                log.info("Connected to database using DBurl '"
                        + Settings.get(CommonSettings.DB_URL) + "'  using driver '"
                        + DBSpecifics.getInstance().getDriverClassName() + "'");
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
        return connectionPool.get(Thread.currentThread());
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
        final String updateSchemaversionSql = "UPDATE schemaversions SET version = "
            + newVersion + " WHERE tablename = '" + table + "'"; 
        for (int i = 0; i < updates.length; i++) {
            sqlStatements[i] = updates[i];
        }
        sqlStatements[updates.length] = updateSchemaversionSql;

        DBUtils.executeSQL(sqlStatements);
    }
}
