/* File:        $Id: DBSpecifics.java 2804 2013-11-01 16:06:07Z svc $
 * Revision:    $Revision: 2804 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-11-01 17:06:07 +0100 (Fri, 01 Nov 2013) $
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

package dk.netarkivet.harvester.dao.spec;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.dao.HarvestDatabaseDAO;
import dk.netarkivet.harvester.dao.HarvesterDatabaseTables;
import dk.netarkivet.harvester.dao.ParameterMap;

/**
 * Defines database specific implementations used by the Harvester.
 *
 * The actual actual implementation which is loaded is defined by the
 * {@link CommonSettings#DB_SPECIFICS_CLASS} setting. See the sub class list for
 * available implementations
 */
public abstract class DBSpecifics extends SettingsFactory<DBSpecifics> {

	/**
	 * DAO class that exposes methods needed by the programmatic data model 
	 * version checking and updating code.
	 */
	protected final class DBSpecificsDAO extends HarvestDatabaseDAO {

		@Override
		protected Collection<HarvesterDatabaseTables> getRequiredTables() {
			return new ArrayList<HarvesterDatabaseTables>();
		}
		
		/** 
		 * Returns the version of a table according to schemaversions, or 0
	     * for the initial, unnumbered version.
	     * @param tablename The name of a table in the database.
	     * @return Version of the given table.
	     */
	    public int getTableVersion(final String tablename) {
	        ArgumentNotValid.checkNotNullOrEmpty(tablename, "String tablenname");
	        Integer version = queryIntValue(
	        		"SELECT version FROM schemaversions WHERE tablename=:name",
	        		new ParameterMap("name", tablename));
	        if (version == null) {
	        	throw new IOFailure("No version for table '" + tablename + "'");
	        }
	        return version;
	    }
	    
	    public void updateTable(
	    		final String table,
	            final int newVersion,
	            final String... updates) {

	        if (log.isInfoEnabled()) {
	            log.info("Updating table '" + table + "' to version " + newVersion);
	        }

	        String[] sqlStatements = new String[updates.length + 1];
	        String updateSchemaversionSql = null;
	        if (newVersion == 1) {
	            updateSchemaversionSql = 
	            		"INSERT INTO schemaversions(tablename, version)"
	            		+ " VALUES ('" + table + "', 1)";
	        } else {
	            updateSchemaversionSql =
	                "UPDATE schemaversions SET version=" + newVersion 
	                + " WHERE tablename = '" + table + "'";
	        }
	        System.arraycopy(updates, 0, sqlStatements, 0, updates.length);
	        sqlStatements[updates.length] = updateSchemaversionSql;
	        
	        executeUpdates(sqlStatements);
	    }
	    
	    /**
	     * Executes series of updates with the given SQL request in a transaction.
	     * @param sql an SQL request
	     */
	    public void executeUpdates(String... updates) {
	    	executeTransaction("doExecuteUpdates", String[].class, updates);
	    }
	    
	    @SuppressWarnings("unused")
		private void doExecuteUpdates(String... updates) {
	    	for (String sql : updates) {
	    		executeUpdate(sql, false);
	    	}
	    }
	    
	    /**
	     * Executes an update with the given SQL request.
	     * @param sql an SQL request
	     * @param transaction if true executes the update in a transaction
	     */
	    public void executeUpdate(String sql, boolean transaction) {
	    	if (transaction) {
	    		executeTransaction("doExecuteUpdate", String.class, sql);
	    	} else {
	    		doExecuteUpdate(sql);
	    	}
	    }
	    
	    private final void doExecuteUpdate(String sql) {
	    	super.executeUpdate(sql);
	    }

	}
	
	/**
	 * The class logger.
	 */
	private Log log = LogFactory.getLog(DBSpecifics.class);
	
    /** The instance of the DBSpecifics class. */
    private static DBSpecifics instance;
    
    private DBSpecificsDAO dao;    

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
	 * @return the dao
	 */
	protected synchronized final DBSpecificsDAO getDao() {
		if (dao == null) {
			dao = new DBSpecificsDAO();
		}
		return dao;
	}

	/**
     * Get a temporary table for short-time use. The table should be disposed of
     * with dropTemporaryTable. The table has two columns domain_name
     * varchar(Constants.MAX_NAME_SIZE) + config_name
     * varchar(Constants.MAX_NAME_SIZE) All rows in the table must be deleted at
     * commit or rollback.
     * @return The name of the created table
     */
    public abstract String getJobConfigsTmpTable();

    /**
     * Dispose of a temporary table gotten with getTemporaryTable. This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     *
     * @param tableName The name of the temporarily created table.
     */
    public abstract void dropJobConfigsTmpTable(String tableName);

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

        int currentVersion = dao.getTableVersion(tableName);
        if (currentVersion == toVersion) {
            // Nothing to do. Version of table is already correct.
            return;
        }
        log.info("Trying to migrate table '" + tableName + "' from version '"
                + currentVersion + "' to version '" + toVersion + "'.");

        if (currentVersion > toVersion) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version of table '" + tableName
                    + "' is not acceptable "
                    + "(current version is greater than requested version).");
        }

        if (tableName.equals(HarvesterDatabaseTables.JOBS.getTablename())) {
            upgradeJobsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.FULLHARVESTS.getTablename())) {
            upgradeFullharvestsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.CONFIGURATIONS.getTablename())) {
            upgradeConfigurationsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.GLOBALCRAWLERTRAPLISTS.getTablename())) {
            upgradeGlobalcrawlertraplistsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.GLOBALCRAWLERTRAPEXPRESSIONS.getTablename())) {
            upgradeGlobalcrawlertrapexpressionsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.RUNNINGJOBSHISTORY.getTablename())) {
            upgradeRunningjobshistoryTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.RUNNINGJOBSMONITOR.getTablename())) {
            upgradeRunningjobsmonitor(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.FRONTIERREPORTMONITOR.getTablename())) {
            upgradeFrontierreportmonitorTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.EXTENDEDFIELD.getTablename())) {
            upgradeExtendedFieldTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.EXTENDEDFIELDVALUE.getTablename())) {
            upgradeExtendedFieldValueTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.EXTENDEDFIELDTYPE.getTablename())) {
            upgradeExtendedFieldTypeTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.DOMAINS.getTablename())) {
            upgradeDomainsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.HARVESTDEFINITIONS.getTablename())) {
            upgradeHarvestdefinitionsTable(currentVersion, toVersion);
        } else if (tableName.equals(HarvesterDatabaseTables.HARVESTCHANNELS.getTablename())) {
            upgradeHarvestchannelTable(currentVersion, toVersion);
            // Add new if else when other tables need to be upgraded
        } else {
            throw new NotImplementedException(
                    "No method exists for migrating table '" + tableName
                    + "' to version " + toVersion);
        }
    }

    private void upgradeHarvestdefinitionsTable(int currentVersion,
            int toVersion) {
        if (currentVersion < 2) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version " + currentVersion
                    + " of table '" + HarvesterDatabaseTables.HARVESTDEFINITIONS.getTablename() 
                    + "' is not acceptable. The current table version is less than open source version 2. "
                    + "Probably a wrong entry in the schemaversions table");
        }
        if (currentVersion == 2 && toVersion >= 3) {
            migrateHarvestdefinitionsv2tov3();
            currentVersion = 3;
        }
        if (currentVersion == 3 && toVersion >= 4) {
            migrateHarvestdefinitionsv3tov4();
            currentVersion = 4;
        }
        // insert new migrations here
        if (currentVersion != HarvesterDatabaseTables.HARVESTDEFINITIONS.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '"
                    + HarvesterDatabaseTables.HARVESTDEFINITIONS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }    
        
    }

    private void upgradeExtendedFieldTypeTable(int currentVersion, int toVersion) {
        if (currentVersion == 0 && toVersion == 1) {
            createExtendedFieldTypeTable();
            currentVersion = 1;
        }
        if (currentVersion > HarvesterDatabaseTables.EXTENDEDFIELDTYPE.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '"
                            + HarvesterDatabaseTables.EXTENDEDFIELDTYPE.getTablename() 
                            + "' from version " + currentVersion 
                            + " to version " + toVersion);
        }
    }

    private void upgradeExtendedFieldValueTable(int currentVersion,
            int toVersion) {
        if (currentVersion == 0 && toVersion == 1) {
            createExtendedFieldValueTable();
            currentVersion = 1;
        }
        if (currentVersion > HarvesterDatabaseTables.EXTENDEDFIELDVALUE.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '"
                            + HarvesterDatabaseTables.EXTENDEDFIELDVALUE.getTablename() 
                            + "' from version " + currentVersion 
                            + " to version " + toVersion);
        }
    }

    private void upgradeExtendedFieldTable(int currentVersion, int toVersion) {
        if (currentVersion == 0 && toVersion == 1) {
            createExtendedFieldTable();
            currentVersion = 1;
        }
        if (currentVersion > HarvesterDatabaseTables.EXTENDEDFIELD.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '"
                            + HarvesterDatabaseTables.EXTENDEDFIELD.getTablename() 
                            + "' from version " + currentVersion 
                            + " to version " + toVersion);
        }
    }

    /** Migrate the frontierreportmonitor table.
     * 
     * @param currentVersion the current version of the frontierreportmonitor 
     * table
     * @param toVersion the required version of the frontierreportmonitor table
     */
    private void upgradeFrontierreportmonitorTable(
    		int currentVersion,
            int toVersion) {
        if (currentVersion == 0 && toVersion == 1) {
            createFrontierReportMonitorTable();
            currentVersion = 1;
        }
        // insert new migrations here
        if (currentVersion > HarvesterDatabaseTables.FRONTIERREPORTMONITOR.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '"
                    + HarvesterDatabaseTables.FRONTIERREPORTMONITOR.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }    
    }
    
    /** Migrate the runningjobsmonitor table.
     * 
     * @param currentVersion the current version of the runningjobsmonitor table
     * @param toVersion the required version of the runningjobsmonitor table
     */
    private void upgradeRunningjobsmonitor(int currentVersion, int toVersion) {
        if (currentVersion == 0 && toVersion >= 1) {
            createRunningJobsMonitorTable();
            currentVersion = 1;
        }
        if (currentVersion == 1 && toVersion >= 2) {
            migrateRunningJobsMonitorTableV1ToV2();
            currentVersion = 2;
        }
        if (currentVersion > HarvesterDatabaseTables.RUNNINGJOBSMONITOR.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.RUNNINGJOBSMONITOR.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }
    }
    
    /** Migrate the runningjobshistory table.
     * 
     * @param currentVersion the current version of the runningjobshistory table
     * @param toVersion The required version of the runningjobshistory table
     */
    private void upgradeRunningjobshistoryTable(int currentVersion,
            int toVersion) {
        if (currentVersion == 0 && toVersion >= 1) {
            createRunningJobsHistoryTable();
            currentVersion = 1;
        }
        if (currentVersion == 1 && toVersion >= 2) {
            migrateRunningJobsHistoryTableV1ToV2();
            currentVersion = 2;
        }
        // insert new migrations here
        if (currentVersion > HarvesterDatabaseTables.RUNNINGJOBSHISTORY.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.RUNNINGJOBSHISTORY.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

    }
    /** Migrate the globalecrawlertrapexpressions table.
     * 
     * @param currentVersion the current version of the jobs table
     * @param toVersion The required version of the jobs table
     */
    private void upgradeGlobalcrawlertrapexpressionsTable(int currentVersion,
            int toVersion) {
        if (currentVersion == 0 && toVersion >= 1) {
            createGlobalCrawlerTrapExpressions();
            currentVersion = 1;
        }
        // insert new migrations here
        if (currentVersion 
                > HarvesterDatabaseTables.GLOBALCRAWLERTRAPEXPRESSIONS.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.GLOBALCRAWLERTRAPEXPRESSIONS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

    }
    /** Migrate the globalecrawlertraplists table.
     * 
     * @param currentVersion the current version of the 
     *  globalecrawlertraplists table
     * @param toVersion The required version of the 
     * globalecrawlertraplists table
     */
    private void upgradeGlobalcrawlertraplistsTable(
    		int currentVersion,
            int toVersion) {
        if (currentVersion == 0 && toVersion >= 1) {
            createGlobalCrawlerTrapLists();
            currentVersion = 1;
        }
        // insert new migrations here
        if (currentVersion > HarvesterDatabaseTables.GLOBALCRAWLERTRAPLISTS.getRequiredVersion()) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.GLOBALCRAWLERTRAPLISTS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

    } 

    /** Migrate the jobs table.
     * 
     * @param currentVersion the current version of the jobs table
     * @param toVersion The required version of the jobs table
     */
    private void upgradeJobsTable(int currentVersion, int toVersion) {
        if (currentVersion < 3) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version " + currentVersion
                    + " of table '" + HarvesterDatabaseTables.JOBS.getTablename() + "' is not acceptable. "
                    + "(current version is less than open source version).");
        }
        if (currentVersion == 3 && toVersion >= 4) {
            migrateJobsv3tov4();
            currentVersion = 4;
        }
        if (currentVersion == 4 && toVersion >= 5) {
            migrateJobsv4tov5();
            currentVersion = 5;
        }
        if (currentVersion == 5 && toVersion >= 6) {
            migrateJobsv5tov6();
            currentVersion = 6;
        }
        if (currentVersion == 6 && toVersion >= 7) {
            migrateJobsv6tov7();
            currentVersion = 7;
        }
        if (currentVersion == 7 && toVersion >= 8) {
            migrateJobsv7tov8();
            currentVersion = 8;
        }
        if (currentVersion == 8 && toVersion >= 9) {
            migrateJobsv8tov9();
            currentVersion = 9;
        }
        if (currentVersion == 9 && toVersion >= 10) {
            migrateJobsv9tov10();
            currentVersion = 10;
        }
        // future updates of the jobs table are inserted here
        if (currentVersion == HarvesterDatabaseTables.JOBS.getRequiredVersion() 
                && toVersion >= HarvesterDatabaseTables.JOBS.getRequiredVersion() + 1) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                            + HarvesterDatabaseTables.JOBS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

        if (currentVersion > HarvesterDatabaseTables.JOBS.getRequiredVersion()) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version (" + currentVersion
                    + ") of table '" + HarvesterDatabaseTables.JOBS.getTablename()
                    + "' is not an acceptable/known version. ");
        }

    }

    /** Migrate the configurations table.
     * 
     * @param currentVersion the current version of the configurations table
     * @param toVersion the required version of the configurations table
     */
    private void upgradeConfigurationsTable(int currentVersion, int toVersion) {
        if (currentVersion < 3) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version " + currentVersion
                    + " of table '" + HarvesterDatabaseTables.CONFIGURATIONS.getTablename() 
                    + "' is not acceptable. "
                    + "(current version is less than open source version).");
        }
        if (currentVersion == 3 && toVersion >= 4) {
            migrateConfigurationsv3ov4();
            currentVersion = 4;
        }

        if (currentVersion == 4 && toVersion >= 5) {
            migrateConfigurationsv4tov5();
            currentVersion = 5;
        }
        // insert future migrations here
        if (currentVersion == HarvesterDatabaseTables.CONFIGURATIONS.getRequiredVersion()
                && toVersion >= HarvesterDatabaseTables.CONFIGURATIONS.getRequiredVersion()+ 1) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.CONFIGURATIONS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

        if (currentVersion > HarvesterDatabaseTables.CONFIGURATIONS.getRequiredVersion()) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version (" + currentVersion
                    + ") of table '" + HarvesterDatabaseTables.CONFIGURATIONS.getTablename()
                    + "' is not an acceptable/known version. ");
        }
    }   

    private void upgradeDomainsTable(int currentVersion, int toVersion)  {
           if (currentVersion < 2) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version " + currentVersion
                    + " of table '" + HarvesterDatabaseTables.DOMAINS.getTablename()
                    + "' is not acceptable. "
                    + "(current version is less than open source version).");
        }
        if (currentVersion == 2 && toVersion >= 3) {
            migrateDomainsv2tov3();
            currentVersion = 3;
        }
    }

    /** Migrate the fullharvests table.
     * 
     * @param currentVersion the current version of the fullharvests table
     * @param toVersion the required version of the fullharvests table
     */
    private void upgradeFullharvestsTable(int currentVersion, int toVersion) {
        if (currentVersion < 2) {
            throw new IllegalState("Database is in an illegalState: "
                    + "The current version " + currentVersion
                    + " of table '" + HarvesterDatabaseTables.FULLHARVESTS.getTablename()
                    + "' is not acceptable. "
                    + "(current version is less than open source version).");
        }
        if (currentVersion == 2 && toVersion >= 3) {
            migrateFullharvestsv2tov3();
            currentVersion = 3;
        }

        if (currentVersion == 3 && toVersion >= 4) {
            migrateFullharvestsv3tov4();
            currentVersion = 4;
        }
        
        if (currentVersion == 4 && toVersion >= 5) {
            migrateFullharvestsv4tov5();
            currentVersion = 5;
        }
        
        // insert future migrations here
        if (currentVersion == HarvesterDatabaseTables.FULLHARVESTS.getRequiredVersion()
                && toVersion >= HarvesterDatabaseTables.FULLHARVESTS.getRequiredVersion() + 1) {
            throw new NotImplementedException(
                    "No method exists for migrating table '" 
                    + HarvesterDatabaseTables.FULLHARVESTS.getTablename()
                    + "' from version " + currentVersion
                    + " to version " + toVersion);
        }

    }
    
    /** Migrate the harvestchannel table.
     * 
     * @param currentVersion the current version of the harvestchannel table
     * @param toVersion the required version of the harvestchannel table
     */
    private void upgradeHarvestchannelTable(int currentVersion, int toVersion) {
        if (currentVersion == 0 && toVersion >= 1) {
            createHarvestChannelTable();
            currentVersion = 1;
        }
    }

    protected abstract void createHarvestChannelTable();

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
     * Migrates the 'runningjobshistory' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    protected abstract void migrateRunningJobsHistoryTableV1ToV2();

    /**
     * Migrates the 'runningjobsmonitor' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    protected abstract void migrateRunningJobsMonitorTableV1ToV2();

     /**
     * Migrates the 'domains' table from version 2 to version 3. This
     * consists of altering the type of the crawlertraps column to "text"
     * in postgres, and noop in derbyDB
     */
    protected abstract void migrateDomainsv2tov3();

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

    /**
     * Create the frontierReportMonitor table in the database.
     */
    public abstract void createFrontierReportMonitorTable();

    /**
     * Create the frontierReportMonitor table in the database.
     */
    public abstract void createRunningJobsHistoryTable();
    /**
     * Create the frontierReportMonitor table in the database.
     */
    public abstract void createRunningJobsMonitorTable();

    /**
     * Migrates the 'jobs' table from version 5 to version 6.
     * Adds the field 'forcemaxrunningtime'.
     *
     * @throws IOFailure
     *             in case of problems in interacting with the database
     */
    protected abstract void migrateJobsv5tov6();

    /**
     * Migrates the 'configurations' table from version 4 to version 5. This
     * consists of altering the field 'maxobjects' from being an int to a 
     * bigint.
     */
    protected abstract void migrateConfigurationsv4tov5();

    /**
     * Migrates the 'fullharvests' table from version 3 to version 4. This
     * consists of adding the field 'maxjobrunningtime'.
     */
    protected abstract void migrateFullharvestsv3tov4();

    /**
     * Migrates the 'fullharvests' table from version 4 to version 5. This
     * consists of adding the field 'isindexready'.
     */
    protected abstract void migrateFullharvestsv4tov5();
    
    /**
     * Create the extendedfieldtype table in the database.
     */
    protected abstract void createExtendedFieldTypeTable();
    
    /**
     * Create the extendedfield table in the database.
     */
    protected abstract void createExtendedFieldTable();

    /**
     * Create the extendedfieldvalue table in the database.
     */
    protected abstract void createExtendedFieldValueTable();
    
    /**
     * Migrates the 'jobs' table from version 6 to version 7 consisting of adding
     * the bigint fieldcontinuationof with null as default.
     */
    protected abstract void migrateJobsv6tov7();

    /**
     * Migrates the 'jobs' table from version 7 to version 8 consisting of adding
     * the date creationdate with null as default.
     */
    protected abstract void migrateJobsv7tov8(); 
    
    /**
     * Migrates the 'jobs' table from version 8 to version 9 consisting of adding
     * the string harvestname_prefix with null as default.
     */
    protected abstract void migrateJobsv8tov9();
    
    /**
     * Migrates the 'harvestdefinitions' table from version 2 to version 3 consisting of adding
     * the string audience with null as default.
     */
    protected abstract void migrateHarvestdefinitionsv2tov3();
    
    /**
     * Migrates the 'harvestdefinitions' table from version 3 to version 4 consisting of adding
     * the bigint channel_id field.
     */
    protected abstract void migrateHarvestdefinitionsv3tov4();
    
    /**
     * Migrates the 'jobs' table from version 9 to version 10 consisting of adding
     * the channel (varchar 300) and a 'snapshot'
     */
    protected abstract void migrateJobsv9tov10();
    
    /**
     * Update all tables in the enum class {@link HarvesterDatabaseTables} to the required
     * version. There is no attempt to undo the update.
     */
    public void updateTables() {
        for (HarvesterDatabaseTables table: HarvesterDatabaseTables.values()) {
            updateTable(table.getTablename(), table.getRequiredVersion());
        }
    }
    
}
