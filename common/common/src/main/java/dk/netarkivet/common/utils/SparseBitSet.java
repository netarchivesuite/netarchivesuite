/* $Id$
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
package dk.netarkivet.common.utils;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/** A sparse implementation of a BitSet, that does not require memory linear
 * to the largest index. This is done at the cost of performance, but should
 * be fairly efficient on few set bits. */
public class SparseBitSet extends BitSet {
    /** A set of the indices of bits that are set in this BitSet. */
    private Set<Integer> setbits = new HashSet<Integer>();

    /**
     * Initialise the bitset.
     */
    public SparseBitSet() {
        //Initialise super class to a zero-length bitset, to avoid allocating
        //a bit array.
        super(0);
    }

    /**
     * @see BitSet#flip(int)
     */
    public void flip(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        if (setbits.contains(bitIndex)) {
            setbits.remove(bitIndex);
        } else {
            setbits.add(bitIndex);
        }
    }

    /**
     * @see BitSet#flip(int, int)
     */
    public void flip(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                                                    " > toIndex: " + toIndex);
        }
        for (int i = fromIndex; i < toIndex; i++) {
            flip(i);
        }
    }

    /**
     * @see BitSet#set(int)
     */
    public void set(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        setbits.add(bitIndex);
    }

    /**
     * @see BitSet#set(int, boolean)
     */
    public void set(int bitIndex, boolean value) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        if (value) {
            setbits.add(bitIndex);
        } else {
            setbits.remove(bitIndex);
        }
    }

    /**
     * @see BitSet#set(int, int)
     */
    public void set(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                                                    " > toIndex: " + toIndex);
        }
        for (int i = fromIndex; i < toIndex; i++) {
            set(i);
        }
    }

    /**
     * @see BitSet#set(int, int, boolean)
     */
    public void set(int fromIndex, int toIndex, boolean value) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                                                    " > toIndex: " + toIndex);
        }
        for (int i = fromIndex; i < toIndex; i++) {
            set(i, value);
        }
    }

    /**
     * @see BitSet#clear(int)
     */
    public void clear(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        setbits.remove(bitIndex);
    }

    /**
     * @see BitSet#clear(int, int)
     */
    public void clear(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                                                    " > toIndex: " + toIndex);
        }
        for (int i = fromIndex; i < toIndex; i++) {
            clear(i);
        }
    }

    /**
     * @see BitSet#clear()
     */
    public void clear() {
        setbits.clear();
    }

    /**
     * @see BitSet#get(int)
     */
    public boolean get(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        return setbits.contains(bitIndex);
    }

    /**
     * @see BitSet#get(int, int)
     */
    public BitSet get(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                                                    " > toIndex: " + toIndex);
        }
        SparseBitSet bitsubset = new SparseBitSet();
        for (int i : setbits) {
            if (i >= fromIndex && i < toIndex) {
                bitsubset.set(i - fromIndex);
            }
        }
        return bitsubset;
    }

    /**
     * @see BitSet#nextSetBit(int)
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        int index = -1;
        for (Integer i : setbits) {
            if (i >= fromIndex && (index == -1 || i < index)) {
                index = i;
            }
        }
        return index;
    }

    /**
     * @see BitSet#nextClearBit(int)
     */
    public int nextClearBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        for (int i = fromIndex; i > 0; i++) {
            if (!get(i)) {
                return i;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * @see BitSet#length()
     */
    public int length() {
        int index = -1;
        for (Integer i : setbits) {
            if (i > index) {
                index = i;
            }
        }
        return index + 1;
    }

    /**
     * @see BitSet#isEmpty()
     */
    public boolean isEmpty() {
        return setbits.isEmpty();
    }

    /**
     * @see BitSet#intersects(BitSet)
     */
    public boolean intersects(BitSet set) {
        for (Integer index : setbits) {
            if (set.get(index)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @see BitSet#cardinality()
     */
    public int cardinality() {
        return setbits.size();
    }

    /**
     * @see BitSet#and(BitSet)
     */
    public void and(BitSet set) {
        Set<Integer> andbits = new HashSet<Integer>();
        for (Integer index : setbits) {
            if (set.get(index)) {
                andbits.add(index);
            }
        }
        setbits = andbits;
    }

    /**
     * @see BitSet#or(BitSet)
     */
    public void or(BitSet set) {
        Set<Integer> orbits = new HashSet<Integer>(setbits);
        for (int index = set.nextSetBit(0); index != -1;
             index = set.nextSetBit(index + 1)) {
            orbits.add(index);
        }
        setbits = orbits;
    }

    /**
     * @see BitSet#xor(BitSet)
     */
    public void xor(BitSet set) {
        Set<Integer> xorbits = new HashSet<Integer>();
        for (Integer index : setbits) {
            if (!set.get(index)) {
                xorbits.add(index);
            }
        }
        for (int index = set.nextSetBit(0); index != -1;
             index = set.nextSetBit(index + 1)) {
            if (!setbits.contains(index)) {
                xorbits.add(index);
            }
        }
        setbits = xorbits;
    }

    /**
     * @see BitSet#andNot(BitSet)
     */
    public void andNot(BitSet set) {
        Set<Integer> andnotbits = new HashSet<Integer>(setbits);
        for (Integer index : setbits) {
            if (set.get(index)) {
                andnotbits.remove(index);
            }
        }
        setbits = andnotbits;
    }

    /**
     * A hash code for this bit set.
     * Note: The hash codes are not implemented to be compatible with
     * java.util.BitSet#hashCode(). Implementing that algorithm would be
     * difficult and inefficient on the current implementation.
     * @return A hashcode. 
     */
    public int hashCode() {
        return setbits.hashCode();
    }

    /**
     * In contrast with {@link BitSet#size()} this does not return the
     * size in bytes used to represent this set. Instead, it returns the
     * same as {@link #length()} for compatibility with
     * {@link BitSet}. The actual space used is a hashset of size
     * {@link #cardinality()}.
     */
    public int size() {
        return length();
    }

    /**
     * Two SparseBitSets are considered equal if they contain the same bits.
     *
     * Note: A SparseBitSet is never considered equal to a BitSet. This would
     * be impossible to implement in a way so equality is symmetric, since
     * {@link BitSet#equals(Object)} is implemented using its private fields to
     * determine equality.
     *
     * @param obj The object to compare for equality.
     *
     * @return true, if obj is a SparseBitSet and contains the same bits as this
     * object.
     */
    public boolean equals(Object obj) {
        return obj instanceof SparseBitSet
               && setbits.equals(((SparseBitSet) obj).setbits);
    }

    /**
     * @see BitSet#clone()
     */
    public Object clone() {
        super.clone();
        SparseBitSet newSparseBitSet = new SparseBitSet();
        newSparseBitSet.setbits = new HashSet<Integer>(setbits);
        return newSparseBitSet;
    }

    /**
     * @see BitSet#toString()
     */
    public String toString() {
        return setbits.toString();
    }
}
