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

import java.io.OutputStream;

/**
 * An OutputStream implementation that simply discards everything it gets. It overrides all the write methods so that
 * they all execute in constant time.
 */
class DiscardingOutputStream extends OutputStream {

    /**
     * Discard a single byte of data.
     *
     * @see OutputStream#write(int)
     */
    public void write(int i) {
    }

    /**
     * Discard many bytes of data, efficiently.
     *
     * @see OutputStream#write(byte[], int, int)
     */
    public void write(byte[] buffer, int offset, int amount) {
    }

    /**
     * Discard all the data we can, efficiently.
     *
     * @see OutputStream#write(byte[])
     */
    public void write(byte[] buffer) {
    }

}
