package dk.netarkivet.harvester.harvesting.frontier;

import java.util.Comparator;

/**
 * This class implements a natural order on {@link FrontierReportLine}. 
 * Comparisons are made :
 * - first by decreasing values of totalEnqueues
 * - secondly by domain name (string natural order)
 *
 */
public class FrontierReportLineNaturalOrder 
implements Comparator<FrontierReportLineOrderKey> {
    
    private static final FrontierReportLineNaturalOrder order = 
        new FrontierReportLineNaturalOrder();
    
    @Override
    public int compare(
            FrontierReportLineOrderKey k1, 
            FrontierReportLineOrderKey k2) {
        int sizeComp = 
            Long.valueOf(k1.getQueueSize()).compareTo(k2.getQueueSize());
        if  (sizeComp == 0) {
            return k1.getQueueId().compareTo(k2.getQueueId());
        }
        return  -sizeComp;
    }
    
    /**
     * Returns the singleton instance of this class.
     * @return the singleton instance of this class.
     */
    public static FrontierReportLineNaturalOrder getInstance() {
        return order;
    }

}
