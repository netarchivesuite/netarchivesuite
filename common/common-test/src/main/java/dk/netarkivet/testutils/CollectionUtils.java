
package dk.netarkivet.testutils;

import java.util.Arrays;
import java.util.List;

/**
 * Miscellaneous utility functions for collections that Sun neglected to make.
 *
 */

public class CollectionUtils {
    /** Return all the arguments as a list.
     *
     * @param args Objects to put into a list
     * @return A list containing all the objects in that order
     */
    public static <T> List<T> list(T... args) {
        return Arrays.asList(args);
    }
}
