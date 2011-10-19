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

package dk.netarkivet.harvester.datamodel.extendedfield;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class for constructing, validating, and keeping the default
 * value for a single ExtendedField.
 */
public class ExtendedFieldDefaultValue {
    
    /** The logger. */    
    private static final Log log = LogFactory.getLog(
            ExtendedFieldDefaultValue.class);
    /** Array of strings considered to be "true" values. */
    static final String[] possibleTrueValues = {"true", "t", "1"};
    /** Array of strings considered to be "false" values. */
    static final String[] possibleFalseValues = {"false", "f", "0"};
    /**
     * The valid state of this ExtendedFieldDefaultValue.
     */
    final boolean valid;
    
    /** The value of this ExtendedFieldDefaultValue. */
    String value;
    /** The formatting pattern of this ExtendedFieldDefaultValue. */
    String format;
    /** The datatype of this ExtendedFieldDefaultValue. */
    int datatype;

    /**
     * Constructor for the ExtendedFieldDefaultValues class.
     * @param aValue The given default value
     * @param aFormat the given formatting pattern
     * @param aDatatype the given datatype
     */
    public ExtendedFieldDefaultValue(String aValue, String aFormat,
            int aDatatype) {
        value = aValue;
        format = aFormat;
        datatype = aDatatype;

        valid = validate();
    }
    
    /**
     * Validate the arguments to the constructor.
     * @return true, if arguments are valid; false otherwise.
     */
    private boolean validate() {
        boolean isValid = false;
        switch (datatype) {
        case ExtendedFieldDataTypes.STRING:
            isValid = true; // Any kind of string currently accepted.
            break;
        case ExtendedFieldDataTypes.BOOLEAN:
            isValid = checkBoolean(value);
            break;
        case ExtendedFieldDataTypes.NUMBER:
            if (format != null && format.length() > 0) {
                DecimalFormat decimalFormat = new DecimalFormat(format);
                try {
                    decimalFormat.parse(value);
                    isValid = true;
                } catch (ParseException e) {
                    log.debug("Invalid NUMBER: " + value);
                }
            } else {
                log.debug("Invalid NUMBER: " + value);
            }
            break;
        case ExtendedFieldDataTypes.TIMESTAMP:
            if (format != null && format.length() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                try {
                    dateFormat.parse(value);
                    isValid = true;
                } catch (ParseException e) {
                    log.debug("Invalid TIMESTAMP: " + value);
                }
            } else {
                log.debug("Invalid TIMESTAMP: " + value);
            }
            break;
        case ExtendedFieldDataTypes.NOTE:
            isValid = true; // Any kind of NOTE value currently accepted.
            break;
        case ExtendedFieldDataTypes.SELECT:
            isValid = true; // Any kind of SELECT value currently accepted.
            break;   
        default:
            throw new ArgumentNotValid("Unable to validate unknown datatype: " 
                    +  datatype);
        }
        
        return isValid;
    }

    /**
     * Check the given string if it can be parsed as a Boolean.
     * @param aBooleanValue A given boolean
     * @return true, if the given string if it can be parsed as a Boolean.
     */
    private static boolean checkBoolean(final String aBooleanValue) {
        String aBooleanValueTrimmed = aBooleanValue.toLowerCase().trim();

        for (String val : possibleTrueValues) {
            if (aBooleanValueTrimmed.equals(val)) {
                return true;
            }
        }

        for (String val : possibleFalseValues) {
            if (aBooleanValueTrimmed.equals(val)) {
                return true;
            }
        }
        log.debug("The string '" + aBooleanValue
                + "' is not a valid Boolean value");
        return false;
    }

    /**
     * @return true, if ExtendedFieldDefaultValue is valid, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }
}
