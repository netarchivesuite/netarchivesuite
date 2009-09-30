/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.archive.bitarchive;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

import java.io.File;
import java.io.FileNotFoundException;

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
    public BitarchiveARCFile(String fn, File fp) {
        ArgumentNotValid.checkNotNull(fp, "File fp");
        if (fp.getName().equals("")) {
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
     *
     * @return The size of this file
     * @throws FileNotFoundException if the file does not exist.
     */
    public long getSize() throws FileNotFoundException {
        return (filePath.length());
    }
}
