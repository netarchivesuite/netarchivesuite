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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Implements a frontier report wrapper that is stored in memory.
 * This implementation is intended for small reports that are the result of
 * the filtering of a full frontier report obtained from Heritrix.
 * This implementation is serializable, so it can be transmitted
 * in a JMS message.
 *
 * The report lines are sorted according to the natural order defined by
 * {@link FrontierReportLine}, e.g. descending size of the queue.
 */
public class InMemoryFrontierReport extends AbstractFrontierReport
implements Serializable {

    /**
     * The lines of the report, sorted by natural order.
     */
    private TreeSet<FrontierReportLine> lines =
        new TreeSet<FrontierReportLine>();

    /**
     * The lines of the report, mapped by domain name.
     */
    private TreeMap<String, FrontierReportLine> linesByDomain =
        new TreeMap<String, FrontierReportLine>();

    /**
     * Default empty contructor.
     */
    InMemoryFrontierReport() {

    }

    /**
     * Builds an empty report.
     * @param jobName the Heritrix job name
     */
    public InMemoryFrontierReport(String jobName) {
        super(jobName);
    }

    /**
     * Returns the lines of the report.
     * @return the lines of the report.
     */
    public FrontierReportLine[] getLines() {
        return (FrontierReportLine[]) lines.toArray(
                new FrontierReportLine[lines.size()]);
    }

    @Override
    public void addLine(FrontierReportLine line) {
        lines.add(line);
        linesByDomain.put(line.getDomainName(), line);
    }

    @Override
    public FrontierReportLine getLineForDomain(String domainName) {
        return linesByDomain.get(domainName);
    }

    /**
     * Returns the report size, e.g. the count of report lines.
     * @return the report size
     */
    public int getSize() {
        return lines.size();
    }

    /**
     * Returns the retired queues, e.g. the queues that have hit the totalBudget
     * value (queue-total-budget).
     * @param maxSize maximum count of elements to fetch
     * @return an array of retired queues.
     */
    @Override
    public FrontierReportLine[] getRetiredQueues(int maxSize) {
        // Get total budget from first line
        long totalBudget = lines.first().getTotalBudget();

        List<FrontierReportLine> retired = new LinkedList<FrontierReportLine>();
        for (FrontierReportLine l : lines) {
            if (retired.size() == maxSize) {
                break;
            }
            if (l.getTotalSpend() == totalBudget) {
                retired.add(l);
            }
        }

        return (FrontierReportLine[]) retired.toArray();
    }

    /**
     * Returns the exhausted queues, e.g. the queues whose current size is zero.
     * @param maxSize maximum count of elements to fetch
     * @return an array of exhausted queues.
     */
    @Override
    public FrontierReportLine[] getExhaustedQueues(int maxSize) {
        List<FrontierReportLine> exhausted =
            new LinkedList<FrontierReportLine>();
        for (FrontierReportLine l : lines) {
            if (exhausted.size() == maxSize) {
                break;
            }
            if (l.getCurrentSize() == 0) {
                exhausted.add(l);
            }
        }

        return (FrontierReportLine[]) exhausted.toArray();
    }

}
