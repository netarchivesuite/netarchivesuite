package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.List;

import javax.servlet.jsp.JspWriter;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;

/**
 * Abstract class for manipulating Heritrix Templates.
 *
 */
public abstract class HeritrixTemplate implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(HeritrixTemplate.class);

	private static final CharSequence H1_SIGNATURE = "<crawl-order xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance"; 
	//private static final CharSequence H3_SIGNATURE = "xmlns=\"http://www.springframework.org/";
	private static final CharSequence H3_SIGNATURE = "http://www.springframework.org/";

	/**
	 * Templates for which isActive is false will be hidden in the web-gui by default.
	 */
	private boolean isActive = true;

	// Constants for the metadata added to the warcinfo record when using WARC

	protected static final String HARVESTINFO_VERSION_NUMBER = "0.6";
	protected static final String HARVESTINFO_VERSION = "harvestInfo.version";
	protected static final String HARVESTINFO_JOBID = "harvestInfo.jobId";
	protected static final String HARVESTINFO_CHANNEL = "harvestInfo.channel";	
	protected static final String HARVESTINFO_HARVESTNUM = "harvestInfo.harvestNum";
	protected static final String HARVESTINFO_ORIGHARVESTDEFINITIONID = "harvestInfo.origHarvestDefinitionID";
	protected static final String HARVESTINFO_MAXBYTESPERDOMAIN = "harvestInfo.maxBytesPerDomain";
	protected static final String HARVESTINFO_MAXOBJECTSPERDOMAIN = "harvestInfo.maxObjectsPerDomain";
	protected static final String HARVESTINFO_ORDERXMLNAME = "harvestInfo.templateName";
	protected static final String HARVESTINFO_ORDERXMLUPDATEDATE = "harvestInfo.templateLastUpdateDate";
	protected static final String HARVESTINFO_ORDERXMLDESCRIPTION = "harvestInfo.templateDescription";
	protected static final String HARVESTINFO_ORIGHARVESTDEFINITIONNAME = "harvestInfo.origHarvestDefinitionName";
	protected static final String HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS = "harvestInfo.origHarvestDefinitionComments";
	protected static final String HARVESTINFO_SCHEDULENAME = "harvestInfo.scheduleName";
	protected static final String HARVESTINFO_HARVESTFILENAMEPREFIX = "harvestInfo.harvestFilenamePrefix";
	protected static final String HARVESTINFO_JOBSUBMITDATE = "harvestInfo.jobSubmitDate";
	protected static final String HARVESTINFO_PERFORMER = "harvestInfo.performer";
	protected static final String HARVESTINFO_OPERATOR = "harvestInfo.operator";
	protected static final String HARVESTINFO_AUDIENCE = "harvestInfo.audience";


	/** insertion-methods 
	 * 
	 * Two methods for adding domain quotas to the quotaEnforcer bean.
	 * maxBytesPerDomain()
	 * maxObjectsPerDomain()
	 * 
	 * One or two methods for inserting crawlertraps
	 * insertGlobalCrawlerTraps
	 * insertDomainSpecificCrawlerTraps 
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
	 * @param maxObjectsIsSetByQuotaEnforcer Decides whether the maxObjectsIsSetByQuotaEnforcer or not.
	 * @param forceMaxBytesPerDomain The number of max bytes per domain enforced (can be no limit)
	 * @param forceMaxObjectsPerDomain The number of max objects per domain enforced (can be no limit)
	 */
	public abstract void configureQuotaEnforcer(
			boolean maxObjectsIsSetByQuotaEnforcer, long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain);

	public boolean isActive() {
		return isActive;
	}

	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	// Getter/Setter for MaxBytesPerDomain value
	public abstract void setMaxBytesPerDomain(Long maxbytesL);
	public abstract Long getMaxBytesPerDomain(); // TODO Is necessary? 

	// Getter/Setter for MaxObjectsPerDomain value
	public abstract void setMaxObjectsPerDomain(Long maxobjectsL);
	public abstract Long getMaxObjectsPerDomain(); // TODO Is necessary? 

	/** We need the persistent template id if we want to attach any attributes to it. */
	public long template_id;

	/**
	 * 
	 * @return true, if deduplication is enabled in the template (used for determine whether or not to request a deduplication index from the indexserver)
	 */
	public abstract boolean IsDeduplicationEnabled();

	/**
	 * @return true, if the template is valid, otherwise false
	 */
	public abstract boolean isValid();

	/**
	 * @return the XML behind this template
	 */
	public abstract String getXML();

	/**
	 * Method to add a list of crawler traps with a given element name. It is used both to add per-domain traps and
	 * global traps.
	 *
	 * @param elementName The name of the added element.
	 * @param crawlertraps A list of crawler trap regular expressions to add to this job.
	 */
	public abstract void insertCrawlerTraps(String elementName, List<String> crawlertraps);

	/**
	 * Make sure that Heritrix will archive its data in the chosen archiveFormat.
	 *
	 * @param archiveFormat the chosen archiveformat ('arc' or 'warc' supported) Throws ArgumentNotValid If the chosen
	 * archiveFormat is not supported.
	 */
	public abstract void setArchiveFormat(String archiveFormat);


	/**
	 * Set the maxRunning time for the harvest
	 * @param maxJobRunningTimeSecondsL Limit the harvest to this number of seconds 
	 */
	public abstract void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL);

	/**
	 * Try to insert the given list of attributes into the template.
	 * @param attributesAndTypes
	 */
	public abstract void insertAttributes(List<AttributeAndType> attributesAndTypes);

	/**
	 * Updates the order.xml to include a MatchesListRegExpDecideRule for each crawler-trap associated with for the given
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
	public void editOrderXMLAddPerDomainCrawlerTraps(DomainConfiguration cfg) {
		List<String> crawlerTraps = cfg.getCrawlertraps();
		String elementName = cfg.getDomainName();
		int trapCount=crawlerTraps.size();
		for (String trap: crawlerTraps){
		    if (trap.isEmpty()) { // Ignore empty traps in the trapcount (NAS-2480)
		        log.warn("Found empty trap for domain {}", cfg.getDomainName());
		        trapCount--; 
		    }
		}
		if (trapCount > 0) {
			log.info("Inserting {} crawlertraps for domain '{}' into the template", crawlerTraps.size(), elementName);
			insertCrawlerTraps(elementName, crawlerTraps);
		}
	}

	public abstract void setDeduplicationIndexLocation(String absolutePath);
	public abstract void setSeedsFilePath(String absolutePath);

	public abstract void setArchiveFilePrefix(String archiveFilePrefix);
	public abstract void setDiskPath(String absolutePath);


	public abstract void writeTemplate(OutputStream os) throws IOException, ArgumentNotValid;
	public abstract void writeTemplate(JspWriter out);
	public abstract boolean hasContent();

	public abstract void writeToFile(File orderXmlFile);
	public abstract void setRecoverlogNode(File recoverlogGzFile);

	/**
	 * Construct a H1HeritrixTemplate or H3HeritrixTemplate based on the signature of the given string.
	 * @param template_id The id of the template
	 * @param templateAsString The template as a String object
	 * @return a HeritrixTemplate based on the signature of the given string.
	 */
	public static HeritrixTemplate getTemplateFromString(long template_id, String templateAsString){
		if (templateAsString.contains(H1_SIGNATURE)) {
			try {
				return new H1HeritrixTemplate(template_id, templateAsString);
			} catch (DocumentException e) {
				throw new IOFailure("Unable to recognize as a valid dom4j Document the following string: " 
						+ templateAsString, e);
			}
		} else if (templateAsString.contains(H3_SIGNATURE)) {
			return new H3HeritrixTemplate(template_id, templateAsString);
		} else {
			throw new ArgumentNotValid("The given template is neither H1 or H3: " + templateAsString);
		}
	}

	/** 
	 * Read the given template from file.
	 * @param orderXmlFile a given HeritrixTemplate (H1 or H3) as a File
	 * @return the given HeritrixTemplate (H1 or H3) as a HeritrixTemplate object
	 */
	public static HeritrixTemplate read(File orderXmlFile){
		try {
			return read(-1, new FileReader(orderXmlFile));
		} catch (FileNotFoundException e) {
			throw new IOFailure("The file '" + orderXmlFile.getAbsolutePath() + "' was not found", e);
		}
	}

	/**
	 * Read the template using the given Reader.
	 * 
	 * @param template_id The id of the template
	 * @param orderTemplateReader A given Reader to read a template
	 * @return a HeritrixTemplate object
	 */
	public static HeritrixTemplate read(long template_id, Reader orderTemplateReader) {
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(orderTemplateReader);
		String line;
		try {
			while ((line = in.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
		} catch (IOException e) {
			throw new IOFailure("IOException thrown", e);
		}
		return getTemplateFromString(template_id, sb.toString());
	}


	/**
	 * Try to remove the deduplicator, if present in the template.
	 */
	public abstract void removeDeduplicatorIfPresent();

	/**
	 *
	 */
	public abstract void enableOrDisableDeduplication(boolean enabled);

	/**
	 * Method to add settings to the WARCWriterProcesser, so that it can generate a proper WARCINFO record. 
	 * @param ajob a HarvestJob
	 * @param origHarvestdefinitionName The name of the harvestdefinition behind this job
	 * @param scheduleName The name of the schedule used. (Will be null, if the job is not a selectiveHarvest).
	 * @param performer The name of organisation/person doing this harvest 
	 */
	public abstract void insertWarcInfoMetadata(Job ajob,
			String origHarvestdefinitionName, String origHarvestdefinitionComments,
			String scheduleName, String performer);

}
