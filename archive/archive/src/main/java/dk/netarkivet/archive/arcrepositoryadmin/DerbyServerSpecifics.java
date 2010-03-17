/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.sql.Connection;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Implementation of DB-specific functions for the server-based Derby.
 */
public class DerbyServerSpecifics extends DerbySpecifics {
    /**
     * Get an instance of the Server Derby specifics.
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyServerSpecifics();
    }

    /**
     * Inherited function. We do not shut down external derby databases, only 
     * the embedded ones.
     */
    public void shutdownDatabase() {
        log.warn("The external database will not be shut down from within "
                + "the code.");
    }

    /**
     * Backup the database.  For server-based databases, where the administrator
     * is expected to perform the backups, this method should do nothing.
     * This method gets called within one hour of the hour-of-day indicated
     * by the DB_BACKUP_INIT_HOUR settings.
     *
     * @param backupDir Directory to which the database should be backed up
     * @param c The connection to the database to backup.
     * @throws ArgumentNotValid If the connection or the backup directory is 
     * null.
     */
    public void backupDatabase(Connection c, File backupDir) throws 
            ArgumentNotValid {
        ArgumentNotValid.checkNotNull(backupDir, "File backupDir");
        ArgumentNotValid.checkTrue(!backupDir.isFile(), "The file backupDir is "
                + "a file and not a directory.");
        ArgumentNotValid.checkNotNull(c, "Connection c");
        log.warn("Attempt to backup the database to directory '" 
                + backupDir.getAbsolutePath() + "'. ignored. Backup of your "
                + "external Derby database should be done by your SysOp");
    }

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.ClientDriver";
    }
}
