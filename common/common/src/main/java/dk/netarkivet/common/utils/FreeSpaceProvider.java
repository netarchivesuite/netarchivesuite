
package dk.netarkivet.common.utils;

import java.io.File;

/**
 * This interface encapsulates providing the number of bytes
 * free on the file system.
 *
 */
public interface FreeSpaceProvider {
    /**
    * @param f a given file
    * @return the number of bytes free on the file system where file f resides.
    * 0 if the file cannot be found.
    */
    long getBytesFree(File f);
}
