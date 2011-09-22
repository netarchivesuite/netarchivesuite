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

import java.util.Map;

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldOptions;

import junit.framework.TestCase;

public class ExtendedFieldOptionsTester extends TestCase {

    public ExtendedFieldOptionsTester(String s) {
        super(s);
    }

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
    	assertEquals(eo.getOptionsString(), "key" + ExtendedFieldOptions.KEYVALUESEPARATOR + "value" + ExtendedFieldOptions.NEWLINE);
    	assertTrue(eo.isKeyValid("key"));
    	assertFalse(eo.isKeyValid("foo"));
    }
}
