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
    public final static String QueueTotalBudgetXpath =
        "/crawl-order/controller"
        + "/newObject[@name='frontier']"
        + "/long[@name='queue-total-budget']";
    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public final static String GroupMaxAllKbXpath =
        "/crawl-order/controller/map[@name='pre-fetch-processors']"
        + "/newObject[@name='QuotaEnforcer']"
        + "/long[@name='group-max-all-kb']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public final static String ExcludeFilterMapXpath =
        "/crawl-order/controller/newObject"
        + "/newObject[@name='exclude-filter']"
        + "/map[@name='filters']";
    /** String array with required xpaths. */
    private final String[] requiredXpaths = new String[] {
            QueueTotalBudgetXpath, GroupMaxAllKbXpath, ExcludeFilterMapXpath };
    
    
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
            for (String required: requiredXpaths) {
                ArgumentNotValid.checkTrue(
                        doc.selectSingleNode(required) != null,
                        "Document does not contain the required xpath: "
                        + required);
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
