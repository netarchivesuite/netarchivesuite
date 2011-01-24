/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
import dk.netarkivet.common.utils.ExceptionUtils;

/**
 * Derby-specific implementation of DB methods.
 *
 */
public abstract class DerbySpecifics extends DBSpecifics {
    Log log = LogFactory.getLog(DerbySpecifics.class);

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
            log.warn("Couldn't drop temporary table " + tableName + "\n" +
                     ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
    
    /**
     * Checks that the connection is valid (i.e. still open on the server side).
     * This implementation can be overriden if a specific RDBM is not handling
     * the {@link Connection#isValid(int)} JDBC4 method properly.
     * @param connection
     * @param validityTimeout The time in seconds to wait for the database operation used to validate the connection 
     to complete. If the timeout period expires before the operation completes, this method returns false. A 
     value of 0 indicates a timeout is not applied to the database operation. 
     * @return true, if the connection is valid; false, otherwise
     * @throws SQLException 
     */
    public boolean connectionIsValid(Connection connection, int validityTimeout)
            throws SQLException {
        return connection.isValid(validityTimeout);
    }
    @Override
    public String getOrderByLimitAndOffsetSubClause(long limit, long offset) {
        // LIMIT sub-clause supported by Derby 10.5.3
        // see http://db.apache.org/derby/docs/10.5/ref/rrefsqljoffsetfetch.html
        return "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
    
    @Override
    public boolean supportsClob() {
        return true;
    }

    /** Migrates the 'jobs' table from version 3 to version 4
     * consisting of a change of the field forcemaxbytes from int to bigint
     * and setting its default to -1.
     * Furthermore the default value for field num_configs is set to 0.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv3tov4() {
        // Change the forcemaxbytes from 'int' to 'bigint'.
        // Procedure for changing the datatype of a derby table was found here:
        // https://issues.apache.org/jira/browse/DERBY-1515
        String[] SqlStatements = {
        "ALTER TABLE jobs ADD COLUMN forcemaxbytes_new bigint NOT NULL DEFAULT -1",
        "UPDATE jobs SET forcemaxbytes_new = forcemaxbytes",
        "ALTER TABLE jobs DROP COLUMN forcemaxbytes",
        "RENAME COLUMN jobs.forcemaxbytes_new TO forcemaxbytes",
        "ALTER TABLE jobs ALTER COLUMN num_configs SET DEFAULT 0"
        
        };
        DBConnect.updateTable("jobs", 4, SqlStatements);
    }
    
    /** Migrates the 'jobs' table from version 4 to version 5
     * consisting of adding new fields 'resubmitted_as_job' and 'submittedDate'.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv4tov5() {
     // Update jobs table to version 5
        String[] SqlStatements = {
                "ALTER TABLE jobs ADD COLUMN submitteddate timestamp",
                "ALTER TABLE jobs ADD COLUMN resubmitted_as_job bigint"    
            };
        DBConnect.updateTable("jobs", 5, SqlStatements);
    }
    
    /** Migrates the 'configurations' table from version 3 to version 4.
     * This consists of altering the default value of field 'maxbytes' to -1.
     */
    protected synchronized void migrateConfigurationsv3ov4() {
     // Update configurations table to version 4
        String[] SqlStatements = {
                "ALTER TABLE configurations ALTER maxbytes WITH DEFAULT -1"
            };
        DBConnect.updateTable("configurations", 4, SqlStatements);
    }

    /** Migrates the 'fullharvests' table from version 2 to version 3.
     * This consists of altering the default value of field 'maxbytes' to -1.
     */
    protected synchronized void migrateFullharvestsv2tov3() {
        // Update fullharvests table to version 3
        String[] SqlStatements = {
                "ALTER TABLE fullharvests ALTER maxbytes WITH DEFAULT -1"
            };
        DBConnect.updateTable("fullharvests", 3, SqlStatements);
    }

    @Override
    protected void createGlobalCrawlerTrapLists() {
        String createStatement = "CREATE TABLE global_crawler_trap_lists(\n"
                                 + "  global_crawler_trap_list_id INT NOT NULL "
                                 + "GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                                 + "  name VARCHAR(300) NOT NULL UNIQUE,  "
                                 + "  description VARCHAR(30000),"
                                 + "  isActive INT NOT NULL) ";
        DBConnect.updateTable("global_crawler_trap_lists", 1, createStatement);
    }

    @Override
    protected void createGlobalCrawlerTrapExpressions() {
        String createStatement = "CREATE TABLE global_crawler_trap_expressions("
                                 + "    crawler_trap_list_id INT NOT NULL, "
                                 + ""
                                 + "    trap_expression VARCHAR(1000),"
                                 + "    PRIMARY KEY (crawler_trap_list_id, "
                                 + "trap_expression))";
        DBConnect.updateTable("global_crawler_trap_expressions", 1,
                              createStatement);
    }
    
    @Override
    public void createFrontierReportMonitorTable() {
        String createStatement = "CREATE TABLE frontierReportMonitor ("
            + " jobId bigint NOT NULL, "
            + "filterId varchar(200) NOT NULL,"     
            + "tstamp timestamp NOT NULL,"
            + "domainName varchar(300) NOT NULL,"
            + "currentSize bigint NOT NULL,"
            + "totalEnqueues bigint NOT NULL,"
            + "sessionBalance bigint NOT NULL,"
            + "lastCost numeric NOT NULL,"
            + "averageCost numeric NOT NULL,"
            + "lastDequeueTime varchar(100) NOT NULL,"
            + "wakeTime varchar(100) NOT NULL,"
            + "totalSpend bigint NOT NULL,"
            + "totalBudget bigint NOT NULL,"
            + "errorCount bigint NOT NULL,"
            + "lastPeekUri varchar(1000) NOT NULL,"
            + "lastQueuedUri varchar(1000) NOT NULL," 
            + "UNIQUE (jobId, filterId, domainName)"
            + ")";
        DBConnect.updateTable("frontierReportMonitor", 1,
                createStatement);
    }
    
    @Override
    public void createRunningJobsHistoryTable() {
        String createStatement = "CREATE TABLE runningJobsHistory ("
                + "jobId bigint NOT NULL, "
                + "harvestName varchar(300) NOT NULL,"
                + "hostUrl varchar(300) NOT NULL,"
                + "progress numeric NOT NULL,"
                + "queuedFilesCount bigint NOT NULL,"
                + "totalQueuesCount bigint NOT NULL,"
                + "activeQueuesCount bigint NOT NULL,"
                + "exhaustedQueuesCount bigint NOT NULL,"
                + "elapsedSeconds bigint NOT NULL,"
                + "alertsCount bigint NOT NULL,"
                + "downloadedFilesCount bigint NOT NULL,"
                + "currentProcessedKBPerSec int NOT NULL,"
                + "processedKBPerSec int NOT NULL,"
                + "currentProcessedDocsPerSec numeric NOT NULL,"
                + "processedDocsPerSec numeric NOT NULL,"
                + "activeToeCount integer NOT NULL,"
                + "status int NOT NULL,"
                + "tstamp timestamp NOT NULL," 
                + "PRIMARY KEY (jobId, harvestName, elapsedSeconds, tstamp)"
                + ")";
        DBConnect.updateTable("runningJobsHistory", 1,
                createStatement);
                      
        DBUtils.executeSQL(DBConnect.getDBConnection(), 
                "CREATE INDEX runningJobsHistoryCrawlJobId on runningJobsHistory (jobId)", 
                "CREATE INDEX runningJobsHistoryCrawlTime on runningJobsHistory (elapsedSeconds)",
                "CREATE INDEX runningJobsHistoryHarvestName on runningJobsHistory (harvestName)"
                );
    }

    @Override
    public void createRunningJobsMonitorTable() {
        String createStatement = "CREATE TABLE runningJobsMonitor ("
            + "jobId bigint NOT NULL, "
            + "harvestName varchar(300) NOT NULL,"
            + "hostUrl varchar(300) NOT NULL,"
            + "progress numeric NOT NULL,"
            + "queuedFilesCount bigint NOT NULL,"
            + "totalQueuesCount bigint NOT NULL,"
            + "activeQueuesCount bigint NOT NULL,"
            + "exhaustedQueuesCount bigint NOT NULL,"
            + "elapsedSeconds bigint NOT NULL,"
            + "alertsCount bigint NOT NULL,"
            + "downloadedFilesCount bigint NOT NULL,"
            + "currentProcessedKBPerSec integer NOT NULL,"
            + "processedKBPerSec integer NOT NULL,"
            + "currentProcessedDocsPerSec numeric NOT NULL,"
            + "processedDocsPerSec numeric NOT NULL,"
            + "activeToeCount integer NOT NULL,"
            + "status integer NOT NULL,"
            + "tstamp timestamp NOT NULL, "
            + "PRIMARY KEY (jobId, harvestName)"
            + ")";
        DBConnect.updateTable("runningJobsMonitor", 1,
                createStatement);
        DBUtils.executeSQL(DBConnect.getDBConnection(), 
                "CREATE INDEX runningJobsMonitorJobId on runningJobsMonitor (jobId)",
                "CREATE INDEX runningJobsMonitorHarvestName on runningJobsMonitor (harvestName)"
        );
    }
    
    // Below DB changes introduced with development release 3.15
    // with changes to tables 'runningjobshistory', 'runningjobsmonitor',
    // 'configurations', 'fullharvests', and 'jobs'.

    /**
     * Migrates the 'runningjobshistory' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    @Override
    protected void migrateRunningJobsHistoryTableV1ToV2() {
        String[] sqlStatements = {
                "ALTER TABLE runningjobshistory "
                + "ADD COLUMN retiredQueuesCount bigint not null DEFAULT 0"
        };
        DBConnect.updateTable("runningJobsHistory", 2, sqlStatements);
    }

    /**
     * Migrates the 'runningjobsmonitor' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    @Override
    protected void migrateRunningJobsMonitorTableV1ToV2() {
        String[] sqlStatements = {
                "ALTER TABLE runningjobsmonitor "
                + "ADD COLUMN retiredQueuesCount bigint not null DEFAULT 0"
        };
        DBConnect.updateTable("runningJobsMonitor", 2, sqlStatements);
    }

    @Override
    protected void migrateConfigurationsv4tov5() { 
        // Change the maxobjects from 'int' to 'bigint'.
        // Procedure for changing the datatype of a derby table was found here:
        // https://issues.apache.org/jira/browse/DERBY-1515
        String[] SqlStatements = {
        "ALTER TABLE configurations ADD COLUMN maxobjects_new bigint NOT NULL DEFAULT -1",
        "UPDATE configurations SET maxobjects_new = maxobjects",
        "ALTER TABLE configurations DROP COLUMN maxobjects",
        "RENAME COLUMN configurations.maxobjects_new TO maxobjects"
        };
        DBConnect.updateTable("configurations", 5, SqlStatements);
}

    @Override
    protected void migrateFullharvestsv3tov4() {
        // Add new bigint field maxjobrunningtime with default 0
        String[] sqlStatements 
        = {"ALTER TABLE fullharvests ADD COLUMN maxjobrunningtime bigint NOT NULL DEFAULT 0"};
        DBConnect.updateTable("fullharvests", 4, sqlStatements);     
    }

    @Override
    protected void migrateJobsv5tov6() {
        // Add new bigint field with default 0
        String[] sqlStatements 
        = {"ALTER TABLE jobs ADD COLUMN forcemaxrunningtime bigint NOT NULL DEFAULT 0"};
        DBConnect.updateTable("jobs", 6, sqlStatements);  
    }
}    