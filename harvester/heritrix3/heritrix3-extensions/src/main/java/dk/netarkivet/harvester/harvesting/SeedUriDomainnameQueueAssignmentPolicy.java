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
public class SeedUriDomainnameQueueAssignmentPolicy
        extends HostnameQueueAssignmentPolicy {
    
    /** A key used for the cases when we can't figure out the URI.
     *  This is taken from parent, where it has private access.  Parent returns
     *  this on things like about:blank.
     */
    static final String DEFAULT_CLASS_KEY = "default...";

    private Log log
            = LogFactory.getLog(getClass());

    /** Return a key for queue names based on domain names (last two parts of
     * host name) or IP address.  They key may include a #<portnr> at the end.
     *
     * @param cauri A potential URI.
     * @return a class key (really an arbitrary string), one of <domainOrIP>,
     * <domainOrIP>#<port>, or "default...".
     * @see HostnameQueueAssignmentPolicy#getClassKey(CrawlURI)
     */
     public String getClassKey(CrawlURI cauri) {
        String candidate;
        
        boolean ignoreSourceSeed =
                cauri != null &&
        		cauri.getCanonicalString().startsWith("dns");
        try {
            // Since getClassKey has no contract, we must encapsulate it from
            // errors.
            candidate = super.getClassKey(cauri);
        } catch (NullPointerException e) {
            log.debug("Heritrix broke getting class key candidate for "
                      + cauri);
            candidate = DEFAULT_CLASS_KEY;
        }
        
        String sourceSeedCandidate = null;
        if (!ignoreSourceSeed) {
            sourceSeedCandidate = getCandidateFromSource(cauri);
        }
        
        if (sourceSeedCandidate != null) {
            return sourceSeedCandidate;
        } else {
            // If sourceSeedCandidates are disabled, use the old method:
        
            String[] hostnameandportnr = candidate.split("#");
            if (hostnameandportnr.length == 0 || hostnameandportnr.length > 2) {
                return candidate;
            }
        
            String domainName = DomainUtils.domainNameFromHostname(hostnameandportnr[0]);
            if (domainName == null) { // Not valid according to our rules
                log.debug("Illegal class key candidate '" + candidate
                      + "' for '" + cauri + "'");
                return candidate;
            }
            return domainName;
        }
    }

     /**
      * Find a candidate from the source.
      * @param cauri A potential URI
      * @return a candidate from the source or null if none found
      */
    private String getCandidateFromSource(CrawlURI cauri) {
        String sourceCandidate = null;  
        try {
        	sourceCandidate = cauri.getSourceTag(); 
        } catch (NoSuchElementException e) {
            log.warn("source-tag-seeds not set in Heritrix template!");
            return null;
        }
         
        String hostname = null;
        try {
             hostname = UURIFactory.getInstance(sourceCandidate).getHost();
        } catch (URIException e) {
            log.warn("Hostname could not be extracted from sourceCandidate: " 
                    + sourceCandidate);
            return null;
        }
        return DomainUtils.domainNameFromHostname(hostname);
    }
}
