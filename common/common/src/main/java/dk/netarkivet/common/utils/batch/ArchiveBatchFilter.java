package dk.netarkivet.common.utils.batch;

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.warc.ArchiveRecordBase;

public abstract class ArchiveBatchFilter implements Serializable {

	/**
	 * UID.
	 */
	private static final long serialVersionUID = 1409369446818539939L;

    /** The name of the BatchFilter. */
    protected String name;

    /** Create a new filter with the given name.
     *
     * @param name The name of this filter, for debugging mostly.
     */
    protected ArchiveBatchFilter(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        this.name = name;
    }

    /**
     * Get the name of the filter.
     * @return the name of the filter.
     */
    protected String getName() {
        return this.name;
    }

    /**
     * Check if a given record is accepted (not filtered out) by this filter.
     * @param record a given ARCRecord
     * @return true, if the given record is accepted by this filter
     */
    public abstract boolean accept(ArchiveRecordBase record);

    /** A default filter: Accepts everything. */
    public static final ArchiveBatchFilter NO_FILTER = new ArchiveBatchFilter("NO_FILTER") {
		private static final long serialVersionUID = 3394083354432326555L;
		public boolean accept(ArchiveRecordBase record) {
			return true;
		}
    };

}
