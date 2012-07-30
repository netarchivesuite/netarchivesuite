/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
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
        return l.getSessionBalance() <= 0
            || (totalBudget >= 0 && l.getTotalSpend() >= totalBudget);
    }

}
