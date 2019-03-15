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
package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * RemoteFile: Interface for encapsulating remote files. Enables us to transmit large files between system components
 * situated on different machines. Our current JMS broker(s) does not allow large message (i.e. messages > 70 MB).
 */
public interface RemoteFile extends Serializable {

    /**
     * Copy remotefile to local disk storage. Used by the data recipient
     *
     * @param destFile local File
     * @throws IOFailure on communication trouble.
     * @throws ArgumentNotValid on null parameter or non-writable file
     */
    default void copyTo(File destFile){
        try (InputStream inputStream = getInputStream()) {
            try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
                org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream);
            }
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
        cleanup();
    }

    /**
     * Write the contents of this remote file to an output stream.
     *
     * @param outputStream OutputStream that the data will be written to. This stream will not be closed by this operation.
     * @throws IOFailure If append operation fails
     * @throws ArgumentNotValid on null parameter
     */
    default void appendTo(OutputStream outputStream) {
        try (InputStream inputStream = getInputStream()) {
            org.apache.commons.io.IOUtils.copyLarge(inputStream, outputStream);
        } catch (IOException e) {
            throw new IOFailure("message", e);
        }
        cleanup();
    }

    /**
     * Get an inputstream that contains the data transferred in this RemoteFile.
     *
     * @return A stream object with the data in the object. Note that the close() method of this may throw exceptions if
     * e.g. a transmission error is detected.
     * @throws IOFailure on communication trouble.
     */
    InputStream getInputStream();

    /**
     * Return the file name.
     *
     * @return the file name
     */
    String getName();

    /**
     * Returns a MD5 Checksum on the file. May return null, if checksums not supported for this operation.
     *
     * @return MD5 checksum
     */
    String getChecksum();

    /**
     * Cleanup this remote file. The file is invalid after this.
     * @see #exists()
     */
    void cleanup();

    /**
     * Checks if the file still exists and can be read
     * @return true if the file is ready for reading
     * @see #cleanup()
     */
    boolean exists();

    /**
     * Returns the total size of the remote file.
     *
     * @return Size of the remote file.
     */
    long getSize();

}
