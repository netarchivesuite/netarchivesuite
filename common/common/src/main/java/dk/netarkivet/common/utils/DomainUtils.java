/*
* File:     $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;

/**
 * Utilities for working with domain names.
 */
public class DomainUtils {
    private static final Log log = LogFactory.getLog(DomainUtils.class);

    /** Valid characters in a domain name, according to RFC3490 */
    public static final String DOMAINNAME_CHAR_REGEX_STRING
            = "[^\\0000-,.-/:-@\\[-`{-\\0177]+";

    /** A string for a regexp recognising a TLD read from settings */
    public static final String TLD_REGEX_STRING = "\\.(" + StringUtils.conjoin(
            "|", readTlds()) + ")";

    /**
     * Regexp for matching a valid domain, that is a single domainnamepart
     * followed by a TLD from settings, or an IP address.
     * */
    public static final Pattern VALID_DOMAIN_MATCHER
            = Pattern.compile("^(" + Constants.IP_REGEX_STRING
                              + "|" + DOMAINNAME_CHAR_REGEX_STRING + "+"
                              + TLD_REGEX_STRING
                              + ")$");

    /**
     * A regular expression matching hostnames, and remembering the hostname
     * in group 1 and the domain in group 2.
     */
    private static final Pattern HOSTNAME_REGEX = Pattern.compile(
            "^(|.*?\\.)(" + DOMAINNAME_CHAR_REGEX_STRING + "+"
            + TLD_REGEX_STRING + ")");

    /** Utillity class, do not initialise. */
    private DomainUtils() {}

    /** Helper method for reading TLDs from settings.
     * Will read all settings, validate them as legal TLDs and
     * warn and ignore them if any are invalid.
     * Settings may be with or without prefix "." */
    private static List<String> readTlds() {
        List<String> tlds = new ArrayList<String>();
        for (String tld : Settings.getAll(CommonSettings.TLDS)) {
            if (tld.startsWith(".")) {
                tld = tld.substring(1);
            }
            if (!tld.matches(DOMAINNAME_CHAR_REGEX_STRING
                             + "(" + DOMAINNAME_CHAR_REGEX_STRING + "|\\.)*")) {
                log.warn("Invalid tld '" + tld + "', ignoring");
                continue;
            }
            tlds.add(Pattern.quote(tld));
        }
        return tlds;
    }

    /** Method mathcing valid domains. A valid domain is an IP address or a
     * domainname part followed by a TLD as defined in settings. */
    public static boolean isValidDomainName(String domainName) {
        return VALID_DOMAIN_MATCHER.matcher(domainName).matches();
    }

    /** Return a domain name.
     * A domain name is defined as either an IP address if the given host is an
     * IP address, or a postfix of the given host name containing one
     * hostnamepart and a TLD as defined in settings.
     *
     * E.g. if '.dk' and 'co.uk' are valid TLDs, www.netarchive.dk will be
     * become netarchive.dk and news.bbc.co.uk will be come bbc.co.uk
     *
     * @param hostname A hostname or IP address.
     * @return A domain name (foo.bar) or IP address, or null if no valid
     * hostname could be obtained from the given hostname.  If non-null,
     * the return value is guaranteed to be a valid hostname as determined
     * by isValidDomainName().
     */
    public static String domainNameFromHostname(String hostname) {
        String result = hostname;
        // IP addresses are kept as-is, others are trimmed down.
        if (!Constants.IP_KEY_REGEXP.matcher(hostname)
                .matches()) {
            Matcher matcher = HOSTNAME_REGEX.matcher(hostname);
            if (matcher.matches()) {
                result = matcher.group(2);
            }
        }
        if (isValidDomainName(result)) {
            return result;
        }
        return null;
    }
}
