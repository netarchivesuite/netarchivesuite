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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Abstract collection of DB methods that are not standard SQL.  This class
 * is a singleton class whose actual implementation is provided by a subclass
 * as determined by the DB_SPECIFICS_CLASS setting.
 *
 */
public abstract class DBSpecifics extends SettingsFactory<DBSpecifics> {
    private static DBSpecifics instance;

    /** Get the singleton instance of the DBSpecifics implementation class
     *
     * @return An instance of DBSpecifics with implementations for a given DB.
     */
    public static synchronized DBSpecifics getInstance() {
        if (instance == null) {
            instance = getInstance(Settings.DB_SPECIFICS_CLASS);
        }
        return instance;
    }

    /**
     * Shutdown the database system, if running embeddedly.  Otherwise, this
     * is ignored.
     *
     * Will log a warning on errors, but otherwise ignore them.
     */
    public abstract void shutdownDatabase();

    /** Get a temporary table for short-time use.  The table should be
     * disposed of with dropTemporaryTable.  The table has two columns
     * domain_name varchar(Constants.MAX_NAME_SIZE)
     + config_name varchar(Constants.MAX_NAME_SIZE)
     * All rows in the table must be deleted at commit or rollback.
     *
     * @param c The DB connection to use.
     * @throws SQLException if there is a problem getting the table.
     * @return The name of the created table
     */
    public abstract String getJobConfigsTmpTable(Connection c) throws SQLException;

    /** Dispose of a temporary table gotten with getTemporaryTable. This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     *
     * @param c The DB connection to use.
     * @param tableName The name of the temporarily created table.
     */
    public abstract void dropJobConfigsTmpTable(Connection c, String tableName);

    /**
     * Backup the database.  For server-based databases, where the administrator
     * is expected to perform the backups, this method should do nothing.
     * This method gets called within one hour of the hour-of-day indicated
     * by the DB_BACKUP_INIT_HOUR settings.
     *
     * @param backupDir Directory to which the database should be backed up
     * @throws SQLException
     * @throws PermissionDenied if the directory cannot be created.
     */
    public abstract void backupDatabase(File backupDir) throws SQLException;

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public abstract String getDriverClassName();

    /** Update a table to a newer version, if necessary.  This will check the
     * schemaversions table to see the current version and perform a
     * table-specific update if required.
     *
     * @param tableName The table to update
     * @param toVersion The version to update the table to.
     */
    public void updateTable(String tableName, int toVersion) {

    }

    
}
