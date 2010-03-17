/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.OutputStream;

/** An OutputStream implementation that simply discards everything it gets.
 *  It overrides all the write methods so that they all execute in constant
 *  time. */
class DiscardingOutputStream extends OutputStream {
    /** Discard a single byte of data.
     * @see OutputStream#write(int)
     */
    public void write(int i) {
    }

    /** Discard many bytes of data, efficiently.
     * @see OutputStream#write(byte[], int, int)
     */
    public void write(byte[] buffer, int offset, int amount) {
    }

    /** Discard all the data we can, efficiently.
     * @see OutputStream#write(byte[])
     */
    public void write(byte[] buffer) {
    }
}
