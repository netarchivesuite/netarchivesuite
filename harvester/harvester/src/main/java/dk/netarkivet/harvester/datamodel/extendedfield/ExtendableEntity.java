package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ExtendableEntity {
    protected static final Log log = LogFactory.getLog(ExtendableEntity.class);
	
    /** List of extended Fields. */
    protected List<ExtendedFieldValue> extendedFieldValues = new ArrayList<ExtendedFieldValue>();
	
    /**
     * @return a List of all ExtendedfieldValues.
     */
    public List<ExtendedFieldValue> getExtendedFieldValues() {
        return extendedFieldValues;
    }

    /**
     * sets a List of extendedFieldValues.
     * 
     * @param aList List of extended Field objects
     * 
     */
    public void setExtendedFieldValues(List<ExtendedFieldValue> aList) {
        extendedFieldValues = aList;
    }

    /**
     * adds a Value to the ExtendedFieldValue List.
     * 
     * @param aValue Valueobject of the extended Field
     */
    public void addExtendedFieldValue(ExtendedFieldValue aValue) {
        extendedFieldValues.add(aValue);
    }

    /**
     * gets a extendedFieldValue by extendedField ID.
     * 
     * @param aExtendedFieldId id of the extendedfield
     * 
     * @return ExtendedFieldValue Object
     */
    public ExtendedFieldValue getExtendedFieldValue(Long aExtendedFieldId) {
        for (int i = 0; i < extendedFieldValues.size(); i++) {
            if (extendedFieldValues.get(i).getExtendedFieldID().equals(aExtendedFieldId)) {
                return extendedFieldValues.get(i);
            }
        }
        log.debug("ExtendedFieldValue w/ id = " + aExtendedFieldId 
                + " does not exist. Returning empty object");
        return new ExtendedFieldValue();
    }

    /**
     * updates a extendedFieldValue by extendedField Id.
     * 
     * @param aExtendedFieldId id of the extendedfield
     * @param aContent id content to set
     * 
     */
    public void updateExtendedFieldValue(Long aExtendedFieldId, 
            String aContent) {
        for (int i = 0; i < extendedFieldValues.size(); i++) {
            if (extendedFieldValues.get(i).getExtendedFieldID().equals(aExtendedFieldId)) {
                extendedFieldValues.get(i).setContent(aContent);
                return;
            }
        }
    }



    /**
     * Adds Defaultvalues for all extended fields of this entity.
     */
    public void addExtendedFieldValues() {
        ExtendedFieldDAO extendedFieldDAO = ExtendedFieldDAO.getInstance();
        List<ExtendedField> list = extendedFieldDAO
                .getAll(getExtendedFieldType());

        Iterator<ExtendedField> it = list.iterator();
        while (it.hasNext()) {
            ExtendedField ef = it.next();

            ExtendedFieldValue efv = new ExtendedFieldValue();
            efv.setContent(new ExtendedFieldDefaultValue(ef.getDefaultValue(), ef.getFormattingPattern(), ef.getDatatype()).getDBValue());
            efv.setExtendedFieldID(ef.getExtendedFieldID());
            getExtendedFieldValues().add(efv);
        }
    }

    /**
     * abstract method for receiving the ExtendedFieldType for concret class which inherits ExtendableEntity
     * 
     * @return ExtendedFieldType
     */
    abstract protected int getExtendedFieldType(); 
}
