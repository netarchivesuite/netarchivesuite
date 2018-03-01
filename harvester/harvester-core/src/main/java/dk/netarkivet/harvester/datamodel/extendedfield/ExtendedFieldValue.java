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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class for holding a value of one ExtendedField.
 */
public class ExtendedFieldValue {

    /** The ID of the value of the ExtendedField. */
    private Long extendedFieldValueID;
    /** The ID of the ExtendedField. */
    private Long extendedFieldID;
    /** The contents of the value itself. */
    private String content;
    /** The instanceid. */
    private Long instanceID;

    /**
     * @return the ID of the value of the ExtendedField
     */
    public Long getExtendedFieldValueID() {
        return extendedFieldValueID;
    }

    /**
     * Set the ID of value of the ExtendedField.
     *
     * @param extendedFieldValueID the ID of the value of the ExtendedField
     */
    public void setExtendedFieldValueID(Long extendedFieldValueID) {
        ArgumentNotValid.checkNotNull(extendedFieldValueID, "Long extendedFieldValueID");
        this.extendedFieldValueID = extendedFieldValueID;
    }

    /**
     * @return the ID of the ExtendedField
     */
    public Long getExtendedFieldID() {
        return extendedFieldID;
    }

    /**
     * Set the ID of the ExtendedField.
     *
     * @param extendedFieldID the ID of the ExtendedField
     */
    public void setExtendedFieldID(Long extendedFieldID) {
        ArgumentNotValid.checkNotNull(extendedFieldID, "Long extendedFieldID");
        this.extendedFieldID = extendedFieldID;
    }

    /**
     * @return the content of the value
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the content. Null arg is not accepted.
     *
     * @param content The content of the value
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the instanceid of the value
     */
    public Long getInstanceID() {
        return instanceID;
    }

    /**
     * Set the instanceId. Null arg is not accepted.
     *
     * @param instanceID The instanceid
     */
    public void setInstanceID(Long instanceID) {
        this.instanceID = instanceID;
    }

    /**
     * Default constructor initializing the contents to the empty string.
     */
    public ExtendedFieldValue() {
        content = "";
    }

    /**
     * Constructor initializing all instance members. Used when reading from persistent storage.
     *
     * @param aExtendedFieldValueID The ID of the value of the ExtendedField.
     * @param aExtendedFieldID The ID of the ExtendedField.
     * @param aInstanceID The instance id of the value
     * @param aContent The contents of the value (the value itself) TODO argument validation
     */
    public ExtendedFieldValue(Long aExtendedFieldValueID, Long aExtendedFieldID, Long aInstanceID, String aContent) {
        extendedFieldValueID = aExtendedFieldValueID;
        extendedFieldID = aExtendedFieldID;
        instanceID = aInstanceID;
        content = aContent;
    }

    /**
     * @return the boolean value of the contents
     */
    public boolean getBooleanValue() {
        String aValue = getContent();
        if (aValue == null) {
            return false;
        }

        aValue = aValue.toLowerCase();
        for (String val : ExtendedFieldDefaultValue.possibleTrueValues) {
            if (aValue.equals(val)) {
                return true;
            }
        }

        return false;
    }

    public String toString() {
        return "" + "extendedFieldValueID:[" + extendedFieldValueID + "]\n" + "extendedFieldID:[" + extendedFieldID
                + "]\n" + "content:[" + content + "]\n" + "instanceID:[" + instanceID + "]\n";
    }

}
