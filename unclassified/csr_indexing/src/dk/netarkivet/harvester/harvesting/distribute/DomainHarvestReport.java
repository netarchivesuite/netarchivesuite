/* File:       $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;

/**
 * Interface to define what kind of statistics, all crawlers
 * are supposed to deliver to this system.
 *
 *
 */
public abstract class DomainHarvestReport implements Serializable {

    /** Datastructure holding the domain-information contained in one
     *  harvest.
     */
    protected final Map<String, DomainStats> domainstats =
            new HashMap<String, DomainStats>();

    /**
     * Default constructor that does nothing.
     * The real construction is supposed to be done
     * in the subclasses by filling out the domainStats map with crawl results.
     */
    public DomainHarvestReport() {
    }

    /**
     * Returns the set of domain names
     * that are contained in hosts-report.txt
     * (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    public final Set<String> getDomainNames() {
        return Collections.unmodifiableSet(domainstats.keySet());
    }

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getObjectCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getObjectCount();
        }
        return null;
    }

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getByteCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getByteCount();
        }
        return null;
    }

    /**
     * Get the StopReason for the given domain.
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain.
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final StopReason getStopReason(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getStopReason();
        }
        return null;
    }
}
