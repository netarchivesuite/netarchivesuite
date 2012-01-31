/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class MySQLSpecifics extends DBSpecifics {

    /** The log. */
    Log log = LogFactory.getLog(MySQLSpecifics.class);
    
    @Override
    public void shutdownDatabase() {
        log.warn("Attempt to shutdown the database ignored. Only meaningful "
                + "for embedded databases");
    }

    @Override
    public void backupDatabase(Connection c, File backupDir)
            throws SQLException, PermissionDenied {
        log.warn("Attempt to backup the database ignored. Only meaningful "
                + "for embedded databases");
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

}
