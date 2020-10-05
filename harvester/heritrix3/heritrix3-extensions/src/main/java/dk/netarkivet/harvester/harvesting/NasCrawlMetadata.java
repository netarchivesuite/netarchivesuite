package dk.netarkivet.harvester.harvesting;

import org.archive.modules.CrawlMetadata;

/** 
 * NetarchiveSuite extension of the org.archive.modules.CrawlMetadata class.
 * Currently the only addition is a date field 
 * similar to the date field in the H1 'meta' entity
 * 
 <meta>
  <name>default_obeyrobots_withforms</name>
 <description>Default profile that obeys robots.txt and includes form URLs</description>
 <operator>Admin</operator>
 <organization/>
 <audience/>
 <date>20080118111217</date>
</meta>

*/
public class NasCrawlMetadata extends CrawlMetadata {
	
	private static final long serialVersionUID = 2505633855092991518L;
	private String date;
	
	public String getDate() {
		return this.date;
	}
	
	public void setDate(String date) {
		this.date = date;
	}	
}