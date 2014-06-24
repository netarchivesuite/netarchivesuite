
package dk.netarkivet.harvester.datamodel;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import dk.netarkivet.common.utils.Named;

/**
 * Utilities for handling named objects. Named objects are objects in our
 * datamodel, which have a name and a comment.
 */
public class NamedUtils {
    /**
     * Sorts List of Named objects according to language defined in parameter
     * loc. The sorting is done via a compare function on named objects. The
     * compare function uses Collator for sorting according to language in loc.
     * The compare function is used as Comparator for the Collection Sorting on
     * Named object Lists.
     *
     * @param loc  contains the language sorting must adhere to
     * @param list contains list to be sorted. Objects in the List must
     *             implement Named
     */
    public static <T extends Named> void sortNamedObjectList(
            final Locale loc, List<T> list) {
        Collections.sort(list,
                         new Comparator<Named>() {
                             public int compare(Named o1, Named o2) {
                                 Collator myCollator
                                         = Collator.getInstance(loc);
                                 return myCollator.compare(o1.getName(),
                                                           o2.getName());
                             }
                         }
        );
    }
}
