package dk.netarkivet.harvester.harvesting.frontier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

abstract class MaxSizeFrontierReportExtract
extends AbstractFrontierReportFilter {

    /** The logger to use.    */
    static final Log LOG = LogFactory.getLog(
            MaxSizeFrontierReportExtract.class);

    private static final int DEFAULT_SIZE = 200;

    private int maxSize = DEFAULT_SIZE;

    @Override
    public void init(String[] args) {
        if (args.length != 1) {
            throw new ArgumentNotValid(
                    getFilterId() + " expects 1 argument: size");
        }
        try {
            maxSize = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            maxSize = DEFAULT_SIZE;
            LOG.warn("Report size not specified, hence set to default value: "
                    + DEFAULT_SIZE + " !");
        }
    }

    @Override
    public abstract InMemoryFrontierReport process(
            FrontierReport initialFrontier);

    /**
     * Returns the list maximum size.
     * @return the list maximum size.
     */
    int getMaxSize() {
        return maxSize;
    }

}
