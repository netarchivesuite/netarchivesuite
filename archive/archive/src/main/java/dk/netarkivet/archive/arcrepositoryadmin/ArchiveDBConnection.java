/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

package dk.netarkivet.archive.arcrepositoryadmin;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;

/**
 * This class handles connections to the Archive database
 * 
 * The statements to create the tables are in
 * scripts/sql/createBitpreservationDB.sql
 *
 * The implementation relies on a connection pool. Once acquired through
 * the get() method, a connection must be explicitly returned to the pool
 * by calling the release(Connection) method.
 *
 * THis class is intended to be used statically, and hence cannot be
 * instantiated and is final.
 */
public final class ArchiveDBConnection {
    /** The class logger. */
    private static final Log log =
        LogFactory.getLog(ArchiveDBConnection.class);

    /** max number of database retries. */
    private static final int maxdatabaseRetries = Settings.getInt(
            ArchiveSettings.RECONNECT_MAX_TRIES_ADMIN_DATABASE);
    /** max time to wait between retries. */
    private static final int delaybetweenretries = Settings.getInt(
            ArchiveSettings.RECONNECT_DELAY_ADMIN_DATABASE);
    /**
     * The c3p0 pooled datasource backing this implementation.
     */
    private static ComboPooledDataSource dataSource = null;

    
    /**
     * Makes sure that the class can't be instantiated, as it is designed to be
     * used statically.
     */
    private ArchiveDBConnection() {
    }

    /**
     * Get a connection to the harvest definition database from the pool.
     * The pool is configured via the following configuration properties:
     * <ul>
     * <li>@see {@link ArchiveSettings#DB_POOL_MIN_SIZE}</li>
     * <li>@see {@link ArchiveSettings#DB_POOL_MAX_SIZE}</li>
     * <li>@see {@link ArchiveSettings#DB_POOL_ACQ_INC}</li>
     * </ul>
     * Note that the connection obtained must be returned to the pool by calling
     * {@link #release(Connection)}.
     * @return a connection to the harvest definition database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    public static synchronized Connection get() {
        DBSpecifics dbSpec = DBSpecifics.getInstance();
        String jdbcUrl = getArchiveUrl();
        int tries = 0;
        Connection con = null;
        while (tries < maxdatabaseRetries && con == null) {
            tries++;
            try {
                if (dataSource == null) {
                    initDataSource(dbSpec, jdbcUrl);
                }
                con = dataSource.getConnection();
                con.setAutoCommit(false); // different from in
                                          // HarvestDBConnection
            } catch (SQLException e) {
                final String message = "Can't connect to database with DBurl: '"
                        + jdbcUrl
                        + "' using driver '"
                        + dbSpec.getDriverClassName()
                        + "'"
                        + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e);

                if (log.isWarnEnabled()) {
                    log.warn(message, e);
                }
                if (tries < maxdatabaseRetries) {
                    log.info("Will wait " + delaybetweenretries
                            / TimeUtils.SECOND_IN_MILLIS + " before retrying");
                    try {
                        Thread.sleep(delaybetweenretries);
                    } catch (InterruptedException e1) {
                        // ignore this exception
                        log.trace("Interruption ignored.", e1);
                    }
                } else {
                    throw new IOFailure(message, e);
                }
            }
        }
        return con;
    }
        
    /**
     * Closes the underlying data source.
     */
    public static synchronized void cleanup() {

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
            dataSource = null;
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
        
        // append the port part of the url, if it exists.
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
     * Initializes the connection pool.
     * @param dbSpec the object representing the chosen DB target system.
     * @param jdbcUrl the JDBC URL to connect to.
     * @throws SQLException 
     */
    private static void initDataSource(DBSpecifics dbSpec, String jdbcUrl)
    throws SQLException {

        dataSource = new ComboPooledDataSource();
        dataSource.setUser(Settings.get(ArchiveSettings.DB_USERNAME));
        dataSource.setPassword(Settings.get(ArchiveSettings.DB_PASSWORD));
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
                Settings.getInt(ArchiveSettings.DB_POOL_MIN_SIZE));
        dataSource.setMaxPoolSize(
                Settings.getInt(ArchiveSettings.DB_POOL_MAX_SIZE));
        dataSource.setAcquireIncrement(
                Settings.getInt(ArchiveSettings.DB_POOL_ACQ_INC));

        // Configure idle connection testing
        int testPeriod =
            Settings.getInt(ArchiveSettings.DB_POOL_IDLE_CONN_TEST_PERIOD);
        if (testPeriod > 0) {
            dataSource.setIdleConnectionTestPeriod(testPeriod);
            dataSource.setTestConnectionOnCheckin(
                    Settings.getBoolean(
                            ArchiveSettings.DB_POOL_IDLE_CONN_TEST_ON_CHECKIN));
            String testQuery =
                Settings.get(ArchiveSettings.DB_POOL_IDLE_CONN_TEST_QUERY);
            if (!testQuery.isEmpty()) {
                dataSource.setPreferredTestQuery(testQuery);
            }
        }

        // Configure statement pooling
        dataSource.setMaxStatements(
                Settings.getInt(ArchiveSettings.DB_POOL_MAX_STM));
        dataSource.setMaxStatementsPerConnection(
                Settings.getInt(ArchiveSettings.DB_POOL_MAX_STM_PER_CONN));

        if (log.isInfoEnabled()) {
            String msg = 
                    "Connection pool initialized with the following values:";
            msg += "\n- minPoolSize=" + dataSource.getMinPoolSize();
            msg += "\n- maxPoolSize=" + dataSource.getMaxPoolSize();
            msg += "\n- acquireIncrement=" + dataSource.getAcquireIncrement();
            msg += "\n- maxStatements=" + dataSource.getMaxStatements();
            msg += "\n- maxStatementsPerConnection="
                + dataSource.getMaxStatementsPerConnection();
            msg += "\n- idleConnTestPeriod="
                + dataSource.getIdleConnectionTestPeriod();
            msg += "\n- idleConnTestQuery='"
                + dataSource.getPreferredTestQuery() + "'";
            msg += "\n- idleConnTestOnCheckin="
                + dataSource.isTestConnectionOnCheckin();
            log.info(msg.toString());
        }
    }
}
