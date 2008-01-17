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

package dk.netarkivet.common.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utilities for handling streams.
 */
public class StreamUtils {
    /**
     * Will copy everything from input stream to output stream, closing input
     * stream afterwards.
     *
     * @param in  Inputstream to copy from
     * @param out Outputstream to copy to
     */
    public static void copyInputStreamToOutputStream(InputStream in,
                                                     OutputStream out) {
        try {
            try {
                if (in instanceof FileInputStream
                    && out instanceof FileOutputStream) {
                    FileChannel inChannel
                            = ((FileInputStream) in).getChannel();
                    FileChannel outChannel
                            = ((FileOutputStream) out).getChannel();
                    long transferred = 0;
                    final long fileLength = inChannel.size();
                    do {
                        transferred += inChannel.transferTo(
                                transferred,
                                Math.min(Constants.IO_CHUNK_SIZE,
                                         fileLength - transferred),
                                outChannel);
                    } while (transferred < fileLength);
                } else {
                    byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buf)) != -1) {
                        out.write(buf, 0, bytesRead);
                    }
                }
                out.flush();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IOFailure("Trouble copying inputstream " + in
                                + " to outputstream " + out, e);
        }
    }
}
