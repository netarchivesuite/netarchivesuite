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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.JspWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;

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

    public static final Pattern DEDUPLICATION_BEAN_REFERENCE_PATTERN = Pattern.compile(".*ref.*bean.*DeDuplicator.*", Pattern.DOTALL);

    public static final Pattern DEDUPLICATION_BEAN_PATTERN =  Pattern.compile(".*bean.*id.*DeDuplicator.*", Pattern.DOTALL);
    public static final String DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER 
    	= "%{DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER}"; 

    public static final String ARCHIVE_FILE_PREFIX_PLACEHOLDER = "%{ARCHIVE_FILE_PREFIX_PLACEHOLDER}";
        
    public static final String FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER 
    	= "%{FRONTIER_QUEUE_TOTAL_BUDGET_PLACEHOLDER}";
    
    public static final String QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER = 
    		"%{QUOTA_ENFORCER_GROUP_MAX_FETCH_SUCCES_PLACEHOLDER}";
    
    public static final String QUOTA_ENFORCER_MAX_BYTES_PLACEHOLDER 
    	= "%{QUOTA_ENFORCER_MAX_BYTES_PLACEHOLDER}";

	public static final String DEDUPLICATION_ENABLED_PLACEHOLDER = "%{DEDUPLICATION_ENABLED_PLACEHOLDER}";
    
    
    // PLACEHOLDERS for archiver beans (Maybe not necessary)
    final String ARCHIVER_BEAN_REFERENCE_PLACEHOLDER = "%{ARCHIVER_BEAN_REFERENCE_PLACEHOLDER}";	
	final String ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER = "%{ARCHIVER_PROCESSOR_BEAN_PLACEHOLDER}";
	
	//match theses properties in crawler-beans.cxml to add them into harvestInfo.xml
	//for preservation purpose
	public enum MetadataInfo {
		TEMPLATE_DESCRIPTION("metadata\\.description=.+[\\r\\n]"),
		TEMPLATE_UPDATE_DATE("metadata\\.date=.+[\\r\\n]"),
		OPERATOR("metadata\\.operator=.+[\\r\\n]");
		
		private final String regex;
		
		private MetadataInfo(String regex) {
			this.regex = regex;
		}
		
		public String toString() {
			return this.regex;
		}
	};
	
	public Map<MetadataInfo, String> metadataInfoMap;
	
    /**
     * Constructor for HeritrixTemplate class.
     *
     * @param template_id The persistent id of the template in the database
     * @param template The template as String object
     * @throws ArgumentNotValid if template is null.
     */
    public H3HeritrixTemplate(long template_id, String template) {
        ArgumentNotValid.checkNotNull(template, "String template");
        this.template_id = template_id;
        this.template = template;
        
        metadataInfoMap = new HashMap<MetadataInfo, String> ();
        for(MetadataInfo metadataInfo : MetadataInfo.values()) {
            Pattern p = Pattern.compile(metadataInfo.regex);
            Matcher m = p.matcher(this.template);
            if(m.find()) {
    	        String operator = this.template.substring(m.start(), m.end()).trim();
    	        //return the value of the property after the =
    	        metadataInfoMap.put(metadataInfo, operator.split("=")[1]);
            }
        }
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
				&& DEDUPLICATION_BEAN_PATTERN.matcher(template).matches()
				&& DEDUPLICATION_BEAN_REFERENCE_PATTERN.matcher(template).matches());
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
     * @throws ArgumentNotValid If the chosen archiveFormat is not supported.
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
	    // "<bean id=\"arcWriter\" class=\"org.archive.modules.writer.ARCWriterProcessor\">";
	    String propertyName="\n<property name=\"";
	    String valuePrefix = "\" value=\"";
	    String valueSuffix = "\"";
	    String propertyEnd="/>";

	    StringBuilder arcWriterBeanBuilder = new StringBuilder();
	    arcWriterBeanBuilder.append("<bean id=\"arcWriter\" class=\"org.archive.modules.writer.ARCWriterProcessor\">\n");
	    arcWriterBeanBuilder.append(propertyName + "compress" + valuePrefix
	            + Settings.get(HarvesterSettings.HERITRIX3_ARC_COMPRESSION) 
	            + valueSuffix + propertyEnd); 
	    arcWriterBeanBuilder.append(propertyName + "prefix" + valuePrefix
	            + ARCHIVE_FILE_PREFIX_PLACEHOLDER
	            + valueSuffix + propertyEnd);
//	    arcWriterBeanBuilder.append(propertyName + "suffix" + valuePrefix
//	            + Settings.get(HarvesterSettings.HERITRIX3_ARC_SUFFIX) 
//	            + valueSuffix + propertyEnd); 
	    arcWriterBeanBuilder.append(propertyName + "maxFileSizeBytes" + valuePrefix
	            + Settings.get(HarvesterSettings.HERITRIX3_ARC_MAXSIZE) 
	            + valueSuffix + propertyEnd); 
	    arcWriterBeanBuilder.append(propertyName + "poolMaxActive" + valuePrefix
	            + Settings.get(HarvesterSettings.HERITRIX3_ARC_POOL_MAXACTIVE) 
	            + valueSuffix + propertyEnd); 
	    arcWriterBeanBuilder.append(propertyName + "skipIdenticalDigests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_ARC_SKIP_IDENTICAL_DIGESTS)
  				+ valueSuffix + propertyEnd);

	    arcWriterBeanBuilder.append("</bean>");

	    return arcWriterBeanBuilder.toString();  			      
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
  		propertyBuilder.append(propertyName + "template" + valuePrefix 
  		      + Settings.get(HarvesterSettings.HERITRIX3_WARC_TEMPLATE)
              + valueSuffix + propertyEnd);  				
  		propertyBuilder.append(propertyName + "compress" + valuePrefix 
  		      + Settings.get(HarvesterSettings.HERITRIX3_WARC_COMPRESSION) 
  				+ valueSuffix + propertyEnd);
  		// Note: The prefix value will be replaced later by the setArchiveFilePrefix() method
  		propertyBuilder.append(propertyName + "prefix" + valuePrefix 
  				+ ARCHIVE_FILE_PREFIX_PLACEHOLDER
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "maxFileSizeBytes" + valuePrefix
  		      + Settings.get(HarvesterSettings.HERITRIX3_WARC_MAXSIZE)
  		      + valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "poolMaxActive" + valuePrefix
                + Settings.get(HarvesterSettings.HERITRIX3_WARC_POOL_MAXACTIVE)
                + valueSuffix + propertyEnd);
          
  		propertyBuilder.append(propertyName + "writeRequests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_WARC_WRITE_REQUESTS)
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "writeMetadata" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_WARC_WRITE_METADATA)
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "writeMetadataOutlinks" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_WARC_WRITE_METADATA_OUTLINKS)
  				+ valueSuffix + propertyEnd);
  		propertyBuilder.append(propertyName + "skipIdenticalDigests" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_WARC_SKIP_IDENTICAL_DIGESTS)
  				+ valueSuffix + propertyEnd);
 		propertyBuilder.append(propertyName + "startNewFilesOnCheckpoint" + valuePrefix 
  				+ Settings.get(HarvesterSettings.HERITRIX3_WARC_START_NEW_FILES_ON_CHECKPOINT)
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
	
	public String getMetadataInfo(MetadataInfo info) {
		String infoStr = null;
		if(metadataInfoMap.containsKey(info)) {
			infoStr = metadataInfoMap.get(info);
		}
		return infoStr;
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
		log.debug("In H3 we don't remove the deduplicator, but just disable it.");
	}

	@Override public void enableOrDisableDeduplication(boolean enabled) {
		final String replacement = Boolean.toString(enabled).toLowerCase();
		log.debug("Replacing deduplication enabled placeholder {} with {}.", DEDUPLICATION_ENABLED_PLACEHOLDER, replacement);
		this.template = template.replace(DEDUPLICATION_ENABLED_PLACEHOLDER, replacement);
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
//        <entry key="harvestInfo.scheduleName" value="EveryHour"/> <!-- Optional. only relevant for Selective Harvests -- only inserted if not null and not-empty.->
//        <entry key="harvestInfo.harvestFilenamePrefix" value="netarkivet-1-1"/>
//        <entry key="harvestInfo.jobSubmitDate" value="22. 10. 2014"/>
//        <entry key="harvestInfo.performer" value="performer"/> <!-- Optional - only inserted if not null and not-empty. -->
//        <entry key="harvestInfo.audience" value="audience"/> <!-- Optional - only inserted if not null and not-empty. -->
//  </map>
//  </property>

	public void insertWarcInfoMetadata(Job ajob, String origHarvestdefinitionName, 
			String origHarvestdefinitionComments, String scheduleName, String performer) {
		if (!template.contains(METADATA_ITEMS_PLACEHOLDER)) {
			throw new IllegalState("The placeholder for the property '" + METADATA_ITEMS_PLACEHOLDER  
					+ "' was not found. Maybe the placeholder has already been replaced with the correct value. The template looks like this: " 
					+ template); 
		}
		log.debug("Now in " + getClass().getName());
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

		/* orderxml update date - only inserted if not null and not-empty. */
		/* take info from crawler-beans.cxml */
		String tmp = getMetadataInfo(MetadataInfo.TEMPLATE_UPDATE_DATE);
		if (tmp != null && !tmp.isEmpty()){
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_ORDERXMLUPDATEDATE + valuePart + tmp  + endMetadataEntry);
		}
		/* orderxml description - only inserted if not null and not-empty. */
		/* take info from crawler-beans.cxml */
		tmp = getMetadataInfo(MetadataInfo.TEMPLATE_DESCRIPTION);
		if (tmp != null && !tmp.isEmpty()){
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_ORDERXMLDESCRIPTION + valuePart + tmp  + endMetadataEntry);
		}

		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_ORIGHARVESTDEFINITIONNAME + valuePart + 
				origHarvestdefinitionName + endMetadataEntry);
		
		if(StringUtils.isNotEmpty(origHarvestdefinitionComments)) {
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS + valuePart + 
				origHarvestdefinitionComments + endMetadataEntry);
		}
		
		/* optional schedule-name - only inserted if not null and not-empty. */
		if (scheduleName != null && !scheduleName.isEmpty()) {
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_SCHEDULENAME + valuePart + scheduleName + endMetadataEntry);
		}
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_HARVESTFILENAMEPREFIX + valuePart + ajob.getHarvestFilenamePrefix() + endMetadataEntry);
		sb.append(startMetadataEntry);
		sb.append(HARVESTINFO_JOBSUBMITDATE + valuePart + ArchiveDateConverter.getWarcDateFormat().format(ajob.getSubmittedDate()) + endMetadataEntry);
		
		/* optional HARVESTINFO_PERFORMER - only inserted if not null and not-empty. */
		if (performer != null && !performer.isEmpty()){
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_PERFORMER + valuePart + performer  + endMetadataEntry);
		}
		
		/* optional OPERATOR - only inserted if not null and not-empty. */
		/* take info from crawler-beans.cxml */
		String operator = getMetadataInfo(MetadataInfo.OPERATOR);
		if (operator != null && !operator.isEmpty()){
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_OPERATOR + valuePart + operator  + endMetadataEntry);
		}
		
		/* optional HARVESTINFO_AUDIENCE - only inserted if not null and not-empty. */
		if (ajob.getHarvestAudience() != null && !ajob.getHarvestAudience().isEmpty()) {
			sb.append(startMetadataEntry);
			sb.append(HARVESTINFO_AUDIENCE + valuePart + ajob.getHarvestAudience() + endMetadataEntry);
		}
		sb.append("\n</map>\n</property>\n");
		
		// Replace command
		log.info("Adding WarcInfoMetadata " + sb.toString());
		String templateNew = template.replace(METADATA_ITEMS_PLACEHOLDER, sb.toString());
		this.template = templateNew;
	}

	@Override
	public void insertAttributes(List<AttributeAndType> attributesAndTypes) {
	    ArgumentNotValid.checkNotNull(attributesAndTypes, "List<AttributeAndType> attributesAndTypes");
	    for (AttributeAndType attributeAndType: attributesAndTypes) {
	        // initialize temp variables
	        Integer intVal = null;
	        String val = null;
	        AttributeTypeBase attributeType = attributeAndType.attributeType;
	        AttributeBase attribute = attributeAndType.attribute;

	        log.debug("Trying to insert the attribute {} into the template", attributeType.name);
	        switch (attributeType.viewtype) {
	        case 1:
	            if (attribute != null) {
	                intVal = attribute.getInteger();
	                log.debug("Read explicitly value for attribute '{}'", attributeType.name, intVal);
	            }
	            if (intVal == null && attributeType.def_int != null) {
	                intVal = attributeType.def_int;
	                log.debug("Viewtype 1 attribute '{}' not set explicitly. Using default value '{}'",  attributeType.name, intVal);
	            }
	            if (intVal != null) {
	                val = intVal.toString();
	            } else {
	                val = "";
	            }
	            log.info("Value selected for attribute {}: {}", attributeType.name, val);
	            break;
	        case 5:
	            if (attribute != null) {
	                intVal = attribute.getInteger();
	                log.debug("Read explicitly value for attribute '{}'", attributeType.name, intVal);
	            }
	            if (intVal == null && attributeType.def_int != null) {
	                intVal = attributeType.def_int;
	                log.debug("Viewtype 5 attribute '{}' not set explicitly. Using default value '{}'", attributeType.name, intVal);
	            }
	            if (intVal != null && intVal > 0) {
	                val = "true";
	            } else {
	                val = "false";
	            }
	            log.info("Value selected for attribute '{}': '{}'", attributeType.name, val);
	            break;
	        case 6:
	            if (attribute != null) {
	                intVal = attribute.getInteger();
	                log.debug("Read explicitly value for attribute '{}'", attributeType.name, intVal);
	            }
	            if (intVal == null && attributeType.def_int != null) {
	                intVal = attributeType.def_int;
	                log.debug("Viewtype 6 attribute '{}' not set explicitly. Using default value '{}'", attributeType.name, intVal);
	            }
	            if (intVal != null && intVal > 0) {
	                val = "obey";
	            } else {
	                val = "ignore";
	            }
	            log.info("Value selected for attribute '{}': '{}'", attributeType.name, val);
	            break;
	        }
	        String placeholder = "%{" + attributeType.name.toUpperCase() + "}";
	        if (template.contains(placeholder)) {
	            String templateNew = template.replace("%{" + attributeType.name.toUpperCase() + "}", val);
	            this.template = templateNew;
	        } else {
	            log.warn("Placeholder '{}' not found in template. Therefore not substituted by '{}' in this template", 
	                    placeholder, val); 
	        }
	    }
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
