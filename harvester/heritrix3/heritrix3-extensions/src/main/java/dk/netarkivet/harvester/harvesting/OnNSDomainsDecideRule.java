/*$Id: OnNSDomainsDecideRule.java 2687 2013-05-03 16:38:47Z svc $
* $Revision: 2687 $
* $Date: 2013-05-03 18:38:47 +0200 (Fri, 03 May 2013) $
* $Author: svc $
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.url.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.surt.SurtPrefixedDecideRule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.SurtPrefixSet;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class that re-creates the SurtPrefixSet to include only domain names
 * according to the domain definition of NetarchiveSuite.
 * The NetarchiveSuite can't use the
 * org.archive.crawler.deciderules.OnDomainsDecideRule because
 * it uses a different domain definition.  
 */
public class OnNSDomainsDecideRule extends SurtPrefixedDecideRule {

    /** This is what SurtPrefixSet.prefixFromPlain returns for
     *  a non valid URI. */
    public static final String NON_VALID_DOMAIN = "http://(http,)";
    
    /** Pattern that matches the first part of SURT - until ?? */
    public static final Pattern SURT_FIRSTPART_PATTERN
        = Pattern.compile("http\\://\\([^\\)]*");
    /** 
     * Constructor for the class OnNSDomainsDecideRule.
     * Makes the configured decision 
     * for any URI which is inside one of the domains in the 
     * configured set of domains - according to the domain 
     * definition of the NetarchiveSuite system.
     * Giving that e.g. sports.tv2.dk will resolve to tv2.dk
     * but www.bbc.co.uk will resolve to bbc.co.uk"
     *  
     */
    public OnNSDomainsDecideRule(){
        super();
    }
    
    /**
     * We override the default readPrefixes, because we want to
     * make our prefixes.
     */
    protected void readPrefixes() {
        buildSurtPrefixSet();
        myBuildSurtPrefixSet();
        dumpSurtPrefixSet();
    }

    /**
     * Method that rebuilds the SurtPrefixSet to include only
     * topmost domains - according to the domain definition
     * in NetarchiveSuite.
     * This is only done once, during the startup phase?
     */
    protected void myBuildSurtPrefixSet() {
        //make copy of original SurtPrefixSet to loop
        SurtPrefixSet newSurtPrefixes = (SurtPrefixSet) surtPrefixes.clone();
        //pattern that matches first part of SURT
        
        //loop all original SURTs
        for (String s : newSurtPrefixes) {
            Matcher m = SURT_FIRSTPART_PATTERN.matcher(s);
            if (m.find()) {
                //cut off http:// (https:// are converted by heritrix classes)
                String hostpart = m.group().substring(8);
                //split in hostname/domainname/TLD parts
                String[] parts = hostpart.split(",");
                StringBuilder domnameBuilder = new StringBuilder();
                //loop through parts in reverse order - add '.'
                //(not after last part)
                for (int j = parts.length - 1; j >= 0; j--) {
                    domnameBuilder.append(parts[j]);
                    if (j != 0) {
                        domnameBuilder.append(".");
                    }
                }
                //add the new domain name to surtPrefixes
                //since this is always shorter SURTs than the originals
                //they will automatically
                //override longer ones (built in SURTs logic)
                surtPrefixes.add(prefixFrom(domnameBuilder.toString()));
            }
        }
    }

    /**
     * Generate the SURT prefix that matches the domain definition
     * of NetarchiveSuite.
     * @param uri URL to convert to SURT
     * @return String with SURT that matches the domain definition
     * of NetarchiveSuite
     */
    protected String prefixFrom(String uri) {
        uri = ArchiveUtils.addImpliedHttpIfNecessary(uri);
        //TODO is this correct now ? 
        return SurtPrefixSet.prefixFromPlainForceHttp(convertToDomain(uri));
    }

    /**
     * Convert a URI to its domain.
     * @param uri URL to convert to Top most domain-name according to
     * NetarchiveSuite definition
     * @return Domain name
     */
    public static String convertToDomain(String uri) {
        ArgumentNotValid.checkNotNullOrEmpty(uri, "String uri");
        DomainnameQueueAssignmentPolicy policy
                = new DomainnameQueueAssignmentPolicy();

        UURI uuri = null;
        try {
            uuri = UURIFactory.getInstance(uri);
        } catch (URIException e) {
            e.printStackTrace();
         // allow to continue with original string uri  
         // FIXME/TODO 
         // AD "allow to continue with original string uri"
         // We cannot do that any more, as the argument to getClassKey is now CrawlURI, except for
         // the string
                         
        }
        try {          
            return policy.getClassKey(new CrawlURI(uuri));
        } catch (Throwable e) {
            // illegal URI - return a SURT that will not match any real URIs
            return NON_VALID_DOMAIN;
        }
    }
}
