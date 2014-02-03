/* File:        $Id: ExtendedFieldDAO.java 2251 2012-02-08 13:03:03Z mss $Id$
 * Revision:    $Revision: 2251 $Revision$
 * Author:      $Author: mss $Author$
 * Date:        $Date: 2012-02-08 14:03:03 +0100 (Wed, 08 Feb 2012) $Date$
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

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldDAO extends HarvestDatabaseDAO {
	
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
     * Check if an extendedfield exists for a given ID.
     * @param extendedFieldId a given ID.
     * @return true, if an extendedfield exists for the given ID
     */
    public abstract boolean exists(Long extendedFieldId);
    
    /**
     * Creates an instance in persistent storage of the given extended Field.
     *
     * @param extendedField a ExtendedField to create in persistent storage.
     */
    public abstract void create(ExtendedField extendedField);

    /**
     * Reads an ExtendedField from persistent storage.
     *
     * @param aExtendedFieldID The ID of the ExtendedField to read
     * @return a ExtendedField instance
     * @throws ArgumentNotValid If failed to create ExtendedField instance
                 in case aExtendedFieldID is invalid
     * @throws UnknownID        If the job with the given jobID
     *                          does not exist in persistent storage.
     * @throws IOFailure If the loaded ID of ExtendedField does not match 
     * the expected.
     */
    public abstract ExtendedField read(Long aExtendedFieldID)
            throws ArgumentNotValid, UnknownID, IOFailure;

    /**
     * Update a ExtendedField in persistent storage.
     *
     * @param aExtendedField The ExtendedField to update
     * @throws IOFailure If writing the ExtendedField to persistent 
     * storage fails
     */
    public abstract void update(ExtendedField aExtendedField) throws IOFailure;

    /**
     * Return a list of all ExtendedFields of the given Extended Field Type.
     *
     * @param aExtendedFieldTypeId extended field type.
     * @return A list of all ExtendedFields with given Extended Field Type
     */
    public abstract List<ExtendedField> getAll(long aExtendedFieldTypeId);
    
    /**
     * deletes an ExtendedField from persistent storage.
     * The implementation of this method must also delete all 
     * belonging extended field values.
     *
     * @param aExtendedFieldID The ID of the ExtendedField to read
     * @throws IOFailure If deleting the ExtendedField fails
     */
    public abstract void delete(long aExtendedFieldID) throws IOFailure;
    
    /**
     * @return an instance of this class.
     */
    public static synchronized ExtendedFieldDAO getInstance() {
        if (instance == null) {
            instance = new ExtendedFieldDBDAO();
        }
        return instance;
    }
}
