package dk.netarkivet.harvester.harvesting.frontier;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport.ReportIterator;

/**
 * Filters the N active queues (i.e. not exhausted or retired)
 * with the biggest totalEnqueues values.
 * The size of the list is defined by the setting property
 *
 */
public class TopTotalEnqueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        if (!(initialFrontier instanceof FullFrontierReport)) {
            throw new ArgumentNotValid(getClass().getSimpleName()
                    + " operates only on "
                    + FullFrontierReport.class.getSimpleName());
        }

        FullFrontierReport full = (FullFrontierReport) initialFrontier;

        InMemoryFrontierReport topRep =
            new InMemoryFrontierReport(initialFrontier.getJobName());

        ReportIterator iter = full.iterateOnTotalEnqueues();
        try {
            int addedLines = 0;
            int howMany = getMaxSize();
            while (addedLines < howMany) {
                if (! iter.hasNext()) {
                    break; // No more values, break loop
                }

                FrontierReportLine fetch = iter.next();
                long totalBudget = fetch.getTotalBudget();

                // Add only lines that are neither retired or exhausted
                if (fetch.getCurrentSize() > 0
                    && fetch.getSessionBalance() > 0
                    && (totalBudget == Constants.HERITRIX_MAXOBJECTS_INFINITY
                            || fetch.getTotalSpend() < totalBudget)) {
                    topRep.addLine(new FrontierReportLine(fetch));
                    addedLines++;
                }
            }

        } finally {
            if (iter != null) {
                iter.close();
            }
        }

        return topRep;
    }

}
