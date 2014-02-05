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
package dk.netarkivet.harvester.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.HarvestChannel;

/**
 * @author ngiraud
 *
 */
public class HarvestChannelDBDAO extends HarvestChannelDAO {
	
	private final class HarvestChannelExtractor implements ResultSetExtractor<HarvestChannel> {
		
		final String noResultMessage;
		
		public HarvestChannelExtractor(String noResultMessage) {
			super();
			this.noResultMessage = noResultMessage;
		}
		
		@Override
		public HarvestChannel extractData(ResultSet rs)
				throws SQLException, DataAccessException {
			if (!rs.next()) {
				throw new IOFailure(noResultMessage);
			}
			return buildFromResultSet(rs);
		}
		
	};

	/**
	 * Create a new HarvestChannelDAO implemented using database.
	 * Throws an {@link IllegalState} exception, if default channels are missing in the DB.
	 */
	protected HarvestChannelDBDAO() {
		
		super();
		
		if (!defaultFocusedChannelExists()) {
			throw new IllegalState("No default harvest channel defined for focused jobs!");
		}
	}

	@Override
	public HarvestChannel getById(final long id) 
			throws ArgumentNotValid, UnknownID {
		return query(
				"SELECT * FROM harvestchannel WHERE id=:id",
				new ParameterMap("id", id),
				new HarvestChannelExtractor("No harvestchannel with id " + id));
	}

	@Override
	public HarvestChannel getByName(final String name) 
			throws ArgumentNotValid, UnknownID {
ArgumentNotValid.checkNotNullOrEmpty(name, "name");
    	
        if (HarvestChannel.SNAPSHOT.getName().equals(name)) {
            return HarvestChannel.SNAPSHOT;
        }
        
		return query(
				"SELECT * FROM harvestchannel WHERE name=:name",
				new ParameterMap("name", name),
				new HarvestChannelExtractor("No harvestchannel with name " + name));
	}

	@Override
	public void create(final HarvestChannel harvestChan) throws IOFailure {
		executeTransaction("doCreate", HarvestChannel.class, harvestChan);
	}
	
	@SuppressWarnings("unused")
	private synchronized void doCreate(final HarvestChannel harvestChan) throws IOFailure {
		executeUpdate(
				"INSERT INTO harvestchannel(name, comments, isdefault)"
				+ " VALUES (:name,:comments,:isDefault)",
				getParameterMap(harvestChan));
	}

	@Override
	public void update(final HarvestChannel harvestChan) {
		executeTransaction("doUpdate", HarvestChannel.class, harvestChan);
	}
		
	@SuppressWarnings("unused")
	private synchronized void doUpdate(final HarvestChannel harvestChan) {
		executeUpdate(
				"UPDATE harvestchannel SET name=:name, comments=:comments WHERE id=:id",
				getParameterMap(harvestChan));
	}

	@Override
	public Iterator<HarvestChannel> iterator() {
		return getAll(true);
	}
	
	@Override
	public Iterator<HarvestChannel> getAll(final boolean includeSnapshot) {
		List<HarvestChannel> chans = query(
				"SELECT * FROM harvestchannel ORDER BY name",
				ParameterMap.EMPTY,
				new RowMapper<HarvestChannel>() {
					@Override
					public HarvestChannel mapRow(ResultSet rs, int pos)
							throws SQLException {
						return buildFromResultSet(rs);
					}					
				});
		if (includeSnapshot) {
			chans.add(HarvestChannel.SNAPSHOT);
		}
		return chans.iterator();
	}

	@Override
	public boolean defaultFocusedChannelExists() {
		return 1 == queryIntValue("SELECT COUNT(*) FROM harvestchannel WHERE isdefault=true");
	}
	
	@Override
	public HarvestChannel getDefaultChannel(final boolean snapshot) {
		if (snapshot) {
			return HarvestChannel.SNAPSHOT;
		}
		return query(
				"SELECT * FROM harvestchannel WHERE isdefault=true",
				ParameterMap.EMPTY,
				new HarvestChannelExtractor("No default harvest channel for snapshot=" + snapshot));
	}
	
	@Override
	public HarvestChannel getChannelForHarvestDefinition(final long harvestDefinitionId) {		
		return query(
				"SELECT * FROM harvestchannel C, harvestdefinitions D"
				+ " WHERE D.channel_id=C.id AND D.harvest_id=:id",
				new ParameterMap("id", harvestDefinitionId),
				new ResultSetExtractor<HarvestChannel>() {
					@Override
					public HarvestChannel extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						if (!rs.next()) {
							return null;
						}
						return buildFromResultSet(rs);
					}
					
				});		
	}
	
	private HarvestChannel buildFromResultSet(ResultSet rs) throws SQLException {
		return new HarvestChannel(
				rs.getLong("id"), 
				rs.getString("name"),
				rs.getString("comments"),
				rs.getBoolean("isdefault"));
	}
	
	private ParameterMap getParameterMap(final HarvestChannel chan) {
		return new ParameterMap(
				"id", chan.getId(),
				"name", getStorableName(chan),
				"isDefault", chan.isDefault(),
				"comments", chan.getComments());
	}

}
