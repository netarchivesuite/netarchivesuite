/*$Id$
* $Revision$
* $Date$
* $Author$
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.wayback.batch;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface describing a class which can be used to convert duplicate records
 * in a crwal log to wayback-compatible cdx records
 */
public interface DeduplicateToCDXAdapterInterface {

    /**
     * Takes a deduplicate line from a crawl log and converts it to a line in a
     * cdx file suitable for searching in wayback. The target url in the line is
     * canonicalized by this method. Thetype of canonicalization is determined by
     * the default canonicalizer from the wayback settings.xml file.If the
     * input String is not a crawl-log
     * duplicate line, null is returned.
     * @param line a line from a crawl log
     * @return a line for a cdx file or null if the input is not a duplicate
     * line
     */
    String adaptLine(String line);

    /**
     * Scans an input stream from a crawl log and converts all lines containing
     * deduplicate information 
     * to cdx records which it outputs to an output stream.
     * @param is the input stream
     * @param os the output stream
     */
    void adaptStream(InputStream is, OutputStream os);

}
