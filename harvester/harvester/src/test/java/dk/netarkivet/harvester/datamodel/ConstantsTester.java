/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.regex.Matcher;

import junit.framework.TestCase;

/**
 * Unit-tests for the Constants class.
 * Only tests the static method getIdMatcher.
 */
public class ConstantsTester extends TestCase {
    public ConstantsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testGetIdMatcher() throws Exception {
        Matcher m1 = Constants.getIdMatcher();
        Matcher m2 = Constants.getIdMatcher();
        assertNotSame("Two calls to getIdMatcher() should return two objects",
                m1, m2);
        m1.reset("foobar_87.xml");
        assertTrue("Should match properly formed string", m1.matches());
        assertEquals("Group 1 should be set to the number", "87", m1.group(1));
        m2.reset("foobar_89.foo");
        assertFalse("Should not match with wrong suffix", m2.matches());
        m2.reset("foobar_89_.xml");
        assertFalse("Should not match with extra chars", m2.matches());
        m2.reset("foobar_.xml");
        assertFalse("Should not match with no numbers", m2.matches());
        m2.reset("foobar_8#xml");
        assertFalse("Should not match with wrong suffix", m2.matches());
    }
}