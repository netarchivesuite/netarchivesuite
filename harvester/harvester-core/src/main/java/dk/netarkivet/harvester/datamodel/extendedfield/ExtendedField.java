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

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.webinterface.ExtendedFieldConstants;

/**
 * This class represents one Extended Field.
 */
@SuppressWarnings({"serial"})
public class ExtendedField implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(ExtendedField.class);

    /** persistent id of this extended field. */
    private Long extendedFieldID;
    /** The Id of the Reference to which the extended field belongs. */
    private Long extendedFieldTypeID;

    /** name of the extended Field. This name will not be translated. */
    private String name;
    /** formatting patterns of the extended Field. */
    private String formattingPattern;
    /** datatype of the extended Field. see datatype list. */
    private int datatype;
    /** is extendedfield mandatory. */
    private boolean mandatory;
    /** sequencenr to sort fields. */
    private int sequencenr = 1;
    /** maxlen of extended Field. */
    private int maxlen = ExtendedFieldConstants.MAXLEN_EXTF_NAME;

    /** default value for this field. */
    private String defaultValue;

    /** key-value pairs for Options. */
    private String options;

    /**
     * @return the extendedFieldID
     */
    public Long getExtendedFieldID() {
        return extendedFieldID;
    }

    /**
     * Set the ID of the extendedField..
     *
     * @param extendedFieldID the ID of the extendedField..
     */
    public void setExtendedFieldID(Long extendedFieldID) {
        ArgumentNotValid.checkNotNull(extendedFieldID, "Long extendedFieldID");
        this.extendedFieldID = extendedFieldID;
    }

    /**
     * @return the extendedFieldTypeID
     */
    public Long getExtendedFieldTypeID() {
        return extendedFieldTypeID;
    }

    /**
     * Set the name of the extendedFieldTypeID.
     *
     * @param extendedFieldTypeID an extendedfieldtypeId
     */
    public void setExtendedFieldTypeID(Long extendedFieldTypeID) {
        ArgumentNotValid.checkNotNull(extendedFieldID, "Long extendedFieldID");
        this.extendedFieldTypeID = extendedFieldTypeID;
    }

    /**
     * @return the name of the extendedField
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the extendedField.
     *
     * @param name the name of the extendedField
     */
    public void setName(String name) {
        ArgumentNotValid.checkNotNull(name, "String name");
        this.name = name;
    }

    /**
     * @return the formatting pattern of the extendedField
     */
    public String getFormattingPattern() {
        return formattingPattern;
    }

    /**
     * Set a formatting pattern for this extendefield.
     *
     * @param aFormattingPattern a formatting pattern for this extendedfield
     */
    public void setFormattingPattern(String aFormattingPattern) {
        ArgumentNotValid.checkNotNull(aFormattingPattern, "String aFormattingPattern");
        this.formattingPattern = aFormattingPattern;
    }

    /**
     * @return the datatype of the extendedField
     */
    public int getDatatype() {
        return datatype;
    }

    /**
     * Set the datatype of this extendedField.
     *
     * @param datatype a datatype for this extendedfield
     */
    public void setDatatype(int datatype) {
        this.datatype = datatype;
    }

    /**
     * @return true, if extendedfield is mandatory, otherwise false.
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Set the mandatory-state of this extendedField.
     *
     * @param mandatory A mandatory-state of this extendedField
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return the sequencenr of the extendedField
     */
    public int getSequencenr() {
        return sequencenr;
    }

    /**
     * Set the sequencenr of this extendedField.
     *
     * @param sequencenr a new sequencenr of this extendedField.
     */
    public void setSequencenr(int sequencenr) {
        this.sequencenr = sequencenr;
    }

    /**
     * @return the default value of the extendedField
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the defaultvalue of this extendedField.
     *
     * @param defaultValue the defaultvalue of this extendedField.
     */
    public void setDefaultValue(String defaultValue) {
        ArgumentNotValid.checkNotNull(defaultValue, "String defaultValue");
        this.defaultValue = defaultValue;
    }

    /**
     * @return the options of the extendedField
     */
    public String getOptions() {
        return options;
    }

    /**
     * @return the max length of the extendedField
     */
    public int getMaxlen() {
        return maxlen;
    }

    /**
     * Set the maxlen of this extendedField.
     *
     * @param aMaxlen for this extendedfield
     */
    public void setMaxlen(int aMaxlen) {
        maxlen = aMaxlen;
    }

    /**
     * Set the options of the extendedField.
     *
     * @param options the options of the extendedField
     */
    public void setOptions(String options) {
        ArgumentNotValid.checkNotNull(options, "String options");
        this.options = options;
    }

    /**
     * Constructor for the extendedfield with only one value - the id.
     *
     * @param aExtendedFieldTypeID the Id of the extendededfieldtype
     */
    public ExtendedField(String aExtendedFieldTypeID) {
        ArgumentNotValid.checkNotNull(aExtendedFieldTypeID, "aExtendedFieldTypeID");

        extendedFieldTypeID = Long.parseLong(aExtendedFieldTypeID);
        datatype = ExtendedFieldDataTypes.STRING;
        mandatory = false;
        name = "";
        formattingPattern = "";
        defaultValue = "";
        options = "";
    }

    /**
     * Constructor for ExtendedField, that requires all data.
     *
     * @param aExtendedFieldID The extendedfieldId of the extendedfield
     * @param aExtendedFieldTypeID The extendedfieldtypeId of the extendedfield
     * @param aName The name of the extendedfield
     * @param aFormattingPattern The name of the extendedfield
     * @param aDatatype The datatype of the extendedfield
     * @param aMandatory The mandatory state of the extendedfield
     * @param aSequenceNr The sequencenr of the extendedfield
     * @param aDefaultValue The default value of the extendedfield
     * @param aOptions The options of the extendedfield
     * @param aMaxlen The maxlen of the extendedfield
     */
    public ExtendedField(Long aExtendedFieldID, Long aExtendedFieldTypeID, String aName, String aFormattingPattern,
            int aDatatype, boolean aMandatory, int aSequenceNr, String aDefaultValue, String aOptions, int aMaxlen) {
        extendedFieldID = aExtendedFieldID;
        extendedFieldTypeID = aExtendedFieldTypeID;
        name = aName;
        formattingPattern = aFormattingPattern;
        datatype = aDatatype;
        mandatory = aMandatory;
        sequencenr = aSequenceNr;
        defaultValue = aDefaultValue;
        options = aOptions;
        maxlen = aMaxlen;

        log.debug(toString());
    }

    /**
     * @return a map of option values.
     */
    public Map<String, String> getOptionValues() {
        return new ExtendedFieldOptions(getOptions()).getOptions();
    }

    /**
     * @return the JSP field name.
     */
    public String getJspFieldname() {
        return ExtendedFieldConstants.EXTF_PREFIX + getExtendedFieldID() + "_" + getDatatype();
    }

    public String toString() {
        return "" + "extendedFieldID:[" + extendedFieldID + "]\n" + "extendedFieldTypeID:[" + extendedFieldTypeID
                + "]\n" + "name:[" + name + "]\n" + "formattingPattern:[" + formattingPattern + "]\n" + "datatype:["
                + datatype + "]\n" + "mandatory:[" + mandatory + "]\n" + "sequencenr:[" + sequencenr + "]\n"
                + "defaultValue:[" + defaultValue + "]\n" + "options:[" + options + "]\n" + "maxlen:[" + maxlen + "]\n";
    }

}
