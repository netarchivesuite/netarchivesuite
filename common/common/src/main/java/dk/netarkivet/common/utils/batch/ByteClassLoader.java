/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.batch;

import java.io.File;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** A subclass of ClassLoader that can take a byte[] containing a class file.
 */
public class ByteClassLoader extends ClassLoader {
    /** Binary class data loaded from file. */
    byte[] binaryData;

    /** Constructor that reads data from a file
     *
     * @param binaryFile A file containing a Java class.
     */
    public ByteClassLoader(File binaryFile) {
        ArgumentNotValid.checkNotNull(binaryFile, "File binaryFile");
        this.binaryData = FileUtils.readBinaryFile(binaryFile);
    }

    /** Constructor taking a class as an array of bytes.
     *
     * @param bytes Array of bytes containing a class definition.
     */
    public ByteClassLoader(byte[] bytes) {
        ArgumentNotValid.checkNotNull(bytes, "byte[] bytes");
        this.binaryData = bytes;
    }

    /** Define the class that this class loader knows about.  The name of
     * the class is taken from the data given in the constructor.
     *
     * Note that this does *not* override any of the
     * java.lang.ClassLoader#defineClass methods.  Calling this method directly
     * is the only way to get the class defined by this classloader. 
     *
     * @return A new Class object for this class.
     */
    @SuppressWarnings("rawtypes")
	public Class defineClass() {
        return super.defineClass(null, binaryData, 0, binaryData.length);
    }
}
