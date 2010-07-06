/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.monitor.MonitorSettings;

/**
 * A LogHandler that keeps the last N messages in memory, and exposes each as a
 * CachingLogRecordMBean.
 */

public class CachingLogHandler extends Handler {
    /** The size of the logging cache. */
    private final int loggingHistorySize;
    /** The logging cache itself, caching the last
     * "loggingHistorySize" log entries. */
    private final List<LogRecord> loggingHistory;
    /** The log entries exposed as MBeans. */
    private final List<CachingLogRecord> loggingMBeans;
    /** The place in the loggingHistory for the next LogRecord. */ 
    private int currentIndex;

    /**
     * Private method to get a Level property. If the property is not defined or
     * cannot be parsed we return the given default value.
     *
     * This method was copied from java.util.logging.LogManager, where it is
     * package private :-(
     *
     * @param name         The log property name
     * @param defaultValue The level if that property is not specified or
     *                     unparsable
     * @return The level from the property if set and parsable, the defaultValue
     *         otherwise
     */
    private Level getLevelProperty(String name, Level defaultValue) {
        String val = LogManager.getLogManager().getProperty(name);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Level.parse(val.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * private method to get a filter property. We return an instance of the
     * class named by the "name" property. If the property is not defined or has
     * problems we return the defaultValue.
     *
     * This method was copied from java.util.logging.LogManager, where it is
     * package private :-(
     *
     * @param name         The log property name
     * @param defaultValue The filter if that property is not specified or
     *                     unparsable
     * @return The filter from the property if set and parsable, the
     *         defaultValue otherwise
     */
    @SuppressWarnings("rawtypes")
	private Filter getFilterProperty(String name, Filter defaultValue) {
        String val = LogManager.getLogManager().getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Filter) clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }


    /**
     * Package private method to get a formatter property. We return an instance
     * of the class named by the "name" property. If the property is not defined
     * or has problems we return the defaultValue.
     *
     * This method was copied from java.util.logging.LogManager, where it is
     * package private :-(
     *
     * @param name         The log property name
     * @param defaultValue The formatter if that property is not specified or
     *                     unparsable
     * @return The formatter from the property if set and parsable, the
     *         defaultValue otherwise
     */
    @SuppressWarnings("rawtypes")
	private Formatter getFormatterProperty(String name,
                                           Formatter defaultValue) {
        String val = LogManager.getLogManager().getProperty(name);
        try {
            if (val != null) {
                Class clz = ClassLoader.getSystemClassLoader().loadClass(val);
                return (Formatter) clz.newInstance();
            }
        } catch (Exception ex) {
            // We got one of a variety of exceptions in creating the
            // class or creating an instance.
            // Drop through.
        }
        // We got an exception.  Return the defaultValue.
        return defaultValue;
    }

    /**
     * Initialise the handler datastructures, and register MBeans for all log
     * records. Note this last thing is actually done in the constructor of the
     * CachingLogRecord.
     *
     * @see dk.netarkivet.common.management.SingleMBeanObject
     * @see SingleLogRecord
     *
     *      The number of remembered log records is read from the setting
     *      Settings.LOGGING_HISTORY_SIZE
     */
    public CachingLogHandler() {
        super();
        String cname = getClass().getName();
        setLevel(getLevelProperty(cname + ".level", Level.ALL));
        setFilter(getFilterProperty(cname + ".filter", null));
        setFormatter(getFormatterProperty(cname + ".formatter",
                                          new SimpleFormatter()));

        loggingHistorySize = Settings.getInt(
                MonitorSettings.LOGGING_HISTORY_SIZE);
        loggingHistory = Collections.synchronizedList(
                new ArrayList<LogRecord>(loggingHistorySize));
        //Fill out the list with loggingHistorySize null-records.
        loggingHistory.addAll(Arrays.asList(new LogRecord[loggingHistorySize]));
        loggingMBeans = new ArrayList<CachingLogRecord>(loggingHistorySize);
        for (int i = 0; i < loggingHistorySize; i++) {
            loggingMBeans.add(new CachingLogRecord(i, this));
        }
        currentIndex = 0;
    }

    /**
     * Publish a <tt>LogRecord</tt>. This simply remembers the record in
     * datastructures, and thus exposes it in an MBean.
     *
     * @param record description of the log event. A null record is silently
     *               ignored and is not published
     * @see Handler#publish(LogRecord)
     */
    public void publish(LogRecord record) {
        if (record == null || !isLoggable(record)) {
            return;
        }
        loggingHistory.set(currentIndex, record);
        currentIndex = (currentIndex + 1) % loggingHistorySize;
    }

    /**
     * Does nothing. No flushing necessary in this handler.
     */
    public void flush() {
    }

    /**
     * Close the <tt>Handler</tt> and free all associated resources. <p> The
     * close method will perform a <tt>flush</tt> and then close the
     * <tt>Handler</tt>.   After close has been called this <tt>Handler</tt>
     * should no longer be used.  Method calls may either be silently ignored or
     * may throw runtime exceptions.
     *
     * @throws SecurityException never.
     */
    public void close() throws SecurityException {
        flush();
        for (CachingLogRecord cachingLogRecord : loggingMBeans) {
            cachingLogRecord.close();
        }
        loggingMBeans.clear();
        loggingHistory.clear();
    }

    /**
     * Returns the nth logrecord from the top.
     *
     * @param n The number of the log record to get
     * @return The LogRecord which is number n from the top, or null for none.
     */
    public LogRecord getNthLogRecord(int n) {
        if ((n < 0) || (n >= loggingHistorySize)) {
            throw new ArgumentNotValid("Argument 'int n' must be between 0 and "
                                       + loggingHistorySize + ", but was "
                                       + n + ".");
        }
        return loggingHistory.get((currentIndex - n - 1 + loggingHistorySize)
                                  % loggingHistorySize);
    }
}
