/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * 
 * Unit tests for the DiscardingOutputStream class.   
 *
 */
public class DiscardingOutputStreamTester extends TestCase{

    public void testWriteInt() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        try {
            os.write(20);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }

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
