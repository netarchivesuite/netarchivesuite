/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;
import dk.netarkivet.harvester.harvesting.report.Heritrix1Constants;

/**
 * Class encapsulating the Heritrix order.xml. Enables verification that dom4j Document obey the constraints required by
 * our software, specifically the Job class.
 * <p>
 * The class assumes the type of order.xml used in configuring Heritrix version 1.10+. Information about the Heritrix
 * crawler, and its processes and modules can be found in the Heritrix developer and user manuals found on <a
 * href="http://crawler.archive.org">http://crawler.archive.org<a/>
 *  
 */
public class H1HeritrixTemplate extends HeritrixTemplate implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(H1HeritrixTemplate.class);

    /** the dom4j Document hiding behind this instance of HeritrixTemplate. */
    private Document template;

    /** has this HeritrixTemplate been verified. */
    private boolean verified;

    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public static final String QUOTA_ENFORCER_ENABLED_XPATH = "/crawl-order/controller/map[@name='pre-fetch-processors']"
            + "/newObject[@name='QuotaEnforcer']" + "/boolean[@name='enabled']";;
    /** Xpath needed by Job.editOrderXML_maxBytesPerDomain(). */
    public static final String GROUP_MAX_ALL_KB_XPATH = "/crawl-order/controller/map[@name='pre-fetch-processors']"
            + "/newObject[@name='QuotaEnforcer']" + "/long[@name='group-max-all-kb']";
    /** Xpath needed by Job.editOrderXML_maxObjectsPerDomain(). */
    public static final String GROUP_MAX_FETCH_SUCCESS_XPATH = "/crawl-order/controller/map[@name='pre-fetch-processors']"
            + "/newObject[@name='QuotaEnforcer']" + "/long[@name='group-max-fetch-successes']";
    /** Xpath needed by Job.editOrderXML_maxObjectsPerDomain(). */
    public static final String QUEUE_TOTAL_BUDGET_XPATH = "/crawl-order/controller/newObject[@name='frontier']"
            + "/long[@name='queue-total-budget']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String DECIDERULES_MAP_XPATH = "/crawl-order/controller/newObject"
            + "/newObject[@name='decide-rules']" + "/map[@name='rules']";
    /** Xpath needed by Job.editOrderXML_crawlerTraps(). */
    public static final String DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH = "/crawl-order/controller/newObject"
            + "/newObject[@name='decide-rules']" + "/map[@name='rules']/newObject[@class="
            + "'org.archive.crawler.deciderules.PrerequisiteAcceptDecideRule']";

    /** Xpath checked by Heritrix for correct user-agent field in requests. */
    public static final String HERITRIX_USER_AGENT_XPATH = "/crawl-order/controller/map[@name='http-headers']"
            + "/string[@name='user-agent']";
    /** Xpath checked by Heritrix for correct mail address. */
    public static final String HERITRIX_FROM_XPATH = "/crawl-order/controller/map[@name='http-headers']/"
            + "string[@name='from']";
    /** Xpath to check, that all templates use the DecidingScope. */
    public static final String DECIDINGSCOPE_XPATH = "/crawl-order/controller/newObject[@name='scope']" + "[@class='"
            + Heritrix1Constants.DECIDINGSCOPE_CLASSNAME + "']";
    /**
     * Xpath for the deduplicator node in order.xml documents.
     */
    public static final String DEDUPLICATOR_XPATH = "/crawl-order/controller/map[@name='write-processors']"
            + "/newObject[@name='DeDuplicator']";

    /**
     * Xpath to check, that all templates use the same ARC archiver path,
     * {@link dk.netarkivet.common.Constants#ARCDIRECTORY_NAME}. The archive path tells Heritrix to which directory it
     * shall write its arc files.
     */
    public static final String ARC_ARCHIVER_PATH_XPATH = "/crawl-order/controller/map[@name='write-processors']/"
            + "newObject[@name='Archiver']/stringList[@name='path']/string";

    /**
     * Xpath to check, that all templates use the same WARC archiver path,
     * {@link dk.netarkivet.common.Constants#WARCDIRECTORY_NAME}. The archive path tells Heritrix to which directory it
     * shall write its arc files.
     */
    public static final String WARC_ARCHIVER_PATH_XPATH = "/crawl-order/controller/map[@name='write-processors']/"
            + "newObject[@name='WARCArchiver']/stringList[@name='path']/string";

    /**
     * Xpath for the deduplicator index directory node in order.xml documents.
     */
    public static final String DEDUPLICATOR_INDEX_LOCATION_XPATH = DEDUPLICATOR_XPATH
            + "/string[@name='index-location']";

    /**
     * Xpath for the boolean telling if the deduplicator is enabled in order.xml documents.
     */
    public static final String DEDUPLICATOR_ENABLED = DEDUPLICATOR_XPATH + "/boolean[@name='enabled']";

    /** Xpath for the 'disk-path' in the order.xml . */
    public static final String DISK_PATH_XPATH = "//crawl-order/controller" + "/string[@name='disk-path']";
    /** Xpath for the arcfile 'prefix' in the order.xml . */
    public static final String ARCHIVEFILE_PREFIX_XPATH = "//crawl-order/controller" + "/map[@name='write-processors']"
            + "/newObject/string[@name='prefix']";
    /** Xpath for the ARCs dir in the order.xml. */
    public static final String ARCSDIR_XPATH = "//crawl-order/controller" + "/map[@name='write-processors']"
            + "/newObject[@name='Archiver']/stringList[@name='path']/string";

    public static final String WARCWRITERPROCESSOR_XPATH = "//crawl-order/controller"
            + "/map[@name='write-processors']" + "/newObject[@name='WARCArchiver']";

    public static final String ARCWRITERPROCESSOR_XPATH = "//crawl-order/controller"
            + "/map[@name='write-processors']" + "/newObject[@name='Archiver']";

    /** Xpath for the WARCs dir in the order.xml. */
    public static final String WARCSDIR_XPATH = WARCWRITERPROCESSOR_XPATH + "/stringList[@name='path']/string";

    /** Xpath for the 'seedsfile' in the order.xml. */
    public static final String SEEDS_FILE_XPATH = "//crawl-order/controller" + "/newObject[@name='scope']"
            + "/string[@name='seedsfile']";

    public static final String ARCS_ENABLED_XPATH = ARCWRITERPROCESSOR_XPATH + "/boolean[@name='enabled']";

    /** Xpath for the WARCs dir in the order.xml. */
    public static final String WARCS_ENABLED_XPATH = WARCWRITERPROCESSOR_XPATH + "/boolean[@name='enabled']";

    public static final String WARCS_WRITE_REQUESTS_XPATH = WARCWRITERPROCESSOR_XPATH
            + "/boolean[@name='write-requests']";
    public static final String WARCS_WRITE_METADATA_XPATH = WARCWRITERPROCESSOR_XPATH
            + "/boolean[@name='write-metadata']";
    public static final String WARCS_WRITE_METADATA_OUTLINKS_XPATH = WARCWRITERPROCESSOR_XPATH
    		+ "/boolean[@name='write-metadata-outlinks']";
    public static final String WARCS_SKIP_IDENTICAL_DIGESTS_XPATH = WARCWRITERPROCESSOR_XPATH
            + "/boolean[@name='skip-identical-digests']";
    public static final String WARCS_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS_XPATH = WARCWRITERPROCESSOR_XPATH
            + "/boolean[@name='write-revisit-for-identical-digests']";
    public static final String WARCS_WRITE_REVISIT_FOR_NOT_MODIFIED_XPATH = WARCWRITERPROCESSOR_XPATH
            + "/boolean[@name='write-revisit-for-not-modified']";

    /** Xpath for the WARC metadata in the order.xml. */
    public static final String METADATA_ITEMS_XPATH = WARCWRITERPROCESSOR_XPATH + "/map[@name='metadata-items']";

    /**
     * Map from required xpaths to a regular expression describing legal content for the path text.
     */
    private static final Map<String, Pattern> requiredXpaths = new HashMap<String, Pattern>();

    /**
     * A regular expression that matches a whole number, possibly negative, and with optional whitespace around it.
     */
    private static final String WHOLE_NUMBER_REGEXP = "\\s*-?[0-9]+\\s*";
    /**
     * A regular expression that matches everything. Except newlines, unless DOTALL is given to Pattern.compile().
     */
    private static final String EVERYTHING_REGEXP = ".*";

    // These two regexps are copied from
    // org.archive.crawler.datamodel.CrawlOrder because they're private there.

    /**
     * A regular expression that matches Heritrix' specs for the user-agent field in order.xml. It should be used with
     * DOTALL. An example match is "Org (ourCrawler, see +http://org.org/aPage for details) harvest".
     */
    private static final String USER_AGENT_REGEXP = "\\S+.*\\(.*\\+http(s)?://\\S+\\.\\S+.*\\).*";
    /**
     * A regular expression that matches Heritrix' specs for the from field. This should be a valid email address.
     */
    private static final String FROM_REGEXP = "\\S+@\\S+\\.\\S+";

    /**
     * Xpath to check, that all templates have the max-time-sec attribute.
     */
    public static final String MAXTIMESEC_PATH_XPATH = "/crawl-order/controller/long[@name='max-time-sec']";


    static {
        requiredXpaths.put(GROUP_MAX_FETCH_SUCCESS_XPATH, Pattern.compile(WHOLE_NUMBER_REGEXP));
        requiredXpaths.put(QUEUE_TOTAL_BUDGET_XPATH, Pattern.compile(WHOLE_NUMBER_REGEXP));
        requiredXpaths.put(GROUP_MAX_ALL_KB_XPATH, Pattern.compile(WHOLE_NUMBER_REGEXP));

        // Required that we use DecidingScope
        // requiredXpaths.put(DECIDINGSCOPE_XPATH,
        // Pattern.compile(EVERYTHING_REGEXP));

        // Required that we have a rules map used to add crawlertraps
        requiredXpaths.put(DECIDERULES_MAP_XPATH, Pattern.compile(EVERYTHING_REGEXP, Pattern.DOTALL));

        requiredXpaths.put(HERITRIX_USER_AGENT_XPATH, Pattern.compile(USER_AGENT_REGEXP, Pattern.DOTALL));
        requiredXpaths.put(HERITRIX_FROM_XPATH, Pattern.compile(FROM_REGEXP));

        // max-time-sec attribute needed, so we can't override it set
        // a timelimit on broad crawls.
        requiredXpaths.put(MAXTIMESEC_PATH_XPATH, Pattern.compile(WHOLE_NUMBER_REGEXP));
    }

    /**
     * Constructor for HeritrixTemplate class.
     *
     * @param doc the order.xml
     * @param verify If true, verifies if the given dom4j Document contains the elements required by our software.
     * @throws ArgumentNotValid if doc is null, or verify is true and doc does not obey the constraints required by our
     * software.
     */
    public H1HeritrixTemplate(Document doc, boolean verify) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        String xpath;
        Node node;
        Pattern pattern;
        Matcher matcher;
        if (verify) {
            for (Map.Entry<String, Pattern> required : requiredXpaths.entrySet()) {
                xpath = required.getKey();
                node = doc.selectSingleNode(xpath);
                ArgumentNotValid.checkTrue(node != null, "Template error: Missing node: " + xpath 
                		+ ". The template looks like this: " + doc.asXML());

                pattern = required.getValue();
                matcher = pattern.matcher(node.getText().trim());

                ArgumentNotValid.checkTrue(matcher.matches(), "Template error: Value '" + node.getText()
                        + "' of node '" + xpath + "' does not match required regexp '" + pattern 
                        + "'. The template looks like this: " + doc.asXML());
            }
            verified = true;
            // Required that Heritrix write its ARC/WARC files to the correct dir
            // relative to the crawldir. This dir is defined by the constant:
            // dk.netarkivet.common.Constants.ARCDIRECTORY_NAME.
            // dk.netarkivet.common.Constants.WARCDIRECTORY_NAME.
            int validArchivePaths = 0;
            node = doc.selectSingleNode(ARC_ARCHIVER_PATH_XPATH);
            if (node != null) {
                pattern = Pattern.compile(dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
                matcher = pattern.matcher(node.getText().trim());
                ArgumentNotValid.checkTrue(matcher.matches(), "Template error: Value '" + node.getText()
                        + "' of node '" + ARC_ARCHIVER_PATH_XPATH + "' does not match required regexp '" + pattern
                        + "'");
                ++validArchivePaths;
            }
            node = doc.selectSingleNode(WARC_ARCHIVER_PATH_XPATH);
            if (node != null) {
                pattern = Pattern.compile(dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
                matcher = pattern.matcher(node.getText().trim());
                ArgumentNotValid.checkTrue(matcher.matches(), "Template error: Value '" + node.getText()
                        + "' of node '" + WARC_ARCHIVER_PATH_XPATH + "' does not match required regexp '" + pattern
                        + "'");
                ++validArchivePaths;
            }
            ArgumentNotValid.checkTrue(validArchivePaths > 0, "Template error: "
                    + "An ARC or WARC writer processor seems to be missing");
        }
        this.template = (Document) doc.clone();
    }

    /**
     * Alternate constructor, which always verifies the given document.
     *
     * @param doc
     */
    public H1HeritrixTemplate(Document doc) {
        this(doc, true);
    }

    public H1HeritrixTemplate(long template_id, String templateAsString) throws DocumentException {
        ArgumentNotValid.checkNotNull(templateAsString, "String template");
        this.template_id = template_id;
    	this.template = XmlUtils.documentFromString(templateAsString);
	}

	/**
     * return the template.
     *
     * @return the template
     */
    public Document getTemplate() {
        return (Document) template.clone();
    }

    /**
     * Has Template been verified?
     *
     * @return true, if verified on construction, otherwise false
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Return HeritrixTemplate as XML.
     *
     * @return HeritrixTemplate as XML
     */
    public String getXML() {
        return template.asXML();
    }

    /**
     * Method to add a list of crawler traps with a given element name. It is used both to add per-domain traps and
     * global traps.
     *
     * @param elementName The name of the added element.
     * @param crawlerTraps A list of crawler trap regular expressions to add to this job.
     */
    @SuppressWarnings("unchecked")
    public static void editOrderXMLAddCrawlerTraps(Document orderXMLdoc, String elementName, List<String> crawlerTraps) {
        if (crawlerTraps.size() == 0) {
            return;
        }

        // Get the node to update
        // If there is an acceptIfPrerequisite decideRule in the template, crawler traps should be
        // placed before (cf. issue NAS-2205)
        // If no such rule exists then we append the crawler traps as to the existing decideRuleds.

        Node rulesMapNode = orderXMLdoc.selectSingleNode(DECIDERULES_MAP_XPATH);
        if (rulesMapNode == null || !(rulesMapNode instanceof Element)) {
            throw new IllegalState("Unable to update order.xml document. It does not have the right form to add"
                    + "crawler trap deciderules.");
        }

        Element rulesMap = (Element) rulesMapNode;

        // Create the root node and append it top existing rules
        Element decideRule = rulesMap.addElement("newObject");

        // If an acceptiIfPrerequisite node exists, detach and insert before it
        Node acceptIfPrerequisiteNode = orderXMLdoc.selectSingleNode(DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH);
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
        decideRule.addAttribute("class", Heritrix1Constants.MATCHESLISTREGEXPDECIDERULE_CLASSNAME);

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

    /**
     * Updates the order.xml to include a MatchesListRegExpDecideRule for each crawlertrap associated with for the given
     * DomainConfiguration.
     * <p>
     * The added nodes have the form
     * <p>
     * <newObject name="domain.dk" class="org.archive.crawler.deciderules.MatchesListRegExpDecideRule"> <string
     * name="decision">REJECT</string> <string name="list-logic">OR</string> <stringList name="regexp-list">
     * <string>theFirstRegexp</string> <string>theSecondRegexp</string> </stringList> </newObject>
     *
     * @param cfg The DomainConfiguration for which to generate crawler trap deciderules
     * @throws IllegalState If unable to update order.xml due to wrong order.xml format
     */
    // FIXME REMOVE IF NOT USED
    /*
    public static void editOrderXMLAddPerDomainCrawlerTraps(Document orderXmlDoc, DomainConfiguration cfg) {
        // Get the regexps to exclude
        List<String> crawlerTraps = cfg.getCrawlertraps();
        String elementName = cfg.getDomainName();
        H1HeritrixTemplate.editOrderXMLAddCrawlerTraps(orderXmlDoc, elementName, crawlerTraps);
    }
    */

    private static void setIfFound(Document doc, String Xpath, String param, String value) {
        if (doc.selectSingleNode(Xpath) != null) {
            XmlUtils.setNode(doc, Xpath, value);
        } else {
            log.warn("Could not replace setting value of '" + param + "' in template. Xpath not found: " + Xpath);
        }
    }

    /**
     * Auxiliary method to modify the orderXMLdoc Document with respect to setting the maximum number of objects to be
     * retrieved per domain. This method updates 'group-max-fetch-success' element of the QuotaEnforcer pre-fetch
     * processor node (org.archive.crawler.frontier.BdbFrontier) with the value of the argument forceMaxObjectsPerDomain
     *
     * @param orderXMLdoc
     * @param forceMaxObjectsPerDomain The maximum number of objects to retrieve per domain, or 0 for no limit.
     * @throws PermissionDenied If unable to replace the frontier node of the orderXMLdoc Document
     * @throws IOFailure If the group-max-fetch-success element is not found in the orderXml. TODO The
     * group-max-fetch-success check should also be performed in TemplateDAO.create, TemplateDAO.update
     */
    public static void editOrderXML_maxObjectsPerDomain(Document orderXMLdoc, long forceMaxObjectsPerDomain,
            boolean maxObjectsIsSetByQuotaEnforcer) {

        String xpath = (maxObjectsIsSetByQuotaEnforcer ? GROUP_MAX_FETCH_SUCCESS_XPATH : QUEUE_TOTAL_BUDGET_XPATH);

        Node orderXmlNode = orderXMLdoc.selectSingleNode(xpath);
        if (orderXmlNode != null) {
            orderXmlNode.setText(String.valueOf(forceMaxObjectsPerDomain));
        } else {
            throw new IOFailure("Unable to locate " + xpath + " element in order.xml: " + orderXMLdoc.asXML());
        }
    }

    /**
     * Activates or deactivate the quota-enforcer, depending on budget definition. Object limit can be defined either by
     * using the queue-total-budget property or the quota enforcer. Which is chosen is set by the argument
     * maxObjectsIsSetByQuotaEnforcer}'s value. So quota enforcer is set as follows:
     * <ul>
     * <li>Object limit is not set by quota enforcer, disabled only if there is no byte limit.</li>
     * <li>Object limit is set by quota enforcer, so it should be enabled whether a byte or object limit is set.</li>
     * </ul>
     *
     * @param orderXMLdoc the template to modify
     * @param maxObjectsIsSetByQuotaEnforcer Decides whether the maxObjectsIsSetByQuotaEnforcer or not.
     * @param forceMaxBytesPerDomain The number of max bytes per domain enforced (can be no limit)
     * @param forceMaxObjectsPerDomain The number of max objects per domain enforced (can be no limit)
     */
    public static void editOrderXML_configureQuotaEnforcer(Document orderXMLdoc,
            boolean maxObjectsIsSetByQuotaEnforcer, long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain) {

        boolean quotaEnabled = true;

        if (!maxObjectsIsSetByQuotaEnforcer) {
            // Object limit is not set by quota enforcer, so it should be disabled only
            // if there is no byte limit.
            quotaEnabled = forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;

        } else {
            // Object limit is set by quota enforcer, so it should be enabled whether
            // a byte or object limit is set.
            quotaEnabled = forceMaxObjectsPerDomain != Constants.HERITRIX_MAXOBJECTS_INFINITY
                    || forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;
        }

        String xpath = QUOTA_ENFORCER_ENABLED_XPATH;
        Node qeNode = orderXMLdoc.selectSingleNode(xpath);
        if (qeNode != null) {
            qeNode.setText(Boolean.toString(quotaEnabled));
        } else {
            throw new IOFailure("Unable to locate " + xpath + " element in order.xml: " + orderXMLdoc.asXML());
        }
    }

    
    
	@Override
	// Always return true
	public boolean isValid() {
		return true;
	}

	@Override
	public void configureQuotaEnforcer(boolean maxObjectsIsSetByQuotaEnforcer,
			long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain) {
		Document orderXMLdoc = this.template;
		boolean quotaEnabled = true;

		if (!maxObjectsIsSetByQuotaEnforcer) {
			// Object limit is not set by quota enforcer, so it should be disabled only
			// if there is no byte limit.
			quotaEnabled = forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;

		} else {
			// Object limit is set by quota enforcer, so it should be enabled whether
			// a byte or object limit is set.
			quotaEnabled = forceMaxObjectsPerDomain != Constants.HERITRIX_MAXOBJECTS_INFINITY
					|| forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;
		}

		String xpath = QUOTA_ENFORCER_ENABLED_XPATH;
		Node qeNode = orderXMLdoc.selectSingleNode(xpath);
		if (qeNode != null) {
			qeNode.setText(Boolean.toString(quotaEnabled));
		} else {
			throw new IOFailure("Unable to locate " + xpath + " element in order.xml: " + orderXMLdoc.asXML());
		}
	}

	/**
    * Auxiliary method to modify the orderXMLdoc Document with respect to setting the maximum number of bytes to
    * retrieve per domain. This method updates 'group-max-all-kb' element of the 'QuotaEnforcer' node, which again is a
    * subelement of 'pre-fetch-processors' node. with the value of the argument forceMaxBytesPerDomain
    *
    * @param forceMaxBytesPerDomain The maximum number of byte to retrieve per domain, or -1 for no limit. Note that
    * the number is divided by 1024 before being inserted into the orderXml, as Heritrix expects KB.
    * @throws PermissionDenied If unable to replace the QuotaEnforcer node of the orderXMLdoc Document
    * @throws IOFailure If the group-max-all-kb element cannot be found. TODO This group-max-all-kb check also be
    * performed in TemplateDAO.create, TemplateDAO.update
    */
	@Override
	public void setMaxBytesPerDomain(Long forceMaxBytesPerDomain) {
		// get and set the group-max-all-kb Node of the orderXMLdoc:
        String xpath = GROUP_MAX_ALL_KB_XPATH;
        Node groupMaxSuccessKbNode = template.selectSingleNode(xpath);
        if (groupMaxSuccessKbNode != null) {
            if (forceMaxBytesPerDomain == 0) {
                groupMaxSuccessKbNode.setText("0");
            } else if (forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY) {
                // Divide by 1024 since Heritrix uses KB rather than bytes,
                // and add 1 to avoid to low limit due to rounding.
                groupMaxSuccessKbNode.setText(Long
                        .toString((forceMaxBytesPerDomain / Constants.BYTES_PER_HERITRIX_BYTELIMIT_UNIT) + 1));
            } else {
                groupMaxSuccessKbNode.setText(String.valueOf(Constants.HERITRIX_MAXBYTES_INFINITY));
            }
        } else {
            throw new IOFailure("Unable to locate QuotaEnforcer object in order.xml: " + template.asXML());
        }	
	}

	@Override
	public Long getMaxBytesPerDomain() {
		// FIXME IMPLEMENT ME
		return null;
	}

	@Override
	public void setMaxObjectsPerDomain(Long maxobjectsL) {
		// FIXME IMPLEMENT ME
		
	}

	@Override
	public Long getMaxObjectsPerDomain() {
		// FIXME IMPLEMENT ME OR DELETE
		return null;
	}

	/**
     * Return true if the templatefile has deduplication enabled.
     * @return True if Deduplicator is enabled.
     */
	@Override
	public boolean IsDeduplicationEnabled() {
        Node xpathNode = template.selectSingleNode(DEDUPLICATOR_ENABLED);
        return xpathNode != null && xpathNode.getText().trim().equals("true");
	}

	@Override
	public void setArchiveFormat(String archiveFormat) {
		Document orderXML = this.template;
        boolean arcMode = false;
        boolean warcMode = false;

        //System.out.println("Document: " + template.asXML()); 
        
        if ("arc".equalsIgnoreCase(archiveFormat)) {
            arcMode = true;
            log.debug("ARC format selected to be used by Heritrix");
        } else if ("warc".equalsIgnoreCase(archiveFormat)) {
            warcMode = true;
            log.debug("WARC format selected to be used by Heritrix");
        } else {
            throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.HERITRIX_ARCHIVE_FORMAT
                    + "' is invalid! Unrecognized format '" + archiveFormat + "'.");
        }

        if (arcMode) {
            // enable ARC writing in Heritrix and disable WARC writing if needed.
            if (orderXML.selectSingleNode(ARCSDIR_XPATH) != null
                    && orderXML.selectSingleNode(ARCS_ENABLED_XPATH) != null) {
                XmlUtils.setNode(orderXML, ARCSDIR_XPATH,
                        dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
                XmlUtils.setNode(orderXML, ARCS_ENABLED_XPATH, "true");
                if (orderXML.selectSingleNode(WARCS_ENABLED_XPATH) != null) {
                    XmlUtils.setNode(orderXML, WARCS_ENABLED_XPATH, "false");
                }
            } else {
                throw new IllegalState("Unable to choose ARC as Heritrix archive format because "
                        + " one of the following xpaths are invalid in the given order.xml: "
                        + ARCSDIR_XPATH + "," + ARCS_ENABLED_XPATH);
            }
        } else if (warcMode) { // WARCmode
            // enable ARC writing in Heritrix and disable WARC writing if needed.
            if (orderXML.selectSingleNode(WARCSDIR_XPATH) != null
                    && orderXML.selectSingleNode(WARCS_ENABLED_XPATH) != null) {
                XmlUtils.setNode(orderXML, WARCSDIR_XPATH,
                        dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
                XmlUtils.setNode(orderXML, WARCS_ENABLED_XPATH, "true");
                if (orderXML.selectSingleNode(ARCS_ENABLED_XPATH) != null) {
                    XmlUtils.setNode(orderXML, ARCS_ENABLED_XPATH, "false");
                }

                String warcParametersOverrideStr = null;
                try {
                	warcParametersOverrideStr = Settings.get(HarvesterSettings.HERITRIX_WARC_PARAMETERS_OVERRIDE);
                } catch (UnknownID e) {
                	//nothing
                }
                //if the parameter is not found or if it exists and equals to true
                if (warcParametersOverrideStr == null || (warcParametersOverrideStr != null
                		&& "true".equals(warcParametersOverrideStr))) {

	                // Update the WARCWriterProcessorSettings with settings values
	                setIfFound(orderXML, WARCS_SKIP_IDENTICAL_DIGESTS_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS));
	
	                setIfFound(orderXML, WARCS_WRITE_METADATA_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_WRITE_METADATA,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA));
	                setIfFound(orderXML, WARCS_WRITE_METADATA_OUTLINKS_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_WRITE_METADATA_OUTLINKS,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA_OUTLINKS));
	                setIfFound(orderXML, WARCS_WRITE_REQUESTS_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS));
	
	                setIfFound(orderXML, WARCS_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS));
	                setIfFound(orderXML, WARCS_WRITE_REVISIT_FOR_NOT_MODIFIED_XPATH,
	                        HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED,
	                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED));
                }
            } else {
                throw new IllegalState("Unable to choose WARC as Heritrix archive format because "
                        + " one of the following xpaths are invalid in the given order.xml: "
                        + WARCSDIR_XPATH + "," + WARCS_ENABLED_XPATH
                        + ". order.xml: " + orderXML.asXML());
            }

        } else {
            throw new IllegalState("Unknown state: "
                    + "Should have selected either ARC or WARC as heritrix archive format");
        }		
	}

	@Override
	public void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL) {
        // get and set the "max-time-sec" node of the orderXMLdoc
        String xpath = MAXTIMESEC_PATH_XPATH;
        Node groupMaxTimeSecNode = template.selectSingleNode(xpath);
        if (groupMaxTimeSecNode != null) {
            String currentMaxTimeSec = groupMaxTimeSecNode.getText();
            groupMaxTimeSecNode.setText(Long.toString(maxJobRunningTimeSecondsL));
            log.trace("Value of groupMaxTimeSecNode changed from " + currentMaxTimeSec + " to " + maxJobRunningTimeSecondsL);
        } else {
            throw new IOFailure("Unable to locate xpath '" + xpath + "' in the order.xml: " + template.asXML());
        }
	}
	
	
	@Override
	public void writeTemplate(OutputStream os) throws IOException, ArgumentNotValid{
     	XMLWriter writer;
		try {
			writer = new XMLWriter(os);
			writer.write(this.template);
		} catch (UnsupportedEncodingException e) {
			String errMsg = "The encoding of this template is unsupported by this environment";
			log.error(errMsg, e);
			throw new ArgumentNotValid(errMsg, e);
		} 
	}

	/**
	 * Only available for H1 templates. 	
	 * @return the template as a String.
	 */
	public String getText()  {
		return this.template.getText();
	}

	@Override
	public void insertCrawlerTraps(String elementName, List<String> crawlerTraps) {
		if (crawlerTraps.size() == 0) {
            return;
        }
        
		//System.out.println("Calling insertCrawlerTraps(String elementName, List<String> crawlerTraps) ");
        // Get the node to update
        // If there is an acceptIfPrerequisite decideRule in the template, crawler traps should be
        // placed before (cf. issue NAS-2205)
        // If no such rule exists then we append the crawler traps as to the existing decideRuleds.

        Node rulesMapNode = template.selectSingleNode(DECIDERULES_MAP_XPATH);
        if (rulesMapNode == null || !(rulesMapNode instanceof Element)) {
            throw new IllegalState("Unable to update order.xml document. It does not have the right form to add"
                    + "crawler trap deciderules.");
        }

        Element rulesMap = (Element) rulesMapNode;

        // Create the root node and append it top existing rules
        Element decideRule = rulesMap.addElement("newObject");

        // If an acceptiIfPrerequisite node exists, detach and insert before it
        Node acceptIfPrerequisiteNode = template
                .selectSingleNode(DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH);
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
        decideRule.addAttribute("class", Heritrix1Constants.MATCHESLISTREGEXPDECIDERULE_CLASSNAME);

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
	
	@Override
	public boolean hasContent() {
		return this.template.hasContent();
	}

	@Override
	public void writeToFile(File orderXmlFile) {
		XmlUtils.writeXmlToFile(this.template, orderXmlFile);
	}

	@Override
	public void setRecoverlogNode(File recoverlogGzFile) {
        final String RECOVERLOG_PATH_XPATH = "/crawl-order/controller/string[@name='recover-path']";
        Node orderXmlNode = template.selectSingleNode(RECOVERLOG_PATH_XPATH);
        if (orderXmlNode != null) {
            orderXmlNode.setText(recoverlogGzFile.getAbsolutePath());
            log.debug("The Heritrix recover path now refers to '{}'.", recoverlogGzFile.getAbsolutePath());
        } else {
            throw new IOFailure("Unable to locate the '" + RECOVERLOG_PATH_XPATH + "' element in order.xml: "
                    + template.asXML());
        }
	}

	@Override
	public void setDeduplicationIndexLocation(String absolutePath) {
		XmlUtils.setNode(template, DEDUPLICATOR_INDEX_LOCATION_XPATH, absolutePath);		
	}

	@Override
	public void setSeedsFilePath(String absolutePath) {
		XmlUtils.setNode(template, SEEDS_FILE_XPATH, absolutePath);
	}

	@Override
	public void setArchiveFilePrefix(String archiveFilePrefix) {
		XmlUtils.setNodes(template, ARCHIVEFILE_PREFIX_XPATH, archiveFilePrefix);
	}

	@Override
	public void setDiskPath(String absolutePath) {
		XmlUtils.setNode(template, DISK_PATH_XPATH, absolutePath);
	}

	@Override
	public void removeDeduplicatorIfPresent() {
		Node xpathNode = template.selectSingleNode(DEDUPLICATOR_XPATH);
	    if (xpathNode != null) {
	        xpathNode.detach();
	    }
	}

    @Override public void enableOrDisableDeduplication(boolean enabled) {
        //NOP
        log.debug("In H1 templates we don't enable/disable deduplication.");
    }

    @Override
	public void insertWarcInfoMetadata(Job ajob, String origHarvestdefinitionName, 
			String origHarvestdefinitionComments, String scheduleName, String performer) {
		
		Node WARCWRITERNODE = template.selectSingleNode(WARCWRITERPROCESSOR_XPATH);
		if (WARCWRITERNODE == null) {
			throw new IOFailure("Unable to locate the '" + WARCWRITERPROCESSOR_XPATH + "' element in order.xml: "
                    + template.asXML());
        } 
		
		Element warcwriterElement = (Element) WARCWRITERNODE;
		Element metadataMap = warcwriterElement.addElement("map");
        metadataMap.addAttribute("name", "metadata-items");
        
        Element metadataItem = null;
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_VERSION);
        metadataItem.addText(HARVESTINFO_VERSION_NUMBER);
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_JOBID);
        metadataItem.addText("" + ajob.getJobID());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_CHANNEL);
        metadataItem.addText(ajob.getChannel());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_HARVESTNUM);
        metadataItem.addText("" + ajob.getHarvestNum());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_ORIGHARVESTDEFINITIONID);
        metadataItem.addText("" + ajob.getOrigHarvestDefinitionID());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_MAXBYTESPERDOMAIN);
        metadataItem.addText("" + ajob.getMaxBytesPerDomain());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_MAXOBJECTSPERDOMAIN);
        metadataItem.addText("" + ajob.getMaxObjectsPerDomain());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_ORDERXMLNAME);
        metadataItem.addText(ajob.getOrderXMLName());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_ORIGHARVESTDEFINITIONNAME);
        metadataItem.addText(origHarvestdefinitionName);
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS);
        metadataItem.addText(origHarvestdefinitionComments);
        
        /* optional schedule-name, only for selective harvests. */
		if (scheduleName != null) {
			metadataItem = metadataMap.addElement("string");
	        metadataItem.addAttribute("name", HARVESTINFO_SCHEDULENAME);
	        metadataItem.addText(scheduleName);
		}

		metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_HARVESTFILENAMEPREFIX);
        metadataItem.addText(ajob.getHarvestFilenamePrefix());
        
        metadataItem = metadataMap.addElement("string");
        metadataItem.addAttribute("name", HARVESTINFO_JOBSUBMITDATE);
        metadataItem.addText("" + ajob.getSubmittedDate());
        
		/* optional HARVESTINFO_PERFORMER */
		if (performer != null) {
			metadataItem = metadataMap.addElement("string");
	        metadataItem.addAttribute("name", HARVESTINFO_PERFORMER);
	        metadataItem.addText(performer);
		}

		/* optional HARVESTINFO_AUDIENCE */
		if (ajob.getHarvestAudience() != null) {
			metadataItem = metadataMap.addElement("string");
	        metadataItem.addAttribute("name", HARVESTINFO_AUDIENCE);
	        metadataItem.addText(ajob.getHarvestAudience());
		} 
	}

	@Override
	public void insertAttributes(List<AttributeAndType> attributesAndTypes) {
		// Unsupported for Heritrix 1 templates at this point.
	    log.warn("No attribute insertion is done for H1 templates");
	}

	@Override
	public void writeTemplate(JspWriter out) throws IOFailure {
		try {
			out.write(template.asXML());
		} catch (IOException e) {
			throw new IOFailure("Unable to write to JspWriter", e);
		}
		
	}

}
