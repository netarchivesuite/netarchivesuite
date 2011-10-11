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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class for constructing, validating, and keeping the default
 * value for a single ExtendedField.
 */
public class ExtendedFieldDefaultValues {

    final static String[] possibleTrueValues = { "true", "t", "1" };

    final static String[] possibleFalseValues = { "false", "f", "0" };

    final boolean valid;

    String value;

    String format;

    int datatype;

    public ExtendedFieldDefaultValues(String aValue, String aFormat,
            int aDatatype) {
        value = aValue;
        format = aFormat;
        datatype = aDatatype;

        valid = validate();
    }

    private boolean validate() {
        switch (datatype) {
        case ExtendedFieldDataTypes.BOOLEAN:
            try {
            	checkBoolean(value);
            } catch (ArgumentNotValid e) {
                return false;
            }
            break;
        case ExtendedFieldDataTypes.NUMBER:
            if (format != null && format.length() > 0) {
                DecimalFormat decimalFormat = new DecimalFormat(format);
                try {
                    decimalFormat.parse(value);
                } catch (ParseException e) {
                    return false;
                }
            }
            break;
        case ExtendedFieldDataTypes.TIMESTAMP:
            if (format != null && format.length() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                try {
                    dateFormat.parse(value);
                } catch (ParseException e) {
                    return false;
                }
            }
            break;
        default:
            break;
        }

        return true;
    }

    private String checkBoolean(String aBooleanValue) throws ArgumentNotValid {
        aBooleanValue = value.toLowerCase().trim();

        for (String val : possibleTrueValues) {
            if (aBooleanValue.equals(val)) {
                return possibleTrueValues[0];
            }
        }

        for (String val : possibleFalseValues) {
            if (aBooleanValue.equals(val)) {
                return possibleFalseValues[0];
            }
        }

        throw new ArgumentNotValid("The string '" 
        		+ aBooleanValue + "' is not a valid Boolean value");
    }

    public boolean isValid() {
        return valid;
    }
}
