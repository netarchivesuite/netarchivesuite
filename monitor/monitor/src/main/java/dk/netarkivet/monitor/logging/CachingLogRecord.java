/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
     * Regsiters this object as an mbean.
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
     * Unregsiters this object as an mbean.
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
