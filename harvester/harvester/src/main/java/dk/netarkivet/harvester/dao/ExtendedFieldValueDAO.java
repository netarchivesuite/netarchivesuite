/* File:        $Id: ExtendedFieldValueDAO.java 2251 2012-02-08 13:03:03Z mss $Id$
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

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldValueDAO extends HarvestDatabaseDAO {
	
    /** The one and only instance of this DAO. */
    protected static ExtendedFieldValueDAO instance;

    /**
     * constructor used when creating singleton. Do not call directly.
     */
    protected ExtendedFieldValueDAO() {
    }

    /**
     * Reset the DAO instance.  Only for use from within tests.
     */
    public static void reset() {
        instance = null;
    }
    
    /**
     * Find out if there exists in persistent storage
     * a ExtendedFieldValue with the given id.
     * @param extendedFieldValueId An id associated with a ExtendedFieldValue
     * @return true, if there already exists in persistent storage
     * a ExtendedFieldValue with the given id.
     */
    public abstract boolean exists(Long extendedFieldValueId);
    
    /**
     * Create a ExtendedFieldValue in persistent storage.
     * @param extendedFieldValue The ExtendedFieldValue to create in 
     * persistent storage.
     */
    public abstract void create(ExtendedFieldValue extendedFieldValue);

    /**
     * Read the ExtendedFieldValue with the given extendedFieldID.
     * @param extendedFieldID A given ID for a ExtendedFieldValue
     * @param instanceID A given instanceID
     * @return the ExtendedFieldValue with the given extendedFieldID.
     */
    public abstract ExtendedFieldValue read(
    		Long extendedFieldID, 
            Long instanceID);
    
    /**
     * Update a ExtendedFieldValue in persistent storage.
     * @param extendedFieldValue The ExtendedFieldValue to update
     */
    public abstract void update(ExtendedFieldValue extendedFieldValue);

    /**
     * Delete a ExtendedFieldValue in persistent storage.
     * @param extendedfieldValueID The ID for a extendedFieldValue to delete
     */
    public abstract void delete(long extendedfieldValueID);

    /**
     * @return the singleton instance of the ExtendedFieldVAlueDAO
     */
    public static synchronized ExtendedFieldValueDAO getInstance() {
        if (instance == null) {
            instance = new ExtendedFieldValueDBDAO();
        }
        return instance;
    }
}

