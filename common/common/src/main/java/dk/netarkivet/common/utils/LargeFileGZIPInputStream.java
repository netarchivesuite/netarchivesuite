/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Subclass of GZIPInputstream, including a workaround to support >2GB files.
 *
 * Java currently has a bug that does not allow unzipping Gzip files with
 * contents larger than 2GB. The result will be an IOException with the message
 * "Corrupt GZIP trailer". This class works around that bug by ignoring that
 * message for all streams which are uncompressed larger than 2GB.
 * This sacrifices CRC checks for those streams, though.
 *
 * See sun bug:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5092263
 *
 * @see GZIPInputStream
 */

public class LargeFileGZIPInputStream extends GZIPInputStream {
    /**
     * Creates a new input stream with a default buffer size.
     * @param in the input stream
     * @throws IOException if an I/O error has occurred.
     *                     Note: We usually don't allow IOException in our code,
     *                     but this is done here to closely mimic
     *                     GZIPInputStream
     */
    public LargeFileGZIPInputStream(InputStream in)
            throws IOException {
        super(in);
    }

    /**
     * Reads uncompressed data into an array of bytes. Blocks until enough input
     * is available for decompression.
     *
     * @param buf the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the
     *         compressed input stream is reached
     * @throws IOException if an I/O error has occurred or the compressed input
     *                     data is corrupt. Note that size differences are
     *                     ignored in this workaround class if size is larger
     *                     than Integer.MAX_VALUE.
     *                     Note: We usually don't allow IOException in our code,
     *                     but this is done here to closely mimic
     *                     GZIPInputStream
     */
    public int read(byte[] buf, int off, int len) throws IOException {
        try {
            return super.read(buf, off, len);
        } catch (IOException e) {
            if (exceptionCausedByJavaException(e)) {
                //mimic succes
                eos = true;
                return -1;
            } else {
                throw e;
            }
        }
    }

    /**
     * Given an IOException caused by read, return whether this is the exception
     * we are working around. This is the case if
     * 1) The message is Corrupt GZIP trailer
     * 2) More then Integer.MAX_VALUE bytes are written
     *
     * @param e An IOException thrown by GZIPInputStream.read
     * @return Whether it is one caused by the bug we are working around
     */
    private boolean exceptionCausedByJavaException(IOException e) {
        return (e.getMessage() != null
                && e.getMessage().equals("Corrupt GZIP trailer")
                && inf.getBytesWritten() >= Integer.MAX_VALUE);
    }
}
