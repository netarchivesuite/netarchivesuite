/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * OutputStream which can be used to redirect all stdout and stderr to a logger. Usage: System.setOut(new
 * PrintStream(new LoggingOutputStream(LoggingOutputStream.LoggingLevel.INFO, log, "StdOut: "))); System.setErr(new
 * PrintStream(new LoggingOutputStream(LoggingOutputStream.LoggingLevel.WARN, log, "StdErr: ")));
 */
public class LoggingOutputStream extends OutputStream {

    /** Enum representing the standard logging levels for commons logging. */
    public static enum LoggingLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    ;

    /** The level at which messages are logged. */
    private LoggingLevel loggingLevel;

    private final Logger logger;

    /** A prefix by which log-messages from this class can be recognised. */
    private String prefix;

    private String lineSeparator = System.getProperty("line.separator");
    private StringBuffer buffer = new StringBuffer();

    /**
     * Constructor for the class.
     *
     * @param loggingLevel The logging level at which to log messages from this instance.
     * @param logger The logger to which messages will be logged.
     * @param prefix A prefix by which output from this instance can be identified.
     */
    public LoggingOutputStream(LoggingLevel loggingLevel, Logger logger, String prefix) {
        this.loggingLevel = loggingLevel;
        this.logger = logger;
        this.prefix = prefix;
    }

    // FIXME cosmically ineffective not to implemente the other write methods.
    @Override
    public void write(int b) throws IOException {
        this.buffer.append((char) b);
        String s = buffer.toString();
        // This next line is an optimisation. Only convert the whole buffer to string
        // if the most recent character might be the end of the line separator.
        if ((lineSeparator.indexOf(b) != -1) && s.contains(lineSeparator)) {
            s = prefix + "'" + s + "'";
            switch (loggingLevel) {
            case TRACE:
                logger.trace(s);
                break;
            case DEBUG:
                logger.debug(s);
                break;
            case INFO:
                logger.info(s);
                break;
            case WARN:
                logger.warn(s);
                break;
            case ERROR:
                logger.error(s);
                break;
            }
            buffer.setLength(0);
        }
    }
}
