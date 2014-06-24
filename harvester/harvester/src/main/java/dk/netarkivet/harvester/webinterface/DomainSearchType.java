package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible ways to search for domains.
 *
 */
public enum DomainSearchType {

        /** search in the crawlertraps associated with a domain. */
        CRAWLERTRAPS,
        /** search the name associated with a domain. */
        NAME,
        /** search the comments associated with a domain. */
        COMMENTS;
                
        /** Helper method that tries to convert a string to
         * a DomainSearchType.
         *
         * @param type a DomainSearchType as string
         * @return the DomainSearchType related to a string
         * @throws ArgumentNotValid
        */
       public static DomainSearchType parse(String type) {
            for (DomainSearchType s : values()) {
                if (s.name().equalsIgnoreCase(type)) {
                    return s;
                }
            }
            throw new ArgumentNotValid("Invalid Domain Search Type '" + type + "'");
        }
 
        
       /**
        * Return the localized key related to this value.
        *
        * @return The localized key for this value.
        */
       public String getLocalizedKey() {
           switch (this) {
               case NAME:
                   return "domain.search.name";
               case CRAWLERTRAPS:
                   return "domain.search.crawlertraps";
               case COMMENTS:
                   return "domain.search.comments";
               default:
                   throw new ArgumentNotValid("Invalid Domain Search Type '" 
                           + this + "'");
           }
       }
}
