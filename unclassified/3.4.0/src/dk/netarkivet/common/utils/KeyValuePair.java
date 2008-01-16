/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.Map;

/**
 * A generic Map.Entry class, useful for returning key-value-like results.
 *
 */

public class KeyValuePair<K, V> implements Map.Entry<K, V>{
    final K key;
    final V value;
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
     * Replaces the value corresponding to this entry with the specified
     * value (optional operation).
     *
     * @param value new value to be stored in this entry.
     * @return old value corresponding to the entry.
     * @throws UnsupportedOperationException if the <tt>put</tt> operation
     *                                       is not supported by the backing map.
     */
    public V setValue(V value) {
        throw new UnsupportedOperationException("Stand-alone entries cannot "
                + "be changed");
    }
}
