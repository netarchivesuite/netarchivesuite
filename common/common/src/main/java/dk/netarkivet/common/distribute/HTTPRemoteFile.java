/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * A remote file implemented with point-to-point HTTP communication.
 * Optimised to communicate locally, if file is on the same host.
 * Optimised to transfer 0 byte files inline.
 */
public class HTTPRemoteFile extends AbstractRemoteFile {
    /** The host name this file orignated on. */
    protected final String hostname;
    /** The url that exposes this remote file. */
    protected final URL url;
    /** If useChecksums is true, contains the file checksum. */
    protected final String checksum;
    /** The logger for this class. */
    private static final Log log = LogFactory.getLog(AbstractRemoteFile.class);

    /**
     * Initialises a remote file implemented by point-to-point HTTP
     * communication.
     * @param file The file to make a remote file for
     * @param useChecksums Whether communications are checksummed. If true,
     * getChecksum will also return the checksum.
     * @param fileDeletable if true, the file given to this method is deletable,
     * once it is transferred.
     * @param multipleDownloads if true, the file may be transferred more than
     * once. Otherwise, all file handles are attempted to be made invalid after
     * the first transfer, although no guarantees are made.
     *
     * @throws ArgumentNotValid if file is null, or not a readable file.
     * @throws IOFailure if checksums are requested, but i/o errors occur while
     * checksumming.
     */
    protected HTTPRemoteFile(File file, boolean useChecksums,
                             boolean fileDeletable, boolean multipleDownloads) {
        super(file, useChecksums, fileDeletable, multipleDownloads);
        this.hostname = SystemUtils.getLocalHostName();
        if (filesize > 0) {
            this.url = getRegistry().registerFile(
                    this.file, this.fileDeletable);
        } else {
            this.url = null;
        }
        if (useChecksums) {
            try {
                this.checksum = MD5.generateMD5onFile(file);
            } catch (IOException e) {
                throw new IOFailure("Unable to checksum file '"
                        + file.getAbsolutePath() + "'", e);
            }
        } else {
            this.checksum = null;
        }
    }

    /**
     * Initialises a remote file implemented by point-to-point HTTP
     * communication.
     * @param f The file to make a remote file for
     * @param useChecksums Whether communications are checksummed. If true,
     * getChecksum will also return the checksum.
     * @param fileDeletable if true, the file given to this method is deletable,
     * once it is transferred.
     * @param multipleDownloads if true, the file may be transferred more than
     * once. Otherwise, all file handles are attempted to be made invalid after
     * the first transfer, although no guarantees are made.
     * @throws ArgumentNotValid if file is null, or not a readable file.
     * @throws IOFailure if checksums are requested, but i/o errors occur while
     * checksumming.
     */
    public static RemoteFile getInstance(File f, Boolean useChecksums,
                                         Boolean fileDeletable,
                                         Boolean multipleDownloads) {
        return new HTTPRemoteFile(f, useChecksums, fileDeletable,
                                  multipleDownloads);
    }

    /** Get the webserver registry for this class of files. Meant to be
     * subclassed for specialised versions of this file.
     * @return The reigstry. 
     */
    protected HTTPRemoteFileRegistry getRegistry() {
        return HTTPRemoteFileRegistry.getInstance();
    }

    /** Copy this remote file to the given file.
     * If the file resides on the current machine, remote file transfer is done
     * locally. Otherwise, the remote file is transferred over http.
     * If the file is not set to be able to be transferred multiple times, it is
     * cleaned up after the transfer.
     * @param destFile The file to write the remote file to.
     * @throws ArgumentNotValid on null destFile, or parent to destfile is not
     * a writeable directory, or destfile exists and cannot be overwritten.
     * @throws IOFailure on I/O trouble writing remote file to destination.
     */
    public void copyTo(File destFile) {
        ArgumentNotValid.checkNotNull(destFile, "File destFile");
        destFile = destFile.getAbsoluteFile();
        if ((!destFile.isFile() || !destFile.canWrite())
            && (!destFile.getParentFile().isDirectory()
                || !destFile.getParentFile().canWrite())) {
            throw new ArgumentNotValid("Destfile '" + destFile
                    + "' does not point to a writable file for remote file '"
                    + file + "'");
        }
        if (isLocal() && fileDeletable && !multipleDownloads && !useChecksums) {
            if (file.renameTo(destFile)) {
                    cleanup();
                return;
            }
            //if rename fails we fall back to normal usage.
        }
        super.copyTo(destFile);
    }

    /** Get an input stream representing the remote file.
     * If the file resides on the current machine, the input stream is to the
     * local file. Otherwise, the remote file is transferred over http.
     * The close method of the input stream will cleanup this handle, and if
     * checksums are requested, will check the checksums on close.
     * If the file is not set to be able to be transferred multiple times, it is
     * cleaned up after the transfer.
     * @return An input stream for the remote file.
     * @throws IOFailure on I/O trouble gemerating inputstream for remote file.
     * Also, the returned remote file will throw IOFailure on close, if
     * checksums are requested, but do not match.
     */
    public InputStream getInputStream() {
        if (filesize == 0) {
            return new ByteArrayInputStream(new byte[]{});
        }
        try {
            InputStream is = null;
            if (isLocal()) {
                is = new FileInputStream(file);
            } else {
                URLConnection urlConnection = getRegistry().openConnection(url);
                //ensure not getting some cached version
                urlConnection.setUseCaches(false);
                is = urlConnection.getInputStream();
            }
            if (useChecksums) {
                is = new DigestInputStream(
                        is, MD5.getMessageDigestInstance());
            }
            return new FilterInputStream(is) {
                public void close() {
                    if (useChecksums) {
                        String newChecksum = MD5.toHex(((DigestInputStream) in)
                                .getMessageDigest().digest());
                        if (!newChecksum.equals(checksum)) {
                            throw new IOFailure("Checksum mismatch! Expected '"
                                    + checksum + "' but was '" + newChecksum
                                    + "'");
                        }
                    }
                    if (!multipleDownloads) {
                        cleanup();
                    }
                }
            };
        } catch (IOException e) {
            throw new IOFailure("Unable to get inputstream for '" + file
                    + "' from '" + url + "'", e);
        }
    }

    /**
     * Invalidate all file handles, by asking the remote registry to remove the
     * url for this remote file from the list of shared files.
     * Invalidating a file handle may delete the original files, if deletable.
     * This method does not throw exceptions, but will warn on errors.
     */
    public void cleanup() {
        if (filesize == 0) {
            return;
        }
        try {
            URLConnection urlConnection = getRegistry().openConnection(
                    getRegistry().getCleanupUrl(url));
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            urlConnection.getInputStream();
        } catch (IOException e) {
            log.warn("Unable to cleanup file '"
                    + file.getAbsolutePath() + "' with URL'" + url +  "'", e);
        }
    }

    /** Get checksum for file, or null if checksums were not requested.
     * @return checksum for file, or null if checksums were not requested.
     */
    public String getChecksum() {
        return checksum;
    }

    /** Helper method to determine if file resides on local machine.
     * @return true if the file is on the local machine, false otherwise.
     */
    protected boolean isLocal() {
        return SystemUtils.getLocalHostName().equals(hostname)
                && file.isFile() && file.canRead();
    }
}
