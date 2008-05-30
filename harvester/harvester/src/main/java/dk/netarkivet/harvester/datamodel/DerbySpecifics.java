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
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.NotificationsFactory;

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
            DBUtils.closeStatementIfOpen(s);
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
            DBUtils.closeStatementIfOpen(s);
        }
    }
    /** Migrates the 'jobs' table from version 3 to version 4
     * consisting of a change of the field forcemaxbytes from int to bigint
     * and setting its default to -1.
     * Furthermore the default value for field num_configs is set to 0.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv3tov4() {
        // Due to use of old version of Derby, it is not possible to use ALTER
        // table for the migration. Thus the migration is done by full backup
        // table of the jobs table.
        // TODO rewrite to simpler SQL when upgrading Derby version:
        // <copy values of jobs.forcemaxbytes and jobs.jobid into backup table>
        // ALTER TABLE jobs DROP COLUMN forcemaxbytes RESTRICT
        // ALTER TABLE jobs ADD COLUMN forcemaxbytes BIGINT NOT NULL DEFAULT -1
        // UPDATE TABLE jobs SET forcemaxbytes =
        // (SELECT forcemaxbytes FROM backupjobcolv3tov4
        // WHERE jobs.job_id = forcemaxbytesvalues.job_id)
        // DELETE table backupjobcolv3tov4;
        String sql;
        int countOfJobsTable;
        int countOfBackuptable;
        String table = "jobs";
        String tmptable = "backupJobs3to4";

        // Check that temporary table from earlier tries does not exist

        try {
            countOfBackuptable = DBUtils
            .selectIntValue("select count(*) from " + tmptable);
        } catch (IOFailure e) {
            // expected, otherwise the backupJobs3to4 table exists
            countOfBackuptable = -1;
        }
        if (countOfBackuptable >= 0) {
            try {
                countOfJobsTable = DBUtils.selectIntValue(
                        "select count(*) from " + table);
            } catch (IOFailure e) {
                // close to worst case, but data can still be found in back-up
                // table
                String errMsg = "Earlier migration of table "
                    + table
                    + " seems to have failed. The "
                    + table
                    + " table is missing, but a temporary table named "
                    + tmptable
                    + " seem to still contain the data. Please check, "
                    + "- make a new "
                    + table
                    + " table, and insert data from the temporary table.";
                NotificationsFactory.getInstance().errorEvent(errMsg, e);
                throw new IOFailure(errMsg);
            }
            if (countOfBackuptable != countOfJobsTable) {
                // close to worst case, but data can maybe still be found in
                // back-up table
                String errMsg = "Earlier migration of table "
                    + table
                    + " seems to have failed. "
                    + "Some data from the jobs table is missing, "
                    + "but a temporary table named "
                    + tmptable
                    + "seem to still contain the extra data. Please check, - "
                    + " and insert missing data in " + table
                    + " table data from the temporary table.";
                NotificationsFactory.getInstance().errorEvent(errMsg);
                throw new IOFailure(errMsg);
            } else {
                // backup table exists already. Delete it. 
                sql = "DROP TABLE " + tmptable;
                DBUtils.executeSQL(sql);
            }
        }
        
        final String partialJobsDefinition = 
            "job_id bigint not null primary key, "
            + "harvest_id bigint not null, "
            + "status int not null, " + "priority int not null, "
            + "forcemaxbytes bigint not null default -1, "
            + "forcemaxcount bigint, "
            + "orderxml varchar(300) not null, "
            + "orderxmldoc clob(64M) not null, "
            + "seedlist clob(64M) not null, "
            + "harvest_num int not null, "
            + "harvest_errors varchar(300), "
            + "harvest_error_details varchar(10000), "
            + "upload_errors varchar(300), "
            + "upload_error_details varchar(10000), "
            + "startdate timestamp, " + "enddate timestamp, "
            + "num_configs int not null default 0, "
            + "edition bigint not null ";
            
            
        // create backup table for jobs table
        sql = "CREATE TABLE " + tmptable 
        + " ("
        +    partialJobsDefinition
        + ")";
        DBUtils.executeSQL(sql);

        // copy contents of jobs table into backup table
        
        final String listOfFieldsInJobsTable =
            "job_id, harvest_id, status, priority, forcemaxbytes, "
            + "forcemaxcount, orderxml, orderxmldoc, seedlist, harvest_num, "
            + "harvest_errors, harvest_error_details, upload_errors, "
            + "upload_error_details, startdate, enddate, num_configs, edition ";
        
        sql = "INSERT INTO " + tmptable
            + " ( " + listOfFieldsInJobsTable + ") "
            + "SELECT " + listOfFieldsInJobsTable
            + "FROM " + table;
        DBUtils.executeSQL(sql);

        // check everything looks okay
        countOfJobsTable = DBUtils.selectIntValue(
                "select count(*) from " + table);
        countOfBackuptable = DBUtils.selectIntValue(
                "select count(*) from " + tmptable);
        if (countOfBackuptable != countOfJobsTable) {
            throw new IOFailure("Unexpected inconsistency: the number of "
                    + "backed up entries from " + table
                    + "does not correspond " + "to the number of entries in "
                    + table);
        }

        // Update jobs table to version 4
        String[] sqlStatements = {
                // drop jobs table (are backed up in backupJobs3to4)
                "DROP TABLE " + table,

                // create jobs table again
                "CREATE TABLE " + table + " (" + partialJobsDefinition + ")",

                // create indices again:
                "create index jobstatus on " + table + "(status)",
                "create index jobharvestid on " + table + "(harvest_id)",

                // insert data from backup table to jobs table:
                "INSERT INTO " + table
                + " ( " + listOfFieldsInJobsTable + ") "
                + "SELECT " + listOfFieldsInJobsTable
                + "FROM " + tmptable };
        DBConnect.updateTable("jobs", 4, sqlStatements);

        //check everything looks okay
        countOfJobsTable = DBUtils.selectIntValue(
                "select count(*) from " + table);
        countOfBackuptable = DBUtils.selectIntValue(
                "select count(*) from " + tmptable);
        if (countOfBackuptable != countOfJobsTable) {
            throw new IOFailure("Unexpected inconsistency: the number of "
                    + "backed up entries from " + table
                    + "does not correspond " + "to the number of entries in "
                    + table + "although no " + "exception has arised");
        }

        //drop backup table
        sql = "DROP TABLE backupJobs3to4";
        DBUtils.executeSQL(sql);  
    }
}