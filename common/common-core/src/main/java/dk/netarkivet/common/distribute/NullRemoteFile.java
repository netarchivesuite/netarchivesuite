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
import java.io.InputStream;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * This is an implementation of RemoteFile which does nothing and can therefore be used in batch jobs for which no
 * output is required.
 */
@SuppressWarnings({"serial"})
public class NullRemoteFile implements RemoteFile {

    /**
     * @see RemoteFileFactory#getInstance(File, boolean, boolean, boolean)
     */
    public static RemoteFile getInstance(File f, Boolean useChecksums, Boolean fileDeletable, Boolean multipleDownloads) {
        return new NullRemoteFile();
    }

    /**
     * @see RemoteFile#copyTo(File)
     */
    public void copyTo(File destFile) {
    }

    /**
     * @see RemoteFile#appendTo(OutputStream)
     */
    public void appendTo(OutputStream out) {
    }

    public InputStream getInputStream() {
        return null;
    }

    /**
     * @see RemoteFile#cleanup()
     */
    public void cleanup() {
    }

    /**
     * @see RemoteFile#getSize()
     */
    public long getSize() {
        return 0;
    }

    /**
     * Return the file name.
     *
     * @return the file name
     * @see RemoteFile#getName()
     */
    public String getName() {
        return null;
    }

    /**
     * Returns a MD5 Checksum on the file.
     *
     * @return MD5 checksum
     * @throws NotImplementedException Because it is not implemented
     * @see RemoteFile#getChecksum()
     */
    public String getChecksum() throws NotImplementedException {
        throw new NotImplementedException("Not implemented!");
    }

}
