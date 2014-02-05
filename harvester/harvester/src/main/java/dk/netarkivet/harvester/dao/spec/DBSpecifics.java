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

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.harvester.dao.HarvestDatabaseDAO;

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
