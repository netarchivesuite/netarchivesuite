package dk.netarkivet.harvester.tools;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import sun.rmi.runtime.NewThreadAction;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
    public static final String ATTR_GEOLOCATIONS = "geo_locations";
    public static final String ATTR_LANG = "language";

    private StringList keywords;
    private StringList geoLocations;
    private String language;
    private int pages;
    private int resultsPerPage = 5;
    private boolean queueLinks = true;
    private boolean queueUserStatus = true;
    private boolean queueKeywordLinks = true;

    private Twitter twitter;
    private int tweetCount = 0;
    private int linkCount = 0;

    @Override
    public void initialize(CrawlController controller) {
        super.initialize(controller);
        twitter = (new TwitterFactory()).getInstance();
                keywords = null;
                pages = 0;
                try {
                    keywords = (StringList) super.getAttribute(ATTR_KEYWORDS);
                    pages = ((Integer) super.getAttribute(ATTR_PAGES)).intValue();
                    geoLocations = (StringList) super.getAttribute(ATTR_GEOLOCATIONS);
                    language = (String) super.getAttribute(ATTR_LANG);
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
                for (Object geolocation: geoLocations) {
                    System.out.println("Twitter GeoLocation: " + geolocation);
                }
                if (keywords == null || keywords.isEmpty()) {
                    keywords = new StringList("keywords", "empty keyword list", new String[]{""});
                }
                if (geoLocations == null || geoLocations.isEmpty()) {
                    geoLocations = new StringList("geolocations", "empty geolocation list",new String[]{""} );
                }
        logger.info("Twitter Scope will queue " + pages + " page(s) of results.");
        System.out.println("Twitter Scope will queue " + pages
                + " page(s) of results.");
        for (Object keyword: keywords) {
            for (Object geoLocation: geoLocations) {
                String urlQuery = "keyword";
                Query query = new Query();
                if (language != null && !language.equals("")) {
                    query.setLang(language);
                    urlQuery += "lang:" + language;
                }
                urlQuery = "http://twitter.com/search/" + URLEncoder.encode(urlQuery);
                if (queueKeywordLinks) {
                    addSeedIfLegal(urlQuery);
                }
                for (int page = 1; page <= pages; page++) {
                    query.setRpp(resultsPerPage);
                    if (!keyword.equals("")) {
                        query.setQuery((String) keyword);
                    }
                    query.setPage(page);
                    if (!geoLocation.equals("")) {
                        String[] locationArray = ((String) geoLocation).split(",");
                        GeoLocation location = new GeoLocation(Double.parseDouble(locationArray[0]), Double.parseDouble(locationArray[1]));
                        query.setGeoCode(location, Double.parseDouble(locationArray[2]), locationArray[3]);
                    }
                    try {
                        final QueryResult result = twitter.search(query);
                        List<Tweet> tweets = result.getTweets();
                        for (Tweet tweet: tweets) {
                            long id = tweet.getId();
                            String fromUser = tweet.getFromUser();
                            String tweetUrl = "http://www.twitter.com/" + fromUser + "/status/" + id;
                            addSeedIfLegal(tweetUrl);
                            if (queueLinks) {
                                for (URLEntity urlEntity : tweet.getURLEntities()) {
                                        final String embeddedUrl = urlEntity.getURL().toString();
                                        addSeedIfLegal(embeddedUrl);
                                }
                            }
                            if (queueUserStatus) {
                                String statusUrl = "http://twitter.com/" + tweet.getFromUser() + "/";
                                addSeedIfLegal(statusUrl);
                            }
                        }
                    } catch (TwitterException e1) {
                        logger.log(Level.SEVERE, e1.getMessage());
                    }
                }
            }

        }
    }

    private void addSeedIfLegal(String tweetUrl) {
        try {
            CandidateURI curi = CandidateURI.createSeedCandidateURI(UURIFactory.getInstance(tweetUrl));
            System.out.println("Adding seed: '" + curi.toString() + "'" );
            addSeed(curi);
            tweetCount++;
        } catch (URIException e1) {
            logger.log(Level.SEVERE, e1.getMessage());
            e1.printStackTrace();
        }
    }

    public TwitterDecidingScope(String name) {
        super(name);
        addElementToDefinition(new StringList(ATTR_KEYWORDS, "Keywords to search for"));
        addElementToDefinition(new SimpleType(ATTR_PAGES, "Number of pages of twitter results to use.", new Integer(0)));
        addElementToDefinition(new StringList(ATTR_GEOLOCATIONS, "Geolocations to search for, comma separated as " +
                "lat,long,radius,units e.g. 56.0,10.1,200.0,km"));
        addElementToDefinition(new SimpleType(ATTR_LANG, "Exclusive language for search", ""));
    }

    @Override
    public boolean addSeed(CandidateURI curi) {
        return super.addSeed(curi);
    }
}
