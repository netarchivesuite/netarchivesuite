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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.TimeUtils;

/**
 * Abstract superclass for easy implementation of remote file.
 * <p>
 * Sub classes should override this class, and do the following: - Implement getChecksum. - Implement getInputStream. -
 * Implement cleanup. - Add getInstance(File, Boolean, Boolean, Boolean)-method to make the file work with the factory.
 */
@SuppressWarnings({"serial"})
public abstract class AbstractRemoteFile implements RemoteFile {

    /** A named logger for this class. */
    private static final transient Logger log = LoggerFactory.getLogger(AbstractRemoteFile.class);

    /** The file this is remote file for. */
    protected final File file;
    /** If true, communication is checksummed. */
    protected final boolean useChecksums;
    /** If true, the file may be deleted after all transfers are done. */
    protected final boolean fileDeletable;
    /**
     * If true, the file may be downloaded multiple times. Otherwise, the remote file is invalidated after first
     * transfer.
     */
    protected final boolean multipleDownloads;
    /** The size of the file. */
    protected final long filesize;

    /**
     * Initialise common fields in remote file. Overriding classes should also initialise checksum field.
     *
     * @param file The file to make remote file for.
     * @param useChecksums If true, communications should be checksummed.
     * @param fileDeletable If true, the file may be downloaded multiple times. Otherwise, the remote file is
     * invalidated after first transfer.
     * @param multipleDownloads If useChecksums is true, contains the file checksum.
     */
    public AbstractRemoteFile(File file, boolean useChecksums, boolean fileDeletable, boolean multipleDownloads) {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.isFile() || !file.canRead()) {
            throw new ArgumentNotValid("File '" + file.getAbsolutePath() + "' is not a readable file");
        }
        this.file = file;
        this.fileDeletable = fileDeletable;
        this.multipleDownloads = multipleDownloads;
        this.useChecksums = useChecksums;
        this.filesize = file.length();
    }

    /**
     * Copy this remote file to the given file. This method will make a fileoutputstream, and use appendTo to write the
     * remote file to this stream.
     *
     * @param destFile The file to write the remote file to.
     * @throws ArgumentNotValid on null destFile, or parent to destfile is not a writeable directory, or destfile exists
     * and cannot be overwritten.
     * @throws IOFailure on I/O trouble writing remote file to destination.
     */
    public void copyTo(File destFile) {
        ArgumentNotValid.checkNotNull(destFile, "File destFile");
        destFile = destFile.getAbsoluteFile();
        if ((!destFile.isFile() || !destFile.canWrite())
                && (!destFile.getParentFile().isDirectory() || !destFile.getParentFile().canWrite())) {
            throw new ArgumentNotValid("Destfile '" + destFile + "' does not point to a writable file for "
                    + "remote file '" + file + "'");
        }
        try {
            FileOutputStream fos = null;
            int retry = 0;
            boolean success = false;

            // retry if it fails, but always make at least one attempt.
            do {
                try {
                    try {
                        fos = new FileOutputStream(destFile);
                        appendTo(fos);
                        success = true;
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                } catch (IOFailure e) {
                    if (retry == 0) {
                        log.warn("Could not retrieve the file '{}' on first attempt. Will retry up to '{}' times.",
                                getName(), getNumberOfRetries(), e);
                    } else {
                        log.warn("Could not retrieve the file '{}' on retry number '{}' of '{}' retries.", getName(),
                                retry, getNumberOfRetries(), e);
                    }
                }
                ++retry;
                if (!success && retry < getNumberOfRetries()) {
                    log.debug("CopyTo attempt #{} of max {} failed. Will sleep a while before trying to copyTo again.",
                            retry, getNumberOfRetries());
                    TimeUtils.exponentialBackoffSleep(retry, Calendar.MINUTE);
                }
            } while (!success && retry < getNumberOfRetries());

            // handle case when the retrieval is unsuccessful.
            if (!success) {
                throw new IOFailure("Unable to retrieve the file '" + getName() + "' in '" + getNumberOfRetries()
                        + "' attempts.");
            }
        } catch (Exception e) {
            FileUtils.remove(destFile);
            throw new IOFailure("IO trouble transferring file", e);
        }
    }

    /**
     * Append this remote file to the given output stream. This method will use getInputStream to get the remote stream,
     * and then copy that stream to the given output stream.
     *
     * @param out The stream to write the remote file to.
     * @throws ArgumentNotValid if outputstream is null.
     * @throws IOFailure on I/O trouble writing remote file to stream.
     */
    public void appendTo(OutputStream out) {
        ArgumentNotValid.checkNotNull(out, "OutputStream out");
        StreamUtils.copyInputStreamToOutputStream(getInputStream(), out);
    }

    /**
     * Get an input stream representing the remote file. The returned input stream should throw IOFailure on close, if
     * checksums are requested, but do not match. The returned inputstream should call cleanup on close, if
     * multipleDownloads is not true.
     *
     * @return An input stream for the remote file.
     * @throws IOFailure on I/O trouble generating inputstream for remote file.
     */
    public abstract InputStream getInputStream();

    /**
     * Get the name of the remote file.
     *
     * @return The name of the remote file.
     */
    public String getName() {
        return file.getName();
    }

    /**
     * Get checksum for file, or null if checksums were not requested.
     *
     * @return checksum for file, or null if checksums were not requested.
     */
    public abstract String getChecksum();

    /**
     * Invalidate all file handles. If file is deletable, it should be deleted after this method is called. This method
     * should never throw exceptions, but only log a warning on trouble. It should be idempotent, meaning it should be
     * safe to call this method twice.
     */
    public abstract void cleanup();

    /**
     * Method for retrieving the number of retries for retrieving a file.
     *
     * @return The number of retries for retrieving a file.
     */
    public abstract int getNumberOfRetries();

    /**
     * Get the size of this remote file.
     *
     * @return The size of this remote file.
     */
    public long getSize() {
        return filesize;
    }
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("File= " + file.getName() + ", fileDeletable=" + fileDeletable + ", multipleDownloads=" 
    			+ multipleDownloads + ", filesize=" + filesize);
    	return sb.toString();
    }

}
