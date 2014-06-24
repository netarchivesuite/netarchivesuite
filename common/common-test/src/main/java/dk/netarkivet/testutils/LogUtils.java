
package dk.netarkivet.testutils;

import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Utilities pertaining to testing of logs
 *
 */

public class LogUtils {
    /** Flush all logs for the given class. 
     * @param className Class to flush logs for. 
     */
    public static void flushLogs(final String className) {
        // Make sure that all logs are flushed before testing.
        Logger controllerLogger =
                Logger.getLogger(className);
        Handler[] handlers = controllerLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].flush();
        }
    }
}
