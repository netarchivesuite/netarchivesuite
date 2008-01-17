/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;

/**
 * A RemoteFile implementation that just takes a string.
 *
 */

public class StringRemoteFile implements RemoteFile {
    /** the contents. */
    String contents;
    /** the filename. */
    String filename;

    public StringRemoteFile(String s) {
        this.filename = "unnamed string";
        contents = s;
    }

    public StringRemoteFile(String filename, String s) {
        this.filename = filename;
        contents = s;
    }

    /**
     * Copy remotefile to local disk storage.
     * Used by the data recipient.
     *
     * @param destFile local File
     */
    public void copyTo(File destFile) {
        FileUtils.writeBinaryFile(destFile, contents.getBytes());
    }

    /**
     * Write the contents of this remote file to an output stream.
     *
     * @param out OutputStream that the data will be written to.  This stream
     *            will not be closed by this operation.
     * @throws IOFailure If append operation fails
     */
    public void appendTo(OutputStream out) {
        try {
            out.write(contents.getBytes());
        } catch (IOException e) {
            throw new IOFailure("Could not write string to " + out);
        }
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(contents.getBytes());
    }

    /**
     * Return the file name.
     *
     * @return the file name
     */
    public String getName() {
        return filename;
    }

    /**
     * Returns a MD5 Checksum on the file.
     *
     * @return MD5 checksum
     */
    public String getChecksum() {
        return MD5.generateMD5(contents.getBytes());
    }

    /**
     * Deletes the local file to which this remote file refers.
     */
    public void cleanup() {
        // Inaccessible after this.
        contents = null;
    }

    /**
     * Returns the total size of the remote file.
     *
     * @return Size of the remote file.
     */
    public long getSize() {
        return contents.length();
    }
}
