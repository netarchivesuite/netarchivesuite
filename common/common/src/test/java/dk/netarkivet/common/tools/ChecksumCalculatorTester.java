/* $Id: HarvestDocumentationTester.java 2566 2012-12-05 15:08:14Z svc $
 * $Revision: 2566 $
 * $Date: 2012-12-05 16:08:14 +0100 (Wed, 05 Dec 2012) $
 * $Author: svc $
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
package dk.netarkivet.common.tools;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import junit.framework.Assert;
import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ChecksumCalculator;

public class ChecksumCalculatorTester extends TestCase {

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testChecksumCalculator() {
        MessageDigest md;
    	//ByteArrayInputStream instream;
    	String expectedChecksum;
    	String checksum;

        try {
        	SecureRandom random = new SecureRandom();
            byte[] payload = new byte[8192];
            random.nextBytes(payload);

            md = MessageDigest.getInstance("MD5");
            md.update(payload);
            expectedChecksum = convertToHex(md.digest(), md.getDigestLength());
            checksum = ChecksumCalculator.calculateMd5(new ByteArrayInputStream(payload));
            Assert.assertEquals(expectedChecksum, checksum);

            md = MessageDigest.getInstance("SHA1");
            md.update(payload);
            expectedChecksum = convertToHex(md.digest(), md.getDigestLength());
            checksum = ChecksumCalculator.calculateSha1(new ByteArrayInputStream(payload));
            Assert.assertEquals(expectedChecksum, checksum);
        } catch (NoSuchAlgorithmException e) {
        	Assert.fail("Digest error!");
        }
    }

    /** The radix used for converting to hex (base 16). */
    private static final int RADIX_SIXTEEN = 16;

    /**
     * Convert a buffer of bytes to a hexadecimal string.
     * 
     * @param data The data to convert to hex
     * @param digestLength Assumed digestLength
     * @return The hexadecimal representation of the given data.
     */
    private static String convertToHex(final byte[] data, int digestLength) {
    	digestLength *= 2;
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

}
