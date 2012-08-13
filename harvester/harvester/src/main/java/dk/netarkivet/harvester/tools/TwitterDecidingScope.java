package dk.netarkivet.harvester.tools;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.deciderules.DecidingFilter;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import twitter4j.Query;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: csr
 * Date: 8/13/12
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TwitterDecidingScope extends DecidingScope {
    static Logger logger = Logger.getLogger(TwitterDecidingScope.class.getName());

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

    private Twitter twitter;
    private int tweetCount = 0;
    private int linkCount = 0;

    public TwitterDecidingScope(String name) {
        super(name);
        Type e = addElementToDefinition(new StringList(ATTR_KEYWORDS, "Keywords to search for"));
        e = addElementToDefinition(new SimpleType(ATTR_PAGES, "Number of pages of twitter results to use.", new Integer(0) ));
        twitter = (new TwitterFactory()).getInstance();
        StringList keywords = null;
        int pages = 0;
        try {
            keywords = (StringList) getAttribute(ATTR_KEYWORDS);
            pages = ((Integer) getAttribute(ATTR_PAGES)).intValue();
        } catch (AttributeNotFoundException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e1);
        } catch (MBeanException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e1);
        } catch (ReflectionException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e1);
        }
        for (Object keyword: keywords) {
            logger.info("Twitter Scope keyword: " + keyword);
            System.out.println("Twitter Scope keyword: " + keyword);
        }
        this.pages = pages;
        logger.info("Twitter Scope will queue " + pages + " page(s) of results.");
        System.out.println("Twitter Scope will queue " + pages
                + " page(s) of results.");
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
                        String tweetUrl = "http://www.twitter.com/" + fromUser + "/status/" + id;
                        try {
                            //URI uri = new URI(tweetUrl);
                            //crawlURI.createAndAddLink(uri.toString(), Link.NAVLINK_MISC, Link.NAVLINK_HOP);
                            //CandidateURI curi = new CandidateURI(UURIFactory.getInstance(tweetUrl));
                            CandidateURI curi = CandidateURI.createSeedCandidateURI(UURIFactory.getInstance(tweetUrl));
                            System.out.println("Adding seed: '" + curi.toString() + "'" );
                            System.out.println("Is seed? " + curi.isSeed());
                            addSeed(curi);
                            //System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + tweetUrl);
                            tweetCount++;
                        } catch (URIException e1) {
                            logger.log(Level.SEVERE, e1.getMessage());
                            e1.printStackTrace();
                        }
                        if (queueLinks) {
                            for (URLEntity urlEntity : tweet.getURLEntities()) {
                                try {
                                    //crawlURI.createAndAddLink(urlEntity.getExpandedURL().toString(), Link.PREREQ_MISC, Link.PREREQ_HOP);
                                    //crawlURI.createAndAddLink(urlEntity.getURL().toURI().toString(), Link.NAVLINK_MISC, Link.NAVLINK_HOP);
                                    //System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + urlEntity.getExpandedURL().toString());
                                    //System.out.println(TwitterHarvesterExtractor.class.getName() + " adding " + urlEntity.getURL().toString());
                                    UURI uuri = UURIFactory.getInstance(urlEntity.getURL().toString());
                                    CandidateURI curi = CandidateURI.createSeedCandidateURI(uuri);
                                    addSeed(curi);
                                    System.out.println("Added seed: '" + curi.toString() + "'");
                                    //getController().getScope().addSeed(new CandidateURI(UURIFactory.getInstance(urlEntity.getURL().toString())));
                                } catch (URIException e1) {
                                    logger.log(Level.SEVERE, e1.getMessage());
                                }
                                linkCount++;
                            }
                        }
                    }
                } catch (TwitterException e1) {
                    logger.log(Level.SEVERE, e1.getMessage());
                }
            }

        }
    }

    @Override
    public boolean addSeed(CandidateURI curi) {
        return super.addSeed(curi);
    }
}
