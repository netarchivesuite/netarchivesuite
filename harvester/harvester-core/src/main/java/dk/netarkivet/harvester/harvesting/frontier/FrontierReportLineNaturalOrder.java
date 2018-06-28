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

import java.util.Comparator;

/**
 * This class implements a natural order on {@link FrontierReportLine}. Comparisons are made : - first by decreasing
 * values of totalEnqueues - secondly by domain name (string natural order)
 */
public class FrontierReportLineNaturalOrder implements Comparator<FrontierReportLineOrderKey> {

    private static final FrontierReportLineNaturalOrder order = new FrontierReportLineNaturalOrder();

    @Override
    public int compare(FrontierReportLineOrderKey k1, FrontierReportLineOrderKey k2) {
        int sizeComp = Long.valueOf(k1.getQueueSize()).compareTo(k2.getQueueSize());
        if (sizeComp == 0) {
            return k1.getQueueId().compareTo(k2.getQueueId());
        }
        return -sizeComp;
    }

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton instance of this class.
     */
    public static FrontierReportLineNaturalOrder getInstance() {
        return order;
    }

}
