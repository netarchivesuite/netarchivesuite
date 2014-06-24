package dk.netarkivet.common.management;

/**
 * Contains the constants for the management classes.
 */
public final class Constants {
    /**
     * Constructor.
     * Private due to constants only class.
     */
    private Constants() {}
    
    /**
     * These constant priority keys are also used by the monitor to find 
     * the value in the Translation files.
     */
    
    /** The location key word.*/
    public static final String PRIORITY_KEY_LOCATION = "location";
    /** The machine key word.*/
    public static final String PRIORITY_KEY_MACHINE = "machine";
    /** The application name key word.*/
    public static final String PRIORITY_KEY_APPLICATIONNAME = "applicationname";
    /** The application instance id key word.*/
    public static final String PRIORITY_KEY_APPLICATIONINSTANCEID =
        "applicationinstanceid";
    /** The http port key word.*/
    public static final String PRIORITY_KEY_HTTP_PORT = "httpport";
    /** The harvest channel key word.*/
    public static final String PRIORITY_KEY_CHANNEL = "channel";
    /** The replica key word.*/
    public static final String PRIORITY_KEY_REPLICANAME = "replicaname";
    /** The index key word.*/
    public static final String PRIORITY_KEY_INDEX = "index";
    /** The remove jmx application keyword. */
    public static final String REMOVE_JMX_APPLICATION = "removeapplication";
}
