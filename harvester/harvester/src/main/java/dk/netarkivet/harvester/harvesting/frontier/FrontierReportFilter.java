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


/**
 * Interface for a frontier report filter.
 *
 * Such a filter takes a frontier report as input, and filters its lines to
 * generate another frontier report.
 *
 */
public interface FrontierReportFilter {

    /**
     * Initialize the filter from arguments.
     * @param args the arguments as strings.
     */
    void init(String[] args);

    /**
     * Filters the given frontier report.
     * @param initialFrontier the report to filter.
     * @return a filtered frontier report.
     */
    InMemoryFrontierReport process(FrontierReport initialFrontier);

    /**
     * Returns a unique identifier for this filter class.
     * @return unique identifier for this filter class
     */
    String getFilterId();

}
