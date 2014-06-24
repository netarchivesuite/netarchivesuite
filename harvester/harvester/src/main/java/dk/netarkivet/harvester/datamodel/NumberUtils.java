
package dk.netarkivet.harvester.datamodel;

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Number related utilities.
 *
 */
public class NumberUtils {
    /**
     * Return the smallest value of two given positive longs, with the addition
     * that -1 means infinity.
     * @param l1 The first value
     * @param l2 The second value
     * @return Smallest value
     */
    public static long minInf(long l1, long l2) {
        if (l1 != Constants.HERITRIX_MAXBYTES_INFINITY
                && l2 != Constants.HERITRIX_MAXBYTES_INFINITY) {
            return Math.min(l1, l2);
        } else if (l2 != Constants.HERITRIX_MAXBYTES_INFINITY) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * Compare two given positive longs, with the addition that
     * -1 means infinity.
     * @param l1 The first value
     * @param l2 The second value
     * @return -1 if first value is smallest, 0 if equal, 1 if second value is
     * smallest
     */
    public static int compareInf(long l1, long l2) {
        if (l1 == l2) {
            return 0;
        }
        return minInf(l1, l2) == l1 ? -1 : 1;
    }

    /**
     * Converts a list to an array of primitive values.
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
