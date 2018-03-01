/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting.frontier;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport.ReportIterator;

/**
 * Filters the N active queues (i.e. not exhausted or retired) with the biggest totalEnqueues values. The size of the
 * list is defined by the setting property
 */
public class TopTotalEnqueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        if (!(initialFrontier instanceof FullFrontierReport)) {
            throw new ArgumentNotValid(getClass().getSimpleName() + " operates only on "
                    + FullFrontierReport.class.getSimpleName() + ", not: " 
            		+ initialFrontier.getClass().getSimpleName());
        }

        FullFrontierReport full = (FullFrontierReport) initialFrontier;

        InMemoryFrontierReport topRep = new InMemoryFrontierReport(initialFrontier.getJobName());

        ReportIterator iter = full.iterateOnTotalEnqueues();
        try {
            int addedLines = 0;
            int howMany = getMaxSize();
            while (addedLines < howMany) {
                if (!iter.hasNext()) {
                    break; // No more values, break loop
                }

                FrontierReportLine fetch = iter.next();
                long totalBudget = fetch.getTotalBudget();

                // Add only lines that are neither retired or exhausted
                if (fetch.getCurrentSize() > 0
                        && fetch.getSessionBalance() > 0
                        && (totalBudget == Constants.HERITRIX_MAXOBJECTS_INFINITY || fetch.getTotalSpend() < totalBudget)) {
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
