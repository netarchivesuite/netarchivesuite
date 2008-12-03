/* $Id$
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
package dk.netarkivet.common.utils;

import java.util.BitSet;

import junit.framework.TestCase;

import dk.netarkivet.testutils.StringAsserts;


public class SparseBitSetTester extends TestCase {
    public SparseBitSetTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testFlip() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(5);
        bs.set(8);
        assertTrue("Bit 5 should be set", bs.get(5));
        assertFalse("Bit 6 should not be set", bs.get(6));
        bs.flip(5);
        bs.flip(6);
        assertFalse("Bit 5 should not be set", bs.get(5));
        assertTrue("Bit 6 should be set", bs.get(6));
        bs.flip(5);
        assertTrue("Bit 5 should be set", bs.get(6));


        bs.flip(4, 10);
        assertFalse("Bit 3 should not be set", bs.get(3));
        assertTrue("Bit 4 should be set", bs.get(4));
        assertFalse("Bit 5 should not be set", bs.get(5));
        assertFalse("Bit 6 should not be set", bs.get(6));
        assertTrue("Bit 7 should be set", bs.get(7));
        assertFalse("Bit 8 should not be set", bs.get(8));
        assertTrue("Bit 9 should be set", bs.get(9));
        assertFalse("Bit 10 should not be set", bs.get(10));
    }

    public void testSet() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(5);
        bs.set(8);
        assertTrue("Bit 5 should be set", bs.get(5));
        assertFalse("Bit 6 should not be set", bs.get(6));

        bs.set(4, 10);
        assertFalse("Bit 3 should not be set", bs.get(3));
        assertTrue("Bit 4 should be set", bs.get(4));
        assertTrue("Bit 5 should be set", bs.get(5));
        assertTrue("Bit 6 should be set", bs.get(6));
        assertTrue("Bit 7 should be set", bs.get(7));
        assertTrue("Bit 8 should be set", bs.get(8));
        assertTrue("Bit 9 should be set", bs.get(9));
        assertFalse("Bit 10 should not be set", bs.get(10));

        bs.set(7, false);
        assertFalse(bs.get(7));

        bs.set(2, true);
        assertTrue(bs.get(2));

        bs.set(6, 8, false);
        assertTrue("Bit 4 should be set", bs.get(5));
        assertFalse("Bit 5 should not be set", bs.get(6));
        assertFalse("Bit 6 should not be set", bs.get(7));
        assertTrue("Bit 7 should be set", bs.get(8));
    }

    public void testClear() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(2, 8);

        bs.clear(4);
        assertFalse(bs.get(4));

        bs.clear(7, 10);

        assertTrue(bs.get(6));
        assertFalse(bs.get(7));
        assertFalse(bs.get(8));
        assertFalse(bs.get(9));
        assertFalse(bs.get(10));

        bs.clear();
        assertFalse(bs.get(2));

        assertEquals(0, bs.cardinality());
    }


    public void testGet() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(10);
        bs.set(2, 8);
        bs.set(12);

        assertTrue(bs.get(4));
        assertFalse(bs.get(9));

        BitSet bs2 = bs.get(3, 12);
        assertEquals(6, bs2.cardinality());
        assertTrue(bs2.get(0));
        assertTrue(bs2.get(1));
        assertTrue(bs2.get(2));
        assertTrue(bs2.get(3));
        assertTrue(bs2.get(4));
        assertTrue(bs2.get(7));
        assertFalse(bs2.get(5));
        assertFalse(bs2.get(6));
        assertFalse(bs2.get(8));
    }

    public void testNextSetBit() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(10);
        bs.set(2, 8);
        bs.set(12);

        assertEquals(4, bs.nextSetBit(4));
        assertEquals(2, bs.nextSetBit(0));
        assertEquals(10, bs.nextSetBit(8));
        assertEquals(10, bs.nextSetBit(9));
        assertEquals(10, bs.nextSetBit(10));
        assertEquals(12, bs.nextSetBit(11));
        assertEquals(-1, bs.nextSetBit(13));
    }

    public void testNextClearBit() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(10);
        bs.set(2, 8);
        bs.set(12);
        bs.set(Integer.MAX_VALUE);

        assertEquals(8, bs.nextClearBit(4));
        assertEquals(8, bs.nextClearBit(2));
        assertEquals(8, bs.nextClearBit(8));
        assertEquals(9, bs.nextClearBit(9));
        assertEquals(11, bs.nextClearBit(10));
        assertEquals(11, bs.nextClearBit(11));
        assertEquals(13, bs.nextClearBit(13));
        assertEquals(Integer.MIN_VALUE, bs.nextClearBit(Integer.MAX_VALUE));

    }

    public void testLength() throws Exception {
        BitSet bs = new SparseBitSet();
        assertEquals(0, bs.length());
        bs.set(2, 8);
        assertEquals(8, bs.length());
        bs.set(10);
        assertEquals(11, bs.length());
        bs.clear(5);
        assertEquals(11, bs.length());
    }

    public void testIsEmpty() throws Exception {
        BitSet bs = new SparseBitSet();
        assertTrue(bs.isEmpty());
        bs.set(7);
        assertFalse(bs.isEmpty());
        bs.clear(7);
        assertTrue(bs.isEmpty());
        bs.set(7, 10);
        assertFalse(bs.isEmpty());
        bs.clear();
        assertTrue(bs.isEmpty());
    }

    public void testIntersects() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        assertFalse(bs1.intersects(bs2));
        assertFalse(bs2.intersects(bs1));
        bs1.set(2, 8);
        assertFalse(bs1.intersects(bs2));
        assertFalse(bs2.intersects(bs1));
        bs2.set(10);
        assertFalse(bs1.intersects(bs2));
        assertFalse(bs2.intersects(bs1));
        bs2.set(5);
        assertTrue(bs1.intersects(bs2));
        assertTrue(bs2.intersects(bs1));
        bs1.clear(5);
        assertFalse(bs1.intersects(bs2));
        assertFalse(bs2.intersects(bs1));
    }

    public void testCardinality() throws Exception {
        BitSet bs = new SparseBitSet();
        assertEquals(0, bs.cardinality());
        bs.set(2, 8);
        assertEquals(6, bs.cardinality());
        bs.set(10);
        assertEquals(7, bs.cardinality());
        bs.clear(5);
        assertEquals(6, bs.cardinality());
    }

    public void testAnd() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        bs1.and(bs2);
        assertEquals(0, bs1.length());
        bs1.set(2, 7);
        bs2.set(3);
        bs2.set(5, 9);
        bs2.set(10, 12);
        bs1.and(bs2);
        assertEquals(3, bs1.cardinality());
        assertTrue(bs1.get(3));
        assertTrue(bs1.get(5));
        assertTrue(bs1.get(6));
    }

    public void testOr() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        bs1.or(bs2);
        assertEquals(0, bs1.length());
        bs1.set(2, 7);
        bs2.set(3);
        bs2.set(5, 9);
        bs2.set(10, 12);
        bs1.or(bs2);
        assertEquals(9, bs1.cardinality());
        assertTrue(bs1.get(2));
        assertTrue(bs1.get(3));
        assertTrue(bs1.get(4));
        assertTrue(bs1.get(5));
        assertTrue(bs1.get(6));
        assertTrue(bs1.get(7));
        assertTrue(bs1.get(8));
        assertTrue(bs1.get(10));
        assertTrue(bs1.get(11));
    }

    public void testXor() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        bs1.xor(bs2);
        assertEquals(0, bs1.length());
        bs1.set(2, 7);
        bs2.set(3);
        bs2.set(5, 9);
        bs2.set(10, 12);
        bs1.xor(bs2);
        assertEquals(6, bs1.cardinality());
        assertTrue(bs1.get(2));
        assertTrue(bs1.get(4));
        assertTrue(bs1.get(7));
        assertTrue(bs1.get(8));
        assertTrue(bs1.get(10));
        assertTrue(bs1.get(11));
    }

    public void testAndNot() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        bs1.andNot(bs2);
        assertEquals(0, bs1.length());
        bs1.set(2, 7);
        bs2.set(3);
        bs2.set(5, 9);
        bs2.set(10, 12);
        bs1.andNot(bs2);
        assertEquals(2, bs1.cardinality());
        assertTrue(bs1.get(2));
        assertTrue(bs1.get(4));
    }

    public void testHashCode() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        assertEquals(bs1.hashCode(), bs2.hashCode());
        bs1.set(3, 5);
        assertFalse(bs1.hashCode() == bs2.hashCode());
        bs2.set(3);
        bs2.set(4);
        assertEquals(bs1.hashCode(), bs2.hashCode());
    }

    public void testEquals() throws Exception {
        BitSet bs1 = new SparseBitSet();
        BitSet bs2 = new SparseBitSet();
        assertEquals(bs1, bs2);
        bs1.set(3, 5);
        assertFalse(bs1.equals(bs2));
        bs2.set(3);
        bs2.set(4);
        assertEquals(bs1, bs2);
    }

    public void testClone() throws Exception {
        BitSet bs1 = new SparseBitSet();
        bs1.set(3, 5);
        BitSet bs2 = (BitSet) bs1.clone();
        assertEquals(bs1, bs2);
        bs1.set(8);
        assertFalse(bs2.get(8));
        bs2.set(10);
        assertFalse(bs1.get(10));
    }

    public void testToString() throws Exception {
        BitSet bs = new SparseBitSet();
        bs.set(54);
        bs.set(42);
        StringAsserts.assertStringContains("Should contain the indices of "
                                           + "set bits", "42", bs.toString());
        StringAsserts.assertStringContains("Should contain the indices of "
                                           + "set bits", "54", bs.toString());
    }

    public void testSize() throws Exception {
        BitSet bs = new SparseBitSet();
        assertTrue(0 <= bs.size());
        bs.set(2, 8);
        assertTrue(8 <= bs.size());
        bs.set(10);
        assertTrue(11 <= bs.size());
        bs.clear(5);
        assertTrue(11 <= bs.size());
        bs.clear();
        assertTrue(0 <= bs.size());
    }
}