package dk.netarkivet.wayback.indexer;

import java.io.Serializable;

/**
 * A generic class for managing storage and retrieval of persistent objects.
 * @param <T> The persistent class.
 * @param <PK> The class of the primary key used to identify the objects.
 */
public interface GenericDAO<T, PK extends Serializable> {

     /** Persist the newInstance object into database.
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

