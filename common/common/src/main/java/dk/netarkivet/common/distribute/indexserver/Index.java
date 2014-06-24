package dk.netarkivet.common.distribute.indexserver;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** An immutable pair if an index and the set this is an index for. 
 * @param <I> The type of set, this is an index for.
 */
public class Index<I> {
    /** The file containing the index over the set. */
    private final File indexFile;
    /** The set this is an index for. */
    private final I indexSet;

    /**
     * Initialise the set.
     * @param indexFile The index file.
     * @param indexSet The set this is an index for. Can be null
     * TODO Should the indexSet be allowed to be null?
     *
     * @throws ArgumentNotValid if indexFile is null.
     */
    public Index(File indexFile, I indexSet) {
        ArgumentNotValid.checkNotNull(indexFile, "File indexFile");
        this.indexFile = indexFile;
        this.indexSet = indexSet;
    }

    /** Get the index file.
     *
     * @return The index file.
     */
    public File getIndexFile() {
        return indexFile;
    }

    /** Get the set this is an index for.
     *
     * @return The set this is an index for.
     */
    public I getIndexSet() {
        return indexSet;
    }
}
