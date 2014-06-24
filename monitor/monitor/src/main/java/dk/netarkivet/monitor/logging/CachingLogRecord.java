
package dk.netarkivet.monitor.logging;

import java.lang.management.ManagementFactory;
import java.util.logging.LogRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.management.SingleMBeanObject;

/**
 * A LogRecord to be exposed as an MBean.
 *
 */
public class CachingLogRecord
        implements SingleLogRecord {
    private final int index;
    private final CachingLogHandler cachingLogHandler;
    private SingleMBeanObject<SingleLogRecord> singleMBeanObject;

    /**
     * Make a caching log record, that exposes a log record at a given index as
     * an MBean.
     *
     * @param index             The index of this log record, counted from the
     *                          top of the list.
     * @param cachingLogHandler The caching log handler this is an exposing view
     *                          on.
     * @throws IOFailure on any trouble registering.
     */
    public CachingLogRecord(int index, CachingLogHandler cachingLogHandler) {
        ArgumentNotValid.checkNotNull(cachingLogHandler,
                                      "CachingLogHandler cachingLogHandler");
        this.index = index;
        this.cachingLogHandler = cachingLogHandler;

        register();
    }

    /**
     * Get the log record on a given index from the top as a string. This will
     * be formatted by the formatter from the CachingLogHandler.
     *
     * @return A String representation of the LogRecord, or null for none.
     */
    public String getRecordString() {
        LogRecord logRecord = cachingLogHandler.getNthLogRecord(this.index);
        if (logRecord == null) {
            return "";
        } else {
            return cachingLogHandler.getFormatter().format(logRecord);
        }
    }

    /**
     * Registers this object as an mbean.
     */
    private void register() {
        singleMBeanObject
                = new SingleMBeanObject<SingleLogRecord>(
                "dk.netarkivet.common.logging",
                this, SingleLogRecord.class,
                ManagementFactory.getPlatformMBeanServer());
        singleMBeanObject.getNameProperties().put("index",
                                                  Integer.toString(this.index));
        singleMBeanObject.register();
    }

    /**
     * Unregisters this object as an mbean.
     */
    private void unregister() {
        if (singleMBeanObject != null) {
            singleMBeanObject.unregister();
            singleMBeanObject = null;
        }
    }

    /**
     * Unregisters this object as an mbean.
     */
    public void close() {
        unregister();
    }
}
