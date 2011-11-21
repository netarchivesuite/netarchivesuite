/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.List;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldTypeDAO {
    /** The database singleton model. */
    protected static ExtendedFieldTypeDAO instance;

    /**
     * constructor used when creating singleton. Do not call directly.
     */
    protected ExtendedFieldTypeDAO() {
    }

    /**
     * Reset the DAO instance.  Only for use from within tests.
     */
    protected static void reset() {
        instance = null;
    }

    /** Tests if exists an ExtendedFieldType with the given ID.
    * @param aExtendedfieldtypeId An id belonging to an ExtendedFieldType
    * @return true, if there exists an ExtendedFieldType with the given ID,
    * otherwise returns false.
    */
    public abstract boolean exists(Long aExtendedfieldtypeId);

    /**
     * Read an ExtendedFieldType belonging to the given id.
     * @param aExtendedfieldtypeId an id belonging to a ExtendedFieldType
     * @return an ExtendedFieldType from belonging to the given id.
     */
    public abstract ExtendedFieldType read(Long aExtendedfieldtypeId);
    /**
     * @return a list of all ExtendedFieldTypes.
     */
    public abstract List<ExtendedFieldType> getAll(); 
    
    /**
     * If an instance exists, return it, otherwise instantiate one,
     * and return it.
     * @return the instance of this class.
     */
    public static synchronized ExtendedFieldTypeDAO getInstance() {
        if (instance == null) {
            instance = new ExtendedFieldTypeDBDAO();
        }
        return instance;
    }
    
}
