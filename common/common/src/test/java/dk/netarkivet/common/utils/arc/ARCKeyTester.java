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
package dk.netarkivet.common.utils.arc;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: lc
 * Date: Nov 4, 2004
 * Time: 2:38:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ARCKeyTester extends TestCase {
    /** Test that the constructore figures out to use .arc.gz files
     * when given a .dat entry.
     */
    public void testConstructor() {
        ARCKey key1 = new ARCKey("foo.arc", 0);
        assertEquals(key1.getFile().getName(), "foo.arc");
        ARCKey key2 = new ARCKey("foo.dat", 0);
        assertEquals(key2.getFile().getName(), "foo.arc.gz");
    }
}
