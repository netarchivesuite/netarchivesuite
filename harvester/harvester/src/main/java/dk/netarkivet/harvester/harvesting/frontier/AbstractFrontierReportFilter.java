package dk.netarkivet.harvester.harvesting.frontier;

/**
 * Base abstract class for frontier report filters.
 */
abstract class AbstractFrontierReportFilter implements FrontierReportFilter {

    /**
     * Initialize the filter from arguments.
     * @param args the arguments as strings.
     */
    public abstract void init(String[] args);

    @Override
    public abstract InMemoryFrontierReport process(
            FrontierReport initialFrontier);

    @Override
    public String getFilterId() {
        return getClass().getSimpleName();
    }

}
