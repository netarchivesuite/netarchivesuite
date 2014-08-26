/*
 * #%L
 * Netarchivesuite - monitor
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
import java.util.List;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.monitor.MonitorSettings;

/**
 *
 * @author tra
 */
public class CachingSLF4JAppender extends AppenderBase<ILoggingEvent> {

    /**
     *
     */
    protected String pattern;

    /**
     *
     */
    protected PatternLayout layout;

    /** The size of the logging cache. */
    private final int loggingHistorySize;

    /**
     * The logging cache itself, caching the last "loggingHistorySize" log entries.
     */
    private final List<String> loggingHistory;

    /** The log entries exposed as MBeans. */
    private final List<CachingSLF4JLogRecord> loggingMBeans;

    /** The place in the loggingHistory for the next LogRecord. */
    private int currentIndex;

    /**
     *
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
     *
     * @param context
     */
    @Override
    public void setContext(Context context) {
        super.setContext(context);
        layout.setContext(this.context);
    }

    /**
     *
     */
    @Override
    public void start() {
        super.start();
        layout.start();
    }

    /**
     *
     */
    @Override
    public void stop() {
        super.stop();
        layout.stop();
    }

    /**
     *
     * @param event
     */
    @Override
    protected void append(ILoggingEvent event) {
        loggingHistory.set(currentIndex, layout.doLayout(event));
        currentIndex = (currentIndex + 1) % loggingHistorySize;
    }

    /**
     *
     * @return
     */
    public String getPattern() {
        return pattern;
    }

    /**
     *
     * @param pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
        layout.setPattern(pattern);
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
