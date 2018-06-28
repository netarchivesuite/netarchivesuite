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
package dk.netarkivet.harvester.harvesting.report;

import java.io.Serializable;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;

/**
 * Base interface for a post-crawl harvest report.
 */
public interface HarvestReport extends Serializable {

    /**
     * Returns the default stop reason initially assigned to every domain.
     */
    StopReason getDefaultStopReason();

    /**
     * Returns the set of domain names that are contained in hosts-report.txt (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    Set<String> getDomainNames();

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    Long getObjectCount(String domainName) throws ArgumentNotValid;

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    Long getByteCount(String domainName) throws ArgumentNotValid;

    /**
     * Get the StopReason for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain.
     * @throws ArgumentNotValid if null or empty domainName
     */
    StopReason getStopReason(String domainName) throws ArgumentNotValid;


    /**
     * Post-processing happens on the scheduler side when ARC files have been uploaded.
     */
    void postProcess(Job job);

}
