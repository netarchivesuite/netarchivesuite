package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;

public class HeritrixArchiveRecordWrapper extends ArchiveRecordBase {

	protected ArchiveRecord record;

	protected ArchiveHeaderBase header;

	public HeritrixArchiveRecordWrapper(ArchiveRecord record) {
		this.record = record;
		this.header = HeritrixArchiveHeaderWrapper.wrapArchiveHeader(record);
	}

	public ArchiveHeaderBase getHeader() {
		return header;
	}

	public InputStream getInputStream() {
		return record;
	}

}
