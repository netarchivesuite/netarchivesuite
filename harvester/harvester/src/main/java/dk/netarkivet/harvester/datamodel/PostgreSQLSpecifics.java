/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DBUtils;

/**
 * PostgreSQL-specific implementation of DB methods.
 * Intended for PostgreSQL 8.3 and above.
 * 
 *  PostgreSQL does not support the CLOB datatype but instead provides a "text"
 *  data type. 
 *  See http://www.postgresql.org/docs/current/static/datatype-character.html.
 * 
 */
public class PostgreSQLSpecifics extends DBSpecifics {

    /** The log. */
    Log log = LogFactory.getLog(PostgreSQLSpecifics.class);

    /**
     * Get an instance of the PostgreSQL specifics class.
     * 
     * @return Instance of the PostgreSQL specifics class.
     */
    public static DBSpecifics getInstance() {
        return new PostgreSQLSpecifics();
    }

    /**
     * Shutdown the database system, if running embeddedly. Otherwise, this is
     * ignored.
     * 
     * Will log a warning on errors, but otherwise ignore them.
     */
    public void shutdownDatabase() {
        log.debug("Attempt to shutdown the database ignored.");
    }

    /**
     * Get a temporary table for short-time use. The table should be disposed of
     * with dropTemporaryTable. The table has two columns domain_name
     * varchar(Constants.MAX_NAME_SIZE) config_name
     * varchar(Constants.MAX_NAME_SIZE)
     * 
     * @param c
     *            The DB connection to use.
     * @throws SQLException
     *             if there is a problem getting the table.
     * @return The name of the created table
     */
    public String getJobConfigsTmpTable(Connection c) throws SQLException {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        PreparedStatement s = null;
        try {
            s = c.prepareStatement("CREATE TEMPORARY TABLE  "
                    + "jobconfignames " + "( domain_name varchar("
                    + Constants.MAX_NAME_SIZE + "), " + " config_name varchar("
                    + Constants.MAX_NAME_SIZE + ") ) ON COMMIT DROP");
            s.execute();
            s.close();
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
        return "jobconfignames";
    }

    /**
     * Dispose of a temporary table created with getTemporaryTable. This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     * 
     * @param c
     *            The DB connection to use.
     * @param tableName
     *            The name of the temporary table
     */
    public void dropJobConfigsTmpTable(Connection c, String tableName) {

    }

    /**
     * Backup the database. For server-based databases, where the administrator
     * is expected to perform the backups, this method should do nothing. This
     * method gets called within one hour of the hour-of-day indicated by the
     * DB_BACKUP_INIT_HOUR settings.
     * 
     * @param backupDir
     *            Directory to which the database should be backed up
     * @throws SQLException
     *             This will never happen
     */
    public void backupDatabase(File backupDir) throws SQLException {
        log.warn("Attempt to backup the database to directory '" + backupDir
                + "'. ignored. "
                + "Backup of the PostgreSQL database should be done by "
                + "your SysOp");
    }

    /**
     * Get the name of the JDBC driver class that handles interfacing to this
     * server.
     * 
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public boolean connectionIsValid(
            Connection connection, int validityTimeout) {
        // PostgreSQL JDBC driver does not yet implement Connection#isValid,
        // hence we send a validation request
        try {
            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery("SELECT 1");
            rs.next();
            rs.close();
            stm.close();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public String getOrderByLimitAndOffsetSubClause(long limit, long offset) {
        return "LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public boolean supportsClob() {
        return false;
    }

    /**
     * Migrates the 'jobs' table from version 3 to version 4 consisting of a
     * change of the field forcemaxbytes from int to bigint and setting its
     * default to -1. Furthermore the default value for field num_configs is set
     * to 0.
     * 
     * @throws IOFailure
     *             in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv3tov4() {
        String[] sqlStatements = {
                "ALTER TABLE jobs DROP COLUMN forcemaxbytes",
                "ALTER TABLE jobs ADD COLUMN forcemaxbytes bigint not null default -1",
                "ALTER TABLE jobs DROP COLUMN num_configs",
                "ALTER TABLE jobs ADD COLUMN num_configs int not null default 0" };
        DBConnect.updateTable("jobs", 4, sqlStatements);
    }

    /**
     * Migrates the 'jobs' table from version 4 to version 5 consisting of
     * adding new fields 'resubmitted_as_job' and 'submittedDate'.
     * 
     * @throws IOFailure
     *             in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv4tov5() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN submitteddate datetime "
                        + "AFTER enddate",
                "ALTER TABLE jobs ADD COLUMN resubmitted_as_job bigint" };
        DBConnect.updateTable("jobs", 5, sqlStatements);
    }

    /**
     * Migrates the 'configurations' table from version 3 to version 4. This
     * consists of altering the default value of field 'maxbytes' to -1.
     */
    protected synchronized void migrateConfigurationsv3ov4() {
        // Update configurations table to version 4
        String[] sqlStatements 
            = {"ALTER TABLE configurations ALTER maxbytes SET DEFAULT -1" };
        DBConnect.updateTable("configurations", 4, sqlStatements);
    }

    /**
     * Migrates the 'fullharvests' table from version 2 to version 3. This
     * consists of altering the default value of field 'maxbytes' to -1
     */
    protected synchronized void migrateFullharvestsv2tov3() {
        // Update fullharvests table to version 3
        String[] sqlStatements 
            = { "ALTER TABLE fullharvests ALTER maxbytes SET DEFAULT -1" };
        DBConnect.updateTable("fullharvests", 3, sqlStatements);
    }

    @Override
    protected void createGlobalCrawlerTrapExpressions() {
        log.warn("Please use the provided SQL scripts to update the DB schema");
        DBConnect.updateTable("global_crawler_trap_lists", 1);
    }

    @Override
    protected void createGlobalCrawlerTrapLists() {
        log.warn("Please use the provided SQL scripts to update the DB schema");
        DBConnect.updateTable("global_crawler_trap_expressions", 1);
    }

}
