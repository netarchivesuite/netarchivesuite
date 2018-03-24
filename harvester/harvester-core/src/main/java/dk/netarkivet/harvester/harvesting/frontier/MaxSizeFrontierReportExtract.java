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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

abstract class MaxSizeFrontierReportExtract extends AbstractFrontierReportFilter {

    /** The logger to use. */
	private static final Logger LOG = LoggerFactory.getLogger(MaxSizeFrontierReportExtract.class);

    private static final int DEFAULT_SIZE = 200;

    private int maxSize = DEFAULT_SIZE;

    @Override
    public void init(String[] args) {
        if (args.length != 1) {
            throw new ArgumentNotValid(getFilterId() + " expects only 1 argument, not " + args.length);
        }
        try {
            maxSize = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            maxSize = DEFAULT_SIZE;
            LOG.warn("Report size not specified, hence set to default value '{}'!", DEFAULT_SIZE);
        }
    }

    @Override
    public abstract InMemoryFrontierReport process(FrontierReport initialFrontier);

    /**
     * Returns the list maximum size.
     *
     * @return the list maximum size.
     */
    int getMaxSize() {
        return maxSize;
    }

}
