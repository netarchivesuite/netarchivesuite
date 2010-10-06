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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

abstract class MaxSizeFrontierReportExtract
extends AbstractFrontierReportFilter {

    /** The logger to use.    */
    static final Log LOG = LogFactory.getLog(
            MaxSizeFrontierReportExtract.class);

    private static final int DEFAULT_SIZE = 200;

    private int maxSize = DEFAULT_SIZE;

    @Override
    public void init(String[] args) {
        if (args.length != 1) {
            throw new ArgumentNotValid(
                    getFilterId() + " expects 1 argument: size");
        }
        try {
            maxSize = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            maxSize = DEFAULT_SIZE;
            LOG.warn(args[0] + " not integer!", e);
        }
    }

    @Override
    public abstract InMemoryFrontierReport process(
            FrontierReport initialFrontier);

    /**
     * Returns the list maximum size.
     * @return the list maximum size.
     */
    int getMaxSize() {
        return maxSize;
    }

}
