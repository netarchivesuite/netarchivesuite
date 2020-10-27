/*
 * #%L
 * Netarchivesuite - archive
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

package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Abstract collection of DB methods that are not standard SQL. This class is a singleton class whose actual
 * implementation is provided by a subclass as determined by the DB_SPECIFICS_CLASS setting.
 */
public abstract class DBSpecifics extends SettingsFactory<DBSpecifics> {

    /** The instance of the DBSpecifics class. */
    private static DBSpecifics instance;

    /**
     * Get the singleton instance of the DBSpecifics implementation class.
     *
     * @return An instance of DBSpecifics with implementations for a given DB.
     */
    public static synchronized DBSpecifics getInstance() {
        if (instance == null) {
            instance = getInstance(ArchiveSettings.CLASS_ARCREPOSITORY_ADMIN_DATABASE);
        }
        return instance;
    }

    /**
     * Shutdown the database system, if running in embedded mode. Otherwise, this is ignored.
     * <p>
     * Will log a warning on errors, but otherwise ignore them.
     */
    public abstract void shutdownDatabase();

    /**
     * Backup the database. For server-based databases, where the administrator is expected to perform the backups, this
     * method should do nothing. This method gets called within one hour of the hour-of-day indicated by the
     * DB_BACKUP_INIT_HOUR settings.
     *
     * @param backupDir Directory to which the database should be backed up
     * @param c The connection to the database.
     * @throws SQLException On SQL trouble backing up database
     * @throws PermissionDenied if the directory cannot be created.
     */
    public abstract void backupDatabase(Connection c, File backupDir) throws SQLException, PermissionDenied;

    /**
     * Get the name of the JDBC driver class that handles interfacing to this server.
     *
     * @return The name of a JDBC driver class
     */
    public abstract String getDriverClassName();
}
