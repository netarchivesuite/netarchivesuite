/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.datamodel;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.webinterface.ExtendedFieldDefinition;


/**
 * This class represents one Extended Field
 */
public class ExtendedField implements Serializable {
    private transient Log log = LogFactory.getLog(getClass());

    /** persistent id of this extended field. */
    private Long extendedFieldID;
    /** The Id of the Reference to which the extended field belongs */
    private Long extendedFieldTypeID;

	/** name of the extended Field. This name will not be translated. */
    private String name;
    /** formatting patterns of the extended Field */
    private String formattingPattern;
    /** datatype of the extended Field. see datatype list */
    private int datatype;
    /** is extendedfield mandatory */
    private boolean mandatory;
    /** sequencenr to sort fields. */
    private int sequencenr = 1;
    
    /** default value for this field */
	private String defaultValue;
	/** key-value pairs for Options */
	private String options;


	public Long getExtendedFieldID() {
		return extendedFieldID;
	}

	public void setExtendedFieldID(Long extendedFieldID) {
		this.extendedFieldID = extendedFieldID;
	}

    public Long getExtendedFieldTypeID() {
		return extendedFieldTypeID;
	}

	public void setExtendedFieldTypeID(Long extendedFieldTypeID) {
		this.extendedFieldTypeID = extendedFieldTypeID;
	}
	
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getFormattingPattern() {
		return formattingPattern;
	}


	public void setFormattingPattern(String aFormattingPattern) {
		this.formattingPattern = aFormattingPattern;
	}


	public int getDatatype() {
		return datatype;
	}


	public void setDatatype(int datatype) {
		this.datatype = datatype;
	}


	public boolean isMandatory() {
		return mandatory;
	}


	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}


	public int getSequencenr() {
		return sequencenr;
	}


	public void setSequencenr(int sequencenr) {
		this.sequencenr = sequencenr;
	}

    public String getDefaultValue() {
		return defaultValue;
	}


	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

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
	
    public ExtendedField(Long aExtendedFieldID, Long aExtendedFieldTypeID, String aName, String aFormattingPattern, int aDatatype, boolean aMandatory, int aSequenceNr, String aDefaultValue, String aOptions) throws ArgumentNotValid {
        extendedFieldID = aExtendedFieldID;
        extendedFieldTypeID = aExtendedFieldTypeID;
        name = aName;
        formattingPattern = aFormattingPattern;
        datatype = aDatatype;
        mandatory = aMandatory;
        sequencenr = aSequenceNr;
        defaultValue = aDefaultValue;
        options = aOptions;
    }

	public HashMap<String, String> getOptionValues() {
		ExtendedFieldOptions efo = new ExtendedFieldOptions(getOptions());
		
		return efo.getOptions();
	}
	
	public String getJspFieldname() {
		return ExtendedFieldDefinition.EXTF_PREFIX + getExtendedFieldID() + "_" + getDatatype();
	}

}
