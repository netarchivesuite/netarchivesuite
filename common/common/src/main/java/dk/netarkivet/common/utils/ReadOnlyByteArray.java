/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
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

/**
 * Implements access to an array in a read-only fashion.
 */

public class ReadOnlyByteArray {
    private byte[] array;
    /** Creates a new instance based on the given array.
     *
     * @param array Array to provide read-only access to.  The array will
     * not be copied by this class.
     */
    public ReadOnlyByteArray(byte[] array) {
        this.array = array;
    }

    /** Returns the length of the array.
     *
     * @return The length of the array.  Always >= 0.
     */
    public int length() {
        return array.length;
    }

    /** Gets the element at the given index.
     *
     * @param index The index to get the element at.
     * @return The byte at the given index.
     * @throws IndexOutOfBoundsException if the index is < 0 or > length()
     */
    public byte get(int index) {
        return array[index];
    }
}
