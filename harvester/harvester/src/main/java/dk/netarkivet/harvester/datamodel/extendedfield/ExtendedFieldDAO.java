package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.DAO;

/**
 * Interface for creating and accessing extended fields in persistent storage.
 */
public abstract class ExtendedFieldDAO implements DAO {
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
     * @param aExtendedfieldId a given ID.
     * @return true, if an extendedfield exists for the given ID
     */
    public abstract boolean exists(Long aExtendedfieldId);
    
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
