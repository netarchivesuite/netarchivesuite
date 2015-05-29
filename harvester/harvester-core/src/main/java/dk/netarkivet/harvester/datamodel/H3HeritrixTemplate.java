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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.jsp.JspWriter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Class encapsulating the Heritrix crawler-beans.cxml file 
 * <p>
 * 
 * Heritrix3 has a new model based on spring, So the XPATH is no good for processing.
 * Instead we use placeholders instead, marked by %{..} instead of ${..}, which is used by 
 * Heritrix3 already.
 * 
 * The template is a H3 template if it contains the string: 
 * 
 * "xmlns="http://www.springframework.org/...."
 * 
 */
public class H3HeritrixTemplate extends HeritrixTemplate implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(H3HeritrixTemplate.class);

    private String template;
    
    /** QuotaEnforcer states for this template. TODO necessary?? */
    private Long forceMaxbytesPerDomain;
    private Long forceMaxobjectsPerDomain; 
   
    /** Has this HeritrixTemplate been verified. */
    private boolean verified;

    public final static String METADATA_ITEMS_PLACEHOLDER = "%{METADATA_ITEMS_PLACEHOLDER}";
    public static final String MAX_TIME_SECONDS_PLACEHOLDER = "%{MAX_TIME_SECONDS_PLACEHOLDER}";
    public static final String CRAWLERTRAPS_PLACEHOLDER = "%{CRAWLERTRAPS_PLACEHOLDER}";
    
    public static final String DEDUPLICATION_BEAN_REFERENCE_PATTERN = "<ref bean=\"DeDuplicator\"/>";
    public static final String DEDUPLICATION_BEAN_PATTERN =  "<bean id=\"DeDuplicator\"";
    public static final String DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER 
    	= "%{DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER}"; 

    public static final String ARCHIVE_FILE_PREFIX_PLACEHOLDER = "%{ARCHIVE_FILE_PREFIX_PLACEHOLDER}";
        
    public static final String FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER 
    	= "%{FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER}";
    
    public static final String QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER = 
    		"%{QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER}";
    
    public static final String QUOTA_ENFORCER_MAX_BYTES_PLACEHOLDER 
    	= "%{QUOTA_ENFORCER_MAX_BYTES_PLACEHOLDER}"; 
    
    
    // PLACEHOLDERS for archiver beans (Maybe not necessary)
    final String ARCHIVER_BEAN_REFERENCE_PLACEHOLDER = "%{ARCHIVER_BEAN_REFERENCE_PLACEHOLDER}";	
	final String ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER = "%{ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER}";
	
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
     * @return HeritrixTemplate as XML
     */
    @Override
    public String getXML() {
        return template;
    }
    
    /**
     * Update the maxTimeSeconds property in the heritrix3 template, if possible.
     * @param maxJobRunningTimeSecondsL Force the harvestJob to end after this number of seconds 
     * Property of the org.archive.crawler.framework.CrawlLimitEnforcer
     * <!-- <property name="maxTimeSeconds" value="0" /> -->
     */
    @Override
	public void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL) {
		if (template.contains(MAX_TIME_SECONDS_PLACEHOLDER)) {
	    	this.template = template.replace(MAX_TIME_SECONDS_PLACEHOLDER, 
	    			Long.toString(maxJobRunningTimeSecondsL));
		} else {
			log.warn("The placeholder '" + MAX_TIME_SECONDS_PLACEHOLDER 
					+ "' was not found in the template. Therefore maxRunningTime not set");
		}
	}

    
	@Override
	public void setMaxBytesPerDomain(Long maxbytesL) {
		this.forceMaxbytesPerDomain = maxbytesL;		
	}	
  

	@Override
	public Long getMaxBytesPerDomain() {
		return this.forceMaxbytesPerDomain;
	}

	@Override
	public void setMaxObjectsPerDomain(Long maxobjectsL) {
		this.forceMaxobjectsPerDomain = maxobjectsL;
	}

	@Override
	public Long getMaxObjectsPerDomain() {
		return this.forceMaxobjectsPerDomain;
	}
    
	@Override
	public boolean isValid() {
		/*
		StringBuilder errors = new StringBuilder();
		// check for Deduplication index-location placeholder and DEDUPLICATION_BEAN_PATTERN
		if (template.contains(DEDUPLICATION_BEAN_PATTERN)) {
			if (!template.contains(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER)) {
				errors.append("Has DefdMissing placeholder '" +  DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER + "'"
			}
		} 
		template.contains(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER) 
		&& template.contains(deduplicationBeanPattern)
		*/
		return true;
	}

	@Override
	// This method is used to decide, whether to request a deduplication index or not.
	// Done by checking, if both  
	//   - a DeDuplicator bean is present in the template
	// and
	//   - a  DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER is also present.
	// and 
	//   - a DeDuplicator reference bean is present in the template
	public boolean IsDeduplicationEnabled() {
		return (template.contains(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER) 
				&& template.contains(DEDUPLICATION_BEAN_PATTERN)
				&& template.contains(DEDUPLICATION_BEAN_REFERENCE_PATTERN)); 
	}	

	/**
     * Configuring the quota-enforcer, depending on budget definition. Object limit can be defined either by
     * using the queue-total-budget property or the quota enforcer. Which is chosen is set by the argument
     * maxObjectsIsSetByQuotaEnforcer}'s value. So quota enforcer is set as follows:
     * If all values in the quotaEnforcer is infinity, it is in effect disabled
     * <ul>
     * <li>Object limit is not set by quota enforcer, disabled only if there is no byte limit.</li>
     * <li>Object limit is set by quota enforcer, so it should be enabled if a byte or object limit is set.</li>
     * </ul>
     *
     * @param maxObjectsIsSetByQuotaEnforcer Decides whether the maxObjectsIsSetByQuotaEnforcer or not.
     * @param forceMaxBytesPerDomain The number of max bytes per domain enforced (can be no limit)
     * @param forceMaxObjectsPerDomain The number of max objects per domain enforced (can be no limit)
     */
	public void configureQuotaEnforcer(boolean maxObjectsIsSetByQuotaEnforcer,
			long forceMaxBytesPerDomain, long forceMaxObjectsPerDomain) {
		this.forceMaxobjectsPerDomain = forceMaxObjectsPerDomain;
		this.forceMaxbytesPerDomain = forceMaxBytesPerDomain;
		String tmp = template;
		if (!maxObjectsIsSetByQuotaEnforcer) {
			// SetMaxObjects in the global budget to forceMaxObjectsPerDomain??
			String tmp1 = tmp.replace(
					FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER, Long.toString( forceMaxObjectsPerDomain ));
			// SetMaxObjects to infinity in the quotaEnforcer
			tmp = tmp1.replace(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER, 
					Long.toString(Constants.HERITRIX_MAXOBJECTS_INFINITY));
		} else {
			// SetMaxObjects in the global budget to Infinity
			String tmp1 = tmp.replace(
					FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER, Long.toString( Constants.HERITRIX_MAXOBJECTS_INFINITY ));			
			// SetMaxObjects to forceMaxObjectsPerDomain in the quotaEnforcer
			tmp = tmp1.replace(QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER, 
					Long.toString(forceMaxObjectsPerDomain));
		}
		
		// SetMaxbytes in the QuotaEnforcer to forceMaxBytesPerDomain
		// Divide by 1024 since Heritrix uses KB rather than bytes,
		// and add 1 to avoid to low limit due to rounding.
		String maxBytesStringValue = "-1";
		if (forceMaxBytesPerDomain != Constants.HERITRIX_MAXBYTES_INFINITY) {
			maxBytesStringValue = Long.toString(( forceMaxBytesPerDomain 
					/ Constants.BYTES_PER_HERITRIX_BYTELIMIT_UNIT) + 1);
			log.debug("MaxbytesPerDomain set to {} Kbytes per domain", maxBytesStringValue);
		} else {
			log.debug("MaxbytesPerDomain set to infinite number of Kbytes per domain");	
		}
		
		this.template = tmp.replace(QUOTA_ENFORCER_MAX_BYTES_PLACEHOLDER, maxBytesStringValue);
		
	}
	
	 /**
     * Make sure that Heritrix will archive its data in the chosen archiveFormat.
     *
     * @param archiveFormat the chosen archiveformat ('arc' or 'warc' supported)
     * @throw ArgumentNotValid If the chosen archiveFormat is not supported.
     */
	@Override
	public void setArchiveFormat(String archiveFormat) {
		if (!template.contains(ARCHIVER_BEAN_REFERENCE_PLACEHOLDER)){
    		throw new IllegalState("The placeholder '" + ARCHIVER_BEAN_REFERENCE_PLACEHOLDER 
  					+ "' is missing. Unable to insert proper archive writer");
    	}
    	if (!template.contains(ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER)) {
  			throw new IllegalState("The placeholder '" + ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER 
  					+ "' is missing. Unable to insert proper archive writer");
  		}
		if ("arc".equalsIgnoreCase(archiveFormat)) {
			log.debug("ARC format selected to be used by Heritrix3");
			setArcArchiveformat();
		} else if ("warc".equalsIgnoreCase(archiveFormat)) {
			log.debug("WARC format selected to be used by Heritrix3");
			setWarcArchiveformat();
		} else {
			throw new ArgumentNotValid("Configuration of '" + HarvesterSettings.HERITRIX_ARCHIVE_FORMAT
					+ "' is invalid! Unrecognized format '" + archiveFormat + "'.");
		}
	}

	/**
	 * Set the archive-format as ARC. This means enabling the ARCWriterProcessor in the template
	 */
	private void setArcArchiveformat(){
		String arcWriterbeanReference = "<ref bean=\"arcWriter\"/>";
    	String templateNew = template.replace(ARCHIVER_BEAN_REFERENCE_PLACEHOLDER, arcWriterbeanReference);
    	template = templateNew.replace(ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER, getArcWriterProcessor()); 
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
// 
  	   String arcWriterBean 
  	   	= "<bean id=\"arcWriter\" class=\"org.archive.modules.writer.ARCWriterProcessor\">";
  	   // TODO Read compress value from heritrix3Settings
  	   arcWriterBean += "\n<property name=\"compress\" value=\"false\"/>"
  			 + "\n<property name=\"prefix\" value=\"" + ARCHIVE_FILE_PREFIX_PLACEHOLDER  
  	   		+ "\"/></bean>";
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
  		String propertyName="\n<property name=\"";
  		String valuePrefix = "\" value=\"";
  		String valueSuffix = "\"";
  		String propertyEnd="/>";
  		if (!template.contains(ARCHIVER_BEAN_REFERENCE_PLACEHOLDER)) {
  			throw new IllegalState("The placeholder '" + ARCHIVER_BEAN_REFERENCE_PLACEHOLDER 
  					+ "' is missing");
  		}
  		if (!template.contains(ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER)) {
  			throw new IllegalState("The placeholder '" + ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER 
  					+ "' is missing");
  		}
  		StringBuilder propertyBuilder = new StringBuilder();
  		// TODO Read template from Heritrix3Settings
  		propertyBuilder.append(propertyName + "template" + valuePrefix 
  				+ "${prefix}-${timestamp17}-${serialno}-${heritrix.hostname}"
  				// Default value: ${prefix}-${timestamp17}-${serialno}-${heritrix.pid}~${heritrix.hostname}~${heritrix.port}
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "compress" + valuePrefix + "false"  // TODO Replace false by Heritrix3Settingsvalue 
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "prefix" + valuePrefix 
  				+ ARCHIVE_FILE_PREFIX_PLACEHOLDER
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "writeRequests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REQUESTS)
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "writeMetadata" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_METADATA)
  				+ valueSuffix + propertyEnd);
 /*
  		propertyBuilder.append(propertyName + "writeRevisitForIdenticalDigests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_IDENTICAL_DIGESTS)
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "writeRevisitForNotModified" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_WRITE_REVISIT_FOR_NOT_MODIFIED)
  				+ valueSuffix + propertyEnd);
  */
  		propertyBuilder.append(propertyName + "skipIdenticalDigests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_SKIP_IDENTICAL_DIGESTS)
  				+ valueSuffix + propertyEnd);
 		propertyBuilder.append(		
  			  propertyName + "startNewFilesOnCheckpoint" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX_WARC_START_NEW_FILES_ON_CHECKPOINT)
  				+ valueSuffix + propertyEnd);
  		
  		warcWriterProcessorBean += propertyBuilder.toString();
  		warcWriterProcessorBean += "\n\n%{METADATA_ITEMS_PLACEHOLDER}\n</bean>";
  		String templateNew = template.replace(
  				ARCHIVER_BEAN_REFERENCE_PLACEHOLDER, warcWriterbeanReference);
  		this.template = templateNew.replace(ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER,
  				warcWriterProcessorBean);
   	}

	@Override
	/**
	 * With H3 template, we insert the crawlertraps into the template at once.
	 * They are inserted to be part of a org.archive.modules.deciderules.MatchesListRegexDecideRule
	 * bean.
	 * 
	 * @param elementName The elementName is currently not used with H3
	 * @param crawlertraps A list of crawlertraps to be inserted
	 */
	public void insertCrawlerTraps(String elementName, List<String> crawlertraps) {
//      <bean class="org.archive.modules.deciderules.MatchesListRegexDecideRule">
//      <!-- <property name="listLogicalOr" value="true" /> -->
//      <!-- <property name="regexList">
//            <list>
//            CRAWLERTRAPS_PLACEHOLDER 
//            </list>
//           </property> -->
//     </bean>
    	if (crawlertraps.isEmpty()) {
    		log.debug("No crawlertraps yet. No insertion is done");
    		return;
    	} else if (!template.contains(CRAWLERTRAPS_PLACEHOLDER)) {	
    		log.warn("The placeholder '" + CRAWLERTRAPS_PLACEHOLDER 
    				+ "' is absent from the template. No insertion is done at all. {} traps were ignored", 
    				crawlertraps);
    		return;
    	} else {
    		log.info("Inserting {} crawlertraps into the template", crawlertraps.size());
    		StringBuilder sb = new StringBuilder();
    		for (String trap: crawlertraps) {
    			sb.append("<value>" + trap + "</value>\n");
    		}
    		// Adding the placeholder again to be able to insert crawlertraps multiple times.
    		sb.append(CRAWLERTRAPS_PLACEHOLDER + "\n"); 
    		String templateNew = template.replace(CRAWLERTRAPS_PLACEHOLDER, sb.toString());
    		this.template = templateNew;
    	}
 	}

	@Override
	public void writeTemplate(OutputStream os) throws IOFailure {
		try {
			os.write(template.getBytes(Charset.forName("UTF-8")));
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
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter(orderXmlFile));
			writer.write(template);
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
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value: " 
					+ template);
		}
    	String templateNew = template.replace(DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER, absolutePath); 
    	this.template = templateNew;
	}

	@Override
	public void setSeedsFilePath(String absolutePath) {
	 log.debug("Note: SeedsFilePath is not set in h3");
	}

	@Override
	public void setArchiveFilePrefix(String archiveFilePrefix) {
		if (!template.contains(ARCHIVE_FILE_PREFIX_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the archive file prefix property '" 
					+ ARCHIVE_FILE_PREFIX_PLACEHOLDER 
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value. The template looks like this: " 
					+ template);
		}
		String templateNew = template.replace(ARCHIVE_FILE_PREFIX_PLACEHOLDER, archiveFilePrefix);		
    	this.template = templateNew;
		
	}

	@Override
	public void setDiskPath(String absolutePath) {
		// NOP
		log.warn("The DiskPath is not settable in the H3 template");
	}

	@Override
	public void removeDeduplicatorIfPresent() {
		//NOP
		log.warn("Removing the Deduplicator is not possible with the H3 templates and should not be required with the H3 template.");
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
		if (!template.contains(METADATA_ITEMS_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the property '" + METADATA_ITEMS_PLACEHOLDER  
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value. The template looks like this: " 
					+ template); 
		}
		String startMetadataEntry = "\n<entry key=\"";
		String endMetadataEntry = "\"/>";
		String valuePart = "\" value=\"";
		StringBuilder sb = new StringBuilder();
		sb.append("<property name=\"metadataItems\">\n<map>\n");
		
		// <entry key="harvestInfo.version" value="1.03"/>
		
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
		sb.append("\n</map>\n</property>\n");
		
		// Replace command
		String templateNew = template.replace(METADATA_ITEMS_PLACEHOLDER, sb.toString());
		this.template = templateNew;
	}
	
	@Override
	public void writeTemplate(JspWriter out) throws IOFailure {
		try {
			out.write(template);
		} catch (IOException e) {
			throw new IOFailure("Unable to write to JspWriter", e);
		}
	}
	
	/**
	 *  Hack to remove existing placeholders, that is still present after template 
	 *  manipulation is completed.
	 */
	public void removePlaceholders() {
		template = template.replace(METADATA_ITEMS_PLACEHOLDER, "");
		template = template.replace(CRAWLERTRAPS_PLACEHOLDER, "");
		
		if (template.contains(METADATA_ITEMS_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the property '" + METADATA_ITEMS_PLACEHOLDER  
					+ "' should have been deleted now."); 
		}
		if (template.contains(CRAWLERTRAPS_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the property '" + CRAWLERTRAPS_PLACEHOLDER  
					+ "' should have been deleted now."); 
		}		
	}
}
