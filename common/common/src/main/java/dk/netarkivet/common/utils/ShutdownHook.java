
package dk.netarkivet.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;

/**
 * Defines a ShutdownHook for a class which has a cleanup method.
 *
 */
public class ShutdownHook extends Thread {
    /** The component to hook up to. */
    private LifeCycleComponent app;
    /** The name of the hooked application. */
    private String appName;    

    /**
     * Returns a ShutdownHook thread for an object with a cleanup() method.
     * @param app the Object to be cleaned up
     */
    public ShutdownHook(LifeCycleComponent app) {
        ArgumentNotValid.checkNotNull(app, "LifeCycleComponent app");
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
            log.info("Shutting down " + appName);
        } catch (Throwable e) {
            //Ignore
        }
        try {
            app.shutdown();
        } catch (Throwable e) {
            System.out.println("Error while  shutting down "
                    + appName);
            e.printStackTrace();
        }
        try {
            System.out.println("Shutting down " + appName);
            log.info("Shutting down " + appName);
        } catch (Throwable e) {
            System.out.println("Shutting down " + appName
                    + " but failed to log afterwards");
            e.printStackTrace();
        }
    }
}
