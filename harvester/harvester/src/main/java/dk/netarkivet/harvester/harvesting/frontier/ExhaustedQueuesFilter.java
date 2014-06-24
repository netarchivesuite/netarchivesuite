package dk.netarkivet.harvester.harvesting.frontier;

import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport.ReportIterator;

/**
 * Filters a frontier report to include only lines that represent
 * exhausted queues.
 * An Heritrix queue is exhausted when its current size is zero.
 */
public class ExhaustedQueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        InMemoryFrontierReport result = new InMemoryFrontierReport(
                initialFrontier.getJobName());

        FullFrontierReport full = (FullFrontierReport) initialFrontier;
        ReportIterator iter = full.iterateOnDuplicateCurrentSize(0L);

        int maxSize = getMaxSize();
        int addedLines = 0;
        while (addedLines <= maxSize && iter.hasNext()) {
            result.addLine(new FrontierReportLine(iter.next()));
            addedLines++;
        }

        return result;
    }

}
