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

package dk.netarkivet.common.utils;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.datamodel.DBSpecifics;

/**
 * Various database related utilities.
 * 
 */
public class DBUtils {

    /** The logger. */
    private static final Log log = LogFactory.getLog(DBUtils.class);

    /**
     * Execute an SQL statement and return the single integer in the result set.
     * 
     * @param s
     *            A prepared statement
     * @return The integer result, or null if the result value was null.
     * @throws IOFailure
     *             if the statement didn't result in exactly one integer.
     */
    public static Integer selectIntValue(PreparedStatement s) {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
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
            throw new IOFailure("SQL error executing statement " + s + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Execute an SQL statement and return the single int in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     * 
     * @param connection
     *            connection to database.
     * @param query
     *            a query with ? for parameters (must not be null or empty
     *            string)
     * @param args
     *            parameters of type string, int, long or boolean
     * @return The integer result
     * @throws IOFailure
     *             if the statement didn't result in exactly one integer
     */
    public static Integer selectIntValue(Connection connection, String query,
            Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = DBUtils.prepareStatement(connection, query, args);
            // We do not test for 0-values here, already tested in
            // selectIntValue(s)
            return selectIntValue(s);
        } catch (SQLException e) {
            throw new IOFailure("SQL error preparing statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Execute an SQL statement and return the single long in the result set.
     * 
     * @param s
     *            A prepared statement
     * @return The long result, or null if the result was a null value Note that
     *         a null value is not the same as no result rows.
     * @throws IOFailure
     *             if the statement didn't result in exactly one row with a long
     *             or null value
     */
    public static Long selectLongValue(PreparedStatement s) {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
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
            throw new IOFailure("SQL error executing statement " + s + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Execute an SQL statement and return the single long in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     * 
     * @param connection
     *            connection to database.
     * @param query
     *            a query with ? for parameters (must not be null or empty
     *            string)
     * @param args
     *            parameters of type string, int, long or boolean
     * @return The long result
     * @throws IOFailure
     *             if the statement didn't result in exactly one long value
     */
    public static Long selectLongValue(Connection connection, String query,
            Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = DBUtils.prepareStatement(connection, query, args);
            // We do not test for 0-values here, already tested in
            // selectLongValue(s)
            return selectLongValue(s);
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Execute an SQL statement and return the first long in the result set, or
     * null if resultset is empty.
     * 
     * @param connection
     *            connection to database.
     * @param query
     *            a query with ? for parameters (must not be null or empty
     *            string)
     * @param args
     *            parameters of type string, int, long or boolean
     * @return The long result, or will return null in one of the two following
     *         cases: There is no results, or the first result is a null-value.
     * @throws IOFailure
     *             on SQL errors.
     */
    public static Long selectFirstLongValueIfAny(Connection connection,
            String query, Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = DBUtils.prepareStatement(connection, query, args);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return DBUtils.getLongMaybeNull(rs, 1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            String message = "SQL error executing '" + query + "'" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Prepare a statement given a query string and some args.
     * 
     * @param c
     *            a Database connection
     * @param query
     *            a query string (must not be null or empty)
     * @param args
     *            some args to insert into this query string (must not be null)
     * @return a prepared statement
     * @throws SQLException
     *             If unable to prepare a statement
     * @throws ArgumentNotValid
     *             If unable to handle type of one the args, or the arguments
     *             are either null or an empty String.
     */
    public static PreparedStatement prepareStatement(Connection c,
            String query, Object... args) throws SQLException {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = c.prepareStatement(query);
        int i = 1;
        for (Object arg : args) {
            if (arg instanceof String) {
                s.setString(i, (String) arg);
            } else if (arg instanceof Integer) {
                s.setInt(i, (Integer) arg);
            } else if (arg instanceof Long) {
                s.setLong(i, (Long) arg);
            } else if (arg instanceof Boolean) {
                s.setBoolean(i, (Boolean) arg);
            } else if (arg instanceof Date) {
                s.setTimestamp(i, new Timestamp(((Date) arg).getTime()));
            } else {
                throw new ArgumentNotValid("Cannot handle type '"
                        + arg.getClass().getName()
                        + "'. We can only handle string, "
                        + "int, long, date or boolean args for query: "
                        + query);
            }
            i++;
        }
        return s;
    }

    /**
     * Execute an SQL statement and return the list of strings in its result
     * set. This uses specifically the harvester database.
     * 
     * @param connection
     *            connection to the database.
     * @param query
     *            the given sql-query (must not be null or empty)
     * @param args
     *            The arguments to insert into this query (must not be null)
     * @throws IOFailure
     *             If this query fails
     * @return the list of strings in its result set
     */
    public static List<String> selectStringList(Connection connection,
            String query, Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, query, args);
            ResultSet result = s.executeQuery();
            List<String> results = new ArrayList<String>();
            while (result.next()) {
                if (result.getString(1) == null) {
                    String warning
                        = "NULL pointer found in resultSet from query: "
                            + query;
                    log.warn(warning);
                    throw new IOFailure(warning);
                }
                results.add(result.getString(1));
            }
            return results;
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Execute an SQL statement and return the list of strings -> id mappings in
     * its result set.
     * 
     * @param connection
     *            connection to the database.
     * @param query
     *            the given sql-query (must not be null or empty string)
     * @param args
     *            The arguments to insert into this query
     * @throws SQLException
     *             If this query fails
     * @return the list of strings -> id mappings
     */
    public static Map<String, Long> selectStringLongMap(Connection connection,
            String query, Object... args) throws SQLException {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, query, args);
            ResultSet result = s.executeQuery();
            Map<String, Long> results = new HashMap<String, Long>();
            while (result.next()) {
                String resultString = result.getString(1);
                long resultLong = result.getLong(2);
                if ((resultString == null)
                        || (resultLong == 0L && result.wasNull())) {
                    String warning = "NULL pointers found in entry ("
                            + resultString + "," + resultLong
                            + ") in resultset from query: " + query;
                    log.warn(warning);
                    // throw new IOFailure(warning);
                }
                results.put(resultString, resultLong);
            }
            return results;
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Execute an SQL statement and return the list of Long-objects in its
     * result set.
     * 
     * @param connection
     *            connection to the database.
     * @param query
     *            the given sql-query (must not be null or empty string)
     * @param args
     *            The arguments to insert into this query
     * @return the list of Long-objects in its resultset
     */
    public static List<Long> selectLongList(Connection connection,
            String query, Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, query, args);
            ResultSet result = s.executeQuery();
            List<Long> results = new ArrayList<Long>();
            while (result.next()) {
                if (result.getLong(1) == 0L && result.wasNull()) {
                    String warning = "NULL value encountered in query: "
                            + query;
                    log.warn(warning);
                    // throw new IOFailure(warning);
                }
                results.add(result.getLong(1));
            }
            return results;
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Get the automatically generated key that was created with the
     * just-executed statement.
     * 
     * @param s
     *            A statement created with Statement.RETURN_GENERATED_KEYS
     * @return The single generated key
     * @throws SQLException
     *             If a database access error occurs or the PreparedStatement is
     *             closed, or the JDBC driver does not support the
     *             setGeneratedKeys() method
     */
    public static long getGeneratedID(PreparedStatement s) throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        ResultSet res = s.getGeneratedKeys();
        if (!res.next()) {
            throw new IOFailure("No keys generated by " + s);
        }
        return res.getLong(1);
    }

    /**
     * Returns the version of a table according to schemaversions, or 0 for the
     * initial, unnumbered version.
     * 
     * @param connection
     *            connection to the database.
     * @param tablename
     *            The name of a table in the database.
     * @return Version of the given table.
     * @throws IOFailure
     *             if DB table schemaversions does not exist
     */
    public static int getTableVersion(Connection connection, String tablename)
            throws IOFailure {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(tablename, "String tablenname");
        PreparedStatement s = null;
        int version = 0;
        try {
            s = connection
                    .prepareStatement("SELECT version FROM schemaversions"
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
            String msg = "SQL Error checking version of table " + tablename
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Close a statement, if not closed already Note: This does not throw any a
     * SQLException, because it is always called inside a finally-clause.
     * Exceptions are logged as warnings, though.
     * 
     * @param s
     *            a statement
     */
    public static void closeStatementIfOpen(PreparedStatement s) {
        if (s != null) {
            try {
                s.close();
            } catch (SQLException e) {
                log.warn("Error closing SQL statement " + s + "\n"
                        + ExceptionUtils.getSQLExceptionCause(e), e);
            }
        }
    }

    /**
     * Set String Max Length. If contents.length() > maxSize, contents is
     * truncated to contain the first maxSize characters of the contents, and a
     * warning is logged.
     * 
     * @param s
     *            a Prepared Statement
     * @param fieldNum
     *            a index into the above statement
     * @param contents
     *            the contents
     * @param maxSize
     *            the maximum size of field: fieldName
     * @param o
     *            the Object, which assumedly have a field named fieldName
     * @param fieldname
     *            the name of a given field
     * @throws SQLException
     *             if set operation fails
     */
    public static void setStringMaxLength(PreparedStatement s, int fieldNum,
            String contents, int maxSize, Object o, String fieldname)
            throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        ArgumentNotValid.checkNotNegative(fieldNum, "int fieldNum");

        if (contents != null) {
            if (contents.length() > maxSize) {
                log.warn(fieldname + " of " + o
                        + " is longer than the allowed " + maxSize
                        + " characters. The contents is truncated to length "
                        + maxSize + ". The untruncated contents was: "
                        + contents);
                // truncate to length maxSize
                contents = contents.substring(0, maxSize);
            }
            s.setString(fieldNum, contents);
        } else {
            s.setNull(fieldNum, Types.VARCHAR);
        }
    }

    /**
     * Set the comments of a Named object into the given field of statement.
     * 
     * @param s
     *            a prepared statement
     * @param fieldNum
     *            the index of the given field to be set
     * @param o
     *            the Named object
     * @param maxFieldSize
     *            max size of the comments field
     * @throws SQLException If SQL problems occur during the operation
     * @throws PermissionDenied
     *             If length of o.getComments() is larger than
     *             Constants.MAX_COMMENT_SIZE
     */
    public static void setComments(PreparedStatement s, int fieldNum, Named o,
            int maxFieldSize) throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        ArgumentNotValid.checkNotNegative(fieldNum, "int fieldNum");
        ArgumentNotValid.checkNotNull(o, "Named o");
        ArgumentNotValid.checkNotNegative(maxFieldSize, "int maxFieldSize");

        if (o.getComments().length() > maxFieldSize) {
            throw new PermissionDenied("Length of comments ("
                    + o.getComments().length()
                    + ") is larger than allowed. Max length is " 
                    + maxFieldSize);
        }
        setStringMaxLength(s, fieldNum, o.getComments(), maxFieldSize, o,
                "comments");
    }

    /**
     * Set the name of a Named object into the given field.
     * 
     * @param s
     *            a prepared statement
     * @param fieldNum
     *            the index of the given field to be set
     * @param o
     *            the Named object
     * @param maxFieldSize
     *            max size of the field           
     * @throws SQLException If SQL problems occur during the operation
     * @throws PermissionDenied
     *             If length of o.getName() is larger than
     *             Constants.MAX_NAME_SIZE
     */
    public static void setName(PreparedStatement s, int fieldNum, Named o,
            int maxFieldSize) throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        ArgumentNotValid.checkNotNegative(fieldNum, "int fieldNum");
        ArgumentNotValid.checkNotNull(o, "Named o");
        ArgumentNotValid.checkNotNegative(maxFieldSize, "int maxFieldSize");

        if (o.getName().length() > maxFieldSize) {
            throw new PermissionDenied("Length of name ("
                    + o.getName().length()
                    + ") is larger than allowed. Max length is "
                    + maxFieldSize);
        }
        setStringMaxLength(s, fieldNum, o.getName(), maxFieldSize, o, "name");
    }

    /**
     * Set the Date into the given field of a statement.
     * 
     * @param s
     *            a prepared statement
     * @param fieldNum
     *            the index of the given field to be set
     * @param date
     *            the date (may be null)
     * @throws SQLException If SQL problems occur during the operation
     */
    public static void setDateMaybeNull(PreparedStatement s, int fieldNum,
            Date date) throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        ArgumentNotValid.checkNotNegative(fieldNum, "int fieldNum");

        if (date != null) {
            s.setTimestamp(fieldNum, new Timestamp(date.getTime()));
        } else {
            s.setNull(fieldNum, Types.DATE);
        }
    }

    /**
     * Get a Date from a column in the resultset. Returns null, if the value in
     * the column is NULL.
     * 
     * @param rs
     *            the resultSet
     * @param columnIndex
     *            The given column, where the Date resides
     * @return a Date from a column in the resultset
     * @throws SQLException
     *             If columnIndex does not correspond to a parameter marker in
     *             the ResultSet, or a database access error occurs or this
     *             method is called on a closed ResultSet
     */
    public static Date getDateMaybeNull(ResultSet rs, final int columnIndex)
            throws SQLException {
        ArgumentNotValid.checkNotNull(rs, "ResultSet rs");
        ArgumentNotValid.checkNotNegative(columnIndex, "int columnIndex");

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
     * Method to perform a rollback of complex DB updates. If no commit has been
     * performed, this will undo the entire transaction, otherwise nothing will
     * happen. This should be called in a finally block with no DB updates after
     * the last commit. Thus exceptions while closing are ignored, but logged as
     * warnings.
     * 
     * @param c
     *            the db-connection
     * @param action
     *            The action going on, before calling this method
     * @param o
     *            The Object being acted upon by this action
     */
    public static void rollbackIfNeeded(Connection c, String action, Object o) {
        ArgumentNotValid.checkNotNull(c, "Connection c");
        try {
            c.rollback();
            c.setAutoCommit(true);
        } catch (SQLException e) {
            log.warn("SQL error doing rollback after " + action + " " + o
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
            // Can't throw here, we want the real exception
        }
    }

    /**
     * Set a string, possibly truncating it. If contents.length() > maxSize,
     * contents is truncated to contain the first maxSize characters of the
     * contents, and a warning is logged.
     * 
     * @param s
     *            a prepared statement
     * @param fieldNum
     *            the field-index, where the contents are inserted
     * @param contents
     *            the contents
     * @param maxSize
     *            the maxsize for this contents
     * @param o
     *            the Object, which assumedly have a field named fieldName
     * @param fieldName
     *            a given field (Assumedly in Object o)
     * @throws SQLException
     *             If fieldNum does not correspond to a parameter marker in the
     *             PreparedStatement, or a database access error occurs or this
     *             method is called on a closed PreparedStatement
     */
    public static void setClobMaxLength(PreparedStatement s, int fieldNum,
            String contents, long maxSize, Object o, String fieldName)
            throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        if (contents != null) {
            if (contents.length() > maxSize) {
                log.warn(fieldName + " of " + o
                        + " is longer than the allowed " + maxSize
                        + " characters. The contents is now truncated to "
                        + "length " + maxSize
                        + ". The untruncated contents was: " + contents);
                // truncate to length maxSize (if maxSize <= Integer.MAX_VALUE)
                // else truncate to length Integer.MAX_VALUE
                if (maxSize > Integer.MAX_VALUE) {
                    maxSize = Integer.MAX_VALUE;
                }
                contents = contents.substring(0, (int) maxSize);
            }
            s.setCharacterStream(fieldNum, new StringReader(contents), contents
                    .length());
            s.setString(fieldNum, contents);
        } else {
            boolean supportsClob = DBSpecifics.getInstance().supportsClob();
            s.setNull(fieldNum, supportsClob ? Types.CLOB : Types.VARCHAR);
        }
    }

    /**
     * Insert a long value (which could be null) into the given field of a
     * statement.
     * 
     * @param s
     *            a prepared Statement
     * @param i
     *            the number of a given field in the prepared statement
     * @param value
     *            the long value to insert (maybe null)
     * @throws SQLException
     *             If i does not correspond to a parameter marker in the
     *             PreparedStatement, or a database access error occurs or this
     *             method is called on a closed PreparedStatement
     */
    public static void setLongMaybeNull(PreparedStatement s, int i, Long value)
            throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
        if (value != null) {
            s.setLong(i, value);
        } else {
            s.setNull(i, Types.BIGINT);
        }
    }

    /**
     * Insert an Integer in prepared statement.
     * 
     * @param s
     *            a prepared statement
     * @param i
     *            the index of the statement, where the Integer should be
     *            inserted
     * @param value
     *            The Integer to insert (maybe null)
     * @throws SQLException
     *             If i does not correspond to a parameter marker in the
     *             PreparedStatement, or a database access error occurs or this
     *             method is called on a closed PreparedStatement
     */
    public static void setIntegerMaybeNull(PreparedStatement s, int i,
            Integer value) throws SQLException {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");

        if (value != null) {
            s.setInt(i, value);
        } else {
            s.setNull(i, Types.INTEGER);
        }
    }

    /**
     * Get an Integer from the resultSet in column i.
     * 
     * @param rs
     *            the resultset
     * @param i
     *            the column where the wanted Integer resides
     * @return an Integer object located in column i in the resultset
     * @throws SQLException
     *             If the columnIndex is not valid, or a database access error
     *             occurs or this method is called on a closed result set
     */
    public static Integer getIntegerMaybeNull(ResultSet rs, int i)
            throws SQLException {
        ArgumentNotValid.checkNotNull(rs, "ResultSet rs");
        Integer res = rs.getInt(i);
        if (rs.wasNull()) {
            return null;
        }
        return res;
    }

    /**
     * Get a Long from the resultSet in column i.
     * 
     * @param rs
     *            the resultset
     * @param i
     *            the column where the wanted Long resides
     * @return a Long object located in column i in the resultset
     * @throws SQLException
     *             If the columnIndex is not valid, or a database access error
     *             occurs or this method is called on a closed result set
     */
    public static Long getLongMaybeNull(ResultSet rs, int i)
            throws SQLException {
        ArgumentNotValid.checkNotNull(rs, "ResultSet rs");
        Long res = rs.getLong(i);
        if (rs.wasNull()) {
            return null;
        }
        return res;
    }

    /**
     * Check whether an object is used elsewhere in the database.
     * 
     * @param connection
     *            connection to the database.
     * @param select
     *            A select statement finding the names of other uses. The
     *            statement should result in exactly one column of string
     *            values.
     * @param victim
     *            The object being used.
     * @param args
     *            Any objects that may be used to prepare the select statement.
     * @throws PermissionDenied
     *             if the object has usages.
     */
    public static void checkForUses(Connection connection, String select,
            Object victim, Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        String usages = DBUtils.getUsages(connection, select, victim, args);
        if (usages != null) {
            String message = "Cannot delete " + victim + " as it is used in "
                    + usages;
            throw new PermissionDenied(message);
        }
        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, select, args);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                List<String> usedIn = new ArrayList<String>();
                do {
                    usedIn.add(res.getString(1));
                } while (res.next());
            }
        } catch (SQLException e) {
            final String message = "SQL error deleting " + victim;
            log
                    .warn(message + "\n"
                            + ExceptionUtils.getSQLExceptionCause(e), e);
            throw new IOFailure(message + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Return a description of where an object is used elsewhere in the
     * database, or null.
     * 
     * @param connection
     *            connection to the database.
     * @param select
     *            A select statement finding the names of other uses. The
     *            statement should result in exactly one column of string
     *            values.
     * @param victim
     *            The object being used.
     * @param args
     *            Any objects that may be used to prepare the select statement.
     * @return A string describing the usages, or null if no usages were found.
     */
    public static String getUsages(Connection connection, String select,
            Object victim, Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, select, args);
            ResultSet res = s.executeQuery();
            if (res.next()) {
                List<String> usedIn = new ArrayList<String>();
                do {
                    usedIn.add(res.getString(1));
                } while (res.next());
                return usedIn.toString();
            }
            return null;
        } catch (SQLException e) {
            final String message = "SQL error checking for usages of " + victim
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Check that a database table is of the expected version.
     * 
     * @param connection
     *            connection to the database.
     * @param tablename
     *            The table to check.
     * @param desiredVersion
     *            The version it should be.
     * @throws IllegalState
     *             if the version isn't as expected.
     */
    public static void checkTableVersion(Connection connection,
            String tablename, int desiredVersion) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(tablename, "String tablename");
        ArgumentNotValid.checkPositive(desiredVersion, "int desiredVersion");
        int actualVersion = getTableVersion(connection, tablename);
        if (actualVersion != desiredVersion) {
            String message = "Wrong table version for '" + tablename
                    + "': Should be " + desiredVersion + ", but is "
                    + actualVersion;
            log.warn(message);
            throw new IllegalState(message);
        }
    }

    /**
     * Execute an SQL statement and return the single string in the result set.
     * This variant takes a query string and a single string arg and combines
     * them to form a normal query.
     * 
     * This assumes the connection is to the harvester database.
     * 
     * @param connection
     *            connection to the database.
     * @param query
     *            a query with ? for parameters (must not be null or an empty
     *            string)
     * @param args
     *            parameters of type string, int, long or boolean
     * @return The string result
     * @throws IOFailure
     *             if the statement didn't result in exactly one string value
     */
    public static String selectStringValue(Connection connection, String query,
            Object... args) {
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");
        ArgumentNotValid.checkNotNull(connection, "Connection connection");

        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, query, args);
            // We do not test for null-values here, already tested in
            // selectStringValue(s)
            return DBUtils.selectStringValue(s);
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Execute an SQL statement and return the single string in the result set.
     * 
     * @param s
     *            A prepared statement
     * @return The string result, or null if the result was a null value Note
     *         that a null value is not the same as no result rows.
     * @throws IOFailure
     *             if the statement didn't result in exactly one row with a
     *             string or null value
     */
    public static String selectStringValue(PreparedStatement s) {
        ArgumentNotValid.checkNotNull(s, "PreparedStatement s");
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
            throw new IOFailure("SQL error executing statement " + s + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        }
    }

    /**
     * Execute an SQL query and return whether the result contains any rows.
     * 
     * @param connection
     *            connection to the database.
     * @param query
     *            a query with ? for parameters (must not be null or an empty
     *            String)
     * @param args
     *            parameters of type string, int, long or boolean
     * @return True if executing the query resulted in at least one row.
     * @throws IOFailure
     *             if there were problems with the SQL query
     */
    public static boolean selectAny(Connection connection, String query,
            Object... args) {
        ArgumentNotValid.checkNotNull(connection, "Connection connection");
        ArgumentNotValid.checkNotNullOrEmpty(query, "String query");
        ArgumentNotValid.checkNotNull(args, "Object... args");

        PreparedStatement s = null;
        try {
            s = prepareStatement(connection, query, args);
            return s.executeQuery().next();
        } catch (SQLException e) {
            throw new IOFailure("Error preparing SQL statement " + query
                    + " args " + Arrays.toString(args) + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            closeStatementIfOpen(s);
        }
    }

    /**
     * Translate a "normal" glob (with * and .) into SQL syntax.
     * 
     * @param glob
     *            A shell-like glob string (must not be null)
     * @return A string that implements glob in SQL "LIKE" constructs.
     */
    public static String makeSQLGlob(String glob) {
        ArgumentNotValid.checkNotNull(glob, "String glob");
        return glob.replace("*", "%").replace("?", "_");
    }

    /**
     * Update a database by executing all the statements in the updates String
     * array. NOTE: this must NOT be used for tables under version control It
     * must only be used in connection with temporary tables e.g. used for
     * backup.
     * 
     * @param connection
     *            connection to the database.
     * @param updates
     *            The SQL statements that makes the necessary updates.
     * @throws IOFailure
     *             in case of problems in interacting with the database
     */
    public static void executeSQL(Connection connection,
            final String... updates) {
        ArgumentNotValid.checkNotNull(updates, "String... updates");
        PreparedStatement st = null;
        String s = "";

        try {
            connection.setAutoCommit(false);
            for (String update : updates) {
                s = update;
                log.debug("Executing SQL-statement: " + update);
                st = prepareStatement(connection, update);
                st.executeUpdate();
                st.close();
            }
            connection.setAutoCommit(true);
            log.debug("Updated database using updates '"
                    + StringUtils.conjoin(";", updates) + "'.");
        } catch (SQLException e) {
            String msg = "SQL error updating database with sql: " + s + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } finally {
            rollbackIfNeeded(connection, "updating table with SQL: ",
                    StringUtils.conjoin(";", updates) + "'.");
            closeStatementIfOpen(st);
        }
    }
}
