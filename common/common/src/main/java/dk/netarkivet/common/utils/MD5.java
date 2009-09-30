/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
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

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * A class that does MD5 checksumming
 * We don't want everybody to have to do MessageDigest.getInstance() and hex
 * conversion.
 */
public class MD5 {
    static final int MAGIC_INTEGER_4 = 4;
    static final int MAGIC_INTEGER_OxOF = 0x0F;
    private static final String MD5_ALGORITHM = "MD5";

    /** Return na MD5 MessageDigest object.
     *  @return a MessageDigest object
     */
    public static MessageDigest getMessageDigestInstance() {
        try {
            return MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Very unlikely to not exist, so we can die if it doesn't.
            throw new NotImplementedException("JVM has no MD5 implementation!");
        }
    }

    /**
     * Generate an MD5 for a byte array.
     * @param msg The given bytearray
     * @return the MD5 for a byte array
     */
    public static String generateMD5(final byte[] msg) {
        return toHex(getMessageDigestInstance().digest(msg));
    }

    /**
     * Generates an MD5 on a given file. Reads the entire file into a byte array
     * - inefficient.
     * Very inefficient if file is retrieved over network.
     *
     * @param file Unique reference to file for which to generate checksum
     * @return The generated MD5 checksum as string.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static String generateMD5onFile(final File file)
        throws IOException, FileNotFoundException {
        final MessageDigest messageDigest = getMessageDigestInstance();
        byte[] bytes = new byte[4000];
        int bytes_read;
        DataInputStream fis = null;
        try {
            fis = new DataInputStream(new BufferedInputStream(
                                          new FileInputStream(file)));
            messageDigest.reset();
            while ((bytes_read = fis.read(bytes)) > 0) {
                messageDigest.update(bytes, 0, bytes_read);
            }
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return toHex(messageDigest.digest());
    }

    /** Generates an MD5 on an InputStream, throwing away the data itself.
     *
     * @param instream An inputstream to generate MD5 on.  The contents of
     * the stream will be consumed by this call, but the stream will not
     * be closed.
     * @return The generated MD5 checksum as a string.
     * @throws IOFailure if there is an error reading from the stream
     */
    public static String generateMD5(final InputStream instream) {
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        final MessageDigest messageDigest = getMessageDigestInstance();
        messageDigest.reset();
        int bytesRead;
        try {
            while ((bytesRead = instream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Error doing MD5 on inputstream", e);
        }
        return toHex(messageDigest.digest());
    }

    /**
     * Converts a byte array to a hexstring.
     *
     * @param ba the bytearray to be converted
     * @return ba converted to a hexstring
     */
    public static String toHex(final byte[] ba) {
        char[] hexdigit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                'd', 'e', 'f'
            };

        StringBuffer sb = new StringBuffer("");
        int ba_len = ba.length;

        for (int i = 0; i < ba_len; i++) {
            sb.append(hexdigit[(ba[i] >> MAGIC_INTEGER_4)
                               & MAGIC_INTEGER_OxOF]);
            sb.append(hexdigit[ba[i] & MAGIC_INTEGER_OxOF]);
        }

        return sb.toString();
    }
}
