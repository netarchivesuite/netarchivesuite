package dk.netarkivet.common.utils.archive;

import java.io.InputStream;

import org.archive.io.ArchiveRecord;

public abstract class ArchiveRecordBase {

	public abstract ArchiveHeaderBase getHeader();

	public abstract InputStream getInputStream();

	public static ArchiveRecordBase wrapArchiveRecord(ArchiveRecord archiveRecord) {
		return new HeritrixArchiveRecordWrapper(archiveRecord);
	}

}
