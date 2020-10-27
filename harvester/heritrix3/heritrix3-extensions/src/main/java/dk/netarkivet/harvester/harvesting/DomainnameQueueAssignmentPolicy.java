/* File:        $Id: DomainnameQueueAssignmentPolicy.java 2687 2013-05-03 16:38:47Z svc $
 * Revision:    $Revision: 2687 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-05-03 18:38:47 +0200 (Fri, 03 May 2013) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2018 The Royal Danish Library,
 * the National Library of France and the Austrian
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;
import org.archive.net.UURI;

import dk.netarkivet.common.utils.DomainUtils;

/**
 * Using the domain as the queue-name.
 * The domain is defined as the last two names in the entire hostname or
 * the entirety of an IP address.
 * x.y.z -> y.z
 * y.z -> y.z
 * nn.nn.nn.nn -> nn.nn.nn.nn
 *  
 */
public class DomainnameQueueAssignmentPolicy extends HostnameQueueAssignmentPolicy {

	/** A key used for the cases when we can't figure out the URI.
     *  This is taken from parent, where it has private access.  Parent returns
     *  this on things like about:blank.
     */
    static final String DEFAULT_CLASS_KEY = "default...";

    private Log log = LogFactory.getLog(getClass());

    /**
     * Return a key for queue names based on domain names (last two parts of
     * host name) or IP address.  They key may include a #<portnr> at the end.
     *
     * @param basis A potential URI.
     * @return a class key (really an arbitrary string), one of <domainOrIP>,
     * <domainOrIP>#<port>, or "default...".
     * @see HostnameQueueAssignmentPolicy#getClassKey(org.archive.modules.CrawlURI)
     */
    @Override
    protected String getCoreKey(UURI basis) {
        String candidate; 
        try {
            candidate = super.getCoreKey(basis);
        } catch (NullPointerException e) {
            log.debug("Heritrix broke getting class key candidate for " + basis);
            candidate = DEFAULT_CLASS_KEY;
        }
        if (candidate == null) { //FIXME the candidate should not be null with dns: schema
        	// is this a dns url?
        	if (basis.getScheme().equalsIgnoreCase("dns")) {
        		log.warn("The url is a dns-url '" + basis + "'. Returning: " +  DEFAULT_CLASS_KEY);
        	} else {
        		log.warn("The url is not a dns-url '" + basis + "'. Returning: " +  DEFAULT_CLASS_KEY);
        	}
        	return DEFAULT_CLASS_KEY;
        }
        
        String[] hostnameandportnr = candidate.split("#");
        
        if (hostnameandportnr.length == 0 || hostnameandportnr.length > 2) {
            return candidate;
        }
        String domainName = DomainUtils.domainNameFromHostname(hostnameandportnr[0]);
        if (domainName == null) { // Not valid according to our rules
            log.debug("Illegal class key candidate '" + candidate + "' for '" + basis + "'");
            return candidate;
        }
        return domainName;
    }
 
}
