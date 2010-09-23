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
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;


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
     * Get a connection to our database. If a connection is already registered 
     * to the current thread, checks that it is valid, and if not renews it. 
     * Assumes that AutoCommit is true.
     * @return a connection to our database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    public static Connection getDBConnection() {

        DBSpecifics dbSpec = DBSpecifics.getInstance();
        String url = getDBUrl();
        
        try {
            int validityCheckTimeout = Settings
                    .getInt(CommonSettings.DB_CONN_VALID_CHECK_TIMEOUT);

            Connection connection = connectionPool.get(Thread.currentThread());
            boolean renew = ((connection == null) || (!dbSpec
                    .connectionIsValid(connection, validityCheckTimeout)));

            if (renew) {
                Class.forName(dbSpec.getDriverClassName());
                connection = DriverManager.getConnection(url);
                connectionPool.put(Thread.currentThread(), connection);
                log.info("Connected to database using DBurl '" + url
                        + "'  using driver '" + dbSpec.getDriverClassName()
                        + "'");
            }

            return connection;
        } catch (ClassNotFoundException e) {
            final String message = "Can't find driver '"
                    + dbSpec.getDriverClassName() + "'";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } catch (SQLException e) {
            final String message = "Can't connect to database with DBurl: '"
                    + url + "' using driver '"
                    + dbSpec.getDriverClassName() + "'" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }

    }

    /** Update a table by executing all the statements in
     *  the updates String array. If newVersion=1 then the
     *  table is created. Note that this method does not make
     *  any checks that the SQL statements in the updates
     *  parameter actually update or create the correct table.
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
        String updateSchemaversionSql = null;
        if (newVersion == 1) {
           updateSchemaversionSql = "INSERT INTO schemaversions(tablename, "
                                    + "version) VALUES ('" + table + "', 1)";
        } else {
            updateSchemaversionSql =
                    "UPDATE schemaversions SET version = "
                    + newVersion + " WHERE tablename = '" + table + "'";
        }
        System.arraycopy(updates, 0, sqlStatements, 0, updates.length);
        sqlStatements[updates.length] = updateSchemaversionSql;

        DBUtils.executeSQL(getDBConnection(), sqlStatements);
    }

    /**
     * Method for retrieving the url for the harvest definition database.
     * This url will be constructed from the base-url, the machine, 
     * the port and the directory. If the database is internal, then only the
     * base-url should have a value.
     * 
     * @return The url for the harvest definition database.
     */
    public static String getDBUrl() {
        StringBuilder res = new StringBuilder();
        res.append(Settings.get(
                CommonSettings.DB_BASE_URL));

        // append the machine part of the url, if it exists.
        String tmp = Settings.get(
                CommonSettings.DB_MACHINE);
        if(!tmp.isEmpty()) {
            res.append("://");
            res.append(tmp);
        }
        
        // append the machine part of the url, if it exists.
        tmp = Settings.get(
                CommonSettings.DB_PORT);
        if(!tmp.isEmpty()) {
            res.append(":");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(
                CommonSettings.DB_DIR);
        if(!tmp.isEmpty()) {
            res.append("/");
            res.append(tmp);
        }

        return res.toString();
    }
}
