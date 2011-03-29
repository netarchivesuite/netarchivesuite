/*$Id$
* $Revision$
* $Date$
* $Author$
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

import java.util.Date;
import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * 
 * Unit tests for the DomainOwner class.
 *
 */
public class DomainOwnerInfoTester extends TestCase {
    public DomainOwnerInfoTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testCompareTo() throws Exception {
        DomainOwnerInfo i1 = new DomainOwnerInfo(new Date(1), "foo");
        DomainOwnerInfo i2 = new DomainOwnerInfo(new Date(2), "bar");
        DomainOwnerInfo i3 = new DomainOwnerInfo(new Date(0), "baz");

        try {
            i1.compareTo(null);
            fail("Failed to throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertTrue("Earlier domain owner info should compare less",
                i1.compareTo(i2) < 0);
        assertTrue("Later domain owner info should compare greater",
                i2.compareTo(i3) > 0);
        assertTrue("Same domain owner info should compare equals",
                i2.compareTo(i2) == 0);
    }
}