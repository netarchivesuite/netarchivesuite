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

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldValueDAO {

    /** The one and only instance of this DAO. */
    protected static ExtendedFieldValueDAO instance;

    /**
     * constructor used when creating singleton. Do not call directly.
     */
    protected ExtendedFieldValueDAO() {
    }

    /**
     * Reset the DAO instance. Only for use from within tests.
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Find out if there exists in persistent storage a ExtendedFieldValue with the given id.
     *
     * @param aExtendedFieldValueID An id associated with a ExtendedFieldValue
     * @return true, if there already exists in persistent storage a ExtendedFieldValue with the given id.
     */
    public abstract boolean exists(Long aExtendedFieldValueID);

    /**
     * Create a ExtendedFieldValue in persistent storage.
     *
     * @param aExtendedFieldValue The ExtendedFieldValue to create in persistent storage.
     */
    public abstract void create(ExtendedFieldValue aExtendedFieldValue);

    /**
     * Read the ExtendedFieldValue with the given extendedFieldID.
     *
     * @param aExtendedFieldID A given ID for a ExtendedFieldValue
     * @param aInstanceID A given instanceID
     * @return the ExtendedFieldValue with the given extendedFieldID.
     */
    public abstract ExtendedFieldValue read(Long aExtendedFieldID, Long aInstanceID);

    /**
     * Update a ExtendedFieldValue in persistent storage.
     *
     * @param aExtendedFieldValue The ExtendedFieldValue to update
     */
    public abstract void update(ExtendedFieldValue aExtendedFieldValue);

    /**
     * Delete a ExtendedFieldValue in persistent storage.
     *
     * @param aExtendedfieldValueID The ID for a extendedFieldValue to delete
     */
    public abstract void delete(long aExtendedfieldValueID);

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
