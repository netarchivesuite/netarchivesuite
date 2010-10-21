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

import java.util.Date;

final class FrontierTestUtils {

    /** Outputs a frontier lien as a string. */
    public static String toString(FrontierReportLine l) {
        return l.getDomainName()
            + " " + l.getCurrentSize()
            + " " + getDisplayValue(l.getTotalEnqueues())
            + " " + getDisplayValue(l.getSessionBalance())
            + " " + getDisplayValue(l.getLastCost())
            + "(" + getDisplayValue(l.getAverageCost()) + ")"
            + " " + l.getLastDequeueTime()
            + " " + l.getWakeTime()
            + " " + getDisplayValue(l.getTotalSpend())
            + "/" + getDisplayValue(l.getTotalBudget())
            + " " + getDisplayValue(l.getErrorCount())
            + " " + l.getLastPeekUri()
            + " " + l.getLastQueuedUri();
    }

    private static String getDisplayValue(long val) {
        return Long.MIN_VALUE == val ?
                FrontierReportLine.EMPTY_VALUE_TOKEN : "" + val;
    }

    private static String getDisplayValue(double val) {
        if (Double.MIN_VALUE == val) {
            return FrontierReportLine.EMPTY_VALUE_TOKEN;
        }
        return (Math.rint(val) == val ?
                Integer.toString((int) val) : "" + Double.toString(val));
    }

}
