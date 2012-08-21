/* File:       $Id: $
 * Revision:   $Revision:  $
 * Author:     $Author:  $
 * Date:       $Date: $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.tools;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.net.UURIFactory;
import twitter4j.GeoLocation;
import twitter4j.MediaEntity;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Heritrix CrawlScope that uses the Twitter Search API (https://dev.twitter.com/docs/api/1/get/search)
 * to add seeds to a crawl.
 */
public class TwitterDecidingScope extends DecidingScope {

    /**
     * Logger for this class.
     */
    static Logger logger = Logger.getLogger(TwitterDecidingScope.class.getName());

    /**
     * Here we define bean properties which specify the search parameters for Twitter
     *
     */

    /**
     * Attribute/value pair. The list of keywords to search for
     */
    public static final String ATTR_KEYWORDS= "keywords";
    private StringList keywords;

    /**
     * Attribute/value pair. The number of pages of results to process.
     */
    public static final String ATTR_PAGES = "pages";
    private int pages = 1;

    /**
     * Attribute/value pair. The number of results per twitter page.
     */
    public static final String ATTR_RESULTS_PER_PAGE = "twitter_results_per_page";
    private int resultsPerPage = 100;

    /**
     * Attribute/value pair. A list of geo_locations to include in the search. These
     * have the form lat,long,radius,units e.g. 100.1,10.5,25.0,km
     */
    public static final String ATTR_GEOLOCATIONS = "geo_locations";
    private StringList geoLocations;


    /**
     * Attribute/value pair. If set, the language to which results are restricted.
     */
    public static final String ATTR_LANG = "language";
    private String language;

    /**
     * Attribute/value pair pecifying whether embedded links should be queued.
     */
    public static final String ATTR_QUEUE_LINKS = "queue_links";
    private boolean queueLinks = true;

    /**
     *Attribute/value pair specifying whether the status of discovered users should be harvested.
     */
    public static final String ATTR_QUEUE_USER_STATUS = "queue_user_status";
    private boolean queueUserStatus = true;

    /**
     * Attribute/value pair specifying whether one should additionally queue all links embedded in a users status.
     */
    public static final String ATTR_QUEUE_USER_STATUS_LINKS = "queue_user_status_links";
    private boolean queueUserStatusLinks = true;


    /**
     * Attribute/value pair specifying whether an html search for the given keyword(s) should also be queued.
     */
    public static final String ATTR_QUEUE_KEYWORD_LINKS = "queue_keyword_links";
    private boolean queueKeywordLinks = true;

    private Twitter twitter;
    private int tweetCount = 0;
    private int linkCount = 0;

    /**
     * This routine makes any necessary Twitter API calls and queues the content discovered.
     * @param controller
     */
    @Override
    public void initialize(CrawlController controller) {
        super.initialize(controller);
        twitter = (new TwitterFactory()).getInstance();
        keywords = null;
        try {
            keywords = (StringList) super.getAttribute(ATTR_KEYWORDS);
            pages = ((Integer) super.getAttribute(ATTR_PAGES)).intValue();
            geoLocations = (StringList) super.getAttribute(ATTR_GEOLOCATIONS);
            language = (String) super.getAttribute(ATTR_LANG);
            resultsPerPage = (Integer) super.getAttribute(ATTR_RESULTS_PER_PAGE);
            queueLinks = (Boolean) super.getAttribute(ATTR_QUEUE_LINKS);
            queueUserStatus = (Boolean) super.getAttribute(ATTR_QUEUE_USER_STATUS);
            queueUserStatusLinks = (Boolean) super.getAttribute(ATTR_QUEUE_USER_STATUS_LINKS);
            queueKeywordLinks = (Boolean) super.getAttribute(ATTR_QUEUE_KEYWORD_LINKS);
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
        //If keywords or geoLocations is missing, add a list with a single empty string so that the main loop is
        // executed at least once.
        if (keywords == null || keywords.isEmpty()) {
            keywords = new StringList("keywords", "empty keyword list", new String[]{""});
        }
        if (geoLocations == null || geoLocations.isEmpty()) {
            geoLocations = new StringList("geolocations", "empty geolocation list",new String[]{""} );
        }
        logger.info("Twitter Scope will queue " + pages + " page(s) of results.");
        System.out.println("Twitter Scope will queue " + pages
                + " page(s) of results with " + resultsPerPage + " results per page.");
        //Nested loop over keywords, geo_locations and pages.
        for (Object keyword: keywords) {
            for (Object geoLocation: geoLocations) {
                String urlQuery = (String) keyword;
                Query query = new Query();
                query.setRpp(resultsPerPage);
                if (language != null && !language.equals("")) {
                    query.setLang(language);
                    urlQuery += " lang:" + language;
                }
                urlQuery = "http://twitter.com/search/" + URLEncoder.encode(urlQuery);
                if (queueKeywordLinks) {
                    addSeedIfLegal(urlQuery);
                }
                for (int page = 1; page <= pages; page++) {
                    query.setPage(page);
                    if (!keyword.equals("")) {
                        query.setQuery((String) keyword);
                    }
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
                            tweetCount++;
                            if (queueLinks) {
                                extractEmbeddedLinks(tweet);
                            }
                            if (queueUserStatus) {
                                String statusUrl = "http://twitter.com/" + tweet.getFromUser() + "/";
                                addSeedIfLegal(statusUrl);
                                linkCount++;
                                if (queueUserStatusLinks) {
                                    queueUserStatusLinks(tweet.getFromUser());
                                }
                            }
                        }
                    } catch (TwitterException e1) {
                        logger.log(Level.SEVERE, e1.getMessage());
                    }
                }
            }

        }
        System.out.println(TwitterDecidingScope.class + " added " + tweetCount + " tweets and " + linkCount + " other links.");
    }

    /**
     * Adds links to embedded url's and media in a tweet.
     * @param tweet
     */
    private void extractEmbeddedLinks(Tweet tweet) {
        final URLEntity[] urlEntities = tweet.getURLEntities();
        if (urlEntities != null) {
            for (URLEntity urlEntity : urlEntities) {
                final String embeddedUrl = urlEntity.getURL().toString();
                addSeedIfLegal(embeddedUrl);
                linkCount++;
            }
        }
        final MediaEntity[] mediaEntities = tweet.getMediaEntities();
        if (mediaEntities != null) {
            for (MediaEntity mediaEntity: mediaEntities) {
                final String mediaUrl = mediaEntity.getMediaURL().toString();
                addSeedIfLegal(mediaUrl);
                linkCount++;
            }
        }
    }

    /**
     * Searches for a given users recent tweets and queues and embedded material found.
     * @param user
     */
    private void queueUserStatusLinks(String user) {
        Query query = new Query();
        query.setQuery("@"+user);
        query.setRpp(20);
        try {
            List<Tweet> results = twitter.search(query).getTweets();
            if (results != null && !results.isEmpty()) {
                System.out.println("Extracting embedded links for user " + user);
            }
            for (Tweet result: results) {
                extractEmbeddedLinks(result);
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a url as a seed if possible. Otherwise just prints an error description and returns.
     * @param tweetUrl
     */
    private void addSeedIfLegal(String tweetUrl) {
        try {
            CandidateURI curi = CandidateURI.createSeedCandidateURI(UURIFactory.getInstance(tweetUrl));
            System.out.println("Adding seed: '" + curi.toString() + "'");
            addSeed(curi);
        } catch (URIException e1) {
            logger.log(Level.SEVERE, e1.getMessage());
            e1.printStackTrace();
        }
    }

    /**
     * Constructor for the method. Sets up all known attributes.
     * @param name
     */
    public TwitterDecidingScope(String name) {
        super(name);
        addElementToDefinition(new StringList(ATTR_KEYWORDS, "Keywords to search for"));
        addElementToDefinition(new SimpleType(ATTR_PAGES, "Number of pages of twitter results to use.", new Integer(1)));
        addElementToDefinition(new StringList(ATTR_GEOLOCATIONS, "Geolocations to search for, comma separated as " +
                "lat,long,radius,units e.g. 56.0,10.1,200.0,km"));
        addElementToDefinition(new SimpleType(ATTR_LANG, "Exclusive language for search", ""));
        addElementToDefinition(new SimpleType(ATTR_RESULTS_PER_PAGE, "Number of results per twitter search page (max 100)", new Integer(100)));
        addElementToDefinition(new SimpleType(ATTR_QUEUE_KEYWORD_LINKS, "Whether to queue an html search result for the specified keywords", new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_QUEUE_LINKS, "Whether to queue links discovered in search results", new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_QUEUE_USER_STATUS, "Whether to queue an html status listing for discovered users.", new Boolean(true)));
        addElementToDefinition(new SimpleType(ATTR_QUEUE_USER_STATUS_LINKS, "Whether to search for and queue links embedded in the status of discovered users.", new Boolean(true)));
    }

    @Override
    public boolean addSeed(CandidateURI curi) {
        return super.addSeed(curi);
    }
}
