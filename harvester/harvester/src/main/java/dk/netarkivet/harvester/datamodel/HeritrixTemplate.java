/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.deciderules.MatchesListRegExpDecideRule;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

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
    
    private static Log log = LogFactory.getLog(HeritrixTemplate.class.getName());
    
    /** the dom4j Document hiding behind this instance of HeritrixTemplate. */
    private Document template;

    /** has this HeritrixTemplate been verified. */
    private boolean verified;

    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public static final String QUOTA_ENFORCER_ENABLED_XPATH =
        "/crawl-order/controller/map[@name='pre-fetch-processors']"
        + "/newObject[@name='QuotaEnforcer']"
        + "/boolean[@name='enabled']";;
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
    /** Xpath needed by Job.editOrderXML_maxObjectsPerDomain(). */
    public static final String QUEUE_TOTAL_BUDGET_XPATH =
        "/crawl-order/controller/newObject[@name='frontier']"
        + "/long[@name='queue-total-budget']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String DECIDERULES_MAP_XPATH =
        "/crawl-order/controller/newObject"
        + "/newObject[@name='decide-rules']"
        + "/map[@name='rules']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH =
        "/crawl-order/controller/newObject"
        + "/newObject[@name='decide-rules']"
        + "/map[@name='rules']/newObject[@class="
        + "'org.archive.crawler.deciderules.PrerequisiteAcceptDecideRule']";

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

    /** Xpath to check, that all templates use the same ARC archiver path,
     * {@link dk.netarkivet.common.Constants#ARCDIRECTORY_NAME}.
     * The archive path tells Heritrix to which directory it shall write
     * its arc files.
     */
    public static final String ARC_ARCHIVER_PATH_XPATH =
        "/crawl-order/controller/map[@name='write-processors']/"
        + "newObject[@name='Archiver']/stringList[@name='path']/string";

    /** Xpath to check, that all templates use the same WARC archiver path,
     * {@link dk.netarkivet.common.Constants#WARCDIRECTORY_NAME}.
     * The archive path tells Heritrix to which directory it shall write
     * its arc files.
     */
    public static final String WARC_ARCHIVER_PATH_XPATH =
            "/crawl-order/controller/map[@name='write-processors']/"
            + "newObject[@name='WARCArchiver']/stringList[@name='path']/string";
    
    /** Xpath for the deduplicator index directory node in order.xml 
     * documents. */
    public static final String DEDUPLICATOR_INDEX_LOCATION_XPATH
            = HeritrixTemplate.DEDUPLICATOR_XPATH
              + "/string[@name='index-location']";

    /**
     * Xpath for the boolean telling if the deduplicator is enabled in order.xml
     * documents.
     */
    public static final String DEDUPLICATOR_ENABLED
            = HeritrixTemplate.DEDUPLICATOR_XPATH + "/boolean[@name='enabled']";

    
    /** Xpath for the 'disk-path' in the order.xml . */
    public static final String DISK_PATH_XPATH =
            "//crawl-order/controller"
            + "/string[@name='disk-path']";
    /** Xpath for the arcfile 'prefix' in the order.xml . */
    public static final String ARCHIVEFILE_PREFIX_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject/string[@name='prefix']";
    /** Xpath for the ARCs dir in the order.xml. */
    public static final String ARCSDIR_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject[@name='Archiver']/stringList[@name='path']/string";
    
    private static final String WARCWRITERPROCESSOR_XPATH = 
    "//crawl-order/controller"
    + "/map[@name='write-processors']"
    + "/newObject[@name='WARCArchiver']";
    
    private static final String ARCWRITERPROCESSOR_XPATH = 
    	    "//crawl-order/controller"
    	    + "/map[@name='write-processors']"
    	    + "/newObject[@name='Archiver']";
    
    /** Xpath for the WARCs dir in the order.xml. */
    public static final String WARCSDIR_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/stringList[@name='path']/string";
    
    /** Xpath for the 'seedsfile' in the order.xml. */
    public static final String SEEDS_FILE_XPATH =
            "//crawl-order/controller"
            + "/newObject[@name='scope']"
            + "/string[@name='seedsfile']";
    
    public static final String ARCS_ENABLED_XPATH =
    		ARCWRITERPROCESSOR_XPATH + "/boolean[@name='enabled']";
    
    /** Xpath for the WARCs dir in the order.xml. */
    public static final String WARCS_ENABLED_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='enabled']";

    public static final String WARCS_WRITE_REQUESTS_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='write-requests']";
    public static final String WARCS_WRITE_METADATA_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='write-metadata']";
    public static final String WARCS_SKIP_IDENTICAL_DIGESTS_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='skip-identical-digests']";
    public static final String WARCS_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='write-revisit-for-identical-digests']";
    public static final String WARCS_WRITE_REVISIT_FOR_NOT_MODIFIED_XPATH =
    		WARCWRITERPROCESSOR_XPATH + "/boolean[@name='write-revisit-for-not-modified']";
    
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
    
    /** Xpath to check, that all templates have the max-time-sec attribute.
     */
    public static final String MAXTIMESEC_PATH_XPATH =
        "/crawl-order/controller/long[@name='max-time-sec']";

    static {
        requiredXpaths.put(GROUP_MAX_FETCH_SUCCESS_XPATH,
                           Pattern.compile(WHOLE_NUMBER_REGEXP));
        requiredXpaths.put(QUEUE_TOTAL_BUDGET_XPATH,
                Pattern.compile(WHOLE_NUMBER_REGEXP));
        requiredXpaths.put(GROUP_MAX_ALL_KB_XPATH,
                           Pattern.compile(WHOLE_NUMBER_REGEXP));

        //Required that we use DecidingScope
        //requiredXpaths.put(DECIDINGSCOPE_XPATH,
        //                    Pattern.compile(EVERYTHING_REGEXP));

        //Required that we have a rules map used to add crawlertraps
        requiredXpaths.put(DECIDERULES_MAP_XPATH,
                           Pattern.compile(EVERYTHING_REGEXP, Pattern.DOTALL));

        requiredXpaths.put(HERITRIX_USER_AGENT_XPATH,
                           Pattern.compile(USER_AGENT_REGEXP, Pattern.DOTALL));
        requiredXpaths.put(HERITRIX_FROM_XPATH, Pattern.compile(FROM_REGEXP));

        // max-time-sec attribute needed, so we can't override it set
        // a timelimit on broad crawls.
        requiredXpaths.put(MAXTIMESEC_PATH_XPATH, Pattern.compile(
                WHOLE_NUMBER_REGEXP));
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
        String xpath;
        Node node;
        Pattern pattern;
        Matcher matcher;
        if (verify) {
            for (Map.Entry<String, Pattern> required: requiredXpaths.entrySet()) {
                xpath = required.getKey();
                node = doc.selectSingleNode(xpath);
                ArgumentNotValid.checkTrue(node != null,
                        "Template error: Missing node: "
                        + xpath);

                pattern = required.getValue();
                matcher = pattern.matcher(node.getText().trim());

                ArgumentNotValid.checkTrue(
                        matcher.matches(),
                        "Template error: Value '" + node.getText()
                        + "' of node '" + xpath
                        + "' does not match required regexp '"
                        + pattern + "'");
            }
            verified = true;
            //Required that Heritrix write its ARC/WARC files to the correct dir
            // relative to the crawldir. This dir is defined by the constant:
            //dk.netarkivet.common.Constants.ARCDIRECTORY_NAME.
            //dk.netarkivet.common.Constants.WARCDIRECTORY_NAME.
            int validArchivePaths = 0;
            node = doc.selectSingleNode(ARC_ARCHIVER_PATH_XPATH);
            if (node != null) {
                pattern = Pattern.compile(
                        dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
                matcher = pattern.matcher(node.getText().trim());
                ArgumentNotValid.checkTrue(
                        matcher.matches(),
                        "Template error: Value '" + node.getText()
                        + "' of node '" + ARC_ARCHIVER_PATH_XPATH
                        + "' does not match required regexp '"
                        + pattern + "'");
                ++validArchivePaths;
            }
            node = doc.selectSingleNode(WARC_ARCHIVER_PATH_XPATH);
            if (node != null) {
                pattern = Pattern.compile(
                        dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
                matcher = pattern.matcher(node.getText().trim());
                ArgumentNotValid.checkTrue(
                        matcher.matches(),
                        "Template error: Value '" + node.getText()
                        + "' of node '" + WARC_ARCHIVER_PATH_XPATH
                        + "' does not match required regexp '"
                        + pattern + "'");
                ++validArchivePaths;
            }
            ArgumentNotValid.checkTrue(
                    validArchivePaths > 0,
                    "Template error: "
                    + "An ARC or WARC writer processor seems to be missing");
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
     
     /**
      * Method to add a list of crawler traps with a given element name. It is
      * used both to add per-domain traps and global traps.
      * @param elementName The name of the added element.
      * @param crawlerTraps A list of crawler trap regular expressions to add
      * to this job.
      */
     @SuppressWarnings("unchecked")
     public static void editOrderXMLAddCrawlerTraps(Document orderXMLdoc, String elementName,
                                              List<String> crawlerTraps) {
         if (crawlerTraps.size() == 0) {
             return;
         }

         // Get the node to update
         // If there is an acceptIfPrerequisite decideRule in the template, crawler traps should be
         // placed before (cf. issue NAS-2205)
         // If no such rule exists then we append the crawler traps as to the existing decideRuleds.
         
         Node rulesMapNode = orderXMLdoc.selectSingleNode(HeritrixTemplate.DECIDERULES_MAP_XPATH);
         if (rulesMapNode == null || !(rulesMapNode instanceof Element)) {
             throw new IllegalState(
                     "Unable to update order.xml document."
                     + "It does not have the right form to add"
                     + "crawler trap deciderules.");
         }
         
         Element rulesMap = (Element) rulesMapNode;
         
         // Create the root node and append it top existing rules
         Element decideRule = rulesMap.addElement("newObject");
         
         // If an acceptiIfPrerequisite node exists, detach and insert before it
         Node acceptIfPrerequisiteNode = orderXMLdoc.selectSingleNode(
                 HeritrixTemplate.DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH);
         if (acceptIfPrerequisiteNode != null) {
            List<Node> elements = rulesMap.elements();
            int insertPosition = elements.indexOf(acceptIfPrerequisiteNode);
            decideRule.detach();
            elements.add(insertPosition, decideRule);
         } else {
             rulesMap.elements().size();
         }
         
         // Add all regexps in the list to a single MatchesListRegExpDecideRule        
         decideRule.addAttribute("name", elementName);
         decideRule.addAttribute("class",
                 MatchesListRegExpDecideRule.class.getName()
             );

         Element decision = decideRule.addElement("string");
         decision.addAttribute("name", "decision");
         decision.addText("REJECT");

         Element listlogic = decideRule.addElement("string");
         listlogic.addAttribute("name", "list-logic");
         listlogic.addText("OR");

         Element regexpList = decideRule.addElement("stringList");
         regexpList.addAttribute("name", "regexp-list");
         for (String trap : crawlerTraps) {
                 regexpList.addElement("string").addText(trap);
         }
     }

     /** Updates the order.xml to include a MatchesListRegExpDecideRule
      *  for each crawlertrap associated with for the given DomainConfiguration.
      *
      * The added nodes have the form
      *
      * <newObject name="domain.dk"
      *      class="org.archive.crawler.deciderules.MatchesListRegExpDecideRule">
      *       <string name="decision">REJECT</string>
      *       <string name="list-logic">OR</string>
      *       <stringList name="regexp-list">
      *          <string>theFirstRegexp</string>
      *          <string>theSecondRegexp</string>
      *       </stringList> 
      *     </newObject>
      *
      * @param cfg The DomainConfiguration for which to generate crawler trap deciderules
      * @throws IllegalState
      *          If unable to update order.xml due to wrong order.xml format
      */
     public static void editOrderXMLAddPerDomainCrawlerTraps(Document orderXmlDoc, DomainConfiguration cfg) {
         //Get the regexps to exclude
         List<String> crawlerTraps = cfg.getCrawlertraps();
         String elementName = cfg.getDomainName();
         HeritrixTemplate.editOrderXMLAddCrawlerTraps(orderXmlDoc, elementName, crawlerTraps);
     }
     
     /**
      * Make sure that Heritrix will archive its data in the chosen archiveFormat.
      * @param orderXML the specific heritrix template to modify.
      * @param archiveFormat the chosen archiveformat ('arc' or 'warc' supported)
      * Throws ArgumentNotValid If the chosen archiveFormat is not supported.
      */
     public static void editOrderXML_ArchiveFormat(Document orderXML, String archiveFormat) {

         boolean arcMode = false;
         boolean warcMode = false;

         if ("arc".equalsIgnoreCase(archiveFormat)) {
             arcMode = true;
             log.debug("ARC format selected to be used by Heritrix");
         } else if ("warc".equalsIgnoreCase(archiveFormat)) {
             warcMode = true;
             log.debug("WARC format selected to be used by Heritrix");
         } else {
             throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.HERITRIX_ARCHIVE_FORMAT 
                     + "' is invalid! Unrecognized format '"
                     + archiveFormat + "'.");
         }

         if (arcMode) {
             // enable ARC writing in Heritrix and disable WARC writing if needed.
             if (orderXML.selectSingleNode(HeritrixTemplate.ARCSDIR_XPATH) != null 
                     && orderXML.selectSingleNode(HeritrixTemplate.ARCS_ENABLED_XPATH) != null) {
                 XmlUtils.setNode(orderXML, HeritrixTemplate.ARCSDIR_XPATH, 
                         dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
                 XmlUtils.setNode(orderXML, HeritrixTemplate.ARCS_ENABLED_XPATH, "true");
                 if (orderXML.selectSingleNode(HeritrixTemplate.WARCS_ENABLED_XPATH) != null) {
                     XmlUtils.setNode(orderXML, HeritrixTemplate.WARCS_ENABLED_XPATH, "false");
                 }
             } else {
                 throw new IllegalState("Unable to choose ARC as Heritrix archive format because "
                         + " one of the following xpaths are invalid in the given order.xml: " 
                         + HeritrixTemplate.ARCSDIR_XPATH + "," +  HeritrixTemplate.ARCS_ENABLED_XPATH);
             }
         } else if (warcMode) { // WARCmode
             // enable ARC writing in Heritrix and disable WARC writing if needed.
             if (orderXML.selectSingleNode(HeritrixTemplate.WARCSDIR_XPATH) != null 
                     && orderXML.selectSingleNode(HeritrixTemplate.WARCS_ENABLED_XPATH) != null) {
                 XmlUtils.setNode(orderXML, HeritrixTemplate.WARCSDIR_XPATH, 
                         dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
                 XmlUtils.setNode(orderXML, HeritrixTemplate.WARCS_ENABLED_XPATH, "true");
                 if (orderXML.selectSingleNode(HeritrixTemplate.ARCS_ENABLED_XPATH) != null) {
                     XmlUtils.setNode(orderXML, HeritrixTemplate.ARCS_ENABLED_XPATH, "false");
                 }

                 // Update the WARCWriterProcessorSettings with settings values
                 setIfFound(orderXML, HeritrixTemplate.WARCS_SKIP_IDENTICAL_DIGESTS_XPATH,
                         HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS, 
                         Settings.get(HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS)
                         );

                 setIfFound(orderXML, HeritrixTemplate.WARCS_WRITE_METADATA_XPATH,
                         HarvesterSettings.HERITRIX_WARC_WRITE_METADATA, 
                         Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA));

                 setIfFound(orderXML, HeritrixTemplate.WARCS_WRITE_REQUESTS_XPATH,
                         HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS, 
                         Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS));

                 setIfFound(orderXML, HeritrixTemplate.WARCS_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS_XPATH,
                         HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS, 
                         Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS)
                         );  
                 setIfFound(orderXML, HeritrixTemplate.WARCS_WRITE_REVISIT_FOR_NOT_MODIFIED_XPATH,
                         HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED, 
                         Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED));

             } else {
                 throw new IllegalState("Unable to choose WARC as Heritrix archive format because "
                         + " one of the following xpaths are invalid in the given order.xml: " 
                         + HeritrixTemplate.WARCSDIR_XPATH + "," 
                         +  HeritrixTemplate.WARCS_ENABLED_XPATH + ". order.xml: " + orderXML.asXML());
             }

         } else {
             throw new IllegalState(
                     "Unknown state: Should have selected either ARC or WARC as heritrix archive format");
         }
     }

     
     
     private static void setIfFound(Document doc, String Xpath, String param, String value) {
         if (doc.selectSingleNode(Xpath) != null) {
             XmlUtils.setNode(doc, Xpath, value);
         } else {
             log.warn("Could not replace setting value of '" + param 
                     + "' in template. Xpath not found: " + Xpath);
         }
     }
     
     /**
      * @param maxJobRunningTime Force the harvestjob to end after maxJobRunningTime
      */
     public static void editOrderXML_maxJobRunningTime(Document orderXMLdoc, long maxJobRunningTime) {
         // get and set the "max-time-sec" node of the orderXMLdoc
         String xpath = HeritrixTemplate.MAXTIMESEC_PATH_XPATH;
         Node groupMaxTimeSecNode = orderXMLdoc.selectSingleNode(xpath);
         if (groupMaxTimeSecNode != null) {
             String currentMaxTimeSec = groupMaxTimeSecNode.getText();
             groupMaxTimeSecNode.setText(Long.toString(maxJobRunningTime));
             log.trace("Value of groupMaxTimeSecNode changed from " 
                     + currentMaxTimeSec + " to " + maxJobRunningTime);
         } else {
             throw new IOFailure(
                     "Unable to locate xpath '" + xpath + "' in the order.xml: "
                     + orderXMLdoc.asXML());
         }
     }
     
     /**
      * Auxiliary method to modify the orderXMLdoc Document
      * with respect to setting the maximum number of objects to be retrieved
      * per domain.
      * This method updates 'group-max-fetch-success' element of the 
      * QuotaEnforcer pre-fetch processor node
      * (org.archive.crawler.frontier.BdbFrontier)
      * with the value of the argument forceMaxObjectsPerDomain
      * @param orderXMLdoc 
      *
      * @param forceMaxObjectsPerDomain
      *           The maximum number of objects to retrieve per domain, or 0
      *           for no limit.
      * @throws PermissionDenied
      *           If unable to replace the frontier node of
      *           the orderXMLdoc Document
      * @throws IOFailure
      *           If the group-max-fetch-success element is not found in the orderXml.
      * TODO The group-max-fetch-success check should also be performed in
      * TemplateDAO.create, TemplateDAO.update
      */
     public static void editOrderXML_maxObjectsPerDomain(
             Document orderXMLdoc, long forceMaxObjectsPerDomain, boolean maxObjectsIsSetByQuotaEnforcer) {

         String xpath = (maxObjectsIsSetByQuotaEnforcer ?
                 HeritrixTemplate.GROUP_MAX_FETCH_SUCCESS_XPATH :
                     HeritrixTemplate.QUEUE_TOTAL_BUDGET_XPATH);

         Node orderXmlNode = orderXMLdoc.selectSingleNode(xpath);
         if (orderXmlNode != null) {
             orderXmlNode.setText(
                     String.valueOf(forceMaxObjectsPerDomain));
         } else {
             throw new IOFailure(
                     "Unable to locate " +  xpath + " element in order.xml: "
                     + orderXMLdoc.asXML());
         }
     }

     /**
      * Activates or deactivate the quota-enforcer, depending on budget definition.
      * Object limit can be defined either by using the queue-total-budget property or
      * the quota enforcer. Which is chosen is set by the argument maxObjectsIsSetByQuotaEnforcer}'s value.
      * So quota enforcer is set as follows:
      * <ul>
      * <li>Object limit is not set by quota enforcer, disabled only if there is no byte limit.</li>
      * <li>Object limit is set by quota enforcer, so it should be enabled whether
      * a byte or object limit is set.</li>
      * </ul>
      * @param orderXMLdoc the template to modify
      * @param maxObjectsIsSetByQuotaEnforcer Decides whether the maxObjectsIsSetByQuotaEnforcer or not. 
      * @param forceMaxBytesPerDomain The number of max bytes per domain enforced (can be no limit)
      * @param forceMaxObjectsPerDomain The number of max objects per domain enforced (can be no limit)
      */
     public static void editOrderXML_configureQuotaEnforcer(Document orderXMLdoc, 
             boolean maxObjectsIsSetByQuotaEnforcer, long forceMaxBytesPerDomain, 
             long forceMaxObjectsPerDomain ) {

         boolean quotaEnabled = true;

         if (!maxObjectsIsSetByQuotaEnforcer) {
             // Object limit is not set by quota enforcer, so it should be disabled only
             // if there is no byte limit.
             quotaEnabled = forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;

         } else {
             // Object limit is set by quota enforcer, so it should be enabled whether
             // a byte or object limit is set.
             quotaEnabled =
                     forceMaxObjectsPerDomain != Constants.HERITRIX_MAXOBJECTS_INFINITY
                 || forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;
         }

         String xpath = HeritrixTemplate.QUOTA_ENFORCER_ENABLED_XPATH;
         Node qeNode = orderXMLdoc.selectSingleNode(xpath);
         if (qeNode != null) {
             qeNode.setText(Boolean.toString(quotaEnabled));
         } else {
             throw new IOFailure(
                     "Unable to locate " +  xpath
                     + " element in order.xml: " + orderXMLdoc.asXML());
         }
     }

     /**
      * Auxiliary method to modify the orderXMLdoc Document
      * with respect to setting the maximum number of bytes to retrieve
      * per domain. This method updates 'group-max-all-kb' element of
      * the 'QuotaEnforcer' node,
      * which again is a subelement of 'pre-fetch-processors' node.
      * with the value of the argument forceMaxBytesPerDomain
      *
      * @param forceMaxBytesPerDomain
      *      The maximum number of byte to retrieve per domain,
      *      or -1 for no limit.
      *      Note that the number is divided by 1024 before being inserted into
      *      the orderXml, as Heritrix expects KB.
      * @throws PermissionDenied
      *      If unable to replace the QuotaEnforcer node of the
      *      orderXMLdoc Document
      * @throws IOFailure
      *      If the group-max-all-kb element cannot be found.
      * TODO This group-max-all-kb check also be performed in
      * TemplateDAO.create, TemplateDAO.update
      */
     public static void editOrderXML_maxBytesPerDomain(Document orderXMLdoc, long forceMaxBytesPerDomain) {
         // get and set the group-max-all-kb Node of the orderXMLdoc:
         String xpath = HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH;
         Node groupMaxSuccessKbNode = orderXMLdoc.selectSingleNode(xpath);
         if (groupMaxSuccessKbNode != null) {
             if (forceMaxBytesPerDomain == 0) {
                 groupMaxSuccessKbNode.setText("0");
             } else if (forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY) {
                 // Divide by 1024 since Heritrix uses KB rather than bytes,
                 // and add 1 to avoid to low limit due to rounding.
                 groupMaxSuccessKbNode.setText(
                         Long.toString((forceMaxBytesPerDomain
                                        / Constants.BYTES_PER_HERITRIX_BYTELIMIT_UNIT)
                                       + 1)
                 );
             } else {
                 groupMaxSuccessKbNode.setText(
                         String.valueOf(Constants.HERITRIX_MAXBYTES_INFINITY));
             }
         } else {
             throw new IOFailure(
                     "Unable to locate QuotaEnforcer object in order.xml: "
                     + orderXMLdoc.asXML());
         }
     }
     
     /**
      * Return true if the given order.xml file has deduplication enabled.
      *
      * @param doc An order.xml document
      *
      * @return True if Deduplicator is enabled.
      */
     public static boolean isDeduplicationEnabledInTemplate(Document doc) {
         ArgumentNotValid.checkNotNull(doc, "Document doc");
         Node xpathNode = doc.selectSingleNode(HeritrixTemplate.DEDUPLICATOR_ENABLED);
         return xpathNode != null
                && xpathNode.getText().trim().equals("true");

     }
     
     /**
      * This method prepares the orderfile used by the Heritrix crawler. </p> 1.
      * alters the orderfile in the following-way: (overriding whatever is in the
      * orderfile)</br> <ol> <li>sets the disk-path to the outputdir specified in
      * HeritrixFiles.</li> <li>sets the seedsfile to the seedsfile specified in
      * HeritrixFiles.</li> <li>sets the prefix of the arcfiles to unique prefix
      * defined in HeritrixFiles</li> <li>checks that the arcs-file dir is 'arcs'
      * - to ensure that we know where the arc-files are when crawl
      * finishes</li>
      *
      * <li>if deduplication is enabled, sets the node pointing to index
      * directory for deduplication (see step 3)</li> </ol> 2. saves the
      * orderfile back to disk</p>
      *
      * 3. if deduplication is enabled in the order.xml, it writes the absolute
      * path of the lucene index used by the deduplication processor.
      *
      * @throws IOFailure - When the orderfile could not be saved to disk When a
      *                   specific node is not found in the XML-document When the
      *                   SAXReader cannot parse the XML
      */
     public static void makeOrderfileReadyForHeritrix(HeritrixFiles files) throws IOFailure {
         Document doc = XmlUtils.getXmlDoc(files.getOrderXmlFile());
         XmlUtils.setNode(doc, HeritrixTemplate.DISK_PATH_XPATH,
                          files.getCrawlDir().getAbsolutePath());

         XmlUtils.setNodes(doc, HeritrixTemplate.ARCHIVEFILE_PREFIX_XPATH, files.getArchiveFilePrefix());

         XmlUtils.setNode(doc, HeritrixTemplate.SEEDS_FILE_XPATH,
                          files.getSeedsTxtFile().getAbsolutePath());

         
         if (isDeduplicationEnabledInTemplate(doc)) {
             XmlUtils.setNode(doc, HeritrixTemplate.DEDUPLICATOR_INDEX_LOCATION_XPATH,
                              files.getIndexDir().getAbsolutePath());
         }

         files.writeOrderXml(doc);
     }

     
}
