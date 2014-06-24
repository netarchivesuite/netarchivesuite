package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Heritrix wrapper implementation of the abstract archive record interface.
 */
public class HeritrixArchiveRecordWrapper extends ArchiveRecordBase {

    /** The original Heritrix record, since it is also the record payload
     *  input stream. */
    protected ArchiveRecord record;

    /** The wrapper archive header. */
    protected ArchiveHeaderBase header;

    /**
     * Construct a Heritrix record wrapper object.
     * @param record Heritrix record object
     */
    public HeritrixArchiveRecordWrapper(ArchiveRecord record) {
        ArgumentNotValid.checkNotNull(record, "record");
        this.record = record;
        this.header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(this, record);
        if (record instanceof ARCRecord) {
            this.bIsArc = true;
        } else if (record instanceof WARCRecord) {
            this.bIsWarc = true;
        } else {
            throw new ArgumentNotValid(
                    "Unsupported ArchiveRecord type: "
                    + record.getClass().getName());
        }
    }

    @Override
    public ArchiveHeaderBase getHeader() {
        return header;
    }

    @Override
    public InputStream getInputStream() {
        return record;
    }

}
