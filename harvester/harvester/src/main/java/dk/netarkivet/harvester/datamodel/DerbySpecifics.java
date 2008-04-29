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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Derby-specific implementation of DB methods.
 *
 */
public abstract class DerbySpecifics extends DBSpecifics {
    Log log = LogFactory.getLog(DBSpecifics.class);

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
    public String getJobConfigsTmpTable(Connection c) throws SQLException {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("DECLARE GLOBAL TEMPORARY TABLE "
                    + "jobconfignames "
                    + "( domain_name varchar(" + Constants.MAX_NAME_SIZE + "), "
                    + " config_name varchar(" + Constants.MAX_NAME_SIZE + ") )"
                    + " ON COMMIT DELETE ROWS NOT LOGGED ON ROLLBACK DELETE ROWS");
            s.execute();
            s.close();
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
        return "session.jobconfignames";
    }

    /**
     * Dispose of a temporary table gotten with getTemporaryTable.  This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     *
     * @param c The DB connection to use.
     * @param tableName The name of the temporary table
     */
    public void dropJobConfigsTmpTable(Connection c, String tableName) {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        ArgumentNotValid.checkNotNullOrEmpty(tableName, "String tableName");
        PreparedStatement s = null;
        try {
            // Now drop the temporary table
            s = c.prepareStatement("DROP TABLE " + tableName);
            s.execute();
        } catch (SQLException e) {
            log.warn("Couldn't drop temporary table " + tableName, e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }
    
    /** Update the database tables to the current versions.
     * @param tableName the name of the table to update
     * @param toVersion the required version of the table
     */
    public void updateTable(String tableName, int toVersion) {
        ArgumentNotValid.checkNotNullOrEmpty(tableName, "String tableName");
        ArgumentNotValid.checkPositive(toVersion, "int toVersion");
        
        int currentVersion = DBConnect.getTableVersion(tableName);
        if (currentVersion == toVersion) {
          // Nothing to do. Version of table is already correct.
          return;
        }
        
        if (tableName.equals("jobs")) {
            if (currentVersion < 3 || currentVersion > 4) {
                throw new IllegalState("Database is in an illegalState: " 
                        + "The current version of table '"
                        + tableName + "' is not acceptable. ");

            }
 
            // Migrate 'jobs' from version 3 to version 4.            
            // Change field forcemaxbytes from int to bigint
            String[] SqlStatements = {
                    // create backup for jobs
                    "CREATE TABLE backup (job_id BIGINT "
                    + "NOT NULL PRIMARY KEY, " +
                            "harvest_id BIGINT NOT NULL, " +
                            "status int NOT NULL, " +
                            "priority int NOT NULL, " +
                            "forcemaxbytes BIGINT NOT NULL DEFAULT -1, " +
                            "forcemaxcount BIGINT, " +
                            "orderxml varchar(300) NOT NULL, " +
                            "orderxmldoc clob(64M), " +
                            "seedlist clob(64M) not null, " +
                            "harvest_num int not null, " +
                            "harvest_errors varchar(300), " +
                            "harvest_error_details varchar(10000), " +
                            "upload_errors varchar(300), " +
                            "upload_error_details varchar(10000), " +
                            "startdate timestamp, " +
                            "enddate timestamp, " +
                            "num_configs int not null default 0, " +
                            "edition bigint not null" +
                            ")",
                    // Copy all entries of jobs into jobsBackup          
                     "INSERT INTO backup ( job_id, harvest_id, status, priority, forcemaxbytes," +
                     " forcemaxcount, orderxml, orderxmldoc, seedlist, harvest_num, harvest_errors, harvest_error_details," +
                     " upload_errors, upload_error_details, startdate, enddate, num_configs, edition)"
                    + " SELECT jobs.job_id, jobs.harvest_id, jobs.status, jobs.priority, jobs.forcemaxbytes," +
                     " jobs.forcemaxcount, jobs.orderxml, jobs.orderxmldoc, jobs.seedlist, jobs.harvest_num, jobs.harvest_errors, jobs.harvest_error_details," +
                     " jobs.upload_errors, jobs.upload_error_details, jobs.startdate, jobs.enddate, jobs.num_configs, jobs.edition " 
                    + " FROM jobs",
                    
                    "DROP TABLE JOBS",
                    // Create jobs table anew
                    
                    "CREATE TABLE jobs (job_id BIGINT "
                    + "NOT NULL PRIMARY KEY, " +
                            "harvest_id BIGINT NOT NULL, " +
                            "status int NOT NULL, " +
                            "priority int NOT NULL, " +
                            "forcemaxbytes BIGINT NOT NULL DEFAULT -1, " +
                            "forcemaxcount BIGINT, " +
                            "orderxml varchar(300) NOT NULL, " +
                            "orderxmldoc clob(64M), " +
                            "seedlist clob(64M) not null, " +
                            "harvest_num int not null, " +
                            "harvest_errors varchar(300), " +
                            "harvest_error_details varchar(10000), " +
                            "upload_errors varchar(300), " +
                            "upload_error_details varchar(10000), " +
                            "startdate timestamp, " +
                            "enddate timestamp, " +
                            "num_configs int not null default 0, " +
                            "edition bigint not null" +
                            ")",
                   // create indices again:
                   "create index jobstatus on jobs(status)",
                   "create index jobharvestid on jobs(harvest_id)",
                    // Insert data from jobsBackup to jobs:
                   "INSERT INTO jobs ( job_id, harvest_id, status, priority, forcemaxbytes," +
                   " forcemaxcount, orderxml, orderxmldoc, seedlist, harvest_num, harvest_errors, harvest_error_details," +
                   " upload_errors, upload_error_details, startdate, enddate, num_configs, edition)"
                  + " SELECT backup.job_id, backup.harvest_id, backup.status, backup.priority, backup.forcemaxbytes," +
                   " backup.forcemaxcount, backup.orderxml, backup.orderxmldoc, backup.seedlist, backup.harvest_num, backup.harvest_errors, backup.harvest_error_details," +
                   " backup.upload_errors, backup.upload_error_details, backup.startdate, backup.enddate, backup.num_configs, backup.edition " 
                  + " FROM backup",
                  "DROP table backup"
                    
//                    "ALTER TABLE jobs DROP COLUMN forcemaxbytes RESTRICT",
//                    "ALTER TABLE jobs ADD COLUMN forcemaxbytes BIGINT NOT NULL "
//                    + "DEFAULT -1",
//                    "UPDATE TABLE jobs SET forcemaxbytes = "
//                    + "(SELECT forcemaxbytesvalues.forcemaxbytes FROM "
//                    + "forcemaxbytesvalues WHERE jobs.job_id " 
//                    + "= forcemaxbytesvalues.job_id)",
//                    "Delete table forcemaxbytevalues;"
//            
            };
            DBConnect.updateTable(tableName, toVersion, SqlStatements);
            
        } else {
            throw new NotImplementedException("No method exists for migrating table '"
                    +  tableName + "' to version " + toVersion);
        }
    }
}
