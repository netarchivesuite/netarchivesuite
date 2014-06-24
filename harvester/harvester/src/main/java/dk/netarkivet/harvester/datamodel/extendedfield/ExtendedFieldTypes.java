
package dk.netarkivet.harvester.datamodel.extendedfield;

/**
 * Class declaring constants for ExtendedFieldTypes and their 
 * corresponding table-names. There are two kinds of extended fields:
 * extendedfields for domains, and extendedfields for harvestdefinitions.
 * TODO change into an enum class instead.
 */
public final class ExtendedFieldTypes {
    
    /** 
     * Private default constructor to avoid instantiation.
    */
    private ExtendedFieldTypes() {
    }
    
    /** constant representing extendedfields for domains. */
    public static final int DOMAIN = 1;
    /** constant representing extendedfields for harvestdefinitions. */
    public static final int HARVESTDEFINITION = 2;
    /**
     * arrays representing the two different types, and which database table
     * they are associated with. used for testing.
     */
    protected static String[] tableNames = {
        "", "domains", "harvestdefinitions"};
}
