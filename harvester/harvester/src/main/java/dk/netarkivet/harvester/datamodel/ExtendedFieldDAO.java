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
package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldDAO {
    /** The database singleton model. */
    protected static ExtendedFieldDAO instance;

    /**
     * constructor used when creating singleton. Do not call directly.
     */
    protected ExtendedFieldDAO() {
    }

    /**
     * Reset the DAO instance.  Only for use from within tests.
     */
    public static void reset() {
        instance = null;
    }
    
    /**
     * Gets the ExtendedFieldDAO singleton.
     *
     * @return the ExtendedFieldDAO singleton
     */
    protected abstract Connection getConnection();

    public abstract boolean exists(Long aExtendedfield_id);
    
    /**
     * Creates an instance in persistent storage of the given extended Field.
     *
     * @param aExtendedField a ExtendedField to create in persistent storage.
     */
    public abstract void create(ExtendedField aExtendedField);

    /**
     * Reads an ExtendedField from persistent storage.
     *
     * @param aExtendedFieldID The ID of the ExtendedField to read
     * @return a ExtendedField instance
     * @throws ArgumentNotValid If failed to create ExtendedField instance
                 in case aExtendedFieldID is invalid
     * @throws UnknownID        If the job with the given jobID
     *                          does not exist in persistent storage.
     * @throws IOFailure If the loaded ID of ExtendedField does not match the expected.
     */
    public abstract ExtendedField read(Long aExtendedFieldID)
            throws ArgumentNotValid, UnknownID, IOFailure;

    /**
     * Update a ExtendedField in persistent storage.
     *
     * @param aExtendedField The ExtendedField to update
     * @throws IOFailure If writing the ExtendedField to persistent storage fails
     */
    public abstract void update(ExtendedField aExtendedField) throws IOFailure;

    /**
     * Return a list of all ExtendedFields of the given Extended Field Type
     *
     * @param aExtendedFieldType_id extended field type.
     * @return A list of all ExtendedFields with given Extended Field Type
	 */
    public abstract List<ExtendedField> getAll(long aExtendedFieldType_id);
    
    /**
     * deletes an ExtendedField from persistent storage.
     *
     * @param aExtendedFieldID The ID of the ExtendedField to read
	 *
     * the implementation of this method must also delete all belonging extended field values.
     *
     */
    public abstract void delete(long aExtendedfieldId) throws IOFailure;
}
