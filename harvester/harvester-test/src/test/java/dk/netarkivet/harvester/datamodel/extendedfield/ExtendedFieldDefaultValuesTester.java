/*
 * #%L
 * Netarchivesuite - harvester - test
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExtendedFieldDefaultValuesTester {

    @Test
    public void testInValid() {
        ExtendedFieldDefaultValue e = null;

        e = new ExtendedFieldDefaultValue("", null, ExtendedFieldDataTypes.BOOLEAN);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue("foo", null, ExtendedFieldDataTypes.BOOLEAN);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue("bar", null, ExtendedFieldDataTypes.BOOLEAN);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue(null, null, ExtendedFieldDataTypes.BOOLEAN);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue("a", "0000", ExtendedFieldDataTypes.NUMBER);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue("12:08", "hh:mm a", ExtendedFieldDataTypes.TIMESTAMP);
        assertFalse(e.isValid());

        e = new ExtendedFieldDefaultValue("12:03", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
        assertFalse(e.isValid());

    }

    @Test
    public void testValid() {
        ExtendedFieldDefaultValue e = null;

        e = new ExtendedFieldDefaultValue("true", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("t", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("1", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("false", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("f", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("true", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());
        e = new ExtendedFieldDefaultValue("0", null, ExtendedFieldDataTypes.BOOLEAN);
        assertTrue(e.isValid());

        e = new ExtendedFieldDefaultValue("0012", "0000", ExtendedFieldDataTypes.NUMBER);
        assertTrue(e.isValid());

        e = new ExtendedFieldDefaultValue("12:08 PM", "h:mm a", ExtendedFieldDataTypes.TIMESTAMP);
        assertTrue(e.isValid());

        e = new ExtendedFieldDefaultValue("12:08", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
        assertTrue(e.isValid());

        // this is also valid, because not the whole string will be used for parsing!
        e = new ExtendedFieldDefaultValue("12:08:00", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
        assertTrue(e.isValid());

        e = new ExtendedFieldDefaultValue("12/03/2000", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
        assertTrue(e.isValid());
    }

    @Test
    public void testValuesForDB() {
        ExtendedFieldDefaultValue e = null;
        String val;

        e = new ExtendedFieldDefaultValue("0012", "0000", ExtendedFieldDataTypes.NUMBER);
        val = e.getDBValue();
        assertEquals(val, "12.0");

        e = new ExtendedFieldDefaultValue("00:01", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
        val = e.getDBValue();
        assertEquals(val, "60000");

        e = new ExtendedFieldDefaultValue("12/03/2000", "dd/MM/yyyy", ExtendedFieldDataTypes.JSCALENDAR);
        val = e.getDBValue();
        assertEquals(val, "952819200000");
    }
}
