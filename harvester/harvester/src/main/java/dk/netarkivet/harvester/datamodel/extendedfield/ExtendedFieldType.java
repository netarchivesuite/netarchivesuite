
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.io.Serializable;

/**
 * This class represents one Extended Field Type.
 */
@SuppressWarnings({ "serial"})
public class ExtendedFieldType implements Serializable {
    /** The id of this ExtendedFieldType. */
    private Long extendedFieldTypeID;
    /** The name of this ExtendedFieldType. */
    private String name;
    
    /** 
     * Constructor.
     * TODO Add validation
     * @param aExtendedFieldTypeID The id of this ExtendedFieldType.
     * @param aName The name of this ExtendedFieldType.
     */
    ExtendedFieldType(Long aExtendedFieldTypeID, String aName) {
        extendedFieldTypeID = aExtendedFieldTypeID;
        name = aName;
    }

    /**
     * @return id of this ExtendedFieldType.
     */
    public Long getExtendedFieldTypeID() {
        return extendedFieldTypeID;
    }

    /**
     * Set the id of this ExtendedFieldType.
     * @param extendedFieldTypeID the id of this ExtendedFieldType
     */
    public void setExtendedFieldTypeID(Long extendedFieldTypeID) {
        this.extendedFieldTypeID = extendedFieldTypeID;
    }
    
    /**
     * @return the name of this ExtendedFieldType.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this ExtendedFieldType.
     * @param name the name of this ExtendedFieldType.
     */
    public void setName(String name) {
        this.name = name;
    }
}
