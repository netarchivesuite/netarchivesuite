
package dk.netarkivet.common.utils;

/**
 * Interface for classes which can be cleaned up by a shutdown hook.
 *
 */
public interface CleanupIF {

    /**
     * Used to clean up a class from within a shutdown hook. Must
     * not do any logging. Program defensively, please.
     */
    void cleanup();

}
