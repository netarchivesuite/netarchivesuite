/* File:        $Id: SeedUriDomainnameQueueAssignmentPolicy.java 2688 2013-05-05 18:58:18Z svc $
 * Revision:    $Revision: 2688 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-05-05 20:58:18 +0200 (Sun, 05 May 2013) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

import java.util.NoSuchElementException;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;
import org.archive.modules.CrawlURI;
import org.archive.net.UURIFactory;

import dk.netarkivet.common.utils.DomainUtils;

/**
 * This is a modified version of the {@link DomainnameQueueAssignmentPolicy}
 * where domainname returned is the domainname of the candidateURI
 * except where the the SeedURI belongs to a different domain. 
 * 
 * 
 * Using the domain as the queue-name.
 * The domain is defined as the last two names in the entire hostname or
 * the entirety of an IP address.
 * x.y.z -> y.z
 * y.z -> y.z
 * nn.nn.nn.nn -> nn.nn.nn.nn
 * 
 */
public class SeedUriDomainnameQueueAssignmentPolicy extends HostnameQueueAssignmentPolicy {
    
    /** A key used for the cases when we can't figure out the URI.
     *  This is taken from parent, where it has private access.  Parent returns
     *  this on things like about:blank.
     */
    static final String DEFAULT_CLASS_KEY = "default...";

    private Log log = LogFactory.getLog(getClass());


    /**
     * The logic is as follows:
     * We get try to get the queue-name as the domain-name of the seed.
     * If that fails, or if the uri is a dns entry, we use the "old" logic which is
     * to take the key from the superclass (in the form host#port or just host) and extract
     * a domain-name from that. If all that fails, we fall back to a default value,
     *
     * @param cauri The crawl URI from which to find the key.
     * @return the key value
     */
    public String getClassKey(CrawlURI cauri) {
        log.debug("Finding classKey for cauri: " + cauri);
        String key = null;
        boolean ignoreSourceSeed = (cauri != null && cauri.getCanonicalString().startsWith("dns"));
        if (!ignoreSourceSeed) {
            key = getKeyFromSeed(cauri);
        }
        if (key == null) {
            key = getKeyFromUriHostname(cauri);
        }
        if (key != null) {
            return key;
        } else {
            return DEFAULT_CLASS_KEY;
        }
    }

    private String getKeyFromUriHostname(CrawlURI cauri) {
        String key = null;
        try {
            key = super.getClassKey(cauri);
        }  catch (NullPointerException e) {
            log.debug("Heritrix broke getting class key candidate for " + cauri);
        }
        if (key != null) {
            String[] hostnameandportnr = key.split("#");
            if (hostnameandportnr.length == 1 || hostnameandportnr.length == 2) {
                key = DomainUtils.domainNameFromHostname(hostnameandportnr[0]);
            } else {
                log.debug("Illegal class key candidate from superclass: '" + key + "' for '" + cauri + "'");
                key = null;
            }
        }
        return key;
    }

    private String getKeyFromSeed(CrawlURI cauri) {
        String key = null;
        try {
            key = DomainUtils.domainNameFromHostname(UURIFactory.getInstance(cauri.getSourceTag()).getHost());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

}
