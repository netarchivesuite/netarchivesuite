
package dk.netarkivet.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Defines a ShutdownHook for a class which has a cleanup method.
 *
 */
public class CleanupHook extends Thread {
    /** The application, which this CleanupHook, should help to cleanup. */
    private CleanupIF app;
    /** The name of the application, which this CleanupHook,
     * should help to cleanup. */
    private String appName;    

    /**
     * Returns a ShutdownHook thread for an object with a cleanup() method.
     * @param app the Object to be cleaned up
     */
    public CleanupHook(CleanupIF app) {
        ArgumentNotValid.checkNotNull(app, "CleanupIF app");
        this.app = app;
        appName = app.getClass().getName();
    }

    /**
     * Called by the JVM to clean up the object before exiting.
     * The method calls the cleanup() method 
     * Note: System.out.println is added in this method
     * because logging may or may not be active at this time.
     */
    public void run() {
        Log log = null;
        try {
            log = LogFactory.getLog(appName);
            log.info("Cleaning up " + appName);
        } catch (Throwable e) {
            //Ignore
        }
        try {
            app.cleanup();
        } catch (Throwable e) {
            System.out.println("Error while cleaning up "
                    + appName);
            e.printStackTrace();
        }
        try {
            // FIXME: No println in unit tests.
            System.out.println("Cleaned up " + appName);
            log.info("Cleaned up " + appName);
        } catch (Throwable e) {
            System.out.println("Cleaned up " + appName
                    + " but failed to log afterwards");
            e.printStackTrace();
        }
    }
}
