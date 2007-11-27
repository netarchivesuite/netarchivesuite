/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

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
 */
public class HeritrixTemplate {
    /** the dom4j Document hiding behind this instance of HeritrixTemplate. */
    private Document template;

    /** has this HeritrixTemplate been verified. */
    private boolean verified;

    /** Xpath needed by Job.editOrderXML_maxObjectsPerDomain(). */
    public static final String QUEUE_TOTAL_BUDGET_XPATH =
        "/crawl-order/controller"
        + "/newObject[@name='frontier']"
        + "/long[@name='queue-total-budget']";
    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public static final String GROUP_MAX_ALL_KB_XPATH =
        "/crawl-order/controller/map[@name='pre-fetch-processors']"
        + "/newObject[@name='QuotaEnforcer']"
        + "/long[@name='group-max-all-kb']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String EXCLUDE_FILTER_MAP_XPATH =
        "/crawl-order/controller/newObject"
        + "/newObject[@name='exclude-filter']"
        + "/map[@name='filters']";
    /** Xpath checked by Heritrix for correct user-agent field in requests. */
    public static final String HERITRIX_USER_AGENT_XPATH =
            "/crawl-order/controller/map[@name='http-headers']"
            + "/string[@name='user-agent']";
    /** Xpath checked by Heritrix for correct mail address. */
    public static final String HERITRIX_FROM_XPATH =
            "/crawl-order/controller/map[@name='http-headers']/"
            + "string[@name='from']";

    /** Map from required xpaths to a regular expression describing
     * legal content for the path text. */
    private static final Map<String, Pattern> requiredXpaths
            = new HashMap<String, Pattern>();
    static {
        requiredXpaths.put(QUEUE_TOTAL_BUDGET_XPATH,
                           Pattern.compile("\\s*-?[0-9]+\\s*"));
        requiredXpaths.put(GROUP_MAX_ALL_KB_XPATH,
                           Pattern.compile("\\s*-?[0-9]+\\s*"));
        requiredXpaths.put(EXCLUDE_FILTER_MAP_XPATH,
                           Pattern.compile(".*", Pattern.DOTALL));
        // Copied from org.archive.crawler.datamodel.CrawlOrder because
        // it's private in there:(
        requiredXpaths.put(HERITRIX_USER_AGENT_XPATH,
                           Pattern.compile("\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*",
                                           Pattern.DOTALL));
        requiredXpaths.put(HERITRIX_FROM_XPATH,
                           Pattern.compile("\\S+@\\S+\\.\\S+"));
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
