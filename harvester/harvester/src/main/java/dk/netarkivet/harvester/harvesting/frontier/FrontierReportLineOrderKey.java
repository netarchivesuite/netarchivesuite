package dk.netarkivet.harvester.harvesting.frontier;

import com.sleepycat.persist.model.Persistent;

/**
 * Defines the parameters needed to determine order on frontier report lines.
 *
 */
@Persistent
interface FrontierReportLineOrderKey {
    
    /**
     * Returns the queue's unique identifier.
     * @return the queue's unique identifier.
     */
    String getQueueId();
    
    /**
     * Returns the queue size.
     * @return the queue size.
     */
    long getQueueSize();

}
