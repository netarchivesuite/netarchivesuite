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
package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;

/**
 * Base class for unified ARC/WARC record API:
 */
public abstract class ArchiveRecordBase {

    /** Is this record from an ARC file. */
    public boolean bIsArc;

    /** Is this record from a WARC file. */
    public boolean bIsWarc;

    /**
     * Return the wrapped Heritrix archive header
     * @return the wrapped Heritrix archive header
     */
    public abstract ArchiveHeaderBase getHeader();

    /**
     * Return the payload input stream.
     * @return the payload input stream
     */
    public abstract InputStream getInputStream();

    /**
     * Factory method for creating a wrapped Heritrix record.
     * @param archiveRecord Heritrix archive record
     * @return wrapped Heritrix record
     */
    public static ArchiveRecordBase wrapArchiveRecord(ArchiveRecord archiveRecord) {
        return new HeritrixArchiveRecordWrapper(archiveRecord);
    }

}
