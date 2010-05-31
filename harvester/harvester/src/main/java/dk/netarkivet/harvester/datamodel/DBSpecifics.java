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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Abstract collection of DB methods that are not standard SQL. This class is a
 * singleton class whose actual implementation is provided by a subclass as
 * determined by the DB_SPECIFICS_CLASS setting.
 * 
 */
public abstract class DBSpecifics extends SettingsFactory<DBSpecifics> {

	/** The instance of the DBSpecifics class. */
	private static DBSpecifics instance;

	Log log = LogFactory.getLog(DBSpecifics.class);

	/**
	 * Get the singleton instance of the DBSpecifics implementation class.
	 * 
	 * @return An instance of DBSpecifics with implementations for a given DB.
	 */
	public static synchronized DBSpecifics getInstance() {
		if (instance == null) {
			instance = getInstance(CommonSettings.DB_SPECIFICS_CLASS);
		}
		return instance;
	}

	/**
	 * Shutdown the database system, if running embeddedly. Otherwise, this is
	 * ignored.
	 * 
	 * Will log a warning on errors, but otherwise ignore them.
	 */
	public abstract void shutdownDatabase();

	/**
	 * Get a temporary table for short-time use. The table should be disposed of
	 * with dropTemporaryTable. The table has two columns domain_name
	 * varchar(Constants.MAX_NAME_SIZE) + config_name
	 * varchar(Constants.MAX_NAME_SIZE) All rows in the table must be deleted at
	 * commit or rollback.
	 * 
	 * @param c
	 *            The DB connection to use.
	 * @throws SQLException
	 *             if there is a problem getting the table.
	 * @return The name of the created table
	 */
	public abstract String getJobConfigsTmpTable(Connection c)
			throws SQLException;

	/**
	 * Dispose of a temporary table gotten with getTemporaryTable. This can be
	 * expected to be called from within a finally clause, so it mustn't throw
	 * exceptions.
	 * 
	 * @param c
	 *            The DB connection to use.
	 * @param tableName
	 *            The name of the temporarily created table.
	 */
	public abstract void dropJobConfigsTmpTable(Connection c, String tableName);

	/**
	 * Backup the database. For server-based databases, where the administrator
	 * is expected to perform the backups, this method should do nothing. This
	 * method gets called within one hour of the hour-of-day indicated by the
	 * DB_BACKUP_INIT_HOUR settings.
	 * 
	 * @param backupDir
	 *            Directory to which the database should be backed up
	 * @throws SQLException
	 *             On SQL trouble backing up database
	 * @throws PermissionDenied
	 *             if the directory cannot be created.
	 */
	public abstract void backupDatabase(File backupDir) throws SQLException;

	/**
	 * Get the name of the JDBC driver class that handles interfacing to this
	 * server.
	 * 
	 * @return The name of a JDBC driver class
	 */
	public abstract String getDriverClassName();

	/**
	 * Update a table to a newer version, if necessary. This will check the
	 * schemaversions table to see the current version and perform a
	 * table-specific update if required.
	 * 
	 * @param tableName
	 *            The table to update
	 * @param toVersion
	 *            The version to update the table to.
	 * @throws IllegalState
	 *             If the table is an unsupported version, and the toVersion is
	 *             less than the current version of the table
	 * @throws NotImplementedException
	 *             If no method exists for migration from current version of the
	 *             table to the toVersion of the table.
	 * @throws IOFailure
	 *             in case of problems in interacting with the database
	 */

	public synchronized void updateTable(String tableName, int toVersion) {
		ArgumentNotValid.checkNotNullOrEmpty(tableName, "String tableName");
		ArgumentNotValid.checkPositive(toVersion, "int toVersion");

		int currentVersion = DBUtils.getTableVersion(DBConnect
				.getDBConnection(), tableName);
		log.info("Trying to migrate table '" + tableName + "' from version '"
				+ currentVersion + "' to version '" + toVersion + "'.");
		if (currentVersion == toVersion) {
			// Nothing to do. Version of table is already correct.
			return;
		}

		if (currentVersion > toVersion) {
			throw new IllegalState("Database is in an illegalState: "
					+ "The current version of table '" + tableName
					+ "' is not acceptable "
					+ "(current version is greater than requested version).");
		}

		if (tableName.equals("jobs")) {
			if (currentVersion < 3) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version " + currentVersion
						+ " of table '" + tableName + "' is not acceptable. "
						+ "(current version is less than open source version).");
			}
			if (currentVersion == 3 && toVersion >= 4) {
				migrateJobsv3tov4();
				currentVersion = 4;
			}
			if (currentVersion == 4 && toVersion >= 5) {
				migrateJobsv4tov5();
			}

			if (currentVersion == 5 && toVersion >= 6) {
				throw new NotImplementedException(
						"No method exists for migrating table '" + tableName
								+ "' from version " + currentVersion
								+ " to version " + toVersion);
			}
			// future updates of the job table are inserted here
			if (currentVersion > 5) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version (" + currentVersion
						+ ") of table '" + tableName
						+ "' is not an acceptable/known version. ");
			}

		} else if (tableName.equals("fullharvests")) {
			if (currentVersion < 2) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version " + currentVersion
						+ " of table '" + tableName + "' is not acceptable. "
						+ "(current version is less than open source version).");
			}
			if (currentVersion == 2 && toVersion >= 3) {
				migrateFullharvestsv2tov3();
				currentVersion = 3;
			}

			if (currentVersion == 3 && toVersion >= 4) {
				throw new NotImplementedException(
						"No method exists for migrating table '" + tableName
								+ "' from version " + currentVersion
								+ " to version " + toVersion);
			}

			// future updates of the job table are inserted here

			if (currentVersion > 4) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version (" + currentVersion
						+ ") of table '" + tableName
						+ "' is not an acceptable/known version. ");
			}

		} else if (tableName.equals("configurations")) {
			if (currentVersion < 3) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version " + currentVersion
						+ " of table '" + tableName + "' is not acceptable. "
						+ "(current version is less than open source version).");
			}
			if (currentVersion == 3 && toVersion >= 4) {
				migrateConfigurationsv3ov4();
				currentVersion = 4;
			}

			if (currentVersion == 4 && toVersion >= 5) {
				throw new NotImplementedException(
						"No method exists for migrating table '" + tableName
								+ "' from version " + currentVersion
								+ " to version " + toVersion);
			}

			// future updates of the job table are inserted here

			if (currentVersion > 5) {
				throw new IllegalState("Database is in an illegalState: "
						+ "The current version (" + currentVersion
						+ ") of table '" + tableName
						+ "' is not an acceptable/known version. ");
			}

		} else if (tableName.equals("global_crawler_trap_lists")) {
			if (currentVersion == 0 && toVersion == 1) {
				createGlobalCrawlerTrapLists();
				currentVersion = 1;
			}
			if (currentVersion > 1) {
				throw new NotImplementedException(
						"No method exists for migrating table '" + tableName
								+ "' from version " + currentVersion
								+ " to version " + toVersion);
			}
		} else if (tableName.equals("global_crawler_trap_expressions")) {
			if (currentVersion == 0 && toVersion == 1) {
				createGlobalCrawlerTrapExpressions();
				currentVersion = 1;
			}
			if (currentVersion > 1) {
				throw new NotImplementedException(
						"No method exists for migrating table '" + tableName
								+ "' from version " + currentVersion
								+ " to version " + toVersion);
			}
		} else {
			// This includes cases where currentVersion < toVersion
			// for all tables that does not have migration functions yet
			throw new NotImplementedException(
					"No method exists for migrating table '" + tableName
							+ "' to version " + toVersion);
		}
	}

	/**
	 * Migrates the 'jobs' table from version 3 to version 4 consisting of a
	 * change of the field forcemaxbytes from int to bigint and setting its
	 * default to -1. Furthermore the default value for field num_configs is set
	 * to 0.
	 * 
	 * @throws IOFailure
	 *             in case of problems in interacting with the database
	 */
	protected abstract void migrateJobsv3tov4();

	/**
	 * Migrates the 'jobs' table from version 4 to version 5 consisting of
	 * adding new fields 'resubmitted_as_job' and 'submittedDate'.
	 * 
	 * @throws IOFailure
	 *             in case of problems in interacting with the database
	 */
	protected abstract void migrateJobsv4tov5();

	/**
	 * Migrates the 'configurations' table from version 3 to version 4. This
	 * consists of altering the default value of field 'maxbytes' to -1.
	 */
	protected abstract void migrateConfigurationsv3ov4();

	/**
	 * Migrates the 'fullharvests' table from version 2 to version 3. This
	 * consists of altering the default value of field 'maxbytes' to -1.
	 */
	protected abstract void migrateFullharvestsv2tov3();

	/**
	 * Creates the initial (version 1) of table 'global_crawler_trap_lists'.
	 */
	protected abstract void createGlobalCrawlerTrapLists();

	/**
	 * Creates the initial (version 1) of table
	 * 'global_crawler_trap_expressions'.
	 */
	protected abstract void createGlobalCrawlerTrapExpressions();

	/**
	 * Checks that the connection is valid (i.e. still open on the server side).
	 * This implementation can be overriden if a specific RDBM is not handling
	 * the {@link Connection#isValid(int)} JDBC4 method properly.
	 * 
	 * @param connection the connection to check
	 * @param validityTimeout the time in seconds to wait for the database 
	 * operation used to validate the connection to complete. If the timeout 
	 * period expires before the operation completes, this method returns false. 
	 *  
	 * @return true if the connection is valid false otherwise.
	 * @see Connection#isValid(int)
	 * @throws SQLException
	 */
	public abstract boolean connectionIsValid(Connection connection,
			int validityTimeout) throws SQLException;

	/**
	 * Formats the LIMIT sub-clause of an SQL order clause. This sub-clause
	 * allows to paginate query results and its syntax might be dependant on the
	 * target RDBMS
	 * 
	 * @param limit
	 *            the maximum number of rows to fetch.
	 * @param offset
	 *            the starting offset in the full query results.
	 * @return the proper sub-clause.
	 */
	public abstract String getOrderByLimitAndOffsetSubClause(long limit,
			long offset);

	/**
	 * Returns true if the target RDBMS supports CLOBs. If possible seedlists
	 * will be stored as CLOBs.
	 * 
	 * @return true if CLOBs are supported, false otherwise.
	 */
	public abstract boolean supportsClob();

}
