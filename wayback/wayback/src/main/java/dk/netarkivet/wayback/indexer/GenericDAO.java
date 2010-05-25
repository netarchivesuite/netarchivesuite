/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import java.io.Serializable;

/**
 * A generic class for managing storage and retrieval of persistent objects.
 * @param <T> The persistent class.
 * @param <PK> The class of the primary key used to identify the objects.
 */
public interface GenericDAO<T, PK extends Serializable> {

     /** Persist the newInstance object into database
      * @param newInstance  the object to persist.
      * @return the key assigned to the object.
      */
    PK create(T newInstance);

    /** Retrieve an object that was previously persisted to the database using
     *   the indicated id as primary key.
     * @param id the key of the object to be retrieved.
     * @return the retrieved object.
     */
    T read(PK id);

    /** Save changes made to a persistent object.
     * @param transientObject the object to be updated.
     */
    void update(T transientObject);

    /** Remove an object from persistent storage in the database.
     * @param persistentObject the object to be deleted.
     */
    void delete(T persistentObject);

}
