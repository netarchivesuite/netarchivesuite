/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ChecksumCalculator;

public class ChecksumCalculatorTester {

    @Test
    public void testChecksumCalculator() {
        MessageDigest md;
        // ByteArrayInputStream instream;
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
            assertEquals(expectedChecksum, checksum);

            md = MessageDigest.getInstance("SHA1");
            md.update(payload);
            expectedChecksum = convertToHex(md.digest(), md.getDigestLength());
            checksum = ChecksumCalculator.calculateSha1(new ByteArrayInputStream(payload));
            assertEquals(expectedChecksum, checksum);
        } catch (NoSuchAlgorithmException e) {
            fail("Digest error!");
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
        ArgumentNotValid.checkTrue(digestLength == buf.length(), "The digestLength '" + digestLength
                + "' should be equal to buf.length '" + buf.length() + "'.");
        return buf.toString();
    }

}
