/*$Id: IngestableFilesTester.java 11 2007-07-24 10:11:24Z kfc $
* $Revision: 11 $
* $Date: 2007-07-24 12:11:24 +0200 (ti, 24 jul 2007) $
* $Author: kfc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.deciderules.SurtPrefixedDecideRule;
import org.archive.net.UURIFactory;
import org.archive.util.ArchiveUtils;
import org.archive.util.SurtPrefixSet;

/**
 * Class that re-creates the SurtPrifixSet to include only domain names
 * according to the domain definition of NetarchiveSuite
 */
public class OnNSDomainsDecideRule extends SurtPrefixedDecideRule {

    //this is what SurtPrefixSet.prefixFromPlain returns for a non valid URI
    public final static String NON_VALID_DOMAIN="http://(http,)";

    public OnNSDomainsDecideRule(String s) {
        super(s);
        setDescription(
                "OnNSDomainsDecideRule. Makes the configured decision " +
                        "for any URI which is inside one of the domains in the " +
                        "configured set of domains - according to the domain " +
                        "definition of the NetarchiveSuite system. " +
                        "Giving that e.g. sports.tv2.dk will resolve to tv2.dk" +
                        "but ??????????");
    }

    protected void readPrefixes() {
        buildSurtPrefixSet();
        myBuildSurtPrefixSet();
        dumpSurtPrefixSet();
    }

    /**
     * Method that rebuilds the SurtPrefixSet to include only
     * Top most domains - according to the domain definition in NetarchiveSuite
     */

    protected void myBuildSurtPrefixSet() {
        //make copy of original SurtPrefixSet to loop
        SurtPrefixSet newSurtPrefixes = (SurtPrefixSet) surtPrefixes.clone();
        //pattern that matches fist part of SURT - until
        Pattern p = Pattern.compile("http\\://\\([^\\)]*");
        //loop all original SURTs
        for (String s : newSurtPrefixes) {
            Matcher m = p.matcher(s);
            if (m.find()) {
                //cut off http:// (https:// are converted by heritrix classes)
                String hostpart = m.group().substring(8);
                //split in hostname/domainname/TLD parts
                String[] parts = hostpart.split(",");
                String domname = "";
                //loop through parts in reverse order - add '.' (not after last part)
                for (int j = parts.length - 1; j >= 0; j--) {
                    domname += parts[j];
                    if (j != 0) domname += ".";
                }
                //add the new domain name to surtPrefixes
                //since this is always shorter SURTs than the originals they will automatically
                //override longer ones (built in SURTs logic)
                surtPrefixes.add(prefixFrom(domname));
            }
        }
    }

    /**
     * Generate the SURT prefix that matches the domain definition of NetarchiveSuite
     * @param uri URL to convert to SURT
     * @return String with SURT that matches the domain definition of NetarchiveSuite
     */
    protected String prefixFrom(String uri) {
        uri = ArchiveUtils.addImpliedHttpIfNecessary(uri);
        return SurtPrefixSet.prefixFromPlain(convertToDomain(uri));
    }

    /**
     * Convert a URI to its domain.
     * @param uri URL to convert to Top most domain-name according to NetarchiveSuite definition
     * @return Domain name
     */
    public static String convertToDomain(String uri) {
        //ArgumentNotValid.checkNotNullOrEmpty(uri, "String uri");
        DomainnameQueueAssignmentPolicy policy
                = new DomainnameQueueAssignmentPolicy();
        String u = uri;
        try {
            u = UURIFactory.getInstance(uri).toString();
        } catch (URIException e) {
            e.printStackTrace();
            // allow to continue with original string uri
        }
        try {
            return policy.getClassKey(null, CandidateURI.fromString(u.toString()));
        } catch (URIException e) {
            // illegal URI - return a SURT that will not match any real URIs
            return NON_VALID_DOMAIN;
        }
    }
}
