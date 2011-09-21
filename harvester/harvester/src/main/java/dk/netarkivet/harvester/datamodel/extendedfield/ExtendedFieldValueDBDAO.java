/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
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
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;

public class ExtendedFieldValueDBDAO extends ExtendedFieldValueDAO {
	private final Log log = LogFactory.getLog(getClass());

	/**
	 * Constructor for the ExtendedFieldValueDBDAO class.
	 */
    public ExtendedFieldValueDBDAO() {

        Connection connection = HarvestDBConnection.get();
        try {
            DBSpecifics.getInstance().updateTable(
                    DBSpecifics.EXTENDEDFIELDTYPE_TABLE,
                    DBSpecifics.EXTENDEDFIELDTYPE_TABLE_REQUIRED_VERSION);

            DBSpecifics.getInstance().updateTable(
                    DBSpecifics.EXTENDEDFIELD_TABLE,
                    DBSpecifics.EXTENDEDFIELD_TABLE_REQUIRED_VERSION);

            DBSpecifics.getInstance().updateTable(
                    DBSpecifics.EXTENDEDFIELDVALUE_TABLE,
                    DBSpecifics.EXTENDEDFIELDVALUE_TABLE_REQUIRED_VERSION);
            
        } finally {
            HarvestDBConnection.release(connection);
        }
    }
	
    protected Connection getConnection() {
    	return HarvestDBConnection.get();
    }

	public void create(Connection aConnection, ExtendedFieldValue aExtendedFieldValue, boolean aCommit) throws SQLException {
		ArgumentNotValid.checkNotNull(aExtendedFieldValue, "aExtendedFieldValue");

		if (aExtendedFieldValue.getExtendedFieldValueID() != null) {
			log
				.warn("The extendedFieldValueID for this extendedField Value is already set. "
						+ "This should probably never happen.");
		} else {
			aExtendedFieldValue.setExtendedFieldValueID(generateNextID(aConnection));
		}

		log.debug("Creating " + aExtendedFieldValue.toString());

		PreparedStatement statement = null;
		aConnection.setAutoCommit(false);
		statement = aConnection.prepareStatement(""
		        + "INSERT INTO extendedfieldvalue "
				+ "            (extendedfieldvalue_id, "
				+ "             extendedfield_id, "
				+ "             content, "
				+ "             instance_id) "
				+ "VALUES      (?, "
				+ "             ?, "
				+ "             ?, "
				+ "             ?) ");

		statement.setLong(1, aExtendedFieldValue.getExtendedFieldValueID());
		statement.setLong(2, aExtendedFieldValue.getExtendedFieldID());
		statement.setString(3, aExtendedFieldValue.getContent());
		statement.setLong(4, aExtendedFieldValue.getInstanceID());

		statement.executeUpdate();
		if (aCommit) {
			aConnection.commit();
		}
	}
    
	@Override
	public void create(ExtendedFieldValue aExtendedFieldValue) {
		Connection connection = getConnection();
		
		try {
			create(connection, aExtendedFieldValue, true);
		} catch (SQLException e) {
			String message = "SQL error creating extendedfield value "
					+ aExtendedFieldValue + " in database" + "\n"
					+ ExceptionUtils.getSQLExceptionCause(e);
			log.warn(message, e);
			throw new IOFailure(message, e);
		} finally {
			DBUtils.rollbackIfNeeded(connection, "create extendedfield value",
					aExtendedFieldValue);
			HarvestDBConnection.release(connection);
		}
	}

	private Long generateNextID(Connection c) {
		Long maxVal = DBUtils.selectLongValue(c,
				"SELECT max(extendedfieldvalue_id) FROM extendedfieldvalue");
		
		if (maxVal == null) {
			maxVal = 0L;
		}
		return maxVal + 1L;
	}

	
	@Override
	public void delete(long aExtendedfieldValueID) throws IOFailure {
        ArgumentNotValid.checkNotNull(aExtendedfieldValueID, "aExtendedfieldValueID");

		Connection c = getConnection();
        PreparedStatement stm = null;
        try {
            c.setAutoCommit(false);

            stm = c.prepareStatement("DELETE FROM extendedfieldvalue WHERE extendedfieldvalue_id = ?");
            stm.setLong(1, aExtendedfieldValueID);
            stm.executeUpdate();
            
            c.commit();

        } catch (SQLException e) {
            String message =
                "SQL error deleting extendedfieldvalue for ID " + aExtendedfieldValueID
                + "\n"+ ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(c, "delete extendedfield value", aExtendedfieldValueID);
            HarvestDBConnection.release(c);
        }
		
	}

	public boolean exists(Long aExtendedFieldValueID) {
		ArgumentNotValid.checkNotNull(aExtendedFieldValueID,
				"Long aExtendedFieldValueID");

		Connection c = getConnection();
		try {
			return exists(c, aExtendedFieldValueID);
		} finally {
			HarvestDBConnection.release(c);
		}

	}

	private synchronized boolean exists(Connection c, Long aExtendedFieldValueID) {
		return 1 == DBUtils
				.selectLongValue(
						c,
						"SELECT COUNT(*) FROM extendedfieldvalue WHERE extendedfieldvalue_id = ?",
						aExtendedFieldValueID);
	}
	

	@Override
	public synchronized ExtendedFieldValue read(Long aExtendedFieldID, Long aInstanceID) {
		ArgumentNotValid.checkNotNull(aExtendedFieldID, "aExtendedFieldID");
		ArgumentNotValid.checkNotNull(aInstanceID, "aInstanceID");
		Connection connection = getConnection();
		try {
			return read(connection, aExtendedFieldID, aInstanceID);
		} finally {
			HarvestDBConnection.release(connection);
		}
	}

	private synchronized ExtendedFieldValue read(Connection connection, Long aExtendedFieldID, Long aInstanceID) {
		ExtendedFieldValue extendedFieldValue = null;
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(""
				+ "SELECT extendedfieldvalue_id, "
				+ "       extendedfield_id, "
				+ "       instance_id, "
				+ "       content "
				+ "FROM   extendedfieldvalue "
				+ "WHERE  extendedfield_id = ? and instance_id = ?");
			
			statement.setLong(1, aExtendedFieldID);
			statement.setLong(2, aInstanceID);
			ResultSet result = statement.executeQuery();
			if (!result.next()) {
				return null;
			}
			
			long extendedfieldvalue_id = result.getLong(1);
			long extendedfield_id = result.getLong(2);
			long instance_id = result.getLong(3);
			String content = result.getString(4);

			extendedFieldValue = new ExtendedFieldValue(extendedfieldvalue_id, extendedfield_id, instance_id, content);

			return extendedFieldValue;
		} catch (SQLException e) {
			String message = "SQL error reading extended Field " + aExtendedFieldID + " in database"
					+ "\n" + ExceptionUtils.getSQLExceptionCause(e);
			log.warn(message, e);
			throw new IOFailure(message, e);
		}
	}

	public void update(Connection aConnection, ExtendedFieldValue aExtendedFieldValue, boolean aCommit) throws SQLException {
		PreparedStatement statement = null;
		final Long extendedfieldvalue_id = aExtendedFieldValue.getExtendedFieldID();
		if (!exists(aConnection, extendedfieldvalue_id)) {
			throw new UnknownID("Extended Field Value id " + extendedfieldvalue_id
					+ " is not known in persistent storage");
		}

		aConnection.setAutoCommit(false);
		
		statement = aConnection.prepareStatement(""
		    + "UPDATE extendedfieldvalue "
			+ "SET    extendedfield_id = ?, "
			+ "       instance_id = ?, "
			+ "       content = ? "
			+ "WHERE  extendedfieldvalue_id = ? and instance_id = ?");
		
		statement.setLong(1, aExtendedFieldValue.getExtendedFieldID());
		statement.setLong(2, aExtendedFieldValue.getInstanceID());
		statement.setString(3, aExtendedFieldValue.getContent());
		statement.setLong(4, aExtendedFieldValue.getExtendedFieldValueID());
		statement.setLong(5, aExtendedFieldValue.getInstanceID());
		
		statement.executeUpdate();
		
		if (aCommit) {
			aConnection.commit();
		}
	}
	
	@Override
	public void update(ExtendedFieldValue aExtendedFieldValue) throws IOFailure {
		Connection connection = getConnection();
		
		try {
			update(connection, aExtendedFieldValue, true);
		} catch (SQLException e) {
			String message = "SQL error updating extendedfield Value " + aExtendedFieldValue + " in database"
					+ "\n" + ExceptionUtils.getSQLExceptionCause(e);
			log.warn(message, e);
			throw new IOFailure(message, e);
		} finally {
			DBUtils.rollbackIfNeeded(connection, "update extendedfield Value", aExtendedFieldValue);
			HarvestDBConnection.release(connection);
		}
	}
	
    public static synchronized ExtendedFieldValueDAO getInstance() {
        if (instance == null) {
            instance = new ExtendedFieldValueDBDAO();
        }
        return instance;
    }
	

	
}
