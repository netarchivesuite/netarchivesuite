
package dk.netarkivet.common.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Various utilities to work with iterators more easily.
 *
 */

public class IteratorUtils {

    /** Turns an iterator into a list.
     * @param i an iterator
     * @return a List
     */
    public static <T> List<T> toList(Iterator<T> i) {
        List<T> res = new ArrayList<T>();
        while (i.hasNext()) {
            res.add(i.next());
        }
        return res;
    }
}
