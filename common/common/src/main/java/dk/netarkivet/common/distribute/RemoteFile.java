/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;


/**
 *  RemoteFile: Interface for encapsulating remote files.
 *  Enables us to transmit large files between system components situated
 *  on different machines.
 *  Our current JMS broker(s) does not allow large message
 *  (i.e. messages > 70 MB).
 */
public interface RemoteFile extends Serializable {

    /**
     * Copy remotefile to local disk storage.
     * Used by the data recipient
     * @param destFile local File
     * @throws IOFailure on communication trouble.
     * @throws ArgumentNotValid on null parameter or non-writable file
     */
    public void copyTo(File destFile);

    /**
     * Write the contents of this remote file to an output stream.
     * @param out OutputStream that the data will be written to.  This stream
     * will not be closed by this operation.
     * @throws IOFailure If append operation fails
     * @throws ArgumentNotValid on null parameter
     */
    public void appendTo(OutputStream out);

    /**
     * Get an inputstream that contains the data transferred in this RemoteFile.
     * @return A stream object with the data in the object.  Note that the
     * close() method of this may throw exceptions if e.g. a transmission error
     * is detected.
     * @throws IOFailure on communication trouble.
     */
    public InputStream getInputStream();

    /**
     * Return the file name.
     * @return the file name
     */
    public String getName();

    /**
     * Returns a MD5 Checksum on the file. May return null, if checksums not
     * supported for this operation.
     * @return MD5 checksum
     */
    public String getChecksum();

    /**
     * Cleanup this remote file. The file is invalid after this.
     */
    public void cleanup();

    /** Returns the total size of the remote file.
     * @return Size of the remote file.
     */
    public long getSize();
}
