/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

/** A class that ensure that the list of configurations contains
 * no duplicates.  It provides an equals that makes configurations equal
 * on <domainname, configname> tuple.
 * Note that DomainConfigurationKey is identical to 
 * SparseDomainConfiguration class.
 */
public class DomainConfigurationKey {
    /** the name of a domain. */
    final String domainName;
    /** the name of a domain configuration. */
    final String configName;
    
    /**
     * Constructing a DomainConfigurationKey from a DomainConfiguration.
     * @param dc A DomainConfiguration argument
     */
    public DomainConfigurationKey(DomainConfiguration dc) {
        this.domainName = dc.getDomain();
        this.configName = dc.getName();
    }
    
    /**
     * Constructing a DomainConfigurationKey from a Domainname and a 
     * configname.
     * @param domainName a domain name
     * @param configName a configuration name
     */
    public DomainConfigurationKey(String domainName, String configName) {
        this.domainName = domainName;
        this.configName = configName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainConfigurationKey)) return false;

        final DomainConfigurationKey configKey = (DomainConfigurationKey) o;

        if (!configName.equals(configKey.configName)) return false;
        if (!domainName.equals(configKey.domainName)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = domainName.hashCode();
        result = 29 * result + configName.hashCode();
        return result;
    }
}
