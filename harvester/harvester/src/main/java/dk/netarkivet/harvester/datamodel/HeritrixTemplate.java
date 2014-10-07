package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

public abstract class HeritrixTemplate {

	private static final CharSequence H1_SIGNATURE = "<crawl-order xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance"; 
	private static final CharSequence H3_SIGNATURE = "xmlns:\"http://www.springframework.org/";
	
	public static HeritrixTemplate getTemplate(String templateAsString) {
		if (templateAsString.contains(H1_SIGNATURE)) {
			return new H1HeritrixTemplate(templateAsString);
		} else if (templateAsString.contains(H3_SIGNATURE)) {
			return new H3HeritrixTemplate(templateAsString);
		} else {
			throw new ArgumentNotValid("The given template is neither H1 or H3: " + templateAsString);
		}
	}
	public abstract boolean isValid();
	public abstract String getXML();

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
	
	public abstract void setMaxBytesPerDomain(Long maxbytesL);
	public abstract Long getMaxBytesPerDomain(); // TODO Is necessary? 
	
	public abstract void setMaxObjectsPerDomain(Long maxobjectsL);
	public abstract Long getMaxObjectsPerDomain(); // TODO Is necessary? 
	
	public abstract boolean IsDeduplicationEnabled();
	
	
	
	/**
     * Method to add a list of crawler traps with a given element name. It is used both to add per-domain traps and
     * global traps.
     *
     * @param elementName The name of the added element.
     * @param crawlerTraps A list of crawler trap regular expressions to add to this job.
     */

	public abstract void insertCrawlerTraps(String elementName, List<String> crawlertraps);
	
	public abstract void setArchiveFormat(String archiveFormat);
	
	public abstract void setMaxJobRunningTime(Long maxJobRunningTimeSecondsL);
	
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
    public void editOrderXMLAddPerDomainCrawlerTraps(DomainConfiguration cfg) {
        List<String> crawlerTraps = cfg.getCrawlertraps();
        String elementName = cfg.getDomainName();
        insertCrawlerTraps(elementName, crawlerTraps);
    }
    
    /**
     * 
     * Updates the diskpath value, archivefile_prefix, seedsfile, and deduplication -information.
     * FIXME HeritrixFiles could be different from H1 and H. Consider making this an abstract class as well.
     * @param files
     * @throws IOFailure
     */
    public static void makeTemplateReadyForHeritrix(HeritrixFiles files) throws IOFailure {
    	HeritrixTemplate templ = HeritrixTemplate.read(files.getOrderXmlFile());
    	templ.setDiskPath(files.getCrawlDir().getAbsolutePath());
    	templ.setArchiveFilePrefix(files.getArchiveFilePrefix());
    	templ.setSeedsFilePath(files.getSeedsTxtFile().getAbsolutePath());
        if (templ.IsDeduplicationEnabled()) {
        	templ.setDeduplicationIndexLocation(files.getIndexDir()
                    .getAbsolutePath());
        }
        files.writeOrderXml(templ);
    }
	
    public abstract void setDeduplicationIndexLocation(String absolutePath);
	public abstract void setSeedsFilePath(String absolutePath);

    public abstract void setArchiveFilePrefix(String archiveFilePrefix);
    public abstract void setDiskPath(String absolutePath);
    
    /** 
     * 
     * @param orderXmlFile
     * @return
     */
	public static HeritrixTemplate read(File orderXmlFile) {
		return null;
	}
	
	/**
	 * 
	 * @param orderTemplateReader
	 * @return
	 */
	public static HeritrixTemplate read(Reader orderTemplateReader) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public abstract void writeTemplate(OutputStream os) throws IOException, ArgumentNotValid;
	
	public abstract boolean hasContent();
	
	public abstract void writeToFile(File orderXmlFile);
	public abstract void setRecoverlogNode(File recoverlogGzFile);
	
	/**
     * Try to extract an orderxmldoc from a given Clob. This method is used by the read() method, which catches the
     * thrown DocumentException.
     *
     * @param clob a given Clob returned from the database
     * @return a Document object based on the data in the Clob
     * @throws SQLException If data from the clob cannot be fetched.
     * @throws DocumentException If unable to create a Document object based on the data in the Clob
     */
    public static HeritrixTemplate getOrderXMLdocFromClob(Clob clob) throws SQLException {
        
    	// Taste the first 1000 characters, and look for the signatures of the different types of template.    	
    	String signature = clob.getSubString(0L, 1000);
    	if (signature.contains(HeritrixTemplate.H1_SIGNATURE)) {
    		
    	  /*
    		Document doc;
            try {
                SAXReader reader = new SAXReader();
                
                doc = reader.read(clob.getCharacterStream());
            } catch (DocumentException e) {
                log.warn("Failed to read the contents of the clob as XML:" + clob.getSubString(1, (int) clob.length()));
                throw e;
            }
            return doc;
    		*/
    		return null;
    	} else if (signature.contains(HeritrixTemplate.H3_SIGNATURE)) {
    		return null;
    	} else {
    		throw new IllegalState("The template starting with '" + signature + "' cannot be recognized as either H1 or H3");
    	}
    	
    }
	public static HeritrixTemplate read(String string) {
		return null;
	}
	public abstract void removeDeduplicatorIfPresent();
	
}
