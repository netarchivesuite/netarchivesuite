/*
 * #%L
 * Netarchivesuite - archive - test
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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

// TODO So maybe these methods should be unit-tested... NICL

/**
 * Can be used to test whether logging is perform. The normal usage is:
 * <pre>
 * <code>public void testSomething() {
 *     LogbackRecorder logRecorder = LogbackRecorder.startRecorder();
 *     doTheTesting();
 *     logRecorder.assertLogContains(theStringToVerifyIsInTheLog);
 * }
 * </code>
 * </pre>
 *
 * Remember to call the stopRecorder method on the logRecorder instance when finished. The properbility
 * of doing this on a consistent basis is increased if the LogbackRecorder.startRecorder() and stopRecorder calls
 * are made as part of the @Before and @After test methods.
 */
public class LogbackRecorder extends ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> {

    protected static final ch.qos.logback.classic.LoggerContext context = (ch.qos.logback.classic.LoggerContext) LoggerFactory
            .getILoggerFactory();

    protected static final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
            .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

    protected ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender;

    protected List<ch.qos.logback.classic.spi.ILoggingEvent> events = new ArrayList<ch.qos.logback.classic.spi.ILoggingEvent>();

    protected LogbackRecorder() {
    }

    public static LogbackRecorder startRecorder() {
        LogbackRecorder lu = new LogbackRecorder();
        lu.setName("unit-test");
        lu.setContext(context);
        lu.start();
        root.addAppender(lu);
        return lu;
    }

    public boolean stopRecorder() {
        stop();
        events.clear();
        return root.detachAppender(this);
    }

    public void reset() {
        events.clear();
    }

    @Override
    protected synchronized void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
        events.add(event);
        // System.out.println("#\n#" + event.getLoggerName() + "#\n");
    }

    public synchronized boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Tries to find a logentry with a specific log level containing a specific string and fails the if no match is
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
            StringBuilder sb = new StringBuilder("Unable to find match in " + level + " log: " + logStringToLookup);
            if (!matchedLevels.isEmpty()) {
                sb.append("\nFound matches for other log levels though: " + matchedLevels );
            }
            Assert.fail(sb.toString());
        }
    }

    public synchronized void assertLogNotContains(String str) {
        Iterator<ch.qos.logback.classic.spi.ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getFormattedMessage().indexOf(str) != -1);
        }
        if (bMatched) {
            Assert.fail("Able to match in log: " + str);
        }
    }

    public synchronized void assertLogContains(String msg, String str) {
        Iterator<ch.qos.logback.classic.spi.ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getFormattedMessage().indexOf(str) != -1);
        }
        if (!bMatched) {
            System.out.println("Unable to match in log: " + str);
            Assert.fail(msg);
        }
    }

    public synchronized void assertLogNotContains(String msg, String str) {
        Iterator<ch.qos.logback.classic.spi.ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getFormattedMessage().indexOf(str) != -1);
        }
        if (bMatched) {
            System.out.println("Able to match in log: " + str);
            Assert.fail(msg);
        }
    }

    public synchronized void assertLogMatches(String msg, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Iterator<ch.qos.logback.classic.spi.ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = pattern.matcher((iter.next().getFormattedMessage())).find();
        }
        if (!bMatched) {
            System.out.println("Unable to match in log: " + regex);
            Assert.fail(msg);
        }
    }

    public synchronized int logIndexOf(String str, int index) {
        boolean bMatched = false;
        while (index >= 0 && index < events.size() && !bMatched) {
            if (events.get(index).getFormattedMessage().indexOf(str) != -1) {
                bMatched = true;
            } else {
                ++index;
            }
        }
        if (!bMatched) {
            index = -1;
        }
        return index;
    }

    public synchronized void assertLogNotContainsLevel(String msg, ch.qos.logback.classic.Level level) {
        Iterator<ch.qos.logback.classic.spi.ILoggingEvent> iter = events.iterator();
        boolean bMatched = false;
        while (!bMatched && iter.hasNext()) {
            bMatched = (iter.next().getLevel() == level);
        }
        if (bMatched) {
            System.out.println("Able to match level=" + level.toString() + " + in log.");
            Assert.fail(msg);
        }
    }

    public static class DenyFilter extends ch.qos.logback.core.filter.Filter<ILoggingEvent> {
        @Override
        public FilterReply decide(ILoggingEvent event) {
            return FilterReply.DENY;
        }
    }

    ;

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

    public void clearAllFilters(String loggerName) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
        if (logger != null) {
            Iterator<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> index = logger
                    .iteratorForAppenders();
            while (index.hasNext()) {
                appender = index.next();
                appender.clearAllFilters();
            }
        }
    }
}
