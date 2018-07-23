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
package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Implementation class for the DAO handling the persistence of {@link HarvestChannel} instances.
 *
 * @author ngiraud
 */
public class HarvestChannelDBDAO extends HarvestChannelDAO {

    /**
     * Create a new HarvestChannelDAO implemented using database. This constructor also tries to upgrade the jobs and
     * jobs_configs tables in the current database. Throws an {@link IllegalState} exception, if default channels are
     * missing in the DB.
     */
    protected HarvestChannelDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.HARVESTCHANNELS);
        } finally {
            HarvestDBConnection.release(connection);
        }

        /*
         * String defaultSnapshotChannelName = Settings.get(HarvesterSettings.SNAPSHOT_HARVEST_CHANNEL_ID); if
         * (defaultSnapshotChannelName != null && defaultSnapshotChannelName.length() > 0) { HarvestChannel
         * harvestChannel = lookupName(defaultSnapshotChannelName); if (harvestChannel == null) { harvestChannel = new
         * HarvestChannel(defaultSnapshotChannelName, true, true, "Default channel for focused harvest.");
         * create(harvestChannel); } }
         */

        if (!defaultFocusedChannelExists()) {
            throw new IllegalState("No default harvest channel defined for focused jobs!");
        }
    }

    private static final String get_by_id_sql = "SELECT * FROM harvestchannel WHERE id=?";

    @Override
    public HarvestChannel getById(final long id) throws ArgumentNotValid, UnknownID {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_by_id_sql);
            stm.setLong(1, id);
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) {
                throw new UnknownID("No harvestchannel with id " + id);
            }
            return buildFromResultSet(rs);
        } catch (SQLException e) {
            throw new UnknownID("Failed to get harvestchannel with id " + id, e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private static final String get_by_name_sql = "SELECT * FROM harvestchannel WHERE name=?";

    @Override
    public HarvestChannel getByName(final String name) throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        /*
         * if (HarvestChannel.SNAPSHOT.getName().equals(name)) { return HarvestChannel.SNAPSHOT; }
         */
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_by_name_sql);
            stm.setString(1, name);
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) {
                throw new UnknownID("No harvestchannel with name '" + name + "'");
            }

            return buildFromResultSet(rs);
        } catch (SQLException e) {
            throw new UnknownID("Failed to get harvestchannel with name '" + name + "'", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /*
     * private HarvestChannel lookupName(final String name) throws ArgumentNotValid, UnknownID {
     * ArgumentNotValid.checkNotNullOrEmpty(name, "name"); HarvestChannel harvestChannel = null; Connection connection =
     * HarvestDBConnection.get(); try { PreparedStatement stm = connection.prepareStatement( get_by_name_sql);
     * stm.setString(1, name); ResultSet rs = stm.executeQuery(); if (rs.next()) { harvestChannel =
     * buildFromResultSet(rs); } } catch (SQLException e) { throw new
     * UnknownID("Failed to get harvestchannel with name '" + name + "'", e); } finally {
     * HarvestDBConnection.release(connection); } return harvestChannel; }
     */

    private static final String create_sql = "INSERT INTO harvestchannel(name, issnapshot, isdefault, comments) VALUES (?, ?, ?, ?)";

    @Override
    public void create(final HarvestChannel harvestChan) {
        ArgumentNotValid.checkNotNull(harvestChan, "HarvestChannel harvestChan");
        /*
         * if (HarvestChannel.SNAPSHOT.equals(harvestChan)) { throw new
         * PermissionDenied("Cannot store SNAPSHOT channel!"); }
         */
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(create_sql);
            stm.setString(1, harvestChan.getName());
            stm.setBoolean(2, harvestChan.isSnapshot());
            stm.setBoolean(3, harvestChan.isDefault());
            stm.setString(4, harvestChan.getComments());
            if (stm.executeUpdate() < 1) {
                throw new IOFailure("Failed to create harvestchannel '" + harvestChan.getName() + "'");
            }
        } catch (SQLException e) {
            throw new IOFailure("Failed to create harvestchannel '" + harvestChan.getName() + "'", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private static final String update_sql = "UPDATE harvestchannel SET name=?, isDefault=?, comments=? WHERE id=?";

    @Override
    public void update(HarvestChannel harvestChan) {
        ArgumentNotValid.checkNotNull(harvestChan, "HarvestChannel harvestChan");
        if (harvestChan.isSnapshot()) {
            throw new PermissionDenied("Cannot update SNAPSHOT channel!");
        }
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(update_sql);
            stm.setString(1, harvestChan.getName());
            stm.setBoolean(2, harvestChan.isDefault());
            stm.setString(3, harvestChan.getComments());
            stm.setLong(4, harvestChan.getId());
            if (stm.executeUpdate() != 1) {
                throw new IOFailure("Failed to update harvestchannel with id " + harvestChan.getId());
            }
        } catch (SQLException e) {
            throw new IOFailure("Failed to update harvestchannel with id " + harvestChan.getId(), e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public Iterator<HarvestChannel> iterator() {
        return getAll(true);
    }

    private static final String get_all_sql = "SELECT * FROM harvestchannel ORDER BY name";

    @Override
    public Iterator<HarvestChannel> getAll(final boolean includeSnapshot) {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_all_sql);
            ResultSet rs = stm.executeQuery();
            ArrayList<HarvestChannel> channelList = new ArrayList<HarvestChannel>();
            while (rs.next()) {
                boolean isSnapshot = rs.getBoolean("issnapshot");
                if (!isSnapshot || includeSnapshot) {
                    channelList.add(new HarvestChannel(rs.getLong("id"), rs.getString("name"), isSnapshot, rs
                            .getBoolean("isdefault"), rs.getString("comments")));
                }
            }
            return channelList.iterator();
        } catch (SQLException e) {
            throw new IOFailure("Failed to get harvest channels", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private static final String get_default_focused_channel_exists_sql = "SELECT * FROM harvestchannel WHERE issnapshot = true AND isdefault=true";

    @Override
    public boolean defaultFocusedChannelExists() {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_default_focused_channel_exists_sql);
            ResultSet rs = stm.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new IOFailure("Failed to get default harvest channel for focused jobs", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private static final String get_default_channel_sql = "SELECT * FROM harvestchannel WHERE issnapshot = ? AND isdefault=true";

    @Override
    public HarvestChannel getDefaultChannel(boolean isSnapshot) {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_default_channel_sql);
            stm.setBoolean(1, isSnapshot);
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) {
                throw new IOFailure("No default harvest channel for snapshot=" + isSnapshot);
            }
            return buildFromResultSet(rs);
        } catch (SQLException e) {
            throw new IOFailure("Failed to get default harvest channel for snapshot=" + isSnapshot, e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private static final String get_channel_for_hd_sql;

    static {
        get_channel_for_hd_sql = "SELECT * FROM harvestchannel C, harvestdefinitions D "
                + "WHERE D.channel_id=C.id AND D.harvest_id=?";
    }

    @Override
    public HarvestChannel getChannelForHarvestDefinition(long harvestDefinitionId) {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(get_channel_for_hd_sql);
            stm.setLong(1, harvestDefinitionId);
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return buildFromResultSet(rs);
        } catch (SQLException e) {
            throw new IOFailure("Failed to find harvestdefinition-channel association", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    private HarvestChannel buildFromResultSet(ResultSet rs) throws SQLException {
        return new HarvestChannel(rs.getLong("id"), rs.getString("name"), rs.getBoolean("issnapshot"),
                rs.getBoolean("isdefault"), rs.getString("comments"));
    }

}
