/* File:        $Id: HarvestDBConnection.java 2637 2013-04-10 06:25:19Z csr $
 * Revision:    $Revision: 2637 $
 * Author:      $Author: csr $
 * Date:        $Date: 2013-04-10 08:25:19 +0200 (Wed, 10 Apr 2013) $
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

package dk.netarkivet.harvester.dao;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.harvester.dao.spec.DerbyEmbeddedSpecifics;


/**
 * This class handles connections to the harvest database, and also
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
 *
 * THis class is intended to be used statically, and hence cannot be
 * instantiated and is final.
 */
public final class HarvestDatabase {

	private static final Log log = LogFactory.getLog(HarvestDatabase.class);

	/**
	 * Singleton instance
	 */
	private static HarvestDatabase instance;

	/**
	 * The c3p0 pooled datasource backing this implementation.
	 */
	private ComboPooledDataSource dataSource = null;

	private DataSourceTransactionManager transactionManager;

	private List<TransactionStatus> transactions = new ArrayList<TransactionStatus>();

	/**
	 * Makes sure that the class can't be instantiated, as it is designed to be
	 * used statically.
	 */
	private HarvestDatabase() {

		String jdbcUrl = getDBUrl();
		DBSpecifics dbSpec = DBSpecifics.getInstance();

		// Initialize the data source
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
		String username = Settings.get(CommonSettings.DB_USERNAME);
		if (!username.isEmpty()) {
			dataSource.setUser(username);
		}
		String password = Settings.get(CommonSettings.DB_PASSWORD);
		if (!password.isEmpty()) {
			dataSource.setPassword(password);
		}
		// Configure pool size
		dataSource.setMinPoolSize(
				Settings.getInt(CommonSettings.DB_POOL_MIN_SIZE));
		dataSource.setMaxPoolSize(
				Settings.getInt(CommonSettings.DB_POOL_MAX_SIZE));
		dataSource.setAcquireIncrement(
				Settings.getInt(CommonSettings.DB_POOL_ACQ_INC));

		// Configure idle connection testing
		int testPeriod =
				Settings.getInt(CommonSettings.DB_POOL_IDLE_CONN_TEST_PERIOD);
		//TODO This looks odd. Why is checkin-testing inside this if statement?
		if (testPeriod > 0) {
			dataSource.setIdleConnectionTestPeriod(testPeriod);
			dataSource.setTestConnectionOnCheckin(
					Settings.getBoolean(
							CommonSettings.DB_POOL_IDLE_CONN_TEST_ON_CHECKIN));
			String testQuery =
					Settings.get(CommonSettings.DB_POOL_IDLE_CONN_TEST_QUERY);
			if (!testQuery.isEmpty()) {
				dataSource.setPreferredTestQuery(testQuery);
			}
		}

		// Configure statement pooling
		dataSource.setMaxStatements(
				Settings.getInt(CommonSettings.DB_POOL_MAX_STM));
		dataSource.setMaxStatementsPerConnection(
				Settings.getInt(CommonSettings.DB_POOL_MAX_STM_PER_CONN));

		//dataSource.setTestConnectionOnCheckout(true);
		//dataSource.setBreakAfterAcquireFailure(false);
		//dataSource.setAcquireRetryAttempts(10000);
		//dataSource.setAcquireRetryDelay(10);

		// Initialize transaction manager
		transactionManager = new DataSourceTransactionManager(dataSource);

		// this is only done for embedded database!
		// For external databases, use the HarvestdatabaseUpdateApplication tool
		if (dbSpec instanceof DerbyEmbeddedSpecifics) {
			
		}

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

	public final void finalize() {

		if (dataSource == null) {
			log.warn("Datasource has not been initialized.");
			return;
		}

		int incompleteCount = 0;
		for (TransactionStatus ts : transactions) {			
			if (!ts.isCompleted()) {
				incompleteCount++;
				transactionManager.rollback(ts);				
			}
		}
		if (incompleteCount > 0) {
			throw new IllegalState("[SEVERE] Resource leak! "
					+ incompleteCount + " incomplete transactions were found and rolled back.");
		}

		// Close the data source
		dataSource.close();
		log.info("Closed the harvest data source.");
	}
	
	public static synchronized HarvestDatabase getInstance() {
		if (instance == null) {
			instance = new HarvestDatabase();
		}
		return instance;
	}
	
	public synchronized TransactionStatus beginTransaction() {
		TransactionDefinition def = new DefaultTransactionDefinition();
	    TransactionStatus ts = transactionManager.getTransaction(def);
	    this.transactions.add(ts);
	    return ts;
	}
	
	public synchronized void commit(TransactionStatus ts) {
		transactionManager.commit(ts);
		this.transactions.remove(ts);
	}
	
	public synchronized void rollback(TransactionStatus ts) {
		transactionManager.rollback(ts);
		this.transactions.remove(ts);
	}

	public synchronized NamedParameterJdbcTemplate newTemplate() {
		return new NamedParameterJdbcTemplate(dataSource);
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
	public synchronized void updateTable(final String table,
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

		TransactionStatus ts = beginTransaction();
		try {
			JdbcTemplate t = new JdbcTemplate(dataSource);
			t.batchUpdate(sqlStatements);
			commit(ts);
		} catch (DataAccessException e) {
			log.error("Failed to update tables!", e);
			rollback(ts);			
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
	private String getDBUrl() {
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

}
