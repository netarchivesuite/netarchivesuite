package dk.netarkivet.common.utils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream which can be used to redirect all stdout and stderr to a logger.
 * Usage:
 *  System.setOut(new PrintStream(new LoggingOutputStream(LoggingOutputStream.LoggingLevel.INFO, log, "StdOut: ")));
    System.setErr(new PrintStream(new LoggingOutputStream(LoggingOutputStream.LoggingLevel.WARN, log, "StdErr: ")));
 */
public class LoggingOutputStream extends OutputStream {

    /**
     * Enum representing the standard logging levels for commons logging.
     */
    public static enum LoggingLevel {TRACE, DEBUG, INFO, WARN, ERROR};

    /**
     * The level at which messages are logged.
     */
    private LoggingLevel loggingLevel;

    /**
     * The Log to which messages are logged.
     */
    private Log logger;

    /**
     * A prefix by which log-messages from this class can be recognised.
     */
    private String prefix;

    private String lineSeparator = System.getProperty("line.separator");
    private StringBuffer buffer = new StringBuffer();

    /**
     * Constructor for the class.
     * @param loggingLevel  The logging level at which to log messages from this instance.
     * @param logger The logger to which messages will be logged.
     * @param prefix A prefix by which output from this instance can be identified.
     */
    public LoggingOutputStream(LoggingLevel loggingLevel, Log logger, String prefix) {
        this.loggingLevel= loggingLevel;
        this.logger = logger;
        this.prefix = prefix;
    }

    @Override
    public void write(int b) throws IOException {
        this.buffer.append((char) b);
        String s = buffer.toString();
        //This next line is an optimisation. Only convert the whole buffer to string
        //if the most recent character might be the end of the line separator.
        if ( (lineSeparator.indexOf(b) != -1)  && s.contains(lineSeparator)) {
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
