/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.TableSort.SortOrder;

/**
 * Unittests for the {@link TableSort} class.
 */
public class TablesortTester {

    /**
     * Test of TableSort constructor. Note: no validation of columnId in TableSort class.
     */
    @Test
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

    @Test
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
