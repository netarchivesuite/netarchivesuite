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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;


/**
 * This class handles connections to the harvest definition database, and also
 * defines basic logic for checking versions of tables.
 *
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 *
 * The implementation relies on a connection pool. Once acquired through
 * the get() method, a connection must be explicitly returned to the pool
 * by calling the release(Connection) method.
 */
public class HarvestDBConnection {

    private static final Log log =
        LogFactory.getLog(HarvestDBConnection.class);

    /**
     * The c3p0 pooled datasource backing this implementation.
     */
    private static ComboPooledDataSource dataSource = null;

    /**
     * Get a connection to the harvest definition database from the pool.
     * The pool is configured via the following configuration properties:
     * <ul>
     * <li>@see {@link CommonSettings#DB_POOL_MIN_SIZE}</li>
     * <li>@see {@link CommonSettings#DB_POOL_MAX_SIZE}</li>
     * <li>@see {@link CommonSettings#DB_POOL_ACQ_INC}</li>
     * </ul>
     * Note that the connection obtained must be returned to the pool by calling
     * {@link #release(Connection)}.
     * @return a connection to the harvest definition database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    public static synchronized Connection get() {

        DBSpecifics dbSpec = DBSpecifics.getInstance();
        String jdbcUrl = getDBUrl();

        try {
            if (dataSource == null) {
                initDataSource(dbSpec, jdbcUrl);
            }

            return dataSource.getConnection();
        } catch (SQLException e) {
            final String message = "Can't connect to database with DBurl: '"
                + jdbcUrl + "' using driver '"
                + dbSpec.getDriverClassName() + "'" + "\n"
                + ExceptionUtils.getSQLExceptionCause(e);
            if (log.isWarnEnabled()) {
                log.warn(message, e);
            }
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

        if (log.isInfoEnabled()) {
            log.info("Updating table '" + table + "' to version " + newVersion);
        }

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

        Connection c = get();
        try {
            DBUtils.executeSQL(c, sqlStatements);
        } finally {
            release(c);
        }
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
        res.append(Settings.get(CommonSettings.DB_BASE_URL));

        // append the machine part of the url, if it exists.
        String tmp = Settings.get(CommonSettings.DB_MACHINE);
        if(!tmp.isEmpty()) {
            res.append("://");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(CommonSettings.DB_PORT);
        if(!tmp.isEmpty()) {
            res.append(":");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(CommonSettings.DB_DIR);
        if(!tmp.isEmpty()) {
            res.append("/");
            res.append(tmp);
        }

        return res.toString();
    }

    /**
     * Closes the underlying data source.
     */
    public static void cleanup() {

		if (dataSource == null) {
            return;
        }

        try {
            // Unclosed connections are not supposed to be found.
            // Anyway log if there are some.
            int numUnclosedConn = dataSource.getNumBusyConnections();
            if (numUnclosedConn > 0) {
                log.error("There are "
                        + numUnclosedConn + " unclosed connections!");
            }
        } catch (SQLException e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not query pool status", e);
            }
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /**
     * Helper method to return a connection to the pool.
     * @param connection a connection
     */
    public static synchronized void release(Connection connection) {
        ArgumentNotValid.checkNotNull(connection, "connection");
        try {
            connection.close();
        } catch (SQLException e) {
           log.error("Failed to close connection", e);
        }
    }

    /**
     * Initializes the connection pool.
     * @param dbSpec the object representing the chosen DB target system.
     * @param jdbcUrl the JDBC URL to connect to.
     * @throws SQLException
     */
    private static void initDataSource(DBSpecifics dbSpec, String jdbcUrl)
    throws SQLException {

        dataSource = new ComboPooledDataSource();
        try {
            dataSource.setDriverClass(dbSpec.getDriverClassName());
        } catch (PropertyVetoException e) {
            final String message =
                "Failed to set datasource JDBC driver class '"
                + dbSpec.getDriverClassName() + "'" + "\n";
            throw new IOFailure(message, e);
        }
        dataSource.setJdbcUrl(jdbcUrl);

        // Configure pool size
        dataSource.setMinPoolSize(
                Settings.getInt(CommonSettings.DB_POOL_MIN_SIZE));
        dataSource.setMaxPoolSize(
                Settings.getInt(CommonSettings.DB_POOL_MAX_SIZE));
        dataSource.setAcquireIncrement(
                Settings.getInt(CommonSettings.DB_POOL_ACQ_INC));

        // Configure statement pooling
        dataSource.setMaxStatements(
                Settings.getInt(CommonSettings.DB_POOL_MAX_STM));
        dataSource.setMaxStatementsPerConnection(
                Settings.getInt(CommonSettings.DB_POOL_MAX_STM_PER_CONN));

        if (log.isInfoEnabled()) {
            String msg =
                "Connection pool initialized with the following values:";
            msg += "\n- minPoolSize=" + dataSource.getMinPoolSize();
            msg += "\n- maxPoolSize=" + dataSource.getMaxPoolSize();
            msg += "\n- acquireIncrement=" + dataSource.getAcquireIncrement();
            msg += "\n- maxStatements=" + dataSource.getMaxStatements();
            msg += "\n- maxStatementsPerConnection="
                + dataSource.getMaxStatementsPerConnection();
            log.info(msg);
        }
    }

}
