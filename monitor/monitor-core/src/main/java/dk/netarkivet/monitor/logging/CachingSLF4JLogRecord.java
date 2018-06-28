/*
 * #%L
 * Netarchivesuite - monitor
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

package dk.netarkivet.monitor.logging;

import java.lang.management.ManagementFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.management.SingleMBeanObject;

/**
 * Cached log entry and JMX bean in one.
 */
public class CachingSLF4JLogRecord implements SingleLogRecord {

	/** Index in cached list. */
    private final int index;

    /** Caching appender that created this caching log entry. */
    private final CachingSLF4JAppender cachingSLF4JAppender;

    /** JMX bean object. */
    private SingleMBeanObject<SingleLogRecord> singleMBeanObject;

    /**
     * Make a caching log record, that exposes a log record at a given index as an MBean.
     *
     * @param index The index of this log record, counted from the top of the list.
     * @param cachingSLF4JAppender The caching log handler this is an exposing view on.
     * @throws IOFailure on any trouble registering.
     */
    public CachingSLF4JLogRecord(int index, CachingSLF4JAppender cachingSLF4JAppender) {
        ArgumentNotValid.checkNotNull(cachingSLF4JAppender, "CachingSLF4JAppender cachingSLF4JAppender");
        this.index = index;
        this.cachingSLF4JAppender = cachingSLF4JAppender;
        register();
    }

    @Override
    public String getRecordString() {
        String logMsg = cachingSLF4JAppender.getNthLogRecord(this.index);
        if (logMsg == null) {
            return "";
        } else {
            return logMsg;
        }
    }

    /**
     * Registers this object as an mbean.
     */
    private void register() {
        singleMBeanObject = new SingleMBeanObject<>("dk.netarkivet.common.logging", this,
                SingleLogRecord.class, ManagementFactory.getPlatformMBeanServer());
        singleMBeanObject.getNameProperties().put("index", Integer.toString(this.index));
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
