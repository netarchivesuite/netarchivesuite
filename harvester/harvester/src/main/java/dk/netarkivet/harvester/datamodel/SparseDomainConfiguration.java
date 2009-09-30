/* File:        $Id$
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
package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Sparse version for DomainConfiguration class. To be used for GUI purposes
 * only. Immutable.
 * @see dk.netarkivet.harvester.datamodel.DomainConfiguration
 *
 */
public class SparseDomainConfiguration {
    /** Name of domain this is a configuration for. */
    private final String domainName;
    /** Name of this configuration. */
    private final String configurationName;

    /** Create a sparse configuration.
     *
     * @param domainName Name of domain this is a configuration for.
     * @param configurationName Name of configuration.
     * @throws ArgumentNotValid if either argument is null or empty.
     */
    SparseDomainConfiguration(String domainName,
                              String configurationName){
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkNotNullOrEmpty(configurationName,
                                             "configurationName");
        this.domainName = domainName;
        this.configurationName = configurationName;
    }

    /**
     * Get domain name.
     * @return The domain name.
     */
    public String getDomainName(){
        return this.domainName;
    }

    /**
     * Get configuration name.
     * @return The configuration name.
     */
    public String getConfigurationName(){
        return this.configurationName;
    }
}
