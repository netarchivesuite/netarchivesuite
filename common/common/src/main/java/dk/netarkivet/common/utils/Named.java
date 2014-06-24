package dk.netarkivet.common.utils;

/**
 * This interface describes objects that have a name.
 * The name of an object should uniquely identify that object
 * within a certain collection of objects, but not necessarily globally.
*
 */
public interface Named {
    /** Get the name of this object.
     *
     * @return The name of this object.
     */
    String getName();

    /** Get the comment of this object.
     *
     * @return The name of this object.
     */
    String getComments();

}
