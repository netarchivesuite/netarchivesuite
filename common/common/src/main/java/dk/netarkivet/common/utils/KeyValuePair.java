/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.Map;

/**
 * A generic Map.Entry class, useful for returning key-value-like results.
 * @param <K> the Object type used as key
 * @param <V> the Object type used as value
 */
public class KeyValuePair<K, V> implements Map.Entry<K, V> {
    /** The key in this key-value pair. */
    private final K key;
    /** The value in this key-value pair. */
    private final V value;
    
    /**
     * Constructs a Key-Value pair using the given key and value.
     * @param k The key object
     * @param v The value object
     */
    public KeyValuePair(K k, V v) {
        this.key = k;
        this.value = v;
    }

    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry.
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the value corresponding to this entry.
     *
     * @return the value corresponding to this entry.
     */
    public V getValue() {
        return value;
    }

    /**
     * Replaces the value corresponding to this entry with the specified value
     * (optional operation).
     * 
     * @param newValue
     *            new value to be stored in this entry.
     * @return old value corresponding to the entry.
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by the backing
     *             map.
     */
    public V setValue(V newValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Stand-alone entries cannot "
                + "be changed");
    }
}
