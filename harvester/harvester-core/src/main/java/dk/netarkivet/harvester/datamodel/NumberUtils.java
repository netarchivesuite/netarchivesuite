/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.datamodel;

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Number related utilities.
 */
public class NumberUtils {

    /**
     * Return the smallest value of two given positive longs, with the addition that -1 means infinity.
     *
     * @param l1 The first value
     * @param l2 The second value
     * @return Smallest value
     */
    public static long minInf(long l1, long l2) {
        if (l1 != Constants.HERITRIX_MAXBYTES_INFINITY && l2 != Constants.HERITRIX_MAXBYTES_INFINITY) {
            return Math.min(l1, l2);
        } else if (l2 != Constants.HERITRIX_MAXBYTES_INFINITY) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * Compare two given positive longs, with the addition that -1 means infinity.
     *
     * @param l1 The first value
     * @param l2 The second value
     * @return -1 if first value is smallest, 0 if equal, 1 if second value is smallest
     */
    public static int compareInf(long l1, long l2) {
        if (l1 == l2) {
            return 0;
        }
        return minInf(l1, l2) == l1 ? -1 : 1;
    }

    /**
     * Converts a list to an array of primitive values.
     *
     * @param list the list to convert
     * @return an array of primitive values
     */
    public static final double[] toPrimitiveArray(List<Double> list) {
        ArgumentNotValid.checkNotNull(list, "list");
        if (list.isEmpty()) {
            return new double[0];
        }
        double[] retArray = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            retArray[i] = list.get(i);
        }

        return retArray;
    }

}
