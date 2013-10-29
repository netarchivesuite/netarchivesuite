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

import java.util.Iterator;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Abstract class for the DAO handling the persistence of {@link HarvestChannel} 
 * instances.
 * 
 * @author ngiraud
 *
 */
public abstract class HarvestChannelDAO implements Iterable<HarvestChannel> {
	
	/**
	 * The singleton instance
	 */
	private static HarvestChannelDAO instance;

	/**
	 * Default empty constructor
	 */
	HarvestChannelDAO() {
		
	}
	
	/**
     * Gets the {@link HarvestChannelDAO} singleton.
     *
     * @return the {@link HarvestChannelDAO} singleton
     */
    public static synchronized HarvestChannelDAO getInstance() {
        if (instance == null) {
            instance = new HarvestChannelDBDAO();
        }
        return instance;
    }

	@Override
	public abstract Iterator<HarvestChannel> iterator();
	
	/**
	 * Retrieves a {@link HarvestChannel} by its UID.
	 * @param id the UID to look for
	 * @return the corresponding instance
	 * @throws ArgumentNotValid if not ID is supplied
	 * @throws UnknownID if the ID is not present in the persistent storage.
	 */
	public abstract HarvestChannel getById(long id)
			throws ArgumentNotValid, UnknownID;
	
	/**
	 * Retrieves a {@link HarvestChannel} by its unique name.
	 * @param name the name to look for
	 * @return the corresponding instance
	 * @throws ArgumentNotValid if not name is supplied
	 * @throws UnknownID if the name is not present in the persistent storage.
	 */
	public abstract HarvestChannel getByName(String name)
			throws ArgumentNotValid, UnknownID;
	
	/**
	 * Creates a {@link HarvestChannel} object in the storage backend. 
	 * @param jobCat the {@link HarvestChannel} object
	 */
	public abstract void create(HarvestChannel jobCat) throws IOFailure;
	
	/**
	 * Updates a {@link HarvestChannel} object in the storage backend. 
	 * @param jobCat the {@link HarvestChannel} object
	 */
	public abstract void update(HarvestChannel jobCat) throws IOFailure;
	
	/**
	 * Deletes a {@link HarvestChannel} object in the storage backend. 
	 * @param jobCat the {@link HarvestChannel} object
	 * @throws NetarkivetException if the job category is still referenced 
	 * by a {@link HarvestDefinition}.
	 */
	public abstract void delete(HarvestChannel jobCat)
			throws NetarkivetException;
	
	/**
	 * Returns harvest channels by type.
	 * @param snapshot if true, fecth only snapshot types, otherwise broad types.
	 * @return an iterator on {@link HarvestChannel}.
	 */
	public abstract Iterator<HarvestChannel> getAll(boolean isSnapshot);
	
	/**
	 * Returns harvest channels by type, sorted first by type 
	 * (focused first, then broad) and then by name.
	 * @return an iterator on {@link HarvestChannel}.
	 */
	public abstract Iterator<HarvestChannel> getAll();
	
	/**
	 * Returns true if a default channel exists for the given type of job (snapshot or focused).
	 * @param snapshot type of job
	 * @return true if a match is found, false otherwise.
	 */
	public abstract boolean defaultChannelExists(boolean snapshot);
	
	/**
	 * Returns the default {@link HarvestChannel} for the given type of harvest.
	 * @param snapshot snapshot or partial harvest
	 * @return the default {@link HarvestChannel}
	 */
	public abstract HarvestChannel getDefaultChannel(boolean snapshot);
	
	/**
	 * Returns the {@link HarvestChannel} mapped to the given {@link HarvestDefinition} id.
	 * If no mapping was explicitly defined, returns null.
	 * @param harvestDefinitionId the {@link HarvestDefinition} id to look for
	 * @return the mapped {@link HarvestChannel} id or null
	 */
	public abstract HarvestChannel getChannelForHarvestDefinition(long harvestDefinitionId);

}
