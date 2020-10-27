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
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;

/**
 * A class that implement functionality specific to the embedded Derby system.
 */
public class DerbyEmbeddedSpecifics extends DerbySpecifics {
    /**
     * Get an instance of the Embedded Derby specifics.
     *
     * @return Instance of the Derby specifics implementation
     */
    public static DBSpecifics getInstance() {
        return new DerbyEmbeddedSpecifics();
    }

    /**
     * Shutdown the database system, if running in embedded mode. Otherwise, this is ignored.
     * <p>
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
            log.debug(
                    "Shutdown down derby gave (as expected) an exception" + "\n"
                            + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Backup the database. For server-based databases, where the administrator is expected to perform the backups, this
     * method should do nothing. This method gets called within one hour of the hour-of-day indicated by the
     * DB_BACKUP_INIT_HOUR settings.
     *
     * @param backupDir Directory to which the database should be backed up
     * @param c The connection to the database.
     * @throws PermissionDenied if the directory cannot be created.
     * @throws IOFailure If we cannot connect to the database
     * @throws ArgumentNotValid If the connection or the backupDir if null.
     */
    public void backupDatabase(Connection c, File backupDir) throws PermissionDenied, ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        ArgumentNotValid.checkNotNull(backupDir, "backupDir");

        FileUtils.createDir(backupDir);
        CallableStatement cs = null;
        try {
            cs = c.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
            cs.setString(1, backupDir.getCanonicalPath());
            cs.execute();
            cs.close();
            log.info("Backed up database to " + backupDir.getCanonicalPath());
        } catch (IOException e) {
            throw new IOFailure("Couldn't back up database to " + backupDir, e);
        } catch (SQLException e) {
            throw new IOFailure("Could not execute sql statememt.", e);
        }
    }

    /**
     * Get the name of the JDBC driver class that handles interfacing to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }
}
