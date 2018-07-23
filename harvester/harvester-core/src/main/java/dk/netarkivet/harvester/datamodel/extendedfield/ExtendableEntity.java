/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtendableEntity {

    private static final Logger log = LoggerFactory.getLogger(ExtendableEntity.class);

    /** List of extended Fields. */
    protected List<ExtendedFieldValue> extendedFieldValues = new ArrayList<ExtendedFieldValue>();

    protected ExtendableEntity(Provider<ExtendedFieldDAO> extendedFieldDAO) {
        addExtendedFieldValues(extendedFieldDAO);
    }

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
     * @return ExtendedFieldValue Object
     */
    public ExtendedFieldValue getExtendedFieldValue(Long aExtendedFieldId) {
        for (int i = 0; i < extendedFieldValues.size(); i++) {
            if (extendedFieldValues.get(i).getExtendedFieldID().equals(aExtendedFieldId)) {
                return extendedFieldValues.get(i);
            }
        }
        log.debug("ExtendedFieldValue w/ id = {} does not exist. Returning empty object", aExtendedFieldId);
        return new ExtendedFieldValue();
    }

    /**
     * updates a extendedFieldValue by extendedField Id.
     *
     * @param aExtendedFieldId id of the extendedfield
     * @param aContent id content to set
     */
    public void updateExtendedFieldValue(Long aExtendedFieldId, String aContent) {
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
    protected void addExtendedFieldValues(Provider<ExtendedFieldDAO> extendedFieldDAOProvider) {
        if (extendedFieldDAOProvider == null) {
            return; // No extended fields
        }

        List<ExtendedField> list = extendedFieldDAOProvider.get().getAll(getExtendedFieldType());

        Iterator<ExtendedField> it = list.iterator();
        while (it.hasNext()) {
            ExtendedField ef = it.next();

            ExtendedFieldValue efv = new ExtendedFieldValue();
            efv.setContent(new ExtendedFieldDefaultValue(ef.getDefaultValue(), ef.getFormattingPattern(), ef
                    .getDatatype()).getDBValue());
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
