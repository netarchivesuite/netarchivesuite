/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.datamodel;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.sql.ExecuteSqlFile;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * This class handles connections to the harvest definition database, and also defines basic logic for checking versions
 * of tables.
 * <p>
 * The statements to create the tables are located in:
 * <ul>
 * <li><em>Derby:</em> scripts/sql/createfullhddb.sql</li>
 * <li><em>MySQL:</em> scripts/sql/createfullhddb.mysql</li>
 * <li><em>PostgreSQL:</em> scripts/postgresql/netarchivesuite_init.sql</li>
 * </ul>
 * <p>
 * The implementation relies on a connection pool. Once acquired through the get() method, a connection must be
 * explicitly returned to the pool by calling the release(Connection) method.
 * <p>
 * THis class is intended to be used statically, and hence cannot be instantiated and is final.
 */
public final class HarvestDBConnection {

    private static final Logger log = LoggerFactory.getLogger(HarvestDBConnection.class);

    /** The c3p0 pooled datasource backing this implementation. */
    private static ComboPooledDataSource dataSource = null;

    /**
     * Makes sure that the class can't be instantiated, as it is designed to be used statically.
     */
    private HarvestDBConnection() {

    }

    /**
     * Get a connection to the harvest definition database from the pool. The pool is configured via the following
     * configuration properties:
     * <ul>
     * <li>@see {@link CommonSettings#DB_POOL_MIN_SIZE}</li>
     * <li>@see {@link CommonSettings#DB_POOL_MAX_SIZE}</li>
     * <li>@see {@link CommonSettings#DB_POOL_ACQ_INC}</li>
     * </ul>
     * Note that the connection obtained must be returned to the pool by calling {@link #release(Connection)}.
     *
     * @return a connection to the harvest definition database
     * @throws IOFailure if we cannot connect to the database (or find the driver).
     */
    public static synchronized Connection get() {
        DBSpecifics dbSpec = DBSpecifics.getInstance();
        String jdbcUrl = getDBUrl();

        try {
            if (dataSource == null) {
                initDataSource(dbSpec, jdbcUrl);
                // this is only done for embedded database!
                // For external databases, use the HarvestdatabaseUpdateApplication tool
                if (dbSpec instanceof DerbyEmbeddedSpecifics) {
                    dbSpec.updateTables();
                }
            }

            return dataSource.getConnection();
        } catch (SQLException e) {
            final String message = "Can't connect to database with DBurl: '" + jdbcUrl + "' using driver '"
                    + dbSpec.getDriverClassName() + "'" + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }

    }

    /**
     * Update a table by executing all the statements in the updates String array. If newVersion=1 then the table is
     * created. Note that this method does not make any checks that the SQL statements in the updates parameter actually
     * update or create the correct table.
     *
     * @param table The table to update
     * @param newVersion The version that the table should end up at
     * @param updates The SQL update statements that makes the necessary updates.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected static void updateTable(final String table, final int newVersion, final String... updates) {
        Connection c = get();
        updateTable(c, table, newVersion, updates);
    }

    public static void updateTable(Connection c, final String table, final int newVersion, final String... updates) {
        log.info("Updating table '{}' to version {}", table, newVersion);

        String[] sqlStatements = new String[updates.length + 1];
        String updateSchemaversionSql = null;
        if (newVersion == 1) {
            updateSchemaversionSql = "INSERT INTO schemaversions(tablename, version) VALUES ('" + table + "', 1)";
        } else {
            updateSchemaversionSql = "UPDATE schemaversions SET version = " + newVersion + " WHERE tablename = '"
                    + table + "'";
        }
        System.arraycopy(updates, 0, sqlStatements, 0, updates.length);
        sqlStatements[updates.length] = updateSchemaversionSql;

        try {
            DBUtils.executeSQL(c, sqlStatements);
        } finally {
            release(c);
        }
    }

    protected static void updateTableVersion(final String table, final int newVersion, final String... updates) {
        Connection c = get();
        updateTableVersion(c, table, newVersion);
    }

    public static void updateTableVersion(Connection c, final String table, final int newVersion) {
        log.info("Updating table '{}' to version {}", table, newVersion);
        String updateSchemaversionSql = null;
        if (newVersion == 1) {
            updateSchemaversionSql = "INSERT INTO schemaversions(tablename, version) VALUES ('" + table + "', 1)";
        } else {
            updateSchemaversionSql = "UPDATE schemaversions SET version = " + newVersion + " WHERE tablename = '"
                    + table + "'";
        }
        try {
            DBUtils.executeSQL(c, updateSchemaversionSql);
        } finally {
            release(c);
        }
    }

    /**
     * Method for retrieving the url for the harvest definition database. This url will be constructed from the
     * base-url, the machine, the port and the directory. If the database is internal, then only the base-url should
     * have a value.
     *
     * @return The url for the harvest definition database.
     */
    public static String getDBUrl() {
        StringBuilder res = new StringBuilder();
        res.append(Settings.get(CommonSettings.DB_BASE_URL));

        // append the machine part of the url, if it exists.
        String tmp = Settings.get(CommonSettings.DB_MACHINE);
        if (!tmp.isEmpty()) {
            res.append("://");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(CommonSettings.DB_PORT);
        if (!tmp.isEmpty()) {
            res.append(":");
            res.append(tmp);
        }

        // append the machine part of the url, if it exists.
        tmp = Settings.get(CommonSettings.DB_DIR);
        if (!tmp.isEmpty()) {
            res.append("/");
            res.append(tmp);
        }

        return res.toString();
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
                log.error("There are {} unclosed connections!", numUnclosedConn);
            }
        } catch (SQLException e) {
            log.warn("Could not query pool status", e);
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * Helper method to return a connection to the pool.
     *
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
     *
     * @param dbSpec the object representing the chosen DB target system.
     * @param jdbcUrl the JDBC URL to connect to.
     * @throws SQLException
     */
    private static void initDataSource(DBSpecifics dbSpec, String jdbcUrl) throws SQLException {
        dataSource = new ComboPooledDataSource();
        try {
            dataSource.setDriverClass(dbSpec.getDriverClassName());
        } catch (PropertyVetoException e) {
            final String message = "Failed to set datasource JDBC driver class '" + dbSpec.getDriverClassName() + "'"
                    + "\n";
            throw new IOFailure(message, e);
        }
        dataSource.setJdbcUrl(jdbcUrl);
        String username = Settings.get(CommonSettings.DB_USERNAME);
        if (!username.isEmpty()) {
            dataSource.setUser(username);
        }
        String password = Settings.get(CommonSettings.DB_PASSWORD);
        if (!password.isEmpty()) {
            dataSource.setPassword(password);
        }
        // Configure pool size
        dataSource.setMinPoolSize(Settings.getInt(CommonSettings.DB_POOL_MIN_SIZE));
        dataSource.setMaxPoolSize(Settings.getInt(CommonSettings.DB_POOL_MAX_SIZE));
        dataSource.setAcquireIncrement(Settings.getInt(CommonSettings.DB_POOL_ACQ_INC));
        dataSource.setMaxConnectionAge(Settings.getInt(CommonSettings.DB_POOL_MAX_CONNECTION_AGE));

        // Configure idle connection testing
        int testPeriod = Settings.getInt(CommonSettings.DB_POOL_IDLE_CONN_TEST_PERIOD);
        // TODO This looks odd. Why is checkin-testing inside this if statement?
        if (testPeriod > 0) {
            dataSource.setIdleConnectionTestPeriod(testPeriod);
            dataSource
                    .setTestConnectionOnCheckin(Settings.getBoolean(CommonSettings.DB_POOL_IDLE_CONN_TEST_ON_CHECKIN));
            String testQuery = Settings.get(CommonSettings.DB_POOL_IDLE_CONN_TEST_QUERY);
            if (!testQuery.isEmpty()) {
                dataSource.setPreferredTestQuery(testQuery);
            }
        }

        // Configure statement pooling
        dataSource.setMaxStatements(Settings.getInt(CommonSettings.DB_POOL_MAX_STM));
        dataSource.setMaxStatementsPerConnection(Settings.getInt(CommonSettings.DB_POOL_MAX_STM_PER_CONN));

        // dataSource.setTestConnectionOnCheckout(true);
        // dataSource.setBreakAfterAcquireFailure(false);
        // dataSource.setAcquireRetryAttempts(10000);
        // dataSource.setAcquireRetryDelay(10);

        if (log.isInfoEnabled()) {
            log.info("Connection pool initialized with the following values:\n" + "- minPoolSize={}\n"
                    + "- maxPoolSize={}\n" + "- acquireIncrement={}\n" + "- maxStatements={}\n"
                    + "- maxStatementsPerConnection={}\n" + "- idleConnTestPeriod={}\n" + "- idleConnTestQuery='{}'\n"
                    + "- idleConnTestOnCheckin={}", dataSource.getMinPoolSize(), dataSource.getMaxPoolSize(),
                    dataSource.getAcquireIncrement(), dataSource.getMaxStatements(),
                    dataSource.getMaxStatementsPerConnection(), dataSource.getIdleConnectionTestPeriod(),
                    dataSource.getPreferredTestQuery(), dataSource.isTestConnectionOnCheckin());
        }
    }

    /**
     * Execute the sql to update a given table to a given version.  
     * The necessary scripts are bundled into the root of the harvester-core.jar in the directory sql-migration.
     * The source of these scripts are in one of these directories:
     * 
     *  $BASEDIR/deploy/deploy-core/scripts/postgresql/migration/
     *  $BASEDIR/deploy/deploy-core/scripts/mysql/migration/
     *  $BASEDIR/deploy/deploy-core/scripts/derby/migration/
     *   
     *  To allow the user to update table 'eav_attribute' in postgresql to version 1
     *  the file $BASEDIR/deploy/deploy-core/scripts/postgresql/migration/eav_attribute.1.sql must exist
     *  The postgresql files are during the build-phase put into the sql-migration/postgresql directory
     *  The same holds for mysql and derby.
     *  
     * @param dbm the type of DBMS (mysql, postgresql,derby)  
     * @param tableName The given table to update
     * @param version The table version to update to
     */
    public static void executeSql(String dbm, String tableName, int version) {
    	Connection conn = HarvestDBConnection.get();
        executeSql(conn, dbm, tableName, version);
        HarvestDBConnection.release(conn);
        conn = null;
    }

    /**
     * Look for the file sql-migration/${dbm}/${tableName}.${version}.sql inside in the harvester-core.jar file.
     * 
     * @param conn a valid database connection
     * @param dbm the name of the DBMS used
     * @param tableName the name of the table being updated
     * @param version The new version of the table
     */
    public static void executeSql(Connection conn, String dbm, String tableName, int version) {
        String resource = "sql-migration/" + dbm + "/" + tableName + "." + version + ".sql";
        log.info("Fetching resource {} to update table '{}' to version {} using databasetype {}", resource, tableName, version, dbm);
    	InputStream in = HarvestDBConnection.class.getClassLoader().getResourceAsStream(resource);
    	try {
        	List<Map.Entry<String, String>> statements = ExecuteSqlFile.splitSql(in, "UTF-8", 8192);
        	in.close();
        	in = null;
            ExecuteSqlFile.executeStatements(conn, statements);
            // Update the schemaversions with the new version for this table
            HarvestDBConnection.updateTable(conn, tableName, version);
    	} catch (IOException e) {
    		throw new IOFailure("Unable to update the table '" + tableName 
    		        + "' to version '" + version + "' using database '" + dbm + "'", e);
    	} catch (SQLException e) {
    	    throw new IOFailure("Unable to update the table '" + tableName 
            + "' to version '" + version + "' using database '" + dbm + "':\n" + ExceptionUtils.getSQLExceptionCause(e), e);
    	} finally {
    	    IOUtils.closeQuietly(in);
    	}
    }

}
