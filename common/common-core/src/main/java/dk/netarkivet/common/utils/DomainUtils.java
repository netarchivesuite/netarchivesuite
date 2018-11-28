/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils;

import java.util.regex.Matcher;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Utilities for working with domain names.
 */
public final class DomainUtils {

    /** Valid characters in a domain name, according to RFC3490. */
    public static final String DOMAINNAME_CHAR_REGEX_STRING = "[^\\0000-,.-/:-@\\[-`{-\\0177]+";

    /** Utility class, do not initialise. */
    private DomainUtils() {
    }
    
    /**
     * Check if a given domainName is valid domain. A valid domain is an IP address or a domain name part followed by a
     * TLD as defined in settings.
     *
     * @param domainName A name of a domain (netarkivet.dk)
     * @return true if domain is valid; otherwise it returns false.
     */
    public static boolean isValidDomainName(String domainName) {
        ArgumentNotValid.checkNotNull(domainName, "String domainName");
        return TLD.getInstance().getValidDomainMatcher().matcher(domainName).matches();      
    }

    /**
     * Return a domain name. A domain name is defined as either an IP address if the given host is an IP address, or a
     * postfix of the given host name containing one hostname part and a TLD as defined in settings.
     * <p>
     * E.g. if '.dk' and 'co.uk' are valid TLDs, www.netarchive.dk will be become netarchive.dk and news.bbc.co.uk will
     * be come bbc.co.uk
     *
     * @param hostname A hostname or IP address. Null hostname is not allowed
     * @return A domain name (foo.bar) or IP address, or null if no valid domain could be obtained from the given
     * hostname. If non-null, the return value is guaranteed to be a valid domain as determined by isValidDomainName().
     */
    public static String domainNameFromHostname(String hostname) {
        ArgumentNotValid.checkNotNull(hostname, "String hostname");
        String result = hostname;
        // IP addresses are kept as-is, others are trimmed down.
        if (!Constants.IP_KEY_REGEXP.matcher(hostname).matches()) {
            Matcher matcher = TLD.getInstance().getHostnamePattern().matcher(hostname);
            if (matcher.matches()) {
                result = matcher.group(2);
            }
        }
        if (isValidDomainName(result)) {
            return result;
        }
        return null;
    }

    /**
     * Reduce a hostname to a more readable form.
     *
     * @param hostname A host name, should not be null.
     * @return The same host name with all domain parts stripped off.
     * @throws ArgumentNotValid if argument isn't valid.
     */
    public static String reduceHostname(String hostname) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(hostname, "String hostName");
        String[] split = hostname.split("\\.", 2);
        return split[0];
    }
}
