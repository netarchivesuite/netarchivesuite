/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

import org.archive.crawler.deciderules.DecidingScope;
import org.dom4j.Document;
import org.dom4j.Node;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class encapsulating the Heritrix order.xml. 
 * Enables verification that dom4j Document obey the constraints
 * required by our software, specifically the Job class.
 * 
 * The class assumes the type of order.xml used in configuring Heritrix
 * version 1.10+.
 * Information about the Heritrix crawler, and its processes and modules
 * can be found in the Heritrix developer and user manuals found on
 * <a href="http://crawler.archive.org">http://crawler.archive.org<a/>
 */
public class HeritrixTemplate {
    /** the dom4j Document hiding behind this instance of HeritrixTemplate. */
    private Document template;

    /** has this HeritrixTemplate been verified. */
    private boolean verified;

    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public static final String GROUP_MAX_ALL_KB_XPATH =
        "/crawl-order/controller/map[@name='pre-fetch-processors']"
        + "/newObject[@name='QuotaEnforcer']"
        + "/long[@name='group-max-all-kb']";
    /** Xpath needed by Job.editOrderXML_maxObjectsPerDomain(). */
    public static final String GROUP_MAX_FETCH_SUCCESS_XPATH =
        "/crawl-order/controller/map[@name='pre-fetch-processors']"
        + "/newObject[@name='QuotaEnforcer']"
        + "/long[@name='group-max-fetch-successes']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String DECIDERULES_MAP_XPATH = 
        "/crawl-order/controller/newObject"
        + "/newObject[@name='decide-rules']"
        + "/map[@name='rules']";
    
    /** Xpath checked by Heritrix for correct user-agent field in requests. */
    public static final String HERITRIX_USER_AGENT_XPATH =
            "/crawl-order/controller/map[@name='http-headers']"
            + "/string[@name='user-agent']";
    /** Xpath checked by Heritrix for correct mail address. */
    public static final String HERITRIX_FROM_XPATH =
            "/crawl-order/controller/map[@name='http-headers']/"
            + "string[@name='from']";
    /** Xpath to check, that all templates use the DecidingScope. */
    public static final String DECIDINGSCOPE_XPATH =
            "/crawl-order/controller/newObject[@name='scope']"
            + "[@class='" + DecidingScope.class.getName()
            + "']";
    /**
     * Xpath for the deduplicator node in order.xml documents.
     */
    public static final String DEDUPLICATOR_XPATH =
            "/crawl-order/controller/map[@name='write-processors']"
            + "/newObject[@name='DeDuplicator']";

    
    
    
    /** Xpath to check, that all templates use the same archiver path,
     * {@link dk.netarkivet.common.Constants#ARCDIRECTORY_NAME}.
     * The archive path tells Heritrix to which directory it shall write
     * its arc files.
     */
    public static final String ARCHIVER_PATH_XPATH =
        "/crawl-order/controller/map[@name='write-processors']/"
        + "newObject[@name='Archiver']/stringList[@name='path']/string";
    
    /** Map from required xpaths to a regular expression describing
     * legal content for the path text. */
    private static final Map<String, Pattern> requiredXpaths
            = new HashMap<String, Pattern>();
    
    /** A regular expression that matches a whole number, possibly negative,
     * and with optional whitespace around it.
     */
    private static final String WHOLE_NUMBER_REGEXP = "\\s*-?[0-9]+\\s*";
    /** A regular expression that matches everything.  Except newlines,
     * unless DOTALL is given to Pattern.compile(). */
    private static final String EVERYTHING_REGEXP = ".*";

    // These two regexps are copied from
    // org.archive.crawler.datamodel.CrawlOrder because they're private there.

    /** A regular expression that matches Heritrix' specs for the user-agent
     * field in order.xml.  It should be used with DOTALL.  An example match is
     * "Org (ourCrawler, see +http://org.org/aPage for details) harvest".
     */
    private static final String USER_AGENT_REGEXP
            = "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";
    /** A regular expression that matches Heritrix' specs for the from
     * field.  This should be a valid email address.
     */
    private static final String FROM_REGEXP = "\\S+@\\S+\\.\\S+";

    static {
        requiredXpaths.put(GROUP_MAX_FETCH_SUCCESS_XPATH,
                           Pattern.compile(WHOLE_NUMBER_REGEXP));
        requiredXpaths.put(GROUP_MAX_ALL_KB_XPATH,
                           Pattern.compile(WHOLE_NUMBER_REGEXP));
        
        //Required that we use DecidingScope
        requiredXpaths.put(DECIDINGSCOPE_XPATH,
                            Pattern.compile(EVERYTHING_REGEXP));   
        
        //Required that we have a rules map used to add crawlertraps
        requiredXpaths.put(DECIDERULES_MAP_XPATH,
                           Pattern.compile(EVERYTHING_REGEXP, Pattern.DOTALL));
 
        requiredXpaths.put(HERITRIX_USER_AGENT_XPATH,
                           Pattern.compile(USER_AGENT_REGEXP, Pattern.DOTALL));
        requiredXpaths.put(HERITRIX_FROM_XPATH, Pattern.compile(FROM_REGEXP));
      
        //Required that Heritrix write its arcfiles to the correct dir
        // relative to the crawldir. This dir is defined by the constant: 
        //dk.netarkivet.common.Constants.ARCDIRECTORY_NAME.
        requiredXpaths.put(ARCHIVER_PATH_XPATH, Pattern.compile(
                dk.netarkivet.common.Constants.ARCDIRECTORY_NAME));
    }

    /** Constructor for HeritrixTemplate class.
     * @param doc the order.xml
     * @param verify If true, verifies if the given dom4j Document contains
     * the elements required by our software.
     * @throws ArgumentNotValid if doc is null, or verify is true and doc does
     * not obey the constraints required by our software.
     */
    public HeritrixTemplate(Document doc, boolean verify) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        if (verify) {
            for (Map.Entry<String, Pattern> required: requiredXpaths.entrySet()) {
                final String xpath = required.getKey();
                Node node = doc.selectSingleNode(xpath);
                ArgumentNotValid.checkTrue(node != null,
                        "Template error: Missing node: "
                        + xpath);

                final Pattern pattern = required.getValue();
                final Matcher matcher = pattern.matcher(node.getText().trim());

                ArgumentNotValid.checkTrue(
                        matcher.matches(),
                        "Template error: Value '" + node.getText()
                        + "' of node '" + xpath
                        + "' does not match required regexp '"
                        + pattern + "'");
            }
            verified = true;
        }
        this.template = (Document) doc.clone();
    }

    /**
     * Alternate constructor, which always verifies the given document.
     * @param doc
     */
    public HeritrixTemplate(Document doc) {
        this(doc, true);
    }

    /**
     * return the template.
     * @return the template
     */
    public Document getTemplate() {
        return (Document) template.clone();
    }

    /**
     * Has Template been verified?
     * @return true, if verified on construction, otherwise false
     */
     public boolean isVerified() {
         return verified;
     }

     /**
      * Return HeritrixTemplate as XML.
      * @return HeritrixTemplate as XML
      */
     public String getXML() {
         return template.asXML();
     }
}
