/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
import java.io.OutputStream;
import java.sql.Clob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.archive.crawler.deciderules.MatchesListRegExpDecideRule;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Class encapsulating the Heritrix crawler-beans.cxml file 
 * <p>
 * 
 * Heritrix3 has a new model based on spring.
 * So the XPATH is no good for processing 
 * 
 * template is a H3 template if it contains the string
 * 
 * xmlns:"http://www.springframework.org/...."
 * 
 * template is a H1 template if it contains the string
 * <crawl-order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
 * xsi:noNamespaceSchemaLocation="heritrix_settings.xsd">
 * 
 */
public class H3HeritrixTemplate extends HeritrixTemplate {

    private static final Logger log = LoggerFactory.getLogger(H3HeritrixTemplate.class);

    private String template;  
    
    
    /** has this HeritrixTemplate been verified. */
    private boolean verified;

    /**
     * 
     * TODO howto use the quota-enforcer in H3
     * 
     * 
     */

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

    /**
     * Constructor for HeritrixTemplate class.
     *
     * @param doc the order.xml
     * @param verify If true, verifies if the given dom4j Document contains the elements required by our software.
     * @throws ArgumentNotValid if doc is null, or verify is true and doc does not obey the constraints required by our
     * software.
     */
    public H3HeritrixTemplate(String template) {
        ArgumentNotValid.checkNotNull(template, "String template");
        this.template = template;
    }

    public H3HeritrixTemplate(Clob clob) {
		// TODO Auto-generated constructor stub
	}

	/**
     * return the template.
     *
     * @return the template
     */
    public HeritrixTemplate getTemplate() {
        return this;
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
        return template;
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

        Node rulesMapNode = orderXMLdoc.selectSingleNode(H1HeritrixTemplate.DECIDERULES_MAP_XPATH);
        if (rulesMapNode == null || !(rulesMapNode instanceof Element)) {
            throw new IllegalState("Unable to update order.xml document. It does not have the right form to add"
                    + "crawler trap deciderules.");
        }

        Element rulesMap = (Element) rulesMapNode;

        // Create the root node and append it top existing rules
        Element decideRule = rulesMap.addElement("newObject");

        // If an acceptiIfPrerequisite node exists, detach and insert before it
        Node acceptIfPrerequisiteNode = orderXMLdoc
                .selectSingleNode(H1HeritrixTemplate.DECIDERULES_ACCEPT_IF_PREREQUISITE_XPATH);
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
        decideRule.addAttribute("class", MatchesListRegExpDecideRule.class.getName());

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
    public static void editOrderXMLAddPerDomainCrawlerTraps(Document orderXmlDoc, DomainConfiguration cfg) {
        // Get the regexps to exclude
        List<String> crawlerTraps = cfg.getCrawlertraps();
        String elementName = cfg.getDomainName();
        H1HeritrixTemplate.editOrderXMLAddCrawlerTraps(orderXmlDoc, elementName, crawlerTraps);
    }

    /**
     * Make sure that Heritrix will archive its data in the chosen archiveFormat.
     *
     * @param orderXML the specific heritrix template to modify.
     * @param archiveFormat the chosen archiveformat ('arc' or 'warc' supported) Throws ArgumentNotValid If the chosen
     * archiveFormat is not supported.
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
                    + "' is invalid! Unrecognized format '" + archiveFormat + "'.");
        }

        if (arcMode) {
            // enable ARC writing in Heritrix and disable WARC writing if needed.
            if (orderXML.selectSingleNode(H1HeritrixTemplate.ARCSDIR_XPATH) != null
                    && orderXML.selectSingleNode(H1HeritrixTemplate.ARCS_ENABLED_XPATH) != null) {
                XmlUtils.setNode(orderXML, H1HeritrixTemplate.ARCSDIR_XPATH,
                        dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
                XmlUtils.setNode(orderXML, H1HeritrixTemplate.ARCS_ENABLED_XPATH, "true");
                if (orderXML.selectSingleNode(H1HeritrixTemplate.WARCS_ENABLED_XPATH) != null) {
                    XmlUtils.setNode(orderXML, H1HeritrixTemplate.WARCS_ENABLED_XPATH, "false");
                }
            } else {
                throw new IllegalState("Unable to choose ARC as Heritrix archive format because "
                        + " one of the following xpaths are invalid in the given order.xml: "
                        + H1HeritrixTemplate.ARCSDIR_XPATH + "," + H1HeritrixTemplate.ARCS_ENABLED_XPATH);
            }
        } else if (warcMode) { // WARCmode
            // enable ARC writing in Heritrix and disable WARC writing if needed.
            if (orderXML.selectSingleNode(H1HeritrixTemplate.WARCSDIR_XPATH) != null
                    && orderXML.selectSingleNode(H1HeritrixTemplate.WARCS_ENABLED_XPATH) != null) {
                XmlUtils.setNode(orderXML, H1HeritrixTemplate.WARCSDIR_XPATH,
                        dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
                XmlUtils.setNode(orderXML, H1HeritrixTemplate.WARCS_ENABLED_XPATH, "true");
                if (orderXML.selectSingleNode(H1HeritrixTemplate.ARCS_ENABLED_XPATH) != null) {
                    XmlUtils.setNode(orderXML, H1HeritrixTemplate.ARCS_ENABLED_XPATH, "false");
                }

                // Update the WARCWriterProcessorSettings with settings values
                setIfFound(orderXML, H1HeritrixTemplate.WARCS_SKIP_IDENTICAL_DIGESTS_XPATH,
                        HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS,
                        Settings.get(HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS));

                setIfFound(orderXML, H1HeritrixTemplate.WARCS_WRITE_METADATA_XPATH,
                        HarvesterSettings.HERITRIX_WARC_WRITE_METADATA,
                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA));

                setIfFound(orderXML, H1HeritrixTemplate.WARCS_WRITE_REQUESTS_XPATH,
                        HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS,
                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS));

                setIfFound(orderXML, H1HeritrixTemplate.WARCS_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS_XPATH,
                        HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS,
                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS));
                setIfFound(orderXML, H1HeritrixTemplate.WARCS_WRITE_REVISIT_FOR_NOT_MODIFIED_XPATH,
                        HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED,
                        Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED));

            } else {
                throw new IllegalState("Unable to choose WARC as Heritrix archive format because "
                        + " one of the following xpaths are invalid in the given order.xml: "
                        + H1HeritrixTemplate.WARCSDIR_XPATH + "," + H1HeritrixTemplate.WARCS_ENABLED_XPATH
                        + ". order.xml: " + orderXML.asXML());
            }

        } else {
            throw new IllegalState("Unknown state: "
                    + "Should have selected either ARC or WARC as heritrix archive format");
        }
    }

    private static void setIfFound(Document doc, String Xpath, String param, String value) {
        if (doc.selectSingleNode(Xpath) != null) {
            XmlUtils.setNode(doc, Xpath, value);
        } else {
            log.warn("Could not replace setting value of '" + param + "' in template. Xpath not found: " + Xpath);
        }
    }

    /**
     * @param maxJobRunningTime Force the harvestjob to end after maxJobRunningTime
     */
    public static void editOrderXML_maxJobRunningTime(Document orderXMLdoc, long maxJobRunningTime) {
        // get and set the "max-time-sec" node of the orderXMLdoc
        String xpath = H1HeritrixTemplate.MAXTIMESEC_PATH_XPATH;
        Node groupMaxTimeSecNode = orderXMLdoc.selectSingleNode(xpath);
        if (groupMaxTimeSecNode != null) {
            String currentMaxTimeSec = groupMaxTimeSecNode.getText();
            groupMaxTimeSecNode.setText(Long.toString(maxJobRunningTime));
            log.trace("Value of groupMaxTimeSecNode changed from " + currentMaxTimeSec + " to " + maxJobRunningTime);
        } else {
            throw new IOFailure("Unable to locate xpath '" + xpath + "' in the order.xml: " + orderXMLdoc.asXML());
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
    public static void editOrderXML_maxObjectsPerDomain(Document orderXMLdoc, 
    		long forceMaxObjectsPerDomain,
            boolean maxObjectsIsSetByQuotaEnforcer) {
    	Map<String,String> env = new HashMap<String,String>();
    	
    	String QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE = "QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE"; 
    	String FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE = "FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE";
    	Long FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT = -1L;
    	
    	if (maxObjectsIsSetByQuotaEnforcer) {
    		env.put(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE, 
    				String.valueOf(forceMaxObjectsPerDomain));
    		env.put(FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE, 
    				String.valueOf(FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT));
    	}
    	
    	
    	
    	/*
    	
        String xpath = (maxObjectsIsSetByQuotaEnforcer ? H1HeritrixTemplate.GROUP_MAX_FETCH_SUCCESS_XPATH
                : H1HeritrixTemplate.QUEUE_TOTAL_BUDGET_XPATH);

        
        Node orderXmlNode = orderXMLdoc.selectSingleNode(xpath);
        if (orderXmlNode != null) {
            orderXmlNode.setText(String.valueOf(forceMaxObjectsPerDomain));
        } else {
            throw new IOFailure("Unable to locate " + xpath + " element in order.xml: " + orderXMLdoc.asXML());
        }
        
        */
        
        
    }
/*
    Map<String, String> env = new HashMap<String, String>();
    env.put("machineparameters", app.getMachineParameters().writeJavaOptions());
    env.put("classpath", osGetClassPath(app));
    env.put("confdirpath", ScriptConstants.doubleBackslashes(getConfDirPath()));
    env.put("id", id);
    env.put("appname", app.getTotalName());
    env.put("killpsname", killPsName);
    env.put("tmprunpsname", tmpRunPsName);
    env.put("startlogname", startLogName);
    if (inheritedSlf4jConfigFile != null) {
        env.put("slf4jlogger", Template.untemplate(windowsStartVbsScriptTpl.slf4jLogger, env, true));
    } else {
        env.put("slf4jlogger", "");
    }
    if (app.getTotalName().contains(ScriptConstants.BITARCHIVE_APPLICATION_NAME)) {
        env.put("securityManagement",
                Template.untemplate(windowsStartVbsScriptTpl.securityManagement, env, true));
    } else {
        env.put("securityManagement", "");
    }
    String str = Template.untemplate(windowsStartVbsScriptTpl.mainScript, env, true, "\r\n");
    vbsPrint.print(str);
    
*/    
    
    
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

        String xpath = H1HeritrixTemplate.QUOTA_ENFORCER_ENABLED_XPATH;
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
    public static void editOrderXML_maxBytesPerDomain(Document orderXMLdoc, long forceMaxBytesPerDomain) {
        // get and set the group-max-all-kb Node of the orderXMLdoc:
        String xpath = H1HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH;
        Node groupMaxSuccessKbNode = orderXMLdoc.selectSingleNode(xpath);
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
            throw new IOFailure("Unable to locate QuotaEnforcer object in order.xml: " + orderXMLdoc.asXML());
        }
    }

    /**
     * Return true if the given order.xml file has deduplication enabled.
     *
     * @param doc An order.xml document
     * @return True if Deduplicator is enabled.
     */
    public static boolean isDeduplicationEnabledInTemplate(Document doc) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        Node xpathNode = doc.selectSingleNode(H1HeritrixTemplate.DEDUPLICATOR_ENABLED);
        return xpathNode != null && xpathNode.getText().trim().equals("true");
    }

    
	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		//FIXME always returns true, currently
		return true;
	}

	@Override
	public void setMaxBytesPerDomain(Long maxbytesL) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getMaxBytesPerDomain() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMaxObjectsPerDomain(Long maxobjectsL) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getMaxObjectsPerDomain() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean IsDeduplicationEnabled() {
		// TODO Auto-generated method stub
		return false;
	}	
	
	

	@Override
	public void configureQuotaEnforcer(boolean maxObjectsIsSetByQuotaEnforcer,
			long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setArchiveFormat(String archiveFormat) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insertCrawlerTraps(String elementName, List<String> crawlertraps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeTemplate(OutputStream os) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasContent() {
		throw new NotImplementedException("The hasContent method hasn't been implemented yet");
	}

	@Override
	public void writeToFile(File orderXmlFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRecoverlogNode(File recoverlogGzFile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeduplicationIndexLocation(String absolutePath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSeedsFilePath(String absolutePath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setArchiveFilePrefix(String archiveFilePrefix) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDiskPath(String absolutePath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDeduplicatorIfPresent() {
		//NOP
	}

//<property name="metadataItems">
//  <map>
//        <entry key="harvestInfo.version" value="1.03"/> <!-- TODO maybe not add this one -->
//        <entry key="harvestInfo.jobId" value="1"/>
//        <entry key="harvestInfo.channel" value="HIGH"/>
//        <entry key="harvestInfo.harvestNum" value="1"/>
//        <entry key="harvestInfo.origHarvestDefinitionID" value="1"/>
//        <entry key="harvestInfo.maxBytesPerDomain" value="100000"/>
//        <entry key="harvestInfo.maxObjectsPerDomain" value="-1"/>
//        <entry key="harvestInfo.orderXMLName" value="defaultOrderXml"/>
//        <entry key="harvestInfo.origHarvestDefinitionName" value="ddddddddd"/>
//        <entry key="harvestInfo.scheduleName" value="EveryHour"/> <!-- Optional. only relevant for Selective Harvests -->
//        <entry key="harvestInfo.harvestFilenamePrefix" value="netarkivet-1-1"/>
//        <entry key="harvestInfo.jobSubmitDate" value="22. 10. 2014"/>
//        <entry key="harvestInfo.performer" value="performer"/> <!-- Optional. -->
//        <entry key="harvestInfo.audience" value="audience"/> <!-- Optional. -->
//  </map>
//  </property>

	public void insertWarcInfoMetadata(Job ajob, String origHarvestdefinitionName, 
			String scheduleName, String performer) {

		String startMetadataEntry = "<entry key=\"";
		String endMetadataEntry = "\"</>";
		String valuePart = "\"> value=\"";
		StringBuilder sb = new StringBuilder();
		sb.append("<property name=\"metadata-items\">\n<map>\n");

		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_VERSION + valuePart + HARVESTINFO_VERSION_NUMBER + endMetadataEntry); 
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_JOBID + valuePart + ajob.getJobID() + endMetadataEntry);

		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_CHANNEL + valuePart + ajob.getChannel() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_HARVESTNUM + valuePart + ajob.getHarvestNum() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_ORIGHARVESTDEFINITIONID + valuePart + ajob.getOrigHarvestDefinitionID() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_MAXBYTESPERDOMAIN + valuePart + ajob.getMaxBytesPerDomain() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_MAXOBJECTSPERDOMAIN + valuePart + ajob.getMaxObjectsPerDomain() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_ORDERXMLNAME + valuePart + ajob.getOrderXMLName() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_ORIGHARVESTDEFINITIONNAME + valuePart + 
				origHarvestdefinitionName + endMetadataEntry);
		/* optional schedule-name. */
		if (scheduleName != null) {
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_SCHEDULENAME + valuePart + scheduleName + endMetadataEntry);
		}
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_HARVESTFILENAMEPREFIX + valuePart + ajob.getHarvestFilenamePrefix() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_JOBSUBMITDATE + valuePart + ajob.getSubmittedDate() + endMetadataEntry);
		/* optional HARVESTINFO_PERFORMER */
		if (performer != null){
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_PERFORMER + valuePart + performer  + endMetadataEntry);
		}
		/* optional HARVESTINFO_PERFORMER */
		if (ajob.getHarvestAudience() != null) {
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_AUDIENCE + valuePart + ajob.getHarvestAudience() + endMetadataEntry);
		}
		sb.append("</map>\n");
	}
}
