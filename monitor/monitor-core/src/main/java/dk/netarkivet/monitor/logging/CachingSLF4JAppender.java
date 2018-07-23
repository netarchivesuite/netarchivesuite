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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.monitor.MonitorSettings;

/**
 * SLF4J appender that caches a certain number of log entries in a cyclic manor.
 * "DEBUG and TRACE entries are excluded".
 */
public class CachingSLF4JAppender extends AppenderBase<ILoggingEvent> {

	/** Log format string pattern. */
    protected String pattern;

    /** Pattern layouter used to format log string. */
    protected PatternLayout layout;

    /** The size of the logging cache. */
    protected final int loggingHistorySize;

    /** The logging cache itself, caching the last "loggingHistorySize" log entries. */
    protected final List<String> loggingHistory;

    /** The log entries exposed as MBeans. */
    protected final List<CachingSLF4JLogRecord> loggingMBeans;

    /** The place in the loggingHistory for the next LogRecord. */
    protected int currentIndex;

    /**
     * Initialize an instance of this class.
     */
    public CachingSLF4JAppender() {
        layout = new PatternLayout();
        loggingHistorySize = Settings.getInt(MonitorSettings.LOGGING_HISTORY_SIZE);
        loggingHistory = Collections.synchronizedList(new ArrayList<String>(loggingHistorySize));
        // Fill out the list with loggingHistorySize null-records.
        loggingHistory.addAll(Arrays.asList(new String[loggingHistorySize]));
        loggingMBeans = new ArrayList<CachingSLF4JLogRecord>(loggingHistorySize);
        for (int i = 0; i < loggingHistorySize; i++) {
            loggingMBeans.add(new CachingSLF4JLogRecord(i, this));
        }
        currentIndex = 0;
    }

    /**
     * Returns the pattern used to format the log string.
     * @return the pattern used to format the log string
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Set the pattern used to format the log string.
     * The method should be called before the setContext() or start() methods, most notably if used programmatically..
     * @param pattern log pattern
     */
    public void setPattern(String pattern) {
    	this.isStarted();
        this.pattern = pattern;
        layout.setPattern(pattern);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        layout.setContext(this.context);
    }

    @Override
    public void start() {
        super.start();
        layout.start();
    }

    @Override
    public void stop() {
        super.stop();
        layout.stop();
    }

    /**
     * Close the appender and release associated resources.
     */
    public void close() {
    	layout = null;
    	loggingHistory.clear();
    	if (!loggingMBeans.isEmpty()) {
    		Iterator<CachingSLF4JLogRecord> iter = loggingMBeans.iterator();
    		while (iter.hasNext()) {
    			iter.next().close();
    		}
        	loggingMBeans.clear();
    	}
    }

    @Override
    protected void append(ILoggingEvent event) {
    	switch (event.getLevel().toInt()) {
    	case Level.TRACE_INT:
    	case Level.DEBUG_INT:
    		break;
    	case Level.INFO_INT:
    	case Level.WARN_INT:
    	case Level.ERROR_INT:
   		default:
   	        loggingHistory.set(currentIndex, layout.doLayout(event));
   	        currentIndex = (currentIndex + 1) % loggingHistorySize;
   			break;
    	}
    }

    /**
     * Returns the nth logrecord from the top.
     *
     * @param n The number of the log record to get
     * @return The LogRecord which is number n from the top, or null for none.
     */
    public String getNthLogRecord(int n) {
        if ((n < 0) || (n >= loggingHistorySize)) {
            throw new ArgumentNotValid("Argument 'int n' must be between 0 and " + loggingHistorySize + ", but was "
                    + n + ".");
        }
        return loggingHistory.get((currentIndex - n - 1 + loggingHistorySize) % loggingHistorySize);
    }

}
