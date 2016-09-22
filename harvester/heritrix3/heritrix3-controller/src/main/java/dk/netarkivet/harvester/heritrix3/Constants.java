package dk.netarkivet.harvester.heritrix3;

/**
 * Constants for heritrix3-controller module.
 */
public class Constants {
	/**
     * Groovy script to get full frontier report.
     */
    public static final String FRONTIER_REPORT_GROOVY_SCRIPT = "job.crawlController.frontier.allQueuesReportTo%28rawOut%29";
    
    /**
     * parse this tag to get rawOut value when sending a script to the scripting console of h3
     */
    public static final String XML_RAWOUT_TAG = "rawOutput";
}
