/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.common.utils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.TableSort.SortOrder;
import junit.framework.TestCase;

/**
 * Unittests for the {@link TableSort} class. 
 */
public class TablesortTester extends TestCase {
 
    public TablesortTester(String s) {
        super(s);
    }
    /**
     * Test of TableSort constructor.
     * Note: no validation of columnId in TableSort class.
     */
    public void testConstructor() {
        try {
            new TableSort(0, null);
            fail("Should throw ArgumentNotValid on null SortOrder, but didn't");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        // this is currently valid, but probably shouldn't be
        try {
            new TableSort(-99, SortOrder.INCR);
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid on negative columnID, but did");
        }
        
        TableSort ts = new TableSort(0, TableSort.SortOrder.DESC);
        assertEquals(0, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.DESC, ts.getOrder());
    }
    
    public void testGetters() {
        TableSort ts = new TableSort(0, TableSort.SortOrder.DESC);
        ts.setColumnIdent(99);
        assertEquals(99, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.DESC, ts.getOrder());
        ts.setOrder(TableSort.SortOrder.INCR);
        assertEquals(99, ts.getColumnIdent());
        assertEquals(TableSort.SortOrder.INCR, ts.getOrder());
    }
    
}