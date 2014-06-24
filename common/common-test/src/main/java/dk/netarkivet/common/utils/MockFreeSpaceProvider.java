
package dk.netarkivet.common.utils;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Mock Free Space Provider of the number of bytes free on the file system.
 */
public class MockFreeSpaceProvider implements FreeSpaceProvider {

    /**
     * 1 TB in bytes.
     */
    public static final long ONETB = 1024*1024*1024*1024;
    
    /**
     * Returns 1 TB of bytes as the number of bytes free
     * on the file system that the given file
     * resides on.
     *
     * @param f a given file
     * @return 1 TB of bytes free
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        
        return ONETB;
    }
}
