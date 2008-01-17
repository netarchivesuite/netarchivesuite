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
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;

/**
 * A class that implement functionality specific to the embedded Derby system.
 */
public class DerbyEmbeddedSpecifics extends DerbySpecifics {
    /**
     * Get an instance of the Embedded Derby specifics.
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyEmbeddedSpecifics();
    }

    /**
     * Shutdown the database system, if running embeddedly.  Otherwise, this
     * is ignored.
     * <p/>
     * Will log a warning on errors, but otherwise ignore them.
     */
    public void shutdownDatabase() {
        try {
            // This call throws an exception, see
            // http://db.apache.org/derby/docs/10.2/ref/rrefattrib16471.html
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
            log.warn("Shut down Derby embedded database w/o expected warning");
        } catch (SQLException e) {
            log.info("Embedded Derby database has been shut down");
            log.debug("Shutdown down derby gave (as expected) an exception",
                    e);
        }
    }

    /**
     * Backup the database.  For server-based databases, where the administrator
     * is expected to perform the backups, this method should do nothing.
     * This method gets called within one hour of the hour-of-day indicated
     * by the DB_BACKUP_INIT_HOUR settings.
     *
     * @param backupDir Directory to which the database should be backed up
     * @throws SQLException If the underlying SQL driver throws an exception
     * @throws PermissionDenied if the directory cannot be created.
     * @throws IOFailure If we cannot connect to the database
     */
    public void backupDatabase(File backupDir) throws SQLException {
        ArgumentNotValid.checkNotNull(backupDir, "backupDir");

        FileUtils.createDir(backupDir);
        CallableStatement cs = null;
        try {
            Connection c = DBConnect.getDBConnection();
            cs = c.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
            cs.setString(1, backupDir.getCanonicalPath());
            cs.execute();
            cs.close();
            log.info("Backed up database to " + backupDir.getCanonicalPath());
        } catch (IOException e) {
            String message = "Couldn't back up database to " + backupDir;
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBConnect.closeStatementIfOpen(cs);
        }
    }

    /** Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }
}
