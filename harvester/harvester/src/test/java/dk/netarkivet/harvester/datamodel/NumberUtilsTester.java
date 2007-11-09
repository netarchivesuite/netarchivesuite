/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
/**
 * kfc forgot to comment this!
 */

import junit.framework.TestCase;


public class NumberUtilsTester extends TestCase {
    public NumberUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Test minimum where -1 means inifinite */
    public void testMinInf() throws Exception {
        assertEquals("-1 is greater than all",
                     Long.MAX_VALUE, NumberUtils.minInf(-1L, Long.MAX_VALUE));
        assertEquals("-1 is greater than all",
                     Long.MAX_VALUE, NumberUtils.minInf(Long.MAX_VALUE, -1L));
        assertEquals("-1 is greater than all",
                     0L, NumberUtils.minInf(-1L, 0));
        assertEquals("-1 is greater than all",
                     0L, NumberUtils.minInf(0, -1L));
        assertEquals("-1 and -1 gives -1",
                     -1L, NumberUtils.minInf(-1L, -1L));
        assertEquals("On non-inifinite, give smalles",
                     42L, NumberUtils.minInf(42L, 54L));
        assertEquals("On non-inifinite, give smalles",
                     42L, NumberUtils.minInf(54L, 42L));
        assertEquals("On non-inifinite, give smalles",
                     42L, NumberUtils.minInf(42L, 42L));
    }

    /** Test comparing where -1 means inifinite */
    public void testCompareInf() throws Exception {
        assertEquals("-1 is greater than all",
                     1, NumberUtils.compareInf(-1L, Long.MAX_VALUE));
        assertEquals("-1 is greater than all",
                     -1, NumberUtils.compareInf(Long.MAX_VALUE, -1L));
        assertEquals("-1 is greater than all",
                     1, NumberUtils.compareInf(-1L, 0));
        assertEquals("-1 is greater than all",
                     -1, NumberUtils.compareInf(0, -1L));
        assertEquals("-1 and -1 are equal",
                     0, NumberUtils.compareInf(-1L, -1L));
        assertEquals("On non-inifinite, compare",
                     -1, NumberUtils.compareInf(42L, 54L));
        assertEquals("On non-inifinite, compare",
                     1, NumberUtils.compareInf(54L, 42L));
        assertEquals("On non-inifinite, compare",
                     0, NumberUtils.compareInf(42L, 42L));
    }
}