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

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValues;
import junit.framework.TestCase;

public class ExtendedFieldDefaultValuesTester extends TestCase {

    public ExtendedFieldDefaultValuesTester(String s) {
        super(s);
    }

    public void testInValid() {
    	ExtendedFieldDefaultValues e = null;
    	
    	e = new ExtendedFieldDefaultValues("", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValues("foo", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());

    	e = new ExtendedFieldDefaultValues("bar", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());

    	e = new ExtendedFieldDefaultValues(null, null, ExtendedFieldDataTypes.BOOLEAN);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValues("a", "0000", ExtendedFieldDataTypes.NUMBER);
    	assertFalse(e.isValid());
    	
    	e = new ExtendedFieldDefaultValues("12:08", "hh:mm a", ExtendedFieldDataTypes.TIMESTAMP);
    	assertFalse(e.isValid());
    }
    
    public void testValid() {
    	ExtendedFieldDefaultValues e = null;

    	e = new ExtendedFieldDefaultValues("true", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("t", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("1", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("false", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("f", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("true", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	e = new ExtendedFieldDefaultValues("0", null, ExtendedFieldDataTypes.BOOLEAN);
    	assertTrue(e.isValid());
    	
    	e = new ExtendedFieldDefaultValues("0012", "0000", ExtendedFieldDataTypes.NUMBER);
    	assertTrue(e.isValid());

    	e = new ExtendedFieldDefaultValues("12:08 PM", "h:mm a", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    	
    	e = new ExtendedFieldDefaultValues("12:08", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    	
    	// this is also valid, because not the whole string will be used for parsing!
    	e = new ExtendedFieldDefaultValues("12:08:00", "hh:mm", ExtendedFieldDataTypes.TIMESTAMP);
    	assertTrue(e.isValid());
    }
}
