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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StreamUtils;

/**
 * A file represented as a RemoteFile. To avoid transferring data to and from a remote machine, when you now, that
 * recipient is a local process. The file is deleted during cleanup.
 */
@SuppressWarnings({"serial"})
public class FileRemoteFile implements RemoteFile {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(FileRemoteFile.class);

    /** The local File where the data is stored. */
    private File dataFile;

    public FileRemoteFile(File dataFile) {
        ArgumentNotValid.checkNotNull(dataFile, "File dataFile");
        ArgumentNotValid.checkTrue(dataFile.isFile(), "The dataFile with value '" + dataFile.getAbsolutePath()
                + "' does not exist.");
        this.dataFile = dataFile;
    }

    @Override
    public void copyTo(File destFile) {
        FileUtils.copyFile(dataFile, destFile);
    }

    @Override
    public void appendTo(OutputStream out) {
        InputStream in = null;
        try {
            in = new FileInputStream(dataFile);
            StreamUtils.copyInputStreamToOutputStream(in, out);
        } catch (IOException e) {
            throw new IOFailure("Unable to append data: ", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(dataFile);
        } catch (IOException e) {
            throw new IOFailure("Unable to return inputStream: ", e);
        }
    }

    @Override
    public String getName() {
        return dataFile.getName();
    }

    @Override
    public String getChecksum() {
        return ChecksumCalculator.calculateMd5(dataFile);
    }

    @Override
    public void cleanup() {
        boolean deleted = dataFile.delete();
        if (!deleted) {
            log.warn("Unable to delete file '{}'", dataFile.getAbsolutePath());
        }
    }

    @Override
    public long getSize() {
        return dataFile.length();
    }

}
