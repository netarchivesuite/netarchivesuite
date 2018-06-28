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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class for constructing, validating, and keeping the default value for a single ExtendedField.
 */
public class ExtendedFieldDefaultValue {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ExtendedFieldDefaultValue.class);

    /** Array of strings considered to be "true" values. */
    protected static final String[] possibleTrueValues = {"true", "t", "1"};
    /** Array of strings considered to be "false" values. */
    protected static final String[] possibleFalseValues = {"false", "f", "0"};
    /** The valid state of this ExtendedFieldDefaultValue. */
    protected final boolean valid;

    /** The value of this ExtendedFieldDefaultValue. */
    protected String value;
    /** The formatting pattern of this ExtendedFieldDefaultValue. */
    protected String format;
    /** The datatype of this ExtendedFieldDefaultValue. */
    protected int datatype;

    /**
     * Constructor for the ExtendedFieldDefaultValues class.
     *
     * @param aValue The given default value
     * @param aFormat the given formatting pattern
     * @param aDatatype the given datatype
     */
    public ExtendedFieldDefaultValue(String aValue, String aFormat, int aDatatype) {
        value = aValue;
        format = aFormat;
        datatype = aDatatype;
        valid = validate();
    }

    /**
     * Validate the arguments to the constructor.
     *
     * @return true, if arguments are valid; false otherwise.
     */
    private boolean validate() {
        boolean isValid = false;
        switch (datatype) {
        case ExtendedFieldDataTypes.STRING:
            isValid = true; // Any kind of string currently accepted.
            break;
        case ExtendedFieldDataTypes.BOOLEAN:
            if (value != null) { // null is never a valid boolean!
                isValid = checkBoolean(value);
            }
            break;
        case ExtendedFieldDataTypes.NUMBER:
            if (value == null || value.length() == 0) { // no value no format check
                isValid = true;
                break;
            }

            if (format != null) {
                if (format.length() == 0) {
                    isValid = true;
                    break;
                }

                DecimalFormat decimalFormat = new DecimalFormat(format);
                try {
                    decimalFormat.parse(value);
                    isValid = true;
                } catch (ParseException e) {
                    log.debug("Invalid NUMBER: {}", value);
                }
            } else {
                isValid = true;
            }
            break;
        case ExtendedFieldDataTypes.TIMESTAMP:
        case ExtendedFieldDataTypes.JSCALENDAR:
            if (value == null || value.length() == 0) { // no value no format check
                isValid = true;
                break;
            }

            if (format != null) {
                if (format.length() == 0) {
                    isValid = true;
                    break;
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                try {
                    dateFormat.parse(value);
                    isValid = true;
                } catch (ParseException e) {
                    log.debug("Invalid TIMESTAMP: {}", value);
                }
            } else {
                isValid = true;
            }
            break;
        case ExtendedFieldDataTypes.NOTE:
            isValid = true; // Any kind of NOTE value currently accepted.
            break;
        case ExtendedFieldDataTypes.SELECT:
            isValid = true; // Any kind of SELECT value currently accepted.
            break;
        default:
            throw new ArgumentNotValid("Unable to validate unknown datatype: " + datatype);
        }

        return isValid;
    }

    /**
     * Check the given string if it can be parsed as a Boolean.
     *
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
        log.debug("The string '{}' is not a valid Boolean value", aBooleanValue);
        return false;
    }

    /**
     * @return true, if ExtendedFieldDefaultValue is valid, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return String, the DB-Value of the a Value
     */
    public String getDBValue() {
        // prevent that any null value fills content column of extendedFieldValue
        if (value == null) {
            value = "";
        }

        // only if datatype is Timestamp, JSCalendar or Number. Otherwise DB-Value = Value
        if (value != null && value.length() > 0) {
            if (ExtendedFieldDataTypes.TIMESTAMP == datatype || ExtendedFieldDataTypes.JSCALENDAR == datatype) {
                try {
                    // the Milliseconds from 1.1.1970 will be stored as String
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    return String.valueOf(sdf.parse(value).getTime());
                } catch (ParseException e) {
                    log.debug("Invalid TIMESTAMP: {}", value);
                }
            } else if (ExtendedFieldDataTypes.NUMBER == datatype) {
                try {
                    // a Double Value will be stored String
                    return String.valueOf(new DecimalFormat(format).parse(value).doubleValue());
                } catch (ParseException e) {
                    log.debug("Invalid NUMBER: {}", value);
                }
            }
        }

        return value;
    }

}
