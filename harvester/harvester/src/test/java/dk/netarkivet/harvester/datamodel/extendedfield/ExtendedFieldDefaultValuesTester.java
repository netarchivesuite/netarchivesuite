/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import junit.framework.TestCase;

public class ExtendedFieldDefaultValuesTester extends TestCase {

    public ExtendedFieldDefaultValuesTester(String s) {
        super(s);
    }

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
    }
    
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
    }
}
