package dk.netarkivet.harvester.harvesting.frontier;

import org.archive.crawler.frontier.WorkQueue;

import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport.ReportIterator;


public class RetiredQueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        InMemoryFrontierReport result = new InMemoryFrontierReport(
                initialFrontier.getJobName());

        FullFrontierReport full = (FullFrontierReport) initialFrontier;
        ReportIterator iter = full.iterateOnSpentBudget();
        try {
            int addedLines = 0;
            int maxSize = getMaxSize();
            while (addedLines <= maxSize && iter.hasNext()) {
                FrontierReportLine l = iter.next();
                if (isOverBudget(l)) {
                    result.addLine(new FrontierReportLine(l));
                    addedLines++;
                }
            }
        } finally {
            if (iter != null) {
                iter.close();
            }
        }

        return result;
    }

    /**
     * Determines whether a given frontier queue is retired, e.g. over budget.
     * @param l a {@link FrontierReportLine} representing a frontier queue
     * @return true if the queue is retired, false otherwise.
     * @see WorkQueue#isOverBudget()
     */
    private boolean isOverBudget(FrontierReportLine l) {
        long totalBudget = l.getTotalBudget();
        return totalBudget >= 0 && l.getTotalSpend() >= totalBudget;
    }

}
