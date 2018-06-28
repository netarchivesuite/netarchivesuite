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
package dk.netarkivet.common.distribute.indexserver;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * An immutable pair if an index and the set this is an index for.
 *
 * @param <I> The type of set, this is an index for.
 */
public class Index<I> {

    /** The file containing the index over the set. */
    private final File indexFile;
    /** The set this is an index for. */
    private final I indexSet;

    /**
     * Initialise the set.
     *
     * @param indexFile The index file.
     * @param indexSet The set this is an index for. Can be null TODO Should the indexSet be allowed to be null?
     * @throws ArgumentNotValid if indexFile is null.
     */
    public Index(File indexFile, I indexSet) {
        ArgumentNotValid.checkNotNull(indexFile, "File indexFile");
        this.indexFile = indexFile;
        this.indexSet = indexSet;
    }

    /**
     * Get the index file.
     *
     * @return The index file.
     */
    public File getIndexFile() {
        return indexFile;
    }

    /**
     * Get the set this is an index for.
     *
     * @return The set this is an index for.
     */
    public I getIndexSet() {
        return indexSet;
    }

}
