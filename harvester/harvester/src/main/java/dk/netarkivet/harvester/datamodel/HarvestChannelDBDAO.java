/* File:        $Id: HarvestChannelDAO.java 2712 2013-06-17 14:43:52Z ngiraud $
 * Revision:    $Revision: 2712 $
 * Author:      $Author: ngiraud $
 * Date:        $Date: 2013-06-17 16:43:52 +0200 (Mon, 17 Jun 2013) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
 * @author ngiraud
 *
 */
public class HarvestChannelDBDAO extends HarvestChannelDAO {

    /**
     * Create a new HarvestChannelDAO implemented using database.
     * This constructor also tries to upgrade the jobs and jobs_configs tables
     * in the current database.
     * throws and {@link IllegalState} exception, if it is impossible to
     * make the necessary updates.
     * Also throws an {@link IllegalState} exception, if default channels are missing in the DB.
     */
    protected HarvestChannelDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(
                    connection,
                    HarvesterDatabaseTables.HARVESTCHANNELS);

        } finally {
            HarvestDBConnection.release(connection);
        }

        if (!defaultFocusedChannelExists()) {
            throw new IllegalState("No default harvest channel defined for focused jobs!");
        }
    }

    @Override
    public HarvestChannel getById(final long id)
            throws ArgumentNotValid, UnknownID {
        ArgumentNotValid.checkNotNull(id, "harvestchannel id");
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel WHERE id=?");
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

    @Override
    public HarvestChannel getByName(final String name)
            throws ArgumentNotValid, UnknownID {

        if (HarvestChannel.SNAPSHOT.getName().equals(name)) {
            return HarvestChannel.SNAPSHOT;
        }

        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel WHERE name=?");
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

    @Override
    public void create(final HarvestChannel harvestChan) {
        if (HarvestChannel.SNAPSHOT.equals(harvestChan)) {
            throw new PermissionDenied("Cannot store SNAPSHOT channel!");
        }
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "INSERT INTO harvestchannel(name, comments, isdefault) "
                            + "VALUES (?,?,?)");
            stm.setString(1, harvestChan.getName());
            stm.setString(2, harvestChan.getComments());
            stm.setBoolean(3, harvestChan.isDefault());
            if (stm.executeUpdate() < 1) {
                throw new IOFailure(
                        "Failed to create harvestchannel '" + harvestChan.getName() + "'");
            }
        } catch (SQLException e) {
            throw new IOFailure(
                    "Failed to create harvestchannel '" + harvestChan.getName() + "'", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public void update(HarvestChannel harvestChan) {
        if (HarvestChannel.SNAPSHOT.equals(harvestChan)) {
            throw new PermissionDenied("Cannot update SNAPSHOT channel!");
        }
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "UPDATE harvestchannel SET name=?, comments=? WHERE id=?");
            stm.setString(1, harvestChan.getName());
            stm.setString(2, harvestChan.getComments());
            stm.setLong(3, harvestChan.getId());
            if (stm.executeUpdate() < 1) {
                throw new IOFailure(
                        "Failed to update harvestchannel with id " + harvestChan.getId());
            }
        } catch (SQLException e) {
            throw new IOFailure(
                    "Failed to update harvestchannel with id " + harvestChan.getId(), e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public Iterator<HarvestChannel> iterator() {
        return getAll(true);
    }

    @Override
    public Iterator<HarvestChannel> getAll(final boolean includeSnapshot) {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel ORDER BY name");
            ResultSet rs = stm.executeQuery();
            ArrayList<HarvestChannel> cats = new ArrayList<HarvestChannel>();
            while (rs.next()) {
                cats.add(new HarvestChannel(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("comments"),
                        rs.getBoolean("isdefault")));
            }
            if (includeSnapshot) {
                cats.add(HarvestChannel.SNAPSHOT);
            }
            return cats.iterator();
        } catch (SQLException e) {
            throw new IOFailure("Failed to get harvest channels", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public boolean defaultFocusedChannelExists() {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel WHERE isdefault=true");
            ResultSet rs = stm.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new IOFailure(
                    "Failed to get default harvest channel for focused jobs", e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public HarvestChannel getDefaultChannel(boolean snapshot) {
        if (snapshot) {
            return HarvestChannel.SNAPSHOT;
        }
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel WHERE isdefault=true");
            ResultSet rs = stm.executeQuery();
            if (!rs.next()) {
                throw new IOFailure("No default harvest channel for snapshot=" + snapshot);
            }
            return buildFromResultSet(rs);
        } catch (SQLException e) {
            throw new IOFailure(
                    "Failed to get default harvest channel for snapshot=" + snapshot, e);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public HarvestChannel getChannelForHarvestDefinition(long harvestDefinitionId) {
        Connection connection = HarvestDBConnection.get();
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "SELECT * FROM harvestchannel C, harvestdefinitions D "
                            + "WHERE D.channel_id=C.id AND D.harvest_id=?");
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
        return new HarvestChannel(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("comments"),
                rs.getBoolean("isdefault"));
    }

}
