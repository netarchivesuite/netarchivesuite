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
     *
     * @return the wrapped Heritrix archive header
     */
    public abstract ArchiveHeaderBase getHeader();

    /**
     * Return the payload input stream.
     *
     * @return the payload input stream
     */
    public abstract InputStream getInputStream();

    /**
     * Factory method for creating a wrapped Heritrix record.
     *
     * @param archiveRecord Heritrix archive record
     * @return wrapped Heritrix record
     */
    public static ArchiveRecordBase wrapArchiveRecord(ArchiveRecord archiveRecord) {
        return new HeritrixArchiveRecordWrapper(archiveRecord);
    }

}
