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
 * MySQL-specific implementation of DB methods.
 */
public class MySQLSpecifics extends DBSpecifics {
    /** The log. */
    Log log = LogFactory.getLog(MySQLSpecifics.class);
    
    /**
     * Get an instance of the MySQL specifics class.
     * @return Instance of the MySQL specifics class.
     */
    public static DBSpecifics getInstance() {
        return new MySQLSpecifics();
    }
    
    /** Get a temporary table for short-time use.  The table should be
     * disposed of with dropTemporaryTable.  The table has two columns
     * domain_name varchar(Constants.MAX_NAME_SIZE)
     * config_name varchar(Constants.MAX_NAME_SIZE)
     *
     * @param c The DB connection to use.
     * @throws SQLException if there is a problem getting the table.
     * @return The name of the created table
     */
    public String getJobConfigsTmpTable(Connection c) throws SQLException {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("CREATE TEMPORARY TABLE  "
                    + "jobconfignames "
                    + "( domain_name varchar(" + Constants.MAX_NAME_SIZE + "), "
                    + " config_name varchar(" + Constants.MAX_NAME_SIZE 
                    + ") )");
            s.execute();
            s.close();
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
        return "jobconfignames";
    }

    /**
     * Dispose of a temporary table created with getTemporaryTable.  This can be
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
            s = c.prepareStatement("DROP TEMPORARY TABLE " +  tableName);
            s.execute();
        } catch (SQLException e) {
            log.warn("Couldn't drop temporary table " + tableName + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }
        
    /** Migrates the 'jobs' table from version 3 to version 4
     * consisting of a change of the field forcemaxbytes from int to bigint
     * and setting its default to -1.
     * Furthermore the default value for field num_configs is set to 0.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv3tov4() {
        String[] sqlStatements = {
            "ALTER TABLE jobs CHANGE COLUMN forcemaxbytes forcemaxbytes"
            + " bigint not null default -1",
            "ALTER TABLE jobs CHANGE COLUMN num_configs num_configs"
            + " int not null default 0"
        };
        DBConnect.updateTable("jobs", 4, sqlStatements);
    }
    
    /** Migrates the 'jobs' table from version 4 to version 5
     * consisting of adding new fields 'resubmitted_as_job' and 'submittedDate'.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv4tov5() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN submitteddate datetime "
                    + "AFTER enddate",
                "ALTER TABLE jobs ADD COLUMN resubmitted_as_job bigint"    
            };
        DBConnect.updateTable("jobs", 5, sqlStatements);
    }
    
    /** Migrates the 'configurations' table from version 3 to version 4.
     * This consists of altering the default value of field 'maxbytes' to -1.
     */
    protected synchronized void migrateConfigurationsv3ov4() {
     // Update configurations table to version 4
        String[] sqlStatements = {
                "ALTER TABLE configurations ALTER maxbytes SET DEFAULT -1"
            };
        DBConnect.updateTable("configurations", 4, sqlStatements);
    }
 
    /** Migrates the 'fullharvests' table from version 2 to version 3.
     * This consists of altering the default value of field 'maxbytes' to -1
     */
    protected synchronized void migrateFullharvestsv2tov3() {
        // Update fullharvests table to version 3
        String[] sqlStatements = {
                "ALTER TABLE fullharvests ALTER maxbytes SET DEFAULT -1"
        };
        DBConnect.updateTable("fullharvests", 3, sqlStatements);
    }

    /** Creates the initial (version 1) of table 'global_crawler_trap_lists'. */
    protected void createGlobalCrawlerTrapLists() {
        String createStatement = "CREATE TABLE global_crawler_trap_lists(\n"
                                 + "  global_crawler_trap_list_id INT NOT NULL "
                                 + "AUTO_INCREMENT PRIMARY KEY,\n"
                                 + "  name VARCHAR(300) NOT NULL UNIQUE, "
                                 + "  description VARCHAR(20000), "
                                 + "  isActive INT NOT NULL )";
        DBConnect.updateTable("global_crawler_trap_lists", 1, createStatement);
    }

    /** Creates the initial (version 1) of 
     * table 'global_crawler_trap_expressions'. */
    protected void createGlobalCrawlerTrapExpressions() {
        String createStatement = "CREATE TABLE global_crawler_trap_expressions("
                                 + "    id bigint not null AUTO_INCREMENT "
                                 + "primary key,"
                                 + "    crawler_trap_list_id INT NOT NULL, "
                                 + "    trap_expression VARCHAR(1000) )";
        DBConnect.updateTable("global_crawler_trap_expressions", 1,
                              createStatement);
    }
    
    @Override
    public boolean connectionIsValid(Connection connection, int validityTimeout)
            throws SQLException {
        return connection.isValid(validityTimeout);
    }

    @Override
    public boolean supportsClob() {
        return true;
    }

    @Override
    public String getOrderByLimitAndOffsetSubClause(long limit, long offset) {
        return "LIMIT " + offset + ", " + limit;
    }

    @Override
    public void createFrontierReportMonitorTable() {
        String createStatement = "CREATE TABLE frontierReportMonitor ("
             + "jobId bigint NOT NULL,"
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
             // NB see http://bugs.mysql.com/bug.php?id=6604 about index key length.
             + "UNIQUE (jobId, filterId(100), domainName(100))" 
             + ")";
        DBConnect.updateTable("frontierReportMonitor", 1, createStatement);

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
             + "status integer NOT NULL,"
             + "tstamp timestamp NOT NULL, "
             + "PRIMARY KEY (jobId, harvestName, elapsedSeconds, tstamp)"
             + ")";
        DBConnect.updateTable("runningJobsHistory", 1, createStatement);
        
        DBUtils.executeSQL(DBConnect.getDBConnection(), 
                "CREATE INDEX runningJobsHistoryCrawlJobId on runningJobsHistory (jobId)", 
                "CREATE INDEX runningJobsHistoryCrawlTime on runningJobsHistory (elapsedSeconds)",
                "CREATE INDEX runningJobsHistoryHarvestName on runningJobsHistory (harvestName)",
                "GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE runningJobsHistory TO netarchivesuite"
                );
    }

    @Override
    public void createRunningJobsMonitorTable() {
        String createStatement = "CREATE TABLE runningJobsMonitor ("
             + "jobId bigint NOT NULL,"
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
             + "tstamp timestamp NOT NULL,"
             + "PRIMARY KEY (jobId, harvestName)"
             + ")";
        DBConnect.updateTable("runningJobsMonitor", 1, createStatement);
        DBUtils.executeSQL(DBConnect.getDBConnection(), 
                "CREATE INDEX runningJobsMonitorJobId on runningJobsMonitor (jobId)",
                "CREATE INDEX runningJobsMonitorHarvestName on runningJobsMonitor (harvestName)"
                );
    }

    
}
