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
