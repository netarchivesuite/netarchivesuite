/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;

/** 
 * Class containing the database connection to the replica cache database.
 */
public class ReplicaCacheDatabaseConnector {
    /** The log.*/
    protected static Log log
            = LogFactory.getLog(ReplicaCacheDatabaseConnector.class.getName());
   
    /** The one and only instance of this class. */
    private static ReplicaCacheDatabaseConnector instance;
    
    /** The connection to the database.*/
    private Connection dbConnection;
    
    /** max number of database retries. */
    private final int maxdatabaseRetries = Settings.getInt(
            ArchiveSettings.RECONNECT_MAX_TRIES_ADMIN_DATABASE);
    /** max time to wait between retries. */
    private final int delaybetweenretries = Settings.getInt(
            ArchiveSettings.RECONNECT_DELAY_ADMIN_DATABASE);
    
    /**
     * @return an instance of this class. 
     */
    public static synchronized ReplicaCacheDatabaseConnector getInstance() {
        if (instance == null) {
            instance = new ReplicaCacheDatabaseConnector();
        }
        return instance;
    }
    /** Private constructor of this class. */
    private ReplicaCacheDatabaseConnector() {
            dbConnection = DBConnect.getDBConnection(
                    DBConnect.getArchiveUrl());
    }
    
    
    /**
     * Get the current database connection. Check first, if the connection
     * is valid. If the connection is terminated. Try to reestablish the
     * connection a number of times before giving up.
     * @throws IOFailure If the database connection is down and
     * we are unable to reestablish the connection.
     * @return the current database connection.
     */
    protected synchronized Connection getDbConnection() throws IOFailure {
        final int secondsToWait = 5;
        try {
            if (dbConnection.isValid(secondsToWait)) {
                return this.dbConnection;
            }
        } catch (SQLException e) {
            log.warn("Exception thrown while testing the connection to the "
                    + "database", e);
        }
        // The connection is terminated (maybe server is shutdown?)
        // Try to reestablish the connection a number of times.
        Connection newConnection = null;
        boolean establishedConnection = false;
        int tries = 0;

        while (!establishedConnection && tries <= maxdatabaseRetries) {
            tries++;
            try {
                newConnection = DBConnect.getDBConnection(DBConnect
                        .getArchiveUrl());
                establishedConnection = true;
                this.dbConnection = newConnection;
            } catch (IOFailure e) {
                if (tries == maxdatabaseRetries) {
                    log.warn("Final attempt to reestablish "
                            + "connection to database failed.", e);
                    throw new IOFailure(
                            "Reestablishing of dbconnection failed", e);
                } else {
                    log.info("Another attempt to reestablish "
                            + "connection to database failed. Will try "
                            + (maxdatabaseRetries - tries)
                            + " more attempt(s) before giving up", e);
                    try {
                        Thread.sleep(delaybetweenretries);
                    } catch (InterruptedException e1) {
                        // ignore this exception
                        log.trace("Interruption ignored.", e1);
                    }
                }
            }
        }
        return this.dbConnection;
    }
    
    /** reset the instance. */
    public static synchronized void reset() {
        instance = null;
    }
}
