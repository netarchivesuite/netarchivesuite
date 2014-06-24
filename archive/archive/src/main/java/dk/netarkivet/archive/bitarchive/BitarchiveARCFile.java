
package dk.netarkivet.archive.bitarchive;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

import java.io.File;

/** The representation of an ARC file in the bit archive.
 * This class keeps the connection between the name that was used
 * for lookup and the file that was found.
 */
public class BitarchiveARCFile  {
    /** The ARC file name (with no path). */
    private String fileName;
    /** The path of the file in the archive. */
    private File filePath;

    /** Create a new representation of a file in the archive.
     * Note that <code>fn</code> is not necessarily, though probably, the
     * same as <code>fp.getName()</code>.
     *
     * Failed lookups should be represented by null references rather than
     * an object representing something that doesn't exist.
     *
     * @param fn The ARC name of the file, as used in lookup in the archive.
     * @param fp The actual path of the file in the archive.
     * @throws ArgumentNotValid if either argument is null, or any of the file
     * name representaitons is the empty string.
     */
    public BitarchiveARCFile(String fn, File fp) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(fp, "File fp");
        if (fp.getName().isEmpty()) {
            throw new ArgumentNotValid("fp denotes an empty filename");
        }
        ArgumentNotValid.checkNotNullOrEmpty(fn, "String fn");
        fileName = fn;
        filePath = fp;
    }

    /** Return true if the file exists, false otherwise.
     * Note that failure to exist indicates a severe error in the bit archive,
     * not just that the lookup failed.
     *
     * @return Whether the file exists
     */
    public boolean exists() {
        return filePath.exists();
    }

    /** Get the ARC name of this file.  This is the name that the file can
     * be found under when looking up in the bit archive.
     * @return A String representing the ARC name of this file.
     */
    public String getName() {
        return fileName;
    }

    /** Get the full file path of this file.
     *
     * @return A path where this file can be found in the bit archive.
     */
    public File getFilePath() {
        return filePath;
    }

    /** Get the size of this file.
     * If the file does not exist, the size is 0L.
     *
     * @return The size of this file. 
     */
    public long getSize() {
        return (filePath.length());
    }
}
