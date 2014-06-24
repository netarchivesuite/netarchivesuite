package dk.netarkivet.harvester.datamodel;

import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Reduced version of the Domain class for presentation purposes. Immutable.
 * (domain configuration list not enforced immutable though).
 *
 * @see Domain
 *
 */
public class SparseDomain {
    /**
     * The domain name.
     */
    private final String domainName;
    /**
     * List of names of all configurations.
     */
    private final List<String> domainConfigurationNames;

    /**
     * Create new instance of a sparse domain.
     *
     * @param domainName               Domains name.
     * @param domainConfigurationNames List of names of all configurations for
     *                                 domain.
     * @throws ArgumentNotValid if either of the arguments are null or empty.
     */
    public SparseDomain(String domainName,
                        List<String> domainConfigurationNames) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkNotNullOrEmpty(domainConfigurationNames,
                                             "domainConfigurationNames");
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
