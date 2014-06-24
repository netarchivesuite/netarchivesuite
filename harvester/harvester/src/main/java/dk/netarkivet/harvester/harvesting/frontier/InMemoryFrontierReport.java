package dk.netarkivet.harvester.harvesting.frontier;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Implements a frontier report wrapper that is stored in memory.
 * This implementation is intended for small reports that are the result of
 * the filtering of a full frontier report obtained from Heritrix.
 * This implementation is serializable, so it can be transmitted
 * in a JMS message.
 *
 * The report lines are sorted according to the natural order defined by
 * {@link FrontierReportLine}, e.g. descending size of the queue.
 */
@SuppressWarnings({ "serial"})
public class InMemoryFrontierReport extends AbstractFrontierReport
implements Serializable {

    /**
     * The lines of the report, sorted by natural order.
     */
    private TreeSet<FrontierReportLine> lines =
        new TreeSet<FrontierReportLine>();

    /**
     * The lines of the report, mapped by domain name.
     */
    private TreeMap<String, FrontierReportLine> linesByDomain =
        new TreeMap<String, FrontierReportLine>();

    /**
     * Default empty contructor.
     */
    InMemoryFrontierReport() {

    }

    /**
     * Builds an empty report.
     * @param jobName the Heritrix job name
     */
    public InMemoryFrontierReport(String jobName) {
        super(jobName);
    }

    /**
     * Returns the lines of the report.
     * @return the lines of the report.
     */
    public FrontierReportLine[] getLines() {
        return (FrontierReportLine[]) lines.toArray(
                new FrontierReportLine[lines.size()]);
    }

    @Override
    public void addLine(FrontierReportLine line) {
        lines.add(line);
        linesByDomain.put(line.getDomainName(), line);
    }

    @Override
    public FrontierReportLine getLineForDomain(String domainName) {
        return linesByDomain.get(domainName);
    }

    /**
     * Returns the report size, e.g. the count of report lines.
     * @return the report size
     */
    public int getSize() {
        return lines.size();
    }

}
