package dk.netarkivet.harvester.harvesting.frontier;


/**
 * Interface for a frontier report filter.
 *
 * Such a filter takes a frontier report as input, and filters its lines to
 * generate another frontier report.
 *
 */
public interface FrontierReportFilter {

    /**
     * Initialize the filter from arguments.
     * @param args the arguments as strings.
     */
    void init(String[] args);

    /**
     * Filters the given frontier report.
     * @param initialFrontier the report to filter.
     * @return a filtered frontier report.
     */
    InMemoryFrontierReport process(FrontierReport initialFrontier);

    /**
     * Returns a unique identifier for this filter class.
     * @return unique identifier for this filter class
     */
    String getFilterId();

}
