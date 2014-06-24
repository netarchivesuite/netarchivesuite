package dk.netarkivet.common.lifecycle;

/**
 * Extends the default construction -> deconstruction object life cycle with 
 * addition steps, giving users of
 * <code>ComponentLifeCycle</code> better control over the component startup 
 * and shutdown phases.
 */
public interface ComponentLifeCycle {
    
    /**
     * Implements functionality for starting an instances of this 
     * <code>ComponentLifeCycle</code> object. 
     * This may be loading files, establish connections, initializing data,
     *  starting threads, etc.
     */
    void start();
    
    /**
     * The inverse of the <code>start()</code> method. Contains functionality 
     * for deallocation of ressources, clearing data, closing connections, 
     * stopping threads, etc
     */
    void shutdown();
}
