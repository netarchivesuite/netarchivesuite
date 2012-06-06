package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCRecord;

public class HeritrixArchiveRecordWrapper extends ArchiveRecordBase {

	protected ArchiveRecord record;

	protected ArchiveHeaderBase header;

	public HeritrixArchiveRecordWrapper(ArchiveRecord record) {
		this.record = record;
		this.header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(this, record);
		if (record instanceof ARCRecord) {
			this.bIsArc = true;
		} else if (record instanceof WARCRecord) {
			this.bIsWarc = true;
		}
	}

	public ArchiveHeaderBase getHeader() {
		return header;
	}

	public InputStream getInputStream() {
		return record;
	}

}
