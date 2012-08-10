/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 *
 */
package dk.netarkivet.harvester.tools;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.extractor.Extractor;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;
import twitter4j.Query;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;

/**                                 T
 * A processors which queues urls to tweets and, optionally, embedded links.
 * Although written as a link extractor for heritrix, the processor actually
 * browses twitter through its API using parameters passed in via the order
 * template. Seeds are irrelevant, and all the work is done on the first call
 * to innerProcess().
 */
public class TwitterHarvesterExtractor extends Extractor {

    public static final String PROCESSOR_NAME = "Twitter Harvester Extractor";

    public static final String PROCESSOR_FULL_NAME = TwitterHarvesterExtractor.class
            .getName();

    public static final String PROCESSOR_DESCRIPTION = "Harvests Twitter and embedded url's via Twitter API";

    static Logger logger = Logger.getLogger(PROCESSOR_FULL_NAME);

    /**
     * Here we define bean properties which specify the search parameters for Twitter
     *
     */
    public static final String ATTR_KEYWORDS= "keywords";
    public static final String ATTR_PAGES = "pages";

    private StringList keywords;
    private int pages;
    private int resultsPerPage = 5;
    private boolean queueLinks = true;

    private boolean firstUri = true;
    private Twitter twitter;
    private int tweetCount = 0;
    private int linkCount = 0;


    public TwitterHarvesterExtractor(String name) {
        super(name, PROCESSOR_NAME);
        System.out.println(
                "Constructing instance of " + TwitterHarvesterExtractor.class);
        Type e = addElementToDefinition(new StringList(ATTR_KEYWORDS, "Keywords to search for"));
        e = addElementToDefinition(new SimpleType(ATTR_PAGES, "Number of pages of twitter results to use.", new Integer(0) ));
        twitter = (new TwitterFactory()).getInstance();
    }

    @Override
    protected void initialTasks() {
        super.initialTasks();
        logger.info("Initial tasks for " + PROCESSOR_FULL_NAME);
        System.out.println("Initial tasks for " + PROCESSOR_FULL_NAME);
        StringList keywords = (StringList) getAttributeUnchecked(ATTR_KEYWORDS);
        this.keywords = keywords;
        for (Object keyword: keywords) {
            logger.info("Twitter processor keyword: " + keyword);
            System.out.println("Twitter processor keyword: " + keyword);
        }
        int pages = ((Integer) getAttributeUnchecked(ATTR_PAGES)).intValue();
        this.pages = pages;
        logger.info("Twitter processor will queue " + pages + " page(s) of results.");
        System.out.println("Twitter processor will queue " + pages
                           + " page(s) of results.");
    }

    /**
     * Version of getAttributes that catches and logs exceptions
     * and returns null if failure to fetch the attribute.
     * @param name Attribute name.
     * @return Attribute or null.
     */
    public Object getAttributeUnchecked(String name) {
        Object result = null;
        try {
            result = super.getAttribute(name);
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        }
        return result;
    }

    @Override
    protected void extract(CrawlURI crawlURI) {
        if (firstUri) {
                   for (Object keyword: keywords) {
                        for (int page = 1; page <= pages; page++) {
                            Query query = new Query();
                            query.setRpp(resultsPerPage);
                            query.setQuery((String) keyword);
                            query.setPage(page);
                            try {
                                List<Tweet> tweets = twitter.search(query).getTweets();
                                for (Tweet tweet: tweets) {
                                    long id = tweet.getId();
                                    String fromUser = tweet.getFromUser();
                                    String tweetUrl = "http://twitter.com/" + fromUser + "/status/" + id;
                                    try {
                                        crawlURI.createAndAddLink(tweetUrl, Link.PREREQ_MISC, Link.NAVLINK_HOP);
                                        System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + tweetUrl);
                                        tweetCount++;
                                    } catch (URIException e) {
                                        logger.log(Level.SEVERE, e.getMessage());
                                    }
                                    if (queueLinks) {
                                        for (URLEntity urlEntity : tweet.getURLEntities()) {
                                            try {
                                                crawlURI.createAndAddLink(urlEntity.getExpandedURL().toString(), Link.PREREQ_MISC, Link.PREREQ_HOP);
                                                crawlURI.createAndAddLink(urlEntity.getURL().toString(), Link.NAVLINK_MISC, Link.NAVLINK_HOP);
                                                System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + urlEntity.getExpandedURL().toString());
                                                System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + urlEntity.getURL().toString());
                                            } catch (URIException e) {
                                                logger.log(Level.SEVERE, e.getMessage());
                                            }
                                            linkCount++;
                                        }
                                    }
                                }
                            } catch (TwitterException e) {
                               logger.log(Level.SEVERE, e.getMessage());
                            }
                        }
                   }
                   firstUri = false;
                   crawlURI.linkExtractorFinished();
               }
    }

    @Override
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor:" + TwitterHarvesterExtractor.class.getName() + "\n");
        ret.append("Processed " + keywords.size() + " keywords.\n");
        ret.append("Processed " + pages + " pages with " + resultsPerPage + " results per page.\n");
        ret.append("Queued " + tweetCount + " tweets.\n");
        ret.append("Queued " + linkCount + " external links.\n");
        return ret.toString();
    }

}
