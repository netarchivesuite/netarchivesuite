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

import java.io.PrintWriter;


/**
 * Common interface for an Heritrix frontier report wrapper.
 *
 */
public interface FrontierReport {

    /**
     * @return the jobName
     */
    String getJobName();

    /**
     * @return the creation timestamp
     */
    long getTimestamp();

    /**
     * Add a line to the report.
     * @param line line to add.
     */
    void addLine(FrontierReportLine line);

    /**
     * Returns the line of the frontier report corresponding to the
     * queue for the given domain name.
     * @param domainName the domain name.
     * @return null if no queue for this domain name exists, otherwise the line
     * of the frontier report corresponding to the queue for the
     * given domain name.
     */
    FrontierReportLine getLineForDomain(String domainName);

    /**
     * Returns the retired queues, e.g. the queues that have hit the totalBudget
     * value (queue-total-budget).
     * @param maxSize maximum count of elements to fetch
     * @return an array of retired queues of maxSize.
     */
    FrontierReportLine[] getRetiredQueues(int maxSize);

    /**
     * Returns the exhausted queues, e.g. the queues whose current size is zero.
     * @param maxSize maximum count of elements to fetch
     * @return an array of exhausted queues.
     */
    FrontierReportLine[] getExhaustedQueues(int maxSize);

}
