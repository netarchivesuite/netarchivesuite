package dk.netarkivet.harvester.harvesting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.archive.modules.CrawlMetadata;
import org.archive.modules.writer.WARCWriterProcessor;
import org.archive.util.ArchiveUtils;
import org.archive.util.anvl.ANVLRecord;

/**
 * Custom NAS WARCWriterProcessor addding NetarchiveSuite metadata to the WARCInfo records written
 * by Heritrix by just extending the org.archive.modules.writer.WARCWriterProcessor;
 * This was not possible in H1.
 * @author svc 
 * 
 */
public class NasWARCProcessor extends WARCWriterProcessor {

	// Constants for the contents of the WarcInfo record
	private static final String HARVESTINFO_VERSION = "harvestInfo.version";
	private static final String HARVESTINFO_JOBID = "harvestInfo.jobId";
	private static final String HARVESTINFO_CHANNEL = "harvestInfo.channel";
	private static final String HARVESTINFO_HARVESTNUM = "harvestInfo.harvestNum";
	private static final String HARVESTINFO_ORIGHARVESTDEFINITIONID = "harvestInfo.origHarvestDefinitionID";
	private static final String HARVESTINFO_MAXBYTESPERDOMAIN = "harvestInfo.maxBytesPerDomain";
	private static final String HARVESTINFO_MAXOBJECTSPERDOMAIN = "harvestInfo.maxObjectsPerDomain";
	private static final String HARVESTINFO_ORDERXMLNAME = "harvestInfo.orderXMLName";
	private static final String HARVESTINFO_ORIGHARVESTDEFINITIONNAME = "harvestInfo.origHarvestDefinitionName";
	private static final String HARVESTINFO_SCHEDULENAME = "harvestInfo.scheduleName";
	private static final String HARVESTINFO_HARVESTFILENAMEPREFIX = "harvestInfo.harvestFilenamePrefix";
	private static final String HARVESTINFO_JOBSUBMITDATE = "harvestInfo.jobSubmitDate";
	private static final String HARVESTINFO_PERFORMER = "harvestInfo.performer";
	private static final String HARVESTINFO_AUDIENCE = "harvestInfo.audience";

	public NasWARCProcessor() {
		super();
	}
	
	List<String> cachedMetadata;
	
	 /**
     * metadata items.
     * Add to bean WARCProcessor bean as as
     * <property name="metadataItems"> 
     * <map>
     * 	<entry key="harvestInfo.version" value="0.5"/>
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
        record.addLabelValue("format","WARC File Format 1.1"); 
        record.addLabelValue("conformsTo","http://bibnum.bnf.fr/WARC/WARC_ISO_28500_version1-1_latestdraft.pdf");
        
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
                + dk.netarkivet.common.Constants.getVersionString();
        ANVLRecord recordNAS = new ANVLRecord(); // Previously new ANVLRecord(7); 

	// Add the data from the metadataMap to the WarcInfoRecord.
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
        recordNAS.addLabelValue(HARVESTINFO_ORIGHARVESTDEFINITIONNAME,
		(String) metadataMap.get(HARVESTINFO_ORIGHARVESTDEFINITIONNAME));

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

        if (metadataMap.containsKey(HARVESTINFO_AUDIENCE)) { 
            recordNAS.addLabelValue(HARVESTINFO_AUDIENCE, 
		(String) metadataMap.get(HARVESTINFO_AUDIENCE));
        }
        
        // really ugly to return as List<String>, but changing would require 
        // larger refactoring
        cachedMetadata = Collections.singletonList(record.toString() 
        		+ netarchiveSuiteComment + "\n" + recordNAS.toString());
        return cachedMetadata;
    }
	
}
