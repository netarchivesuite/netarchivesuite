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
package dk.netarkivet.common.utils;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

/**
 * Unit tests for the DiscardingOutputStream class.
 */
// FIXME: Does not close properly if anything fails.
public class DiscardingOutputStreamTester {

    @Test
    public void testWriteInt() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        try {
            os.write(20);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }

    @Test
    public void testWriteBytearray() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        byte[] b = null;
        try {
            os.write(b);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }

    @Test
    public void testWriteBytearrayWithArgs() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        byte[] b = null;
        try {
            os.write(b, 0, 20);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }
}
