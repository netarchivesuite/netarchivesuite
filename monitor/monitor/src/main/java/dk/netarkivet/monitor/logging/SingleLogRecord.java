
package dk.netarkivet.monitor.logging;

/**
 * An interface for reading the contents of a log record.
 *
 */

public interface SingleLogRecord {
    /**
     * Get the log record on a given index from the top as a string.
     * This will be formatted by some formatter, depending on implementation.
     *
     * @return A String representation of the LogRecord, or null for none.
     */
    String getRecordString();
}
