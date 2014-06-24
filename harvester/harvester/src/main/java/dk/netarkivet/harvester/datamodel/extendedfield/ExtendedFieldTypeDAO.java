package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.List;

import dk.netarkivet.harvester.datamodel.DAO;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldTypeDAO implements DAO {
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
