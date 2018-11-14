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

import java.util.Map;

import org.junit.Test;

public class ExtendedFieldOptionsTester {
    @Test
    public void testOptions() {
        String line = null;
        ExtendedFieldOptions eo = null;
        Map<String, String> result = null;

        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "\n";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "\n\n";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "key";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "key=";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "=value";
        eo = new ExtendedFieldOptions(line);
        assertFalse(eo.isValid());

        line = "key=value";
        eo = new ExtendedFieldOptions(line);
        assertTrue(eo.isValid());

        line = "key=value";
        eo = new ExtendedFieldOptions(line);
        assertTrue(eo.isValid());
        result = eo.getOptions();
        assertEquals(result.get("key"), "value");

        line = "key=value\nfoo=bar";
        eo = new ExtendedFieldOptions(line);
        assertTrue(eo.isValid());
        result = eo.getOptions();
        assertEquals(result.get("key"), "value");
        assertEquals(result.get("foo"), "bar");
        assertTrue(eo.isKeyValid("key"));
        assertTrue(eo.isKeyValid("foo"));
        assertFalse(eo.isKeyValid("bar"));

        line = "key=value\nfoo=";
        eo = new ExtendedFieldOptions(line);
        assertTrue(eo.isValid());
        result = eo.getOptions();
        assertEquals(result.get("key"), "value");
        assertEquals(result.size(), 1);
        assertEquals(eo.getOptionsString(), "key" + ExtendedFieldOptions.KEYVALUESEPARATOR + "value"
                + ExtendedFieldOptions.NEWLINE);
        assertTrue(eo.isKeyValid("key"));
        assertFalse(eo.isKeyValid("foo"));
    }
}
