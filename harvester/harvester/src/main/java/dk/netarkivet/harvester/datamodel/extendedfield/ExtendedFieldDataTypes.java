
package dk.netarkivet.harvester.datamodel.extendedfield;

/**
 * Constants for the available ExtendedFieldDatatypes.
 * TODO change into an enum.
 */
public class ExtendedFieldDataTypes {
    /** The datatype STRING. */
    public static final int STRING = 1;
    /** The datatype BOOLEAN. */
    public static final int BOOLEAN = 2;
    /** The datatype NUMBER. */
    public static final int NUMBER = 3;
    /** The datatype TIMESTAMP. */
    public static final int TIMESTAMP = 4;
    /** The datatype NOTE. */
    public static final int NOTE = 5;
    /** The datatype SELECT. */
    public static final int SELECT = 6;
    /** The datatype JSCALENDAR. */
    public static final int JSCALENDAR = 7;
    /** Min datatype value. */
    public static final int MIN_DATATYPE_VALUE = 1;
    /** Max datatype value. */
    public static final int MAX_DATATYPE_VALUE = 7;
}
