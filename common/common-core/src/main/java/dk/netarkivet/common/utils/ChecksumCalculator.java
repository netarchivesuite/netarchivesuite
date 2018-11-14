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
package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * Calculates MD5 or SHA1 checksums on files using the built-in Java methods.
 */
public final class ChecksumCalculator {

    /** Defines the MD5 checksum algorithm */
    public static final String MD5 = "MD5";
    /** Defines the SHA1 checksum algorithm */
    public static final String SHA1 = "SHA1";

    /**
     * Calculate MD5 for a file.
     *
     * @param src The file to calculate MD5 for.
     * @return The MD5 sum of a file as a 32 characters long Hex string.
     */
    public static String calculateMd5(final File src) {
        ArgumentNotValid.checkNotNull(src, "File src");
        ArgumentNotValid.checkTrue(src.isFile(), "Argument should be a file");
        // Get the MD5 and return it
        try {
            final FileInputStream fileInputStream = new FileInputStream(src);
            try {
                return calculateMd5(fileInputStream);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not read file '" + src.getAbsolutePath() + "'", e);
        }
    }

    /**
     * Calculate the SHA-1 DIGEST for a file.
     *
     * @param src The file to calculate SHA-1 for.
     * @return The SHA-1 sum of a file as a 32 characters long Hex string.
     */
    public static String calculateSha1(final File src) {
        ArgumentNotValid.checkNotNull(src, "File src");
        ArgumentNotValid.checkTrue(src.isFile(), "Argument should be a file");
        // Get the SHA-1 digest and return it
        try {
            final FileInputStream fileInputStream = new FileInputStream(src);
            try {
                return calculateSha1(fileInputStream);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not read file '" + src.getAbsolutePath() + "'", e);
        }
    }

    /**
     * Calculates an MD5 digest on an InputStream, throwing away the data itself. Throws Alert if there is an error
     * reading from the stream
     *
     * @param instream An <code>InputStream</code> to calculate the MD5 digest on. The contents of the stream will be
     * consumed by this call, but the stream will not be closed.
     * @return The calculated MD5 digest as a string.
     */
    public static String calculateMd5(final InputStream instream) {
        return calculateDigest(instream, MD5);
    }

    /**
     * Calculates an SHA-1 digest on an InputStream, throwing away the data itself. Throws Alert if there is an error
     * reading from the stream
     *
     * @param instream An <code>InputStream</code> to calculate the SHA-1 digest on. The contents of the stream will be
     * consumed by this call, but the stream will not be closed.
     * @return The calculated SHA-1 digest as a string.
     */
    public static String calculateSha1(final InputStream instream) {
        return calculateDigest(instream, SHA1);
    }

    /**
     * Generate an MD5 for a byte array.
     *
     * @param msg The given bytearray
     * @return the MD5 for a byte array
     */
    public static String calculateMd5(final byte[] msg) {
        return toHex(getMessageDigest(MD5).digest(msg));
    }

    /**
     * Calculates a digest on an InputStream, throwing away the data itself. Throws Alert if there is an error reading
     * from the stream
     *
     * @param instream An <code>InputStream</code> to calculate the digest on. The contents of the stream will be
     * consumed by this call, but the stream will not be closed.
     * @param algorithm digest algorithm to use
     * @return The calculated digest as a string.
     */
    private static String calculateDigest(final InputStream instream, final String algorithm) {
        final byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        final MessageDigest messageDigest = getMessageDigest(algorithm);
        messageDigest.reset();
        int bytesRead;
        try {
            while ((bytesRead = instream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Error making a '" + algorithm + "' digest on the inputstream", e);
        }
        return toHex(messageDigest.digest());
    }

    private static final char[] hexdigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f'};

    /**
     * Converts a byte array to a hexstring.
     *
     * @param ba the bytearray to be converted
     * @return ba converted to a hexstring
     */
    public static String toHex(final byte[] ba) {
        int baLen = ba.length;
        char[] hexchars = new char[baLen * 2];
        int cIdx = 0;
        for (int i = 0; i < baLen; ++i) {
            hexchars[cIdx++] = hexdigit[(ba[i] >> 4) & 0x0F];
            hexchars[cIdx++] = hexdigit[ba[i] & 0x0F];
        }
        return new String(hexchars);
    }

    public static byte[] digestFile(File src, String digestAlgorithm) {
        ArgumentNotValid.checkNotNull(src, "File src");
        ArgumentNotValid.checkTrue(src.isFile(), "Argument should be a file");
        try {
            FileInputStream fileInputStream = new FileInputStream(src);
            try {
                return digestInputStream(fileInputStream, digestAlgorithm);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not read file '" + src.getAbsolutePath() + "'", e);
        }
    }

    public static byte[] digestInputStream(InputStream instream, String algorithm) {
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        MessageDigest messageDigest = getMessageDigest(algorithm);
        messageDigest.reset();
        int bytesRead;
        try {
            while ((bytesRead = instream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Error making a '" + algorithm + "' digest on the inputstream", e);
        }
        return messageDigest.digest();
    }

    /**
     * Get a MessageDigest for a specific algorithm.
     *
     * @param algorithm a specific MessageDigest algorithm.
     * @return a MessageDigest for a specific algorithm
     */
    public static MessageDigest getMessageDigest(final String algorithm) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalState("The '" + algorithm + "' algorithm is not available", e);
        }
        return messageDigest;
    }

}
