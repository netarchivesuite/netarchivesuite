/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * Calculates MD5 or SHA-1 checksums on files using the built-in Java methods.
 */
public final class ChecksumCalculator {
    
    /** The expected length of an MD5 checksum as a Hex string. */
    private static final int MD5_DIGEST_STRING_LENGTH = 32;
    
    /** The expected length of an SHa1 checksum as a Hex string. */
    private static final int SHA1_DIGEST_STRING_LENGTH = 40;
   
    /**
     * The radix used for converting to hex (base 16).
     */
    private static final int RADIX_SIXTEEN = 16;

    /**
     * Calculate MD5 for a file.
     * 
     * @param src The file to calculate MD5 for.
     * @return The MD5 sum of a file as a 32 characters long Hex string.
     */
    public static String md5(final File src) {
        ArgumentNotValid.checkNotNull(src, "File src");
        ArgumentNotValid.checkTrue(src.isFile(), "Argument should be a file");
        // Get the MD5 and return it
        try {
            final FileInputStream fileInputStream = new FileInputStream(src);
            try {
                return md5(fileInputStream);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not read file '" 
                    + src.getAbsolutePath() + "'", e);
        }
    }
    
    /**
     * Calculate the SHA-1 DIGEST for a file.
     * 
     * @param src The file to calculate SHA-1 for.
     * @return The SHA-1 sum of a file as a 32 characters long Hex string.
     */
    public static String sha1(final File src){
        ArgumentNotValid.checkNotNull(src, "File src");
        ArgumentNotValid.checkTrue(src.isFile(), "Argument should be a file");
        // Get the SHA-1 digest and return it
        try {
            final FileInputStream fileInputStream = new FileInputStream(src);
            try {
                return sha1(fileInputStream);
            } finally {
                IOUtils.closeQuietly(fileInputStream);
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure("Could not read file '" 
                    + src.getAbsolutePath() + "'", e);
        }
    }
    
   /**
    * Generates an MD5 digest on an InputStream, throwing away the 
    * data itself. Throws Alert if there is an error reading from the 
    * stream
    *
    * @param instream An inputstream to generate the MD5 digest on.  
    * The contents of the stream will be consumed by this call, but the 
    * stream will not be closed.
    * @return The generated sha1 checksum as a string.
    */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private static String md5(final InputStream instream) {
        final byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        final String algorithm = "MD5";
        final MessageDigest messageDigest = getMessageDigest(algorithm);
        messageDigest.reset();
        int bytesRead;
        try {
            while ((bytesRead = instream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Error making a '" + algorithm
                    + "' digest on the inputstream", e);
        }
        return convertToHex(messageDigest.digest(), 
                MD5_DIGEST_STRING_LENGTH);
    }
    
    /**
     * Generates an SHA-1 digest on an InputStream, throwing away the 
     * data itself. Throws Alert if there is an error reading from the 
     * stream
     *
     * @param instream An inputstream to generate the SHA-1 digest on.  
     * The contents of the stream will be consumed by this call, but the 
     * stream will not be closed.
     * @return The generated sha1 checksum as a string.
     */
    private static String sha1(final InputStream instream) {
        final byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        final String algorithm = "SHA-1";
        final MessageDigest messageDigest = getMessageDigest(algorithm);
        messageDigest.reset();
        int bytesRead;
        try {
            while ((bytesRead = instream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOFailure("Error making a '" + algorithm
                    + "' digest on the inputstream", e);
        }
        return convertToHex(messageDigest.digest(), SHA1_DIGEST_STRING_LENGTH);
    }
    
    /**
     * Convert a buffer of bytes to a hexadecimal string.
     * 
     * @param data The data to convert to hex
     * @param digestLength Assumed digestLength
     * @return The hexadecimal representation of the given data.
     */
    private static String convertToHex(final byte[] data, int digestLength) {
        // BigInteger has the required functionality for
        // converting byte arrays to hex
        final String digest = new BigInteger(1, data).toString(RADIX_SIXTEEN);
        final int leadingZeros = digestLength - digest.length();
        final StringBuffer buf = new StringBuffer();
        for (int i = 0; i < leadingZeros; i++) {
            buf.append('0');
        }
        buf.append(digest);
        ArgumentNotValid.checkTrue(digestLength == buf.length(), 
                "The digestLength '" + digestLength + "' should be equal to buf.length '"
                + buf.length() + "'.");
        return buf.toString();
    }
    
    /**
     * Get a MessageDigest for a specific algorithm.
     * @param algorithm a specific MessageDigest algorithm.
     * @return a MessageDigest for a specific algorithm
     */
    private static MessageDigest getMessageDigest(final String algorithm) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalState(
                    "The '" + algorithm + "' algorithm is not available", e);
        }
        return messageDigest;
    }
}
