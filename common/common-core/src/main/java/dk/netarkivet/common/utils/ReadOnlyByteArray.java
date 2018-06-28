/*
 * #%L
 * Netarchivesuite - common
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

/**
 * Implements access to an array in a read-only fashion.
 */
public class ReadOnlyByteArray {

    private byte[] array;

    /**
     * Creates a new instance based on the given array.
     *
     * @param array Array to provide read-only access to. The array will not be copied by this class.
     */
    public ReadOnlyByteArray(byte[] array) {
        this.array = array;
    }

    /**
     * Returns the length of the array.
     *
     * @return The length of the array. Always >= 0.
     */
    public int length() {
        return array.length;
    }

    /**
     * Gets the element at the given index.
     *
     * @param index The index to get the element at.
     * @return The byte at the given index.
     * @throws IndexOutOfBoundsException if the index is < 0 or > length()
     */
    public byte get(int index) {
        return array[index];
    }

}
