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
package dk.netarkivet.harvester.datamodel;

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Reduced version of the Domain class for presentation purposes. Immutable. (domain configuration list not enforced
 * immutable though).
 *
 * @see Domain
 */
public class SparseDomain {

    /** The domain name. */
    private final String domainName;
    /** List of names of all configurations. */
    private final List<String> domainConfigurationNames;

    /**
     * Create new instance of a sparse domain.
     *
     * @param domainName Domains name.
     * @param domainConfigurationNames List of names of all configurations for domain.
     * @throws ArgumentNotValid if either of the arguments are null or empty.
     */
    public SparseDomain(String domainName, List<String> domainConfigurationNames) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkNotNullOrEmpty(domainConfigurationNames, "domainConfigurationNames");
        this.domainName = domainName;
        this.domainConfigurationNames = domainConfigurationNames;
    }

    /**
     * Gets the name of this domain.
     *
     * @return the name of this domain
     */
    public String getName() {
        return domainName;
    }

    /**
     * Gets the names of configurations in this domain.
     *
     * @return the names of all configurations for this domain.
     */
    public Iterable<String> getDomainConfigurationNames() {
        return domainConfigurationNames;
    }

}
