
package dk.netarkivet.common.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Default Free Space Provider of the number of bytes free on the file system.
 */
public class DefaultFreeSpaceProvider implements FreeSpaceProvider {
    
    /** The error logger we notify about error messages on. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Returns the number of bytes free on the file system that the given file
     * resides on. Will return 0 on non-existing files.
     *
     * @param f a given file
     * @return the number of bytes free on the file system where file f resides.
     * 0 if the file cannot be found.
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            log.warn("The file '" +  f.getAbsolutePath()
                    + "' does not exist. The value 0 returned.");
            return 0;
        }
        return f.getUsableSpace();
    }
}
