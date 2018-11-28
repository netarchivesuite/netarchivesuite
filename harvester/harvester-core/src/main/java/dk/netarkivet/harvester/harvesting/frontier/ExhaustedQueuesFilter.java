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

import dk.netarkivet.harvester.harvesting.frontier.FullFrontierReport.ReportIterator;

/**
 * Filters a frontier report to include only lines that represent exhausted queues. An Heritrix queue is exhausted when
 * its current size is zero.
 */
public class ExhaustedQueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        InMemoryFrontierReport result = new InMemoryFrontierReport(initialFrontier.getJobName());

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
