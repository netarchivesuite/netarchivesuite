/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.io.LineIterator;

/** String Iterator that reads its data from a file. */
public class StringIterator implements Iterator<String> {
    /** The Iterator used by this class. */
    private final LineIterator li;
    
    /**
     * Constructor.
     * @param datafile The file from where the strings are taken.
     * @throws IOException If the file doesn't exist or can't be read.
     */
    public StringIterator(File datafile) throws IOException {
        li = new LineIterator(new FileReader(datafile));
    }
    
    @Override
    public boolean hasNext() {
        return li.hasNext();
    }

    @Override
    public String next() {
        return li.nextLine();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Remove operation is not supported by this iterator"); 
    }
    
    public void close() {
        LineIterator.closeQuietly(li);
    }
}
