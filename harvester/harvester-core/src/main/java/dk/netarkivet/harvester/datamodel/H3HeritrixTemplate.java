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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspWriter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.Template;
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
public class H3HeritrixTemplate extends HeritrixTemplate implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(H3HeritrixTemplate.class);

    private String template;  
    
    
    /** Has this HeritrixTemplate been verified. */
    private boolean verified;

    private final String METADATA_ITEMS_PLACEHOLDER = "METADATA_ITEMS_PLACEHOLDER";
    
    private List<String> crawlertraps = new ArrayList<String>();

    public static final String MAX_TIME_SECONDS_PLACEHOLDER = "MAX_TIME_SECONDS_PLACEHOLDER";
    public static final String CRAWLERTRAPS_PLACEHOLDER = "CRAWLERTRAPS_PLACEHOLDER";
    public static final String DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER = "DEDUPLICATION_INDEX_LOCATION"; 
    public static final String SEEDS_FILE_PATH_PLACEHOLDER = "SEEDS_FILE_PATH";
    public static final String ARCHIVE_FILE_PREFIX_PLACEHOLDER = "ARCHIVE_FILE_PREFIX";
	
    // PLACEHOLDERS for archiver beans
    
    final String ARCHIVER_BEAN_REFERENCE_PLACEHOLDER = "ARCHIVER_BEAN_REFERENCE_PLACEHOLDER";	
	final String ARCHIVE_PROCESSOR_BEAN_PLACEHOLDER = "ARCHIVE_PROCESSOR_BEAN_PLACEHOLDER";
	final String WARC_Write_Requests_PLACEHOLDER = "WARC_Write_Requests_PLACEHOLDER";
	final String WARC_Write_Metadata_PLACEHOLDER = "WARC_Write_Metadata_PLACEHOLDER";
	final String WARC_Write_RevisitForIdenticalDigests_PLACEHOLDER = "WARC_Write_RevisitForIdenticalDigests_PLACEHOLDER";
	final String WARC_Write_RevisitForNotModified_PLACEHOLDER = "WARC_Write_RevisitForNotModified_PLACEHOLDER";
	final String WARC_StartNewFilesOnCheckpoint_PLACEHOLDER = "WARC_StartNewFilesOnCheckpoint_PLACEHOLDER";
	final String WARC_SkipIdenticalDigests_PLACEHOLDER = "WARC_skipIdenticalDigests_PLACEHOLDER";    
    
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
    
    /**
     * Alternate constructor, taking a clob as an argument.
     * @param clob The template as SQL CLOB
     */
    public H3HeritrixTemplate(Clob clob) {

    	StringBuilder sb = new StringBuilder();
    	try {
    		Reader reader = clob.getCharacterStream();
    		BufferedReader br = new BufferedReader(reader);

    		String line;
    		while(null != (line = br.readLine())) {
    			sb.append(line);sb.append("\n");
    		}
    		br.close();
    	} catch (SQLException e) {
    		throw new IOFailure("SQLException occurred during the construction", e);
    	} catch (IOException e) {
    		throw new IOFailure("IOException occurred during the construction", e);
    	}
    	this.template = sb.toString();
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
     * Note that we insert the crawlertraps before resturning the xml.
     * @return HeritrixTemplate as XML
     */
    @Override
    public String getXML() {
        return writeCrawlerTrapsToTemplate();
    }

    /**
     * Method to add a list of crawler traps with a given element name. It is used both to add per-domain traps and
     * global traps.
     *
     * @param elementName The name of the added element. (not used by the H3 - template.
     * @param crawlerTraps A list of crawler trap regular expressions to add to this job.
     */
    public void editOrderXMLAddCrawlerTraps(String elementName, List<String> crawlerTraps) {        
    	// For now only add the crawlertraps to the list;
        this.crawlertraps.addAll(crawlerTraps);	
        System.out.println("Now the list of crawlertraps contain " +  this.crawlertraps.size() + " traps");
    }
        
    private String writeCrawlerTrapsToTemplate() {
    	//
//      <bean class="org.archive.modules.deciderules.MatchesListRegexDecideRule">
//      <!-- <property name="listLogicalOr" value="true" /> -->
//      <!-- <property name="regexList">
//            <list>
//            CRAWLERTRAPS_PLACEHOLDER 
//            </list>
//           </property> -->
//     </bean>
    	if (!template.contains(CRAWLERTRAPS_PLACEHOLDER)) {
    		// TODO Log and return from method instead of throwing an exception????
    		throw new IllegalState("Crawlertraps has already been inserted");
    	}
    	StringBuilder sb = new StringBuilder();
    	for (String trap: crawlertraps) {
    		sb.append("<value>" + trap + "</value>\n");
    	}
    	Map<String,String> env = new HashMap<String,String>();
        env.put(CRAWLERTRAPS_PLACEHOLDER, sb.toString());
    	boolean bFailOnMissing = true;
    	return Template.untemplate(template, env, bFailOnMissing);
    }

    /**
     * Update the maxTimeSeconds property in the heritrix3 template, if possible.
     * @param maxJobRunningTimeSecondsL Force the harvestjob to end after this number of seconds 
     * Property of the org.archive.crawler.framework.CrawlLimitEnforcer
     * <!-- <property name="maxTimeSeconds" value="0" /> -->
     */
    @Override
	public void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL) {
		if (template.contains(MAX_TIME_SECONDS_PLACEHOLDER)) {
			Map<String,String> env = new HashMap<String,String>();
	        env.put(MAX_TIME_SECONDS_PLACEHOLDER, Long.toString(maxJobRunningTimeSecondsL));
	    	boolean bFailOnMissing = true;
	    	this.template = Template.untemplate(template, env, bFailOnMissing);
		} else {
		   // LOG WARNING
		}
	}
    
    final String QUOTA_ENFORCER_MAX_BYTES_TEMPLATE = "QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE";

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
    public void editOrderXML_maxObjectsPerDomain( 
    		long forceMaxObjectsPerDomain,
            boolean maxObjectsIsSetByQuotaEnforcer) {
    	Map<String,String> env = new HashMap<String,String>();
    	
    	String QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE = "QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE"; 
    	String FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE = "FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE";
    		
    	
    	Long FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT = -1L;
    	Long QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_DEFAULT = -1L;
    	
    	if (maxObjectsIsSetByQuotaEnforcer) {
    		env.put(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE, 
    				String.valueOf(forceMaxObjectsPerDomain));
    		env.put(FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE, 
    				String.valueOf(FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT));
    	} else {
    		env.put(FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE, 
    				String.valueOf(forceMaxObjectsPerDomain));
    		env.put(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE,
    				String.valueOf(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_DEFAULT));
    	}
    	
    	boolean bFailOnMissing = false;
    	template = Template.untemplate(template, env, bFailOnMissing);
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
//  
	@Override
	public void setMaxBytesPerDomain(Long maxbytesL) {
		String maxBytesStringValue = "";

		if (maxbytesL == 0) {
			maxBytesStringValue = "0";
		} else if (maxbytesL != Constants.HERITRIX_MAXBYTES_INFINITY) {
          // Divide by 1024 since Heritrix uses KB rather than bytes,
          // and add 1 to avoid to low limit due to rounding.
    	  maxBytesStringValue = Long.toString((maxbytesL / Constants.BYTES_PER_HERITRIX_BYTELIMIT_UNIT) + 1);
		} else {
    	  maxBytesStringValue = String.valueOf(Constants.HERITRIX_MAXBYTES_INFINITY);
		}

		if (template.contains(QUOTA_ENFORCER_MAX_BYTES_TEMPLATE)) {
			// Insert value
			
		} else {
	      throw new IOFailure("Unable to locate QuotaEnforcer template to set maxBytesPerDomain in template: " + template);
	  }
	}	
  

	@Override
	public Long getMaxBytesPerDomain() {
		throw new NotImplementedException("This method has not yet been implemented");
	}

	@Override
	public void setMaxObjectsPerDomain(Long maxobjectsL) {
		Map<String,String> env = new HashMap<String,String>();
    	
    	String QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE = "QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE"; 
    	String FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE = "FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE";
    		
    	Long FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT = -1L;
    	//Long QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_DEFAULT = -1L;
        env.put(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_TEMPLATE, 
    				String.valueOf(maxobjectsL));
    	env.put(FRONTIER_QUEUE_TOTAL_BUDGET_TEMPLATE, 
    				String.valueOf(FRONTIER_QUEUE_TOTAL_BUDGET_DEFAULT));
    	
    	
    	boolean bFailOnMissing = false;
    	template = Template.untemplate(template, env, bFailOnMissing);
		
	}

	@Override
	public Long getMaxObjectsPerDomain() {
		// TODO Auto-generated method stub
		throw new NotImplementedException("This method has not yet been implemented");
		//return null;
	}
    
	@Override
	public boolean isValid() {
		//always returns true, currently
		return true;
	}

	@Override
	// This method is used to decide, whether to request a deduplication index or not.
	public boolean IsDeduplicationEnabled() {
		// LOOK for the string DEDUPLICATION_INDEX_LOCATION or the pattern '<bean id="DeDuplicator"'
		String deduplicationBeanPattern =  "<bean id=\"DeDuplicator\"";
		if (template.contains(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER) || template.contains(deduplicationBeanPattern)) { 
			return true;
		} else {
			return false;
		}
	}	

	/**
     * Activates or deactivate the quota-enforcer, depending on budget definition. Object limit can be defined either by
     * using the queue-total-budget property or the quota enforcer. Which is chosen is set by the argument
     * maxObjectsIsSetByQuotaEnforcer}'s value. So quota enforcer is set as follows:
     * <ul>
     * <li>Object limit is not set by quota enforcer, disabled only if there is no byte limit.</li>
     * <li>Object limit is set by quota enforcer, so it should be enabled if a byte or object limit is set.</li>
     * </ul>
     *
     * @param maxObjectsIsSetByQuotaEnforcer Decides whether the maxObjectsIsSetByQuotaEnforcer or not.
     * @param forceMaxBytesPerDomain The number of max bytes per domain enforced (can be no limit)
     * @param forceMaxObjectsPerDomain The number of max objects per domain enforced (can be no limit)
     */
	@Override
	public void configureQuotaEnforcer(boolean maxObjectsIsSetByQuotaEnforcer,
			long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain) {
		// TODO Auto-generated method stub
		// FIXME see code elsewhere
		boolean quotaEnabled = true;

    	if (!maxObjectsIsSetByQuotaEnforcer) { // We use the frontier globalBudget instead
            // If the Object limit is not set by quota enforcer, it should be disabled 
            // 	iff there is no byte limit (i.e. the maxBytes is infinite)
            quotaEnabled = forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;

        } else {
            // Object limit is set by quota enforcer, so it should be enabled whether
            // a byte or object limit is set.
            quotaEnabled = forceMaxObjectsPerDomain != Constants.HERITRIX_MAXOBJECTS_INFINITY
                    || forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY;
        }

    	//FIXME this method only decides when to disable the QuotaEnforcer, not 
    	// what is should contain. 
    	// Add a Quota class 

    	if (quotaEnabled) {
    	 	// FIXME insert quota-enforcer beans into the cxml-file or not depending on the Jobs values
        	// or whether or not the there are quota-enforcer templates in the cxml-file.    		
    	}
		
	}

	 /**
     * Make sure that Heritrix will archive its data in the chosen archiveFormat.
     *
     * @param archiveFormat the chosen archiveformat ('arc' or 'warc' supported)
     * @throw ArgumentNotValid If the chosen archiveFormat is not supported.
     */
	@Override
	public void setArchiveFormat(String archiveFormat) {		

		if ("arc".equalsIgnoreCase(archiveFormat)) {
			log.debug("ARC format selected to be used by Heritrix");
			setArcArchiveformat();
		} else if ("warc".equalsIgnoreCase(archiveFormat)) {
			log.debug("WARC format selected to be used by Heritrix");
			setWarcArchiveformat();
		} else {
			throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.HERITRIX_ARCHIVE_FORMAT
					+ "' is invalid! Unrecognized format '" + archiveFormat + "'.");
		}
	}

	/**
	 * Set the archiveformat as ARC. This means enabling the ARCWriterProcessor in the template
	 */
	private void setArcArchiveformat(){
    	boolean bFailOnMissing = true;
    	Map<String,String> env = new HashMap<String,String>();
    	
    	String arcWriterbeanReference = "<ref bean=\"arcWriter\"/>";
    	env.put(ARCHIVER_BEAN_REFERENCE_PLACEHOLDER, arcWriterbeanReference);
    	env.put(ARCHIVE_PROCESSOR_BEAN_PLACEHOLDER, getArcWriterProcessor()); 
    	
    	this.template = Template.untemplate(template, env, bFailOnMissing);
    }
    		
  	private String getArcWriterProcessor() {
		
//      <bean id="arcWriter" class="org.archive.modules.writer.ARCWriterProcessor">
//  	  <!-- <property name="compress" value="true" /> -->
//  	  <!-- <property name="prefix" value="IAH" /> -->
//  	  <!-- <property name="suffix" value="${HOSTNAME}" /> -->
//  	  <!-- <property name="maxFileSizeBytes" value="100000000" /> -->
//  	  <!-- <property name="poolMaxActive" value="1" /> -->
//  	  <!-- <property name="poolMaxWaitMs" value="300000" /> -->
//  	  <!-- <property name="skipIdenticalDigests" value="false" /> -->
//  	  <!-- <property name="maxTotalBytesToWrite" value="0" /> -->
//  	  <!-- <property name="directory" value="." /> -->
//  	  <!-- <property name="storePaths">
//  	        <list>
//  	         <value>arcs</value>
//  	        </list>
//  	       </property> -->
//  	 </bean>
  	   //TODO add some properties as properties defined at the front of the arcwriter
  	   // 
  	   String arcWriterBean 
  	   	= "<bean id=\"arcWriter\" class=\"org.archive.modules.writer.ARCWriterProcessor\">";
  	   arcWriterBean += "</bean>"; 
  	   return arcWriterBean;  			      
  	}

		
  	/** 
  	 * Insert WARC-archiver beans and remove placeholder for ARC-Archiver-beans
  	 * It is an error, if the WARC place-holders doesnt't exist.
  	 * It is not an error, if the property placeholder does not exist.
  	 */
  	private void setWarcArchiveformat() {
  		String warcWriterbeanReference = "<ref bean=\"warcWriter\"/>";
  		String warcWriterProcessorBean = "<bean id=\"warcWriter\" class=\"dk.netarkivet.harvester.harvesting.NasWARCProcessor\">";
  		warcWriterProcessorBean += "METADATA_ITEMS\n</bean>"; 
  		String propertyPrefix = "warcWriter.";
  		Map<String,String> envMandatory = new HashMap<String,String>();
  		envMandatory.put(ARCHIVER_BEAN_REFERENCE_PLACEHOLDER, warcWriterbeanReference);
  		envMandatory.put(ARCHIVE_PROCESSOR_BEAN_PLACEHOLDER, warcWriterProcessorBean);
  		Map<String,String> envOptional = new HashMap<String,String>();
  		envOptional.put(WARC_Write_Requests_PLACEHOLDER, 
  				propertyPrefix + "writeRequests=" + 
  						Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS));
  		envOptional.put(WARC_Write_Metadata_PLACEHOLDER,
  				propertyPrefix + "writeMetadata=" +
  						Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA));
  		envOptional.put(WARC_Write_RevisitForIdenticalDigests_PLACEHOLDER,
  				propertyPrefix + "writeRevisitForIdenticalDigests=" +
  						Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS));
  		envOptional.put(WARC_Write_RevisitForNotModified_PLACEHOLDER,
  				propertyPrefix + "writeRevisitForNotModified=" +
  						Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED));

  		envOptional.put(WARC_SkipIdenticalDigests_PLACEHOLDER,
  				propertyPrefix + "skipIdenticalDigests=" +
  						Settings.get(HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS));

  		/* TODO
	    	envOptional.put(WARC_StartNewFilesOnCheckpoint_PLACEHOLDER,
	    			Settings.get(HarvesterSettings.HE)); 
	    			// Add a new setting to HarvesterSettings
  		 */
  		String templateClone = template;

  		templateClone = Template.untemplate(templateClone, envMandatory, true);
  		templateClone = Template.untemplate(templateClone, envOptional, false);
  		this.template = templateClone;
  	}	

	

	@Override
	/**
	 * DUPLICATE method
	 * With H3 template, we has to accumulate the crawlertraps, and then push them to the template when
	 * asked to write it to a file.
	 * The elementName is currently not used.
	 */
	public void insertCrawlerTraps(String elementName, List<String> crawlertraps) {
		editOrderXMLAddCrawlerTraps(elementName, crawlertraps);
	}

	@Override
	public void writeTemplate(OutputStream os) throws IOFailure {
		String templateWithTraps = writeCrawlerTrapsToTemplate();
		try {
			os.write(templateWithTraps.getBytes(Charset.forName("UTF-8")));
		} catch (IOException e) {
			throw new IOFailure("Unable to write template to outputstream", e);
		}
		
	}

	@Override
	public boolean hasContent() {
		throw new NotImplementedException("The hasContent method hasn't been implemented yet");
	}

	@Override
	public void writeToFile(File orderXmlFile) {
		String templateWithTraps = writeCrawlerTrapsToTemplate();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(orderXmlFile));
			writer.write(templateWithTraps);
		} catch(IOException e) {
			throw new IOFailure("Unable to write template to file '" + orderXmlFile.getAbsolutePath() + "'.", e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	@Override
	public void setRecoverlogNode(File recoverlogGzFile) {
		throw new NotImplementedException("This method has not yet been implemented");
		
	}

	@Override
	public void setDeduplicationIndexLocation(String absolutePath) {
		if (!template.contains(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the deduplication index location property '" +  DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER 
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value.");
		}
		boolean bFailOnMissing = true;
    	Map<String,String> env = new HashMap<String,String>();
    	
    	env.put(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER, absolutePath);
    	this.template = Template.untemplate(template, env, bFailOnMissing);
	}

	@Override
	public void setSeedsFilePath(String absolutePath) {
		if (!template.contains(SEEDS_FILE_PATH_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the seeds file path property '" +  SEEDS_FILE_PATH_PLACEHOLDER 
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value.");
		}
		boolean bFailOnMissing = true;
    	Map<String,String> env = new HashMap<String,String>();
    	
    	env.put(SEEDS_FILE_PATH_PLACEHOLDER, absolutePath);
    	this.template = Template.untemplate(template, env, bFailOnMissing);
	}

	@Override
	public void setArchiveFilePrefix(String archiveFilePrefix) {
		if (!template.contains(ARCHIVE_FILE_PREFIX_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the archive file prefix property '" + ARCHIVE_FILE_PREFIX_PLACEHOLDER 
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value.");
		}
		boolean bFailOnMissing = true;
    	Map<String,String> env = new HashMap<String,String>();
    	
    	env.put(ARCHIVE_FILE_PREFIX_PLACEHOLDER, archiveFilePrefix);
    	this.template = Template.untemplate(template, env, bFailOnMissing);
		
	}

	@Override
	public void setDiskPath(String absolutePath) {
		// NOP
		log.warn("The DiskPath is not settable in the H3 template");
	}

	@Override
	public void removeDeduplicatorIfPresent() {
		//NOP
		log.warn("Removing the Deduplicator is not possible with the H3 templates and should not needed either there.");
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
		
		Map<String,String> envMandatory = new HashMap<String,String>();
		String templateClone = template;
		envMandatory.put(METADATA_ITEMS_PLACEHOLDER, sb.toString());
  		templateClone = Template.untemplate(templateClone, envMandatory, true);
	}
	
	@Override
	public void writeTemplate(JspWriter out) throws IOFailure {
		try {
			out.write(template);
		} catch (IOException e) {
			throw new IOFailure("Unable to write to JspWriter", e);
		}
		
	}
}
