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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 * USA
 */
package dk.netarkivet.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides various utilities for inputstreams.
 */
public class InputStreamUtils {
    
    /** Read a line of bytes from an InputStream.  Useful when an InputStream
     * may contain both text and binary data.
     * @param inputStream A source of data
     * @return A line of text read from inputStream, with terminating
     * \r\n or \n removed, or null if no data is available.
     * @throws IOException on trouble reading from input stream
     */
    public static String readLine(InputStream inputStream) throws IOException {
        byte[] rawdata = readRawLine(inputStream);
        if (rawdata == null) {
            return null;
        }
        int len = rawdata.length;
        if (len > 0) {
            if (rawdata[len - 1] == '\n') {
                len--;
                if (len > 0) {
                    if (rawdata[len - 1] == '\r') {
                        len--;
                    }
                }
            }
        }
        return new String(rawdata, 0, len);
    }

    /** Reads a raw line from an InputStream, up till \n.
     * Since HTTP allows \r\n and \n as terminators, this gets the whole line.
     * This code is adapted from org.apache.commons.httpclient.HttpParser
     *
     * @param inputStream A stream to read from.
     * @return Array of bytes read or null if none are available.
     * @throws IOException if the underlying reads fail
     */
    public  static byte[] readRawLine(InputStream inputStream)
        throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        int counter = 0;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            counter++;
            if (ch == '\n') {
                break;
            }
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }

}
