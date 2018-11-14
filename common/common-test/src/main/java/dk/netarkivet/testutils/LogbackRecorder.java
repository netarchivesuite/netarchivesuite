/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.testutils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

// TODO So maybe these methods should be unit-tested... NICL

/**
 * This class implements an <code>Logback</code> appender which can be attached dynamically to an
 * <code>SLF4J</code> context. The appender stores logging events in memory so their occurrence (or lack of) can be
 * validated, most likely, in unit test.
 *
 * It can be used to test whether logging is performed. The normal usage is:
 * <pre>
 * <code>public void testSomething() {
 *     LogbackRecorder logRecorder = LogbackRecorder.startRecorder();
 *     doTheTesting();
 *     logRecorder.assertLogContains(theStringToVerifyIsInTheLog);
 * }
 * </code>
 * </pre>
 *
 * Remember to call the stopRecorder method on the logRecorder instance when finished. The probability
 * of doing this on a consistent basis is increased if the LogbackRecorder.startRecorder() and stopRecorder calls
 * are made as part of the @Before and @After test methods.
 */
public class LogbackRecorder extends ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> {

    /** The Logback context currently in use. */
    protected static final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    /** The root Logback logger, used to attach appender(s). */
    protected static final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    /** This instances appender. */
    protected Appender<ILoggingEvent> appender;

    /** List of archived logging events, can be reset any at point by calling reset(). */
    protected List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

    /**
     * Constructor only for use in unit tests.
     */
    protected LogbackRecorder() {
    }

    /**
     * Create a new <code>LogbackRecorder</code> and attach it to the current logging context's root logger.
     */
    public static LogbackRecorder startRecorder() {
        LogbackRecorder lu = new LogbackRecorder();
        lu.setName("unit-test");
        lu.setContext(context);
        lu.start();
        root.addAppender(lu);
        return lu;
    }

    /**
     * Stops recorder, clears recorded events and detaches appender from logging context's root logger.
     * @return indication of success trying to detach appender
     */
    public boolean stopRecorder() {
        stop();
        events.clear();
        return root.detachAppender(this);
    }

    /**
     * Reset recorder by clearing all recorder events.
     */
    public void reset() {
        events.clear();
    }

    @Override
    protected synchronized void append(ILoggingEvent event) {
        events.add(event);
        // System.out.println("#\n#" + event.getLoggerName() + "#\n");
    }

    /**
     * Returns boolean indicating whether any log entries have been recorded.
     * @return boolean indicating whether any log entries have been recorded
     */
    public synchronized boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Tries to find a log entry with a specific log level containing a specific string and fails the if no match is
     * found.
     * @param level The log level of the log to find
     * @param logStringToLookup The string to find in the log.
     */
    public synchronized void assertLogContains(Level level, String logStringToLookup) {
        boolean matchFound = false;
        Set<Level> matchedLevels = new HashSet<>();
        for (ILoggingEvent logEntry : events) {
            if (logEntry.getFormattedMessage().indexOf(logStringToLookup) != -1) {
                if (logEntry.getLevel() == level) {
                    matchFound = true;
                    break;
                } else {
                    matchedLevels.add(logEntry.getLevel());
                }
            }
        }
        if (!matchFound) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to find match level(" + level + ") in log: " + logStringToLookup);
            if (!matchedLevels.isEmpty()) {
                sb.append("\nFound matches for other log levels though: " + matchedLevels);
            }
            Assert.fail(sb.toString());
        }
    }

    /**
     * Assert that there is a recorded entry than contains the supplied string.
     * @param logStringToLookup string to match for
     */
    public synchronized void assertLogContains(String logStringToLookup) {
    	assertLogContains((String) null, logStringToLookup);
    }

    /**
     * Assert that there is a recorded entry than contains the supplied string.
     * @param msg error message or null
     * @param logStringToLookup string to match for
     */
    public synchronized void assertLogContains(String msg, String logStringToLookup) {
        Iterator<ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getFormattedMessage().indexOf(logStringToLookup) != -1);
        }
        if (!bMatched) {
        	if (msg == null) {
                msg = "Unable to match in log: " + logStringToLookup;
        	}
            Assert.fail(msg);
        }
    }

    /**
     * Assert that there is a recorded entry than matches the supplied regular expression.
     * @param msg error message or null
     * @param regexToLookup regular expression to match for
     */
    public synchronized void assertLogMatches(String msg, String regexToLookup) {
        Pattern pattern = Pattern.compile(regexToLookup);
        Iterator<ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = pattern.matcher((iter.next().getFormattedMessage())).find();
        }
        if (!bMatched) {
        	if (msg == null) {
                msg = "Unable to match regex in log: " + regexToLookup;
        	}
            Assert.fail(msg);
        }
    }

    /**
     * Assert that there is no recorded entry with the supplied string 
     * @param logStringToLookup log message to look for
     */
    public synchronized void assertLogNotContains(String logStringToLookup) {
    	assertLogNotContains(null, logStringToLookup);
    }

    /**
     * Assert that there is no recorded entry with the supplied string 
     * @param msg error message or null
     * @param logStringToLookup log message to look for
     */
    public synchronized void assertLogNotContains(String msg, String logStringToLookup) {
        Iterator<ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getFormattedMessage().indexOf(logStringToLookup) != -1);
        }
        if (bMatched) {
        	if (msg == null) {
                msg = "Able to match in log: " + logStringToLookup;
        	}
            Assert.fail(msg);
        }
    }

    /**
     * Assert that there is no recorded log entry with the supplied log level.
     * @param msg error message or null
     * @param level log level
     */
    public synchronized void assertLogNotContainsLevel(String msg, Level level) {
        Iterator<ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getLevel() == level);
        }
        if (bMatched) {
        	if (msg == null) {
                msg = "Able to match level=" + level.toString() + " + in log.";
        	}
            Assert.fail(msg);
        }
    }

    /**
     * Search the log entry list for a string starting from a specific index.
     * @param logStringToLookup string to find
     * @param fromIndex log entry list start index
     * @return index of next occurrence or -1, if not found
     */
    public synchronized int logIndexOf(String logStringToLookup, int fromIndex) {
        boolean bMatched = false;
        while (fromIndex >= 0 && fromIndex < events.size() && !bMatched) {
            if (events.get(fromIndex).getFormattedMessage().indexOf(logStringToLookup) != -1) {
                bMatched = true;
            } else {
                ++fromIndex;
            }
        }
        if (!bMatched) {
            fromIndex = -1;
        }
        return fromIndex;
    }

    /**
     * Add filter on all appenders registered with the logger with the supplied logger name.
     * @param filter filter to add
     * @param loggerName name of logger
     */
    public void addFilter(Filter<ILoggingEvent> filter, String loggerName) {
        Logger logger = (Logger)LoggerFactory.getLogger(loggerName);
        if (logger != null) {
            Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders();
            while (index.hasNext()) {
                appender = index.next();
                appender.addFilter(filter);
            }
        }
    }

    /**
     * Remove all filters on all appenders registered with the logger with the supplied logger name.
     * @param loggerName name of logger
     */
    public void clearAllFilters(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        if (logger != null) {
            Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders();
            while (index.hasNext()) {
                appender = index.next();
                appender.clearAllFilters();
            }
        }
    }

    /**
     * Simple deny filter.
     */
    public static class DenyFilter extends Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            return FilterReply.DENY;
        }
    }

}
