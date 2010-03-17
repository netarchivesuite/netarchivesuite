/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that filters out and converts items from another iterator.
 * Java 1.5 type:
 * FilterIterator<T>,<S>>
 * <S> filter(<T> o);
 * @param <T> Type of Iterator
 * @param <S> Type of objects returned by the iterator.
 */

public abstract class FilterIterator<T, S> implements Iterator<S> {
    private S objectcache;

    private final Iterator<T> iter;

    /** Create a new iterator based on an old one.
     * The old one must not contain any null entries.
     *
     * @param i An iterator
     */
    public FilterIterator(Iterator<T> i) {
        iter = i;
    }

    /** Returns the object corresponding to the given object, or null if
     * that object is to be skipped.
     *
     * @param o An object in the source iterator domain
     * @return An object in this iterators domain, or null
     */
    protected abstract S filter(T o);

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        while (iter.hasNext() && objectcache == null) {
            objectcache = filter(iter.next());
        }
        return objectcache != null;
    }

    /**
     * Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration.
     * @throws NoSuchElementException iteration has no more elements.
     */
    public S next() {
        if (hasNext()) {
            S o = objectcache;
            objectcache = null;
            return o;
        }

        throw new NoSuchElementException("No more accepted elements");

    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     *
     * @throws UnsupportedOperationException
     *              if the <tt>remove</tt>
     *              operation is not supported by this Iterator.
     * @throws IllegalStateException
     *              if the <tt>next</tt> method has not
     *              yet been called, or the <tt>remove</tt> method has already
     *              been called after the last call to the <tt>next</tt>
     *              method.
     */
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove from this iterator");
    }
}
