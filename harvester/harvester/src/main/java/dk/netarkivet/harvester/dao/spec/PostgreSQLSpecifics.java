/* File:       $Id: PostgreSQLSpecifics.java 2804 2013-11-01 16:06:07Z svc $
 * Revision:   $Revision: 2804 $
 * Author:     $Author: svc $
 * Date:       $Date: 2013-11-01 17:06:07 +0100 (Fri, 01 Nov 2013) $
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

import dk.netarkivet.harvester.datamodel.Constants;

/**
 * PostgreSQL-specific implementation of DB methods.
 * Intended for PostgreSQL 8.3 and above.
 *
 *  PostgreSQL does not support the CLOB datatype but instead provides a "text"
 *  data type.
 *  See http://www.postgresql.org/docs/current/static/datatype-character.html.
 *
 */
public class PostgreSQLSpecifics extends DBSpecifics {
    
    /**
     * Get an instance of the PostgreSQL specifics class.
     *
     * @return Instance of the PostgreSQL specifics class.
     */
    public static DBSpecifics getInstance() {
        return new PostgreSQLSpecifics();
    }
    
    /**
     * Get a temporary table for short-time use. The table should be disposed of
     * with dropTemporaryTable. The table has two columns domain_name
     * varchar(Constants.MAX_NAME_SIZE) config_name
     * varchar(Constants.MAX_NAME_SIZE)
     * @return The name of the created table
     */
    public String getJobConfigsTmpTable() {
    	getDao().executeUpdate(
    			"CREATE TEMPORARY TABLE jobconfignames ("
    			+ "domain_name varchar(" + Constants.MAX_NAME_SIZE + ")"
    			+ ", config_name varchar(" + Constants.MAX_NAME_SIZE + ")"
    			+ ") ON COMMIT DROP",
    			false);
        return "jobconfignames";
    }

    /**
     * Dispose of a temporary table created with getTemporaryTable. This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     * @param tableName The name of the temporary table
     */
    public void dropJobConfigsTmpTable(final String tableName) {
    	// Commit triggers the drop, see getJobConfigsTmpTable
    	getDao().executeUpdate("SELECT 1", true);
    }

    /**
     * Get the name of the JDBC driver class that handles interfacing to this
     * server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getOrderByLimitAndOffsetSubClause(long limit, long offset) {
        return "LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public boolean supportsClob() {
        return false;
    }
    
}
