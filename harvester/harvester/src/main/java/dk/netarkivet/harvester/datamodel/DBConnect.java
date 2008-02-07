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

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.StringUtils;


/**
 * Logic to connect with the harvest definition database.
 * This also defines basic logic for checking versions of tables.
 *
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */

public class DBConnect {

    private static Map<Thread, Connection> connectionPool
            = new WeakHashMap<Thread, Connection>();
    private static final Log log = LogFactory.getLog(DBConnect.class);

    /**
     * Get a connection to our database.
     * Assumes that AutoCommit is true.
     * @return a connection to our database
     * @throws IOFailure if we cannot connect to the database (or find the
     * driver).
     */
    static Connection getDBConnection() {
        if (connectionPool.get(Thread.currentThread()) == null) {
            try {
                Class.forName(DBSpecifics.getInstance().getDriverClassName());
                Connection connection = DriverManager.getConnection(
                        Settings.get(Settings.DB_URL));
                connectionPool.put(Thread.currentThread(), connection);
                log.info("Connected to database using DBurl '"
                        + Settings.get(Settings.DB_URL) + "'  using driver '"
                        + DBSpecifics.getInstance().getDriverClassName() + "'");
            } catch (ClassNotFoundException e) {
                final String message = "Can't find driver '"
                        + DBSpecifics.getInstance().getDriverClassName() + "'";
                log.warn(message, e);
                throw new IOFailure(message, e);
            } catch (SQLException e) {
                final String message = "Can't connect to database with DBurl: '"
                        + Settings.get(Settings.DB_URL) + "' using driver '"
                        + DBSpecifics.getInstance().getDriverClassName() + "'";
                log.warn(message, e);
                throw new IOFailure(message, e);
            }
        }
        return connectionPool.get(Thread.currentThread());
    }

    /** Execute an SQL statement and return the single int in the result set.
     *
     * @param s A prepared statement
     * @return The integer result, or null if the result value was null.
     * @throws IOFailure if the statement didn't result in exactly one integer,
     */
    public static Integer selectIntValue(PreparedStatement s) {
        try {
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new IOFailure("No results from " + s);
            }
            Integer resultInt = res.getInt(1);
            if (res.wasNull()) {
                resultInt = null;
            }
            if (res.next()) {
                throw new IOFailure("Too many results from " + s);
            }
            return resultInt;
        } catch (SQLException e) {
            throw new IOFailure("SQL error executing statement " + s, e);
        }
    }

    /** Execute an SQL statement and return the single int in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     *
     * @param query a query with ? for parameters
     * @param args parameters of type string, int, long or boolean
     * @return The integer result
     * @throws IOFailure if the statement didn't result in exactly one integer
     */
    public static Integer selectIntValue(String query, Object... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            // We do not test for 0-values here, already tested in selectIntValue(s)
            return selectIntValue(s);
        } catch (SQLException e) {
            throw new IOFailure("SQL error preparing statement " + query + " args " + args, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Execute an SQL statement and return the single long in the result set.
     *
     * @param s A prepared statement
     * @return The long result, or null if the result was a null value
     * Note that a null value is not the same as no result rows.
     * @throws IOFailure if the statement didn't result in exactly one row with
     * a long or null value
     */
    public static Long selectLongValue(PreparedStatement s) {
        try {
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new IOFailure("No results from " + s);
            }
            Long resultLong = res.getLong(1);
            if (res.wasNull()) {
                resultLong = null;
            }
            if (res.next()) {
                throw new IOFailure("Too many results from " + s);
            }
            return resultLong;
        } catch (SQLException e) {
            throw new IOFailure("SQL error executing statement " + s, e);
        }
    }
    /** Execute an SQL statement and return the single long in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     *
     * @param query a query with ? for parameters
     * @param args parameters of type string, int, long or boolean
     * @return The long result
     * @throws IOFailure if the statement didn't result in exactly one long value
     */
    public static Long selectLongValue(String query,
                                       Object... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            // We do not test for 0-values here, already tested in selectLongValue(s)
            return selectLongValue(s);
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query + " args " + args, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Execute an SQL statement and return the first long in the result set,
     * or null if resultset is empty.
     *
     * @param query a query with ? for parameters
     * @param args parameters of type string, int, long or boolean
     * @return The long result, or will return null in one of the two following
     * cases: There is no results, or the first result is a null-value.
     * @throws IOFailure on SQL errors.
     */
    public static Long selectFirstLongValueIfAny(String query, Object... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return getLongMaybeNull(rs, 1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            String message = "SQL error executing '" + query + "'";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Prepare a statement given a query string and some args.
     * @param c a Database connection
     * @param query a query string
     * @param args some args to insert into this query string
     * @return a prepared statement
     * @throws SQLException
     */
    private static PreparedStatement prepareStatement(Connection c, String query, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(query);
        int i = 1;
        for (Object arg : args) {
            if (arg instanceof String) {
                s.setString(i, (String)arg);
            } else if (arg instanceof Integer) {
                s.setInt(i, (Integer)arg);
            } else if (arg instanceof Long) {
                s.setLong(i, (Long)arg);
            } else if (arg instanceof Boolean) {
                s.setBoolean(i, (Boolean)arg);
            } else if (arg instanceof Date) {
                s.setTimestamp(i, new Timestamp(((Date)arg).getTime()));
            } else {
                throw new ArgumentNotValid("Cannot handle type '"
                        + arg.getClass().getName() + "'. We can only handle string, "
                        + "int, long, date or boolean args for query: " + query);
            }
            i++;
        }
        return s;
    }

    /** Execute an SQL statement and return the list of strings in its result set.
     *  @param query the given sql-query
     *  @param args The arguments to insert into this query
     *  @throws SQLException If this query fails
     *  @return the list of strings in its result set
     */
    public static List<String> selectStringlist(String query,
                                                Object... args) throws SQLException {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            ResultSet result = s.executeQuery();
            List<String> results = new ArrayList<String>();
            while (result.next()) {
                if (result.getString(1) == null){
                    String warning = "NULL pointer found in resultSet from query: " + query;
                    log.warn(warning);
                    throw new IOFailure(warning);
                }
                results.add(result.getString(1));
            }
            return results;
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Execute an SQL statement and return the list of strings -> id mappings
     * in its result set.
     * @param query the given sql-query
     * @param args The arguments to insert into this query
     * @throws SQLException If this query fails
     * @return the list of strings -> id mappings
     */
    public static Map<String,Long> selectStringLongMap(String query,
                                                       Object... args)
            throws SQLException {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            ResultSet result = s.executeQuery();
            Map<String,Long> results = new HashMap<String,Long>();
            while (result.next()) {
                String resultString = result.getString(1);
                long resultLong = result.getLong(2);
                if ((resultString == null)
                        || (resultLong == 0L && result.wasNull())) {
                    String warning = "NULL pointers found in entry ("
                        + resultString + "," + resultLong
                        + ") in resultset from query: " + query;
                    log.warn(warning);
                    //throw new IOFailure(warning);
                }
                results.put(resultString, resultLong);
            }
            return results;
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Execute an SQL statement and return the list of Long-objects in its result set.
     *  @param query the given sql-query
     *  @param args The arguments to insert into this query
     *  @return the list of Long-objects in its resultset
     *  @throws SQLException If this query fails
     */
    public static List<Long> selectLongList(String query,
                                            Object... args) throws SQLException {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            ResultSet result = s.executeQuery();
            List<Long> results = new ArrayList<Long>();
            while (result.next()) {
                if (result.getLong(1) == 0L && result.wasNull()){
                    String warning = "NULL value encountered in query: "
                                     + query;
                    log.warn(warning);
                    //throw new IOFailure(warning);
                }
                results.add(result.getLong(1));
            }
            return results;
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Get the automatically generated key that was created with the
     * just-executed statement.
     *
     * @param s A statement created with Statement.RETURN_GENERATED_KEYS
     * @return The single generated key
     * @throws SQLException
     */
    public static long getGeneratedID(PreparedStatement s) throws SQLException {
        ResultSet res = s.getGeneratedKeys();
        if (!res.next()) {
            throw new IOFailure("No keys generated by " + s);
        }
        return res.getLong(1);
    }

    /** Returns the version of a table according to schemaversions, or 0
     * for the initial, unnumbered version.
     *
     *
     * @param tablename The name of a table in the database.
     * @return Version of the given table.
     * @throws IOFailure if DB table schemaversions does not exist
     */
    public static int getTableVersion(String tablename) throws IOFailure {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        int version = 0;
        try {
            s = c.prepareStatement("SELECT version FROM schemaversions"
                    + " WHERE tablename = ?");
            s.setString(1, tablename);
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                log.warn("Unknown table '" + tablename + "'");
            } else {
                version = res.getInt(1);
                if (res.wasNull()) {
                    log.warn("Null table version for '" + tablename + "'");
                }
            }
            return version;
        } catch (SQLException e) {
            String msg = "SQL Error checking version of table " + tablename;
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Close a statement, if not closed already
     * Note: This does not throw any a SQLException, because
     * it is always called inside a finally-clause.
     * Exceptions are logged as warnings, though.
     * @param s a statement
     */
    static void closeStatementIfOpen(PreparedStatement s) {
        if (s != null) {
            try {
                s.close();
            } catch (SQLException e) {
                log.warn("Error closing SQL statement " + s, e);
            }
        }
    }
    /**
     * Set String Max Length.
     * If contents.length() > maxSize, contents is truncated to contain
     * the first maxSize characters of the contents, and a warning is logged.
     * @param s a Prepared Statement
     * @param fieldNum a index into the above statement
     * @param contents the contents
     * @param maxSize the maximum size of field: fieldName
     * @param o the Object, which assumedly have a field named fieldName
     * @param fieldname the name of a given field
     * @throws SQLException if set operation fails
     */
    static void setStringMaxLength(PreparedStatement s, int fieldNum,
                                           String contents, int maxSize,
                                           Object o, String fieldname)
            throws SQLException {
        if (contents != null) {
            if (contents.length() > maxSize) {
            	log.warn(fieldname + " of " + o
                        + " is longer than the allowed " + maxSize
                        + " characters. The contents is truncated to length " + maxSize
                        +		". The untruncated contents was: " + contents);
            	// truncate to length maxSize
            	contents = contents.substring(0, maxSize);
            }
            s.setString(fieldNum, contents);
        } else {
            s.setNull(fieldNum, Types.VARCHAR);
        }
    }

    /** Set the name of a Named object into the given field.
     * @param s a prepared statement
     * @param fieldNum the index of the given field to be set
     * @param o the Named object
     * @throws SQLException
     * @throws PermissionDenied If length of o.getName() is larger than
     * Constants.MAX_NAME_SIZE
     */
    static void setName(PreparedStatement s, int fieldNum, Named o)
            throws SQLException {
    	if (o.getName().length() > Constants.MAX_NAME_SIZE) {
    		throw new PermissionDenied("Length of name ("
    				+ o.getName().length()
    				+ ") is larger than allowed. Max length is "
    				+ Constants.MAX_NAME_SIZE);
    	}
        setStringMaxLength(s, fieldNum, o.getName(), Constants.MAX_NAME_SIZE,
                o, "name");
    }

    /** Set the comments of a Named object into the given field of statement.
     *
     * @param s a prepared statement
     * @param fieldNum the index of the given field to be set
     * @param o the Named object
     * @throws SQLException
     * @throws PermissionDenied If length of o.getComments() is larger than
     * Constants.MAX_COMMENT_SIZE
     */
    static void setComments(PreparedStatement s, int fieldNum, Named o)
            throws SQLException {
    	if (o.getComments().length() > Constants.MAX_COMMENT_SIZE) {
    		throw new PermissionDenied("Length of comments ("
    				+ o.getComments().length()
    				+ ") is larger than allowed. Max length is "
    				+ Constants.MAX_COMMENT_SIZE);
    	}
        setStringMaxLength(s, fieldNum, o.getComments(), Constants.MAX_COMMENT_SIZE,
                o, "comments");
    }
    /** Set the Date into the given field of a statement.
     *
     * @param s a prepared statement
     * @param fieldNum the index of the given field to be set
     * @param date the date (may be null)
     * @throws SQLException
     */
    static void setDateMaybeNull(PreparedStatement s, int fieldNum, Date date)
            throws SQLException {
        if (date != null) {
            s.setTimestamp(fieldNum, new Timestamp(date.getTime()));
        } else {
            s.setNull(fieldNum, Types.DATE);
        }
    }
    /**
     * Get a Date from a column in the resultset.
     * Returns null, if the value in the column is NULL.
     * @param rs the resultSet
     * @param columnIndex The given column, where the Date resides
     * @return a Date from a column in the resultset
     * @throws SQLException
     */
    static Date getDateMaybeNull(ResultSet rs, final int columnIndex)
    throws SQLException {
        final Timestamp startTimestamp = rs.getTimestamp(columnIndex);
        if (rs.wasNull()) {
            return null;
        }
        Date startdate;
        if (startTimestamp != null) {
            startdate = new Date(startTimestamp.getTime());
        } else {
            startdate = null;
        }
        return startdate;
    }

    /**
     * Method to perform a rollback of complex DB updates.  If no commit has
     * been performed, this will undo the entire transaction, otherwise
     * nothing will happen.  This should be called in a finally block with
     * no DB updates after the last commit.
     * Thus exceptions while closing are ignored, but logged as warnings.
     *
     * @param c the db-connection
     * @param action The action going on, before calling this method
     * @param o The being acted upon by this action
     */
    static void rollbackIfNeeded(Connection c,
                                 String action, Object o) {
        try {
            c.rollback();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            log.warn("SQL error doing rollback after " + action + " " + o, e);
            // Can't throw here, we want the real exception
        }
    }

    /**
     * Set the CLOB maxlength.
     * If contents.length() > maxSize, contents is truncated to contain
     * the first maxSize characters of the contents, and a warning is logged.
     * @param s a prepared statement
     * @param fieldNum the field-index, where the contents are inserted
     * @param contents the contents
     * @param maxSize the maxsize for this contents
     * @param o the Object, which assumedly have a field named fieldName
     * @param fieldName a given field (Assumedly in Object o)
     * @throws SQLException
     */
    public static void setClobMaxLength(PreparedStatement s, int fieldNum,
                                        String contents, long maxSize,
                                        Object o, String fieldName)
            throws SQLException {
        if (contents != null) {
            if (contents.length() > maxSize) {
            	log.warn(fieldName + " of " + o
                        + " is longer than the allowed " + maxSize
                        + " characters. The contents is now truncated to length " + maxSize
                        +		". The untruncated contents was: " + contents);
               	// truncate to length maxSize (if maxSize <= Integer.MAX_VALUE)
            	// else truncate to length Integer.MAX_VALUE
            	if (maxSize > Integer.MAX_VALUE) {
            		maxSize = Integer.MAX_VALUE;
            	}
            	contents = contents.substring(0, (int) maxSize);
            }
            s.setCharacterStream(fieldNum, new StringReader(contents),
                    contents.length());
            s.setString(fieldNum, contents);
        } else {
            s.setNull(fieldNum, Types.CLOB);
        }
    }

    /**
     * Insert a long value (which could be null) into
     * the given field of a statement.
     * @param s a prepared Statement
     * @param i the number of a given field in the prepared statement
     * @param value the long value to insert (maybe null)
     * @throws SQLException
     */
    public static void setLongMaybeNull(PreparedStatement s, int i,
                                        Long value) throws SQLException {
        if (value != null) {
            s.setLong(i, value);
        } else {
            s.setNull(i, Types.BIGINT);
        }
    }

    /**
     * Insert an Integer in prepared statement.
     * @param s a prepared statement
     * @param i the index of the statement, where the Integer should be inserted
     * @param value The Integer to insert
     * @throws SQLException
     */
    public static void setIntegerMaybeNull(PreparedStatement s, int i,
                                        Integer value) throws SQLException {
        if (value != null) {
            s.setInt(i, value);
        } else {
            s.setNull(i, Types.INTEGER);
        }
    }

    /**
     * Get an Integer from the resultSet in column i.
     * @param rs the resultset
     * @param i the column where the wanted Integer resides
     * @return an Integer object located in column i in the resultset
     * @throws SQLException
     */
    public static Integer getIntegerMaybeNull(ResultSet rs, int i)
            throws SQLException {
        Integer res = rs.getInt(i);
        if (rs.wasNull()) {
            return null;
        }
        return res;
    }

    /**
     * Get a Long from the resultSet in column i.
     * @param rs the resultset
     * @param i the column where the wanted Long resides
     * @return a Long object located in column i in the resultset
     * @throws SQLException
     */
    public static Long getLongMaybeNull(ResultSet rs, int i)
            throws SQLException {
        Long res = rs.getLong(i);
        if (rs.wasNull()) {
            return null;
        }
        return res;
    }

    /** Check whether an object is used otherwhere in the database.
     *
     * @param select A select statement finding the names of other uses.  The
     * statement should result in exactly one column of string values.
     * @param victim The object being used.
     * @param args Any objects that may be used to prepare the select statement.
     * @throws PermissionDenied if the object has usages.
     */
    public static void checkForUses(String select, Object victim, Object... args) {
        String usages = getUsages(select, victim, args);
        if (usages != null) {
            String message = "Cannot delete " + victim
                + " as it is used in " + usages;
            throw new PermissionDenied(message);
        }
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, select, args);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                List<String> used_in = new ArrayList<String>();
                do {
                    used_in.add(res.getString(1));
                } while (res.next());
            }
        } catch (SQLException e) {
            final String message = "SQL error deleting " + victim;
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Return a description of where an object is used otherwhere in the
     * database, or null.
     *
     * @param select A select statement finding the names of other uses.  The
     * statement should result in exactly one column of string values.
     * @param victim The object being used.
     * @param args Any objects that may be used to prepare the select statement.
     * @return A string describing the usages, or null if no usages were found.
     */
    public static String getUsages(String select, Object victim, Object ... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, select, args);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                List<String> used_in = new ArrayList<String>();
                do {
                    used_in.add(res.getString(1));
                } while (res.next());
                return used_in.toString();
            }
            return null;
        } catch (SQLException e) {
            final String message = "SQL error checking for usages of " + victim;
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Check that a database table is of the expected version.
     *
     * @param tablename The table to check.
     * @param desiredVersion The version it should be.
     * @throws IOFailure if the version isn't as expected.
     */
    public static void checkTableVersion(String tablename, int desiredVersion) {
        int actualVersion = getTableVersion(tablename);
        if (actualVersion != desiredVersion) {
            String message = "Wrong table version for '" + tablename
                    + "': Should be " + desiredVersion
                    + ", but is " + actualVersion;
            log.warn(message);
            throw new IOFailure(message);
        }
    }

    /** Execute an SQL statement and return the single string in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     *
     * @param query a query with ? for parameters
     * @param args parameters of type string, int, long or boolean
     * @return The string result
     * @throws IOFailure if the statement didn't result in exactly one string value
     */
    public static String selectStringValue(String query, Object... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            // We do not test for null-values here, already tested in
            // selectStringValue(s)
            return selectStringValue(s);
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + args, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Execute an SQL statement and return the single string in the result set.
     *
     * @param s A prepared statement
     * @return The string result, or null if the result was a null value
     * Note that a null value is not the same as no result rows.
     * @throws IOFailure if the statement didn't result in exactly one row with
     * a string or null value
     */
    private static String selectStringValue(PreparedStatement s) {
        try {
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new IOFailure("No results from " + s);
            }
            String resultString = res.getString(1);
            if (res.wasNull()) {
                resultString = null;
            }
            if (res.next()) {
                throw new IOFailure("Too many results from " + s);
            }
            return resultString;
        } catch (SQLException e) {
            throw new IOFailure("SQL error executing statement " + s, e);
        }
    }

    /** Execute an SQL query and return whether the result contains any rows.
     *
     * @param query a query with ? for parameters
     * @param args parameters of type string, int, long or boolean
     * @return True if executing the query resulted in at least one row.
     * @throws IOFailure if there were problems with the SQL query
     */
    public static boolean selectAny(String query, Object... args) {
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            s = prepareStatement(c, query, args);
            return s.executeQuery().next();
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + args, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /** Update a table by executing all the statements in
     *  the updates String array.
     *
     * @param table The table to update
     * @param newVersion The version that the table should end up at
     * @param updates The SQL update statements that makes the necessary
     * updates.
     */
    protected static void updateTable(final String table,
                                      final int newVersion,
                                      final String... updates) {
        if (true) {
            throw new IllegalStateException("Should have updated table");
        }
        Connection c = getDBConnection();
        PreparedStatement s = null;
        try {
            c.setAutoCommit(false);
            for (String update : updates) {
                s = prepareStatement(c, update);
                s.executeUpdate();
                s.close();
            }
            s = c.prepareStatement("UPDATE schemaversions SET version = ? WHERE tablename = ?");
            s.setInt(1, newVersion);
            s.setString(2, table);
            s.executeUpdate();
            c.setAutoCommit(true);
            log.info("Updated " + table + " to version " + newVersion
                    + " using updates '" + StringUtils.conjoin(";", updates)
                    + "'.");
        } catch (SQLException e) {
            String msg = "SQL error updating " + table + " table to version "
                    + newVersion;
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            rollbackIfNeeded(c, "updating table", table);
            closeStatementIfOpen(s);
        }
    }

    /** Translate a "normal" glob (with * and .) into SQL syntax.
     *
     * @param glob A shell-like glob string
     * @return A string that implements glob in SQL "LIKE" constructs.
     */
    static String makeSQLGlob(String glob) {
        return glob.replace("*", "%").replace("?", "_");
    }
}
