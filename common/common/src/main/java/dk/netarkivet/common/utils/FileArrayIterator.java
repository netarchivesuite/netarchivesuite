
package dk.netarkivet.common.utils;

import java.io.File;
import java.util.Arrays;

/**
 * An iterator that iterates over elements that can be read from files,
 * given an array of files.  It is robust against disappearing files,
 * but does not try to find new ones that appear while iterating.  It
 * keeps the Iterator contract that next() returns an element if hasNext()
 * returned true since last next().  This may mean that the underlying
 * file has disappeared by the time next() is called, but the object is
 * returned anyway.
 *
 * @param <T> The type returned by the FileArrayIterator
 */

public abstract class FileArrayIterator<T> extends FilterIterator<File,T> {
    protected FileArrayIterator(File[] files) {
        super(Arrays.asList(files).iterator());
    }

    /** Returns the T object corresponding to the given file, or null if
     * that object is to be skipped.
     *
     * @param f A given file
     * @return An object in the T domain, or null
     */
    protected T filter(File f) {
        return getNext(f);
    }

    /** Gives an object created from the given file, or null.
     *
     * @param file The file to read
     * @return An object of the type iterated over by the list, or null
     * if the file does not exist or cannot be used to create an appropriate
     * object.
     */
    protected abstract T getNext(final File file);

}
