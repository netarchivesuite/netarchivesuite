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
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.sql.Connection;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldValueDAO {
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
     * Gets the ExtendedFieldValueDAO singleton.
     *
     * @return the ExtendedFieldValueDAO singleton
     */
    protected abstract Connection getConnection();

    public abstract boolean exists(Long aExtendedFieldValueID);
    
    public abstract void create(ExtendedFieldValue aExtendedFieldValue);

    public abstract ExtendedFieldValue read(Long aExtendedFieldID, Long aInstanceID)
            throws ArgumentNotValid, UnknownID, IOFailure;

    public abstract void update(ExtendedFieldValue aExtendedFieldValue) throws IOFailure;

    public abstract void delete(long aExtendedfieldValueID) throws IOFailure;
}
