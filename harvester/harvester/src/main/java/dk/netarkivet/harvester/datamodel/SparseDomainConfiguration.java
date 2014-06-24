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
    public SparseDomainConfiguration(String domainName,
                              String configurationName){
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        ArgumentNotValid.checkNotNullOrEmpty(configurationName,
                                             "configurationName");
        this.domainName = domainName;
        this.configurationName = configurationName;
    }
    
    /** Alternate constructor taking a DomainConfiguration as input.
     * 
     * @param dc a DomainConfiguration 
     */
    public SparseDomainConfiguration(DomainConfiguration dc) {
        ArgumentNotValid.checkNotNull(dc, "DomainConfiguration dc");
        this.domainName = dc.getDomainName();
        this.configurationName = dc.getName();
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
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseDomainConfiguration)) return false;

        final SparseDomainConfiguration configKey = (SparseDomainConfiguration) o;

        if (!configurationName.equals(configKey.getConfigurationName())) return false;
        if (!domainName.equals(configKey.getDomainName())) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = domainName.hashCode();
        result = 29 * result + configurationName.hashCode();
        return result;
    }
    
    
}
