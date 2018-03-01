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

import java.io.File;
import java.util.Arrays;

/**
 * An iterator that iterates over elements that can be read from files, given an array of files. It is robust against
 * disappearing files, but does not try to find new ones that appear while iterating. It keeps the Iterator contract
 * that next() returns an element if hasNext() returned true since last next(). This may mean that the underlying file
 * has disappeared by the time next() is called, but the object is returned anyway.
 *
 * @param <T> The type returned by the FileArrayIterator
 */
public abstract class FileArrayIterator<T> extends FilterIterator<File, T> {

    protected FileArrayIterator(File[] files) {
        super(Arrays.asList(files).iterator());
    }

    /**
     * Returns the T object corresponding to the given file, or null if that object is to be skipped.
     *
     * @param f A given file
     * @return An object in the T domain, or null
     */
    protected T filter(File f) {
        return getNext(f);
    }

    /**
     * Gives an object created from the given file, or null.
     *
     * @param file The file to read
     * @return An object of the type iterated over by the list, or null if the file does not exist or cannot be used to
     * create an appropriate object.
     */
    protected abstract T getNext(final File file);

}
