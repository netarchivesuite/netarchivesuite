/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.common.utils.cdx;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit-tests for class CDXRecord.
 */
public class CDXRecordTester extends TestCase {
    /**
     * Test, that the constructor throws an ArgumentNotValid
     * in case of:
     * 1) argument is null;
     * 2) bad number fields (< 7).
     */
    public void testContructorBadArguments() {
        String[] fields = new String[]{};
        //test 1.
        try {
            new CDXRecord((String[])null);
            fail("null as argument should throw argumentNotValid");
        } catch (ArgumentNotValid e){
            // Expected behaviour
        }

        //test 2.
        try {
            new CDXRecord(fields);
            fail("less than 7 fields should throw argumentNotValid");
        } catch (ArgumentNotValid e){
            // Expected behaviour
        }
    }

    /**
     * Test, that the constructor reacts correctly
     * in case of length of argument is 7 or more.
     * TODO: Shouldn't we log, if fields.length > 7
     */
    public void testContructorValidArguments(){
        String[] fields = new String[]{
                "http://netarkivet.dk/index.html",
                "194.255.126.118",
                "20040511211314",
                "image/jpeg",
                "4202",
                "fyensdk.arc",
                "422381",
                "5aae10cf1c10572a240a99ec2a1b3bd7"
        };
        CDXRecord cr = new CDXRecord(fields);
        assertEquals("URI is not correctly saved in object",
                fields[0], cr.getURL());
        assertEquals("IP is not correctly saved in object",
                fields[1], cr.getIP());
        assertEquals("Timestamp is not correctly saved in object",
                fields[2], cr.getDate());
        assertEquals("Mimetype is not correctly saved in object",
                fields[3], cr.getMimetype());
        assertEquals("Length is not correctly saved in object",
                Long.parseLong(fields[4]), cr.getLength());
        assertEquals("Arcfile is not correctly saved in object",
                fields[5], cr.getArcfile());
        assertEquals("Offset is not correctly saved in object",
                Long.parseLong(fields[6]), cr.getOffset());
    }

}
