/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvesterDatabaseTables;

/**
 * Implementation class for the ExtendedFieldValueDAO interface.
 */
public class ExtendedFieldValueDBDAO extends ExtendedFieldValueDAO {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ExtendedFieldValueDBDAO.class);

    /**
     * Constructor for the ExtendedFieldValueDBDAO class.
     */
    public ExtendedFieldValueDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELD);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDTYPE);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Create a ExtendedFieldValue in persistent storage.
     *
     * @param aConnection an open connection to the HarvestDatabase.
     * @param aExtendedFieldValue The ExtendedFieldValue to create in persistent storage
     * @param aCommit Should we commit this or not
     * @throws SQLException In case of Database access problems.
     */
    public void create(Connection aConnection, ExtendedFieldValue aExtendedFieldValue, boolean aCommit)
            throws SQLException {
        ArgumentNotValid.checkNotNull(aExtendedFieldValue, "aExtendedFieldValue");

        if (aExtendedFieldValue.getExtendedFieldValueID() != null) {
            log.warn("The extendedFieldValueID for this extendedField Value is already set. "
                    + "This should probably never happen.");
        } else {
            aExtendedFieldValue.setExtendedFieldValueID(generateNextID(aConnection));
        }

        log.debug("Creating {}", aExtendedFieldValue.toString());

        PreparedStatement statement = null;
        aConnection.setAutoCommit(false);
        statement = aConnection.prepareStatement("INSERT INTO extendedfieldvalue ("
                + "extendedfieldvalue_id, extendedfield_id, content, instance_id) " + "VALUES (?, ?, ?, ?)");

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
        Connection connection = HarvestDBConnection.get();

        try {
            create(connection, aExtendedFieldValue, true);
        } catch (SQLException e) {
            String message = "SQL error creating extendedfield value " + aExtendedFieldValue + " in database" + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "create extendedfield value", aExtendedFieldValue);
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * @param c an open connection to the HarvestDatabase.
     * @return the ID for next extendedvFieldValue inserted.
     */
    private Long generateNextID(Connection c) {
        // FIXME synchronize or use identity row or generator.
        Long maxVal = DBUtils.selectLongValue(c, "SELECT max(extendedfieldvalue_id) FROM extendedfieldvalue");

        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
    }

    @Override
    public void delete(long aExtendedfieldValueID) throws IOFailure {
        ArgumentNotValid.checkNotNull(aExtendedfieldValueID, "aExtendedfieldValueID");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            c.setAutoCommit(false);

            stm = c.prepareStatement("DELETE FROM extendedfieldvalue WHERE extendedfieldvalue_id = ?");
            stm.setLong(1, aExtendedfieldValueID);
            stm.executeUpdate();

            c.commit();
        } catch (SQLException e) {
            String message = "SQL error deleting extendedfieldvalue for ID " + aExtendedfieldValueID + "\n";
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            DBUtils.rollbackIfNeeded(c, "delete extendedfield value", aExtendedfieldValueID);
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public boolean exists(Long aExtendedFieldValueID) {
        ArgumentNotValid.checkNotNull(aExtendedFieldValueID, "Long aExtendedFieldValueID");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, aExtendedFieldValueID);
        } finally {
            HarvestDBConnection.release(c);
        }

    }

    /**
     * Find out if there already exists in persistent storage a ExtendedFieldValue with the given id.
     *
     * @param c an open connection to the HarvestDatabase.
     * @param aExtendedFieldValueID An id associated with a ExtendedFieldValue
     * @return true, if there already exists in persistent storage a ExtendedFieldValue with the given id.
     */
    private synchronized boolean exists(Connection c, Long aExtendedFieldValueID) {
        return 1 == DBUtils.selectLongValue(c, "SELECT COUNT(*) FROM extendedfieldvalue "
                + "WHERE extendedfieldvalue_id = ?", aExtendedFieldValueID);
    }

    @Override
    public synchronized ExtendedFieldValue read(Long aExtendedFieldID, Long aInstanceID) {
        ArgumentNotValid.checkNotNull(aExtendedFieldID, "aExtendedFieldID");
        ArgumentNotValid.checkNotNull(aInstanceID, "aInstanceID");
        Connection connection = HarvestDBConnection.get();
        try {
            return read(connection, aExtendedFieldID, aInstanceID);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Read the ExtendedFieldValue with the given extendedFieldID.
     *
     * @param connection an open connection to the HarvestDatabase
     * @param aExtendedFieldID A given ID for a ExtendedFieldValue
     * @param aInstanceID A given instanceID
     * @return the ExtendedFieldValue with the given extendedFieldID.
     */
    private synchronized ExtendedFieldValue read(Connection connection, Long aExtendedFieldID, Long aInstanceID) {
        ExtendedFieldValue extendedFieldValue = null;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("" + "SELECT extendedfieldvalue_id, " + "       extendedfield_id, "
                    + "       content " + "FROM extendedfieldvalue "
                    + "WHERE  extendedfield_id = ? and instance_id = ?");

            statement.setLong(1, aExtendedFieldID);
            statement.setLong(2, aInstanceID);
            ResultSet result = statement.executeQuery();
            if (!result.next()) {
                return null;
            }

            long extendedfieldvalueId = result.getLong(1);
            long extendedfieldId = result.getLong(2);
            long instanceId = aInstanceID;
            String content = result.getString(3);

            extendedFieldValue = new ExtendedFieldValue(extendedfieldvalueId, extendedfieldId, instanceId, content);

            return extendedFieldValue;
        } catch (SQLException e) {
            String message = "SQL error reading extended Field " + aExtendedFieldID + " in database" + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Read a ExtendedFieldValue in persistent storage.
     *
     * @param aConnection an open connection to the HarvestDatabase
     * @param aExtendedFieldValue The ExtendedFieldValue to update
     * @param aCommit Should we commit this or not
     * @throws SQLException In case of database problems.
     */
    public void update(Connection aConnection, ExtendedFieldValue aExtendedFieldValue, boolean aCommit)
            throws SQLException {
        PreparedStatement statement = null;
        final Long extendedfieldvalueId = aExtendedFieldValue.getExtendedFieldID();
        if (!exists(aConnection, extendedfieldvalueId)) {
            throw new UnknownID("Extended Field Value id " + extendedfieldvalueId + " is not known in "
                    + "persistent storage");
        }

        aConnection.setAutoCommit(false);

        statement = aConnection.prepareStatement("" + "UPDATE extendedfieldvalue " + "SET    extendedfield_id = ?, "
                + "       instance_id = ?, " + "       content = ? "
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
        Connection connection = HarvestDBConnection.get();

        try {
            update(connection, aExtendedFieldValue, true);
        } catch (SQLException e) {
            String message = "SQL error updating extendedfield Value " + aExtendedFieldValue + " in database" + "\n";
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "update extendedfield Value", aExtendedFieldValue);
            HarvestDBConnection.release(connection);
        }
    }

}
