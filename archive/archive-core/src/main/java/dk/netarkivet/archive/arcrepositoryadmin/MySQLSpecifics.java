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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class MySQLSpecifics extends DBSpecifics {

    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(MySQLSpecifics.class);

    @Override
    public void shutdownDatabase() {
        log.warn("Attempt to shutdown the database ignored. Only meaningful for embedded databases");
    }

    @Override
    public void backupDatabase(Connection c, File backupDir) throws SQLException, PermissionDenied {
        log.warn("Attempt to backup the database ignored. Only meaningful for embedded databases");
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

}
