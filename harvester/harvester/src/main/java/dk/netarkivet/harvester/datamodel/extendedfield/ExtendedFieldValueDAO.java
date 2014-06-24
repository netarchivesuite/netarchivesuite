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
     * Reset the DAO instance.  Only for use from within tests.
     */
    public static void reset() {
        instance = null;
    }
    
    /**
     * Find out if there exists in persistent storage
     * a ExtendedFieldValue with the given id.
     * @param aExtendedFieldValueID An id associated with a ExtendedFieldValue
     * @return true, if there already exists in persistent storage
     * a ExtendedFieldValue with the given id.
     */
    public abstract boolean exists(Long aExtendedFieldValueID);
    
    /**
     * Create a ExtendedFieldValue in persistent storage.
     * @param aExtendedFieldValue The ExtendedFieldValue to create in 
     * persistent storage.
     */
    public abstract void create(ExtendedFieldValue aExtendedFieldValue);

    /**
     * Read the ExtendedFieldValue with the given extendedFieldID.
     * @param aExtendedFieldID A given ID for a ExtendedFieldValue
     * @param aInstanceID A given instanceID
     * @return the ExtendedFieldValue with the given extendedFieldID.
     */
    public abstract ExtendedFieldValue read(Long aExtendedFieldID, 
            Long aInstanceID);
    
    /**
     * Update a ExtendedFieldValue in persistent storage.
     * @param aExtendedFieldValue The ExtendedFieldValue to update
     */
    public abstract void update(ExtendedFieldValue aExtendedFieldValue);

    /**
     * Delete a ExtendedFieldValue in persistent storage.
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

