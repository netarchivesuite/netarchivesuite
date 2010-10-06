/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Filters the N active queues (i.e. not exhausted or retired)
 * with the biggest totalEnqueues values.
 * The size of the list is defined by the setting property
 *
 */
public class TopTotalEnqueuesFilter extends MaxSizeFrontierReportExtract {

    @Override
    public InMemoryFrontierReport process(FrontierReport initialFrontier) {
        if (! (initialFrontier instanceof FullFrontierReport)) {
            throw new ArgumentNotValid(getClass().getSimpleName()
                    + " operates only on "
                    + FullFrontierReport.class.getSimpleName());
        }

        FullFrontierReport full = (FullFrontierReport) initialFrontier;

        InMemoryFrontierReport topRep =
            new InMemoryFrontierReport(initialFrontier.getJobName());
        FrontierReportLine[] topQueues =
            full.getBiggestTotalEnqueues(getMaxSize());
        for (FrontierReportLine l : topQueues) {
            topRep.addLine(new FrontierReportLine(l));
        }


        return topRep;
    }

}
