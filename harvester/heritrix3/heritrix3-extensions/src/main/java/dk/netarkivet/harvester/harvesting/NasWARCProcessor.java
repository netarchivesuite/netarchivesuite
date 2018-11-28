package dk.netarkivet.harvester.harvesting;

import static org.archive.format.warc.WARCConstants.TYPE;
import static org.archive.modules.CoreAttributeConstants.A_FTP_FETCH_STATUS;
import static org.archive.modules.CoreAttributeConstants.A_SOURCE_TAG;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.archive.format.warc.WARCConstants.WARCRecordType;
import org.archive.io.warc.WARCRecordInfo;
import org.archive.io.warc.WARCWriter;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.writer.WARCWriterProcessor;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom NAS WARCWriterProcessor addding NetarchiveSuite metadata to the WARCInfo records written
 * by Heritrix by just extending the org.archive.modules.writer.WARCWriterProcessor;
 * This was not possible in H1.
 * @author svc 
 * 
 */
public class NasWARCProcessor extends WARCWriterProcessor {

    /** Logger instance. */
    private static final Logger logger = LoggerFactory.getLogger(NasWARCProcessor.class);


    // Constants for the contents of the WarcInfo record
	private static final String HARVESTINFO_VERSION = "harvestInfo.version";
	private static final String HARVESTINFO_JOBID = "harvestInfo.jobId";
	private static final String HARVESTINFO_CHANNEL = "harvestInfo.channel";
	private static final String HARVESTINFO_HARVESTNUM = "harvestInfo.harvestNum";
	private static final String HARVESTINFO_ORIGHARVESTDEFINITIONID = "harvestInfo.origHarvestDefinitionID";
	private static final String HARVESTINFO_MAXBYTESPERDOMAIN = "harvestInfo.maxBytesPerDomain";
	private static final String HARVESTINFO_MAXOBJECTSPERDOMAIN = "harvestInfo.maxObjectsPerDomain";
	private static final String HARVESTINFO_ORDERXMLNAME = "harvestInfo.templateName";
	private static final String HARVESTINFO_ORDERXMLUPDATEDATE = "harvestInfo.templateLastUpdateDate";
	private static final String HARVESTINFO_ORDERXMLDESCRIPTION = "harvestInfo.templateDescription";
	private static final String HARVESTINFO_ORIGHARVESTDEFINITIONNAME = "harvestInfo.origHarvestDefinitionName";
	private static final String HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS = "harvestInfo.origHarvestDefinitionComments";
	private static final String HARVESTINFO_SCHEDULENAME = "harvestInfo.scheduleName";
	private static final String HARVESTINFO_HARVESTFILENAMEPREFIX = "harvestInfo.harvestFilenamePrefix";
	private static final String HARVESTINFO_JOBSUBMITDATE = "harvestInfo.jobSubmitDate";
	private static final String HARVESTINFO_PERFORMER = "harvestInfo.performer";
	private static final String HARVESTINFO_OPERATOR = "harvestInfo.operator";
	private static final String HARVESTINFO_AUDIENCE = "harvestInfo.audience";

	public boolean getWriteMetadataOutlinks() {
        return (Boolean) kp.get("writeMetadataOutlinks");
    }
    public void setWriteMetadataOutlinks(boolean writeMetadataOutlinks) {
        kp.put("writeMetadataOutlinks",writeMetadataOutlinks);
    }

    List<String> cachedMetadata;

	public NasWARCProcessor() {
		super();
	}
	
	 /**
     * metadata items.
     * Add to bean WARCProcessor bean as as
     * <property name="metadataItems"> 
     * <map>
     * 	<entry key="harvestInfo.version" value="0.6"/>
	 *	<entry key="harvestInfo.jobId" value="23"/>
	 *  <entry key="harvestInfo.channel" value="FOCUSED"/>
	 * ...	
     * </map>

     */
    protected Map<String,String> metadataMap = new HashMap<String,String>();

    public Map<String,String> getFormItems() {
        return this.metadataMap;
    }
    public void setMetadataItems(Map<String,String> metadataItems) {
        this.metadataMap = metadataItems;
    }

	
	@Override
	public List<String> getMetadata() {
        if (cachedMetadata != null) {
            return cachedMetadata;
        }
        ANVLRecord record = new ANVLRecord();
        record.addLabelValue("software", "Heritrix/" +
                ArchiveUtils.VERSION + " http://crawler.archive.org");
        try {
            InetAddress host = InetAddress.getLocalHost();
            record.addLabelValue("ip", host.getHostAddress());
            record.addLabelValue("hostname", host.getCanonicalHostName());
        } catch (UnknownHostException e) {
            //logger.log(Level.WARNING,"unable top obtain local crawl engine host",e);
        }
        
        // conforms to ISO 28500:2009 as of May 2009
        // as described at http://bibnum.bnf.fr/WARC/ 
        // latest draft as of November 2008
        record.addLabelValue("format","WARC File Format 1.0"); 
        record.addLabelValue("conformsTo","http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1_latestdraft.pdf");
        
        // Get other values from metadata provider

        CrawlMetadata provider = getMetadataProvider();

        addIfNotBlank(record,"operator", provider.getOperator());
        addIfNotBlank(record,"publisher", provider.getOrganization());
        addIfNotBlank(record,"audience", provider.getAudience());
        addIfNotBlank(record,"isPartOf", provider.getJobName());
        // TODO: make date match 'job creation date' as in Heritrix 1.x
        // until then, leave out (plenty of dates already in WARC 
        // records
//            String rawDate = provider.getBeginDate();
//            if(StringUtils.isNotBlank(rawDate)) {
//                Date date;
//                try {
//                    date = ArchiveUtils.parse14DigitDate(rawDate);
//                    addIfNotBlank(record,"created",ArchiveUtils.getLog14Date(date));
//                } catch (ParseException e) {
//                    logger.log(Level.WARNING,"obtaining warc created date",e);
//                }
//            }
        addIfNotBlank(record,"description", provider.getDescription());
        addIfNotBlank(record,"robots", provider.getRobotsPolicyName().toLowerCase());

        addIfNotBlank(record,"http-header-user-agent",
                provider.getUserAgent());
        addIfNotBlank(record,"http-header-from",
                provider.getOperatorFrom());
        
        
        String netarchiveSuiteComment = "#added by NetarchiveSuite "
                + dk.netarkivet.common.Constants.getVersionString(false);
        ANVLRecord recordNAS = new ANVLRecord(); // Previously new ANVLRecord(7); 
        
        // Add the data from the metadataMap to the WarcInfoRecord  if it exists
        if (metadataMap == null) {
        	logger.warn("No NetarchiveSuite harvestInfo data available in the template");
        } else {
        	try {
    
        		recordNAS.addLabelValue(HARVESTINFO_VERSION, (String) metadataMap.get(HARVESTINFO_VERSION));
        		recordNAS.addLabelValue(HARVESTINFO_JOBID, (String) metadataMap.get(HARVESTINFO_JOBID));
        		recordNAS.addLabelValue(HARVESTINFO_CHANNEL, (String) metadataMap.get(HARVESTINFO_CHANNEL));
        		recordNAS.addLabelValue(HARVESTINFO_HARVESTNUM, (String) metadataMap.get(HARVESTINFO_HARVESTNUM));
        		recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONID,
        				(String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONID));
        		recordNAS.addLabelValue(HARVESTINFO_MAXBYTESPERDOMAIN,
        				(String) metadataMap.get(HARVESTINFO_MAXBYTESPERDOMAIN));

        		recordNAS.addLabelValue(HARVESTINFO_MAXOBJECTSPERDOMAIN,
        				(String) metadataMap.get(HARVESTINFO_MAXOBJECTSPERDOMAIN));
        		recordNAS.addLabelValue(HARVESTINFO_ORDERXMLNAME,
        				(String) metadataMap.get(HARVESTINFO_ORDERXMLNAME));
        		if (metadataMap.containsKey(HARVESTINFO_ORDERXMLUPDATEDATE)) {
        			recordNAS.addLabelValue(HARVESTINFO_ORDERXMLUPDATEDATE, (String) metadataMap.get(HARVESTINFO_ORDERXMLUPDATEDATE));
        		}
        		if (metadataMap.containsKey(HARVESTINFO_ORDERXMLDESCRIPTION)) {
        			recordNAS.addLabelValue(HARVESTINFO_ORDERXMLDESCRIPTION, (String) metadataMap.get(HARVESTINFO_ORDERXMLDESCRIPTION));
        		}
        		recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONNAME,
        				(String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONNAME));

        		if (metadataMap.containsKey(HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS)) {
        			recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS,
        					(String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONCOMMENTS));
        		}
        		
        		if (metadataMap.containsKey(HARVESTINFO_SCHEDULENAME)) {
        			recordNAS.addLabelValue(HARVESTINFO_SCHEDULENAME,
        					(String) metadataMap.get(HARVESTINFO_SCHEDULENAME));
        		}
        		recordNAS.addLabelValue(HARVESTINFO_HARVESTFILENAMEPREFIX,
        				(String) metadataMap.get(HARVESTINFO_HARVESTFILENAMEPREFIX));

        		recordNAS.addLabelValue(HARVESTINFO_JOBSUBMITDATE,
        				(String) metadataMap.get(HARVESTINFO_JOBSUBMITDATE));

        		if (metadataMap.containsKey(HARVESTINFO_PERFORMER)) {
        			recordNAS.addLabelValue(HARVESTINFO_PERFORMER,
        					(String) metadataMap.get(HARVESTINFO_PERFORMER));
        		}
        		if (metadataMap.containsKey(HARVESTINFO_OPERATOR)) {
        			recordNAS.addLabelValue(HARVESTINFO_OPERATOR, (String) metadataMap.get(HARVESTINFO_OPERATOR));
        		}

        		if (metadataMap.containsKey(HARVESTINFO_AUDIENCE)) {
        			recordNAS.addLabelValue(HARVESTINFO_AUDIENCE,
        					(String) metadataMap.get(HARVESTINFO_AUDIENCE));
        		}
        	} catch (Exception e) {
        		logger.warn("Error processing harvest info" , e);
        	}
        }

        // really ugly to return as List<String>, but changing would require 
        // larger refactoring
        cachedMetadata = Collections.singletonList(record.toString() 
        		+ netarchiveSuiteComment + "\n" + recordNAS.toString());
        return cachedMetadata;
    }
	
	/**
	 * modify default writeMetadata method to handle the write of outlinks
	 * in metadata or not
	 */
	@Override
	protected URI writeMetadata(final WARCWriter w,
            final String timestamp,
            final URI baseid, final CrawlURI curi,
            final ANVLRecord namedFields) 
    throws IOException {
	    WARCRecordInfo recordInfo = new WARCRecordInfo();
        recordInfo.setType(WARCRecordType.metadata);
        recordInfo.setUrl(curi.toString());
        recordInfo.setCreate14DigitDate(timestamp);
        recordInfo.setMimetype(ANVLRecord.MIMETYPE);
        recordInfo.setExtraHeaders(namedFields);
        recordInfo.setEnforceLength(true);
	    
        recordInfo.setRecordId(qualifyRecordID(baseid, TYPE, WARCRecordType.metadata.toString()));

        // Get some metadata from the curi.
        // TODO: Get all curi metadata.
        // TODO: Use other than ANVL (or rename ANVL as NameValue or use
        // RFC822 (commons-httpclient?).
        ANVLRecord r = new ANVLRecord();
        if (curi.isSeed()) {
            r.addLabel("seed");
        } else {
        	if (curi.forceFetch()) {
        		r.addLabel("force-fetch");
        	}
            if(StringUtils.isNotBlank(flattenVia(curi))) {
                r.addLabelValue("via", flattenVia(curi));
            }
            if(StringUtils.isNotBlank(curi.getPathFromSeed())) {
                r.addLabelValue("hopsFromSeed", curi.getPathFromSeed());
            }
            if (curi.containsDataKey(A_SOURCE_TAG)) {
                r.addLabelValue("sourceTag", 
                        (String)curi.getData().get(A_SOURCE_TAG));
            }
        }
        long duration = curi.getFetchCompletedTime() - curi.getFetchBeginTime();
        if (duration > -1) {
            r.addLabelValue("fetchTimeMs", Long.toString(duration));
        }
        
        if (curi.getData().containsKey(A_FTP_FETCH_STATUS)) {
            r.addLabelValue("ftpFetchStatus", curi.getData().get(A_FTP_FETCH_STATUS).toString());
        }

        if (curi.getRecorder() != null && curi.getRecorder().getCharset() != null) {
            r.addLabelValue("charsetForLinkExtraction", curi.getRecorder().getCharset().name());
        }
        
        for (String annotation: curi.getAnnotations()) {
            if (annotation.startsWith("usingCharsetIn") || annotation.startsWith("inconsistentCharsetIn")) {
                String[] kv = annotation.split(":", 2);
                r.addLabelValue(kv[0], kv[1]);
            }
        }

        //only if parameter is true, add the outlinks
        if (getWriteMetadataOutlinks() == true) {
        	// Add outlinks though they are effectively useless without anchor text.
            Collection<CrawlURI> links = curi.getOutLinks();
            if (links != null && links.size() > 0) {
                for (CrawlURI link: links) {
                    r.addLabelValue("outlink", link.getURI()+" "+link.getLastHop()+" "+link.getViaContext());
                }
            }
        }

        // TODO: Other curi fields to write to metadata.
        // 
        // Credentials
        // 
        // fetch-began-time: 1154569278774
        // fetch-completed-time: 1154569281816
        //
        // Annotations.
        
        byte [] b = r.getUTF8Bytes();
        recordInfo.setContentStream(new ByteArrayInputStream(b));
        recordInfo.setContentLength((long) b.length);
        
        w.writeRecord(recordInfo);
        
        return recordInfo.getRecordId();
    }
	
}
