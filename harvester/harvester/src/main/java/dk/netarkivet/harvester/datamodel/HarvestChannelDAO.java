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
     * @param harvestChannel the {@link HarvestChannel} object
     */
    public abstract void create(HarvestChannel harvestChannel) throws IOFailure;

    /**
     * Updates a {@link HarvestChannel} object in the storage backend.
     * @param harvestChannel the {@link HarvestChannel} object
     */
    public abstract void update(HarvestChannel harvestChannel) throws IOFailure;

    /**
     * Returns harvest channels by type, sorted first by type
     * (focused first, then broad) and then by name.
     * @param includeSnapshot if true, returns the single snapshot channel in the iterator.
     * @return an iterator on {@link HarvestChannel}.
     */
    public abstract Iterator<HarvestChannel> getAll(boolean includeSnapshot);

    /**
     * Returns true if a default channel exists for focused jobs.
     * @return true if a match is found, false otherwise.
     */
    public abstract boolean defaultFocusedChannelExists();

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
