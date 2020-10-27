/* File:        $Id: ExtractorOAI.java 2687 2013-05-03 16:38:47Z svc $
 * Revision:    $Revision: 2687 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-05-03 18:38:47 +0200 (Fri, 03 May 2013) $
 *
 * Copyright 2004-2018 The Royal Danish Library,
 * the National Library of France and the Austrian
 * National Library.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.harvester.harvesting.extractor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.ReplayCharSequence;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.ContentExtractor;
import org.archive.modules.extractor.Hop;
import org.archive.modules.extractor.LinkContext;
import org.archive.net.UURI;
import org.archive.util.TextUtils;

/**
 * This is a link extractor for use with Heritrix. It will find the
 * resumptionToken in an OAI-PMH listMetadata query and construct the link for
 * the next page of the results. This extractor will not extract any other links
 * so if there are additional urls in the OAI metadata then an additional
 * extractor should be used for these. Typically this means that the extractor
 * chain in the order template will end: 
 * <newObject name="ExtractorOAI"
 *      class="dk.netarkivet.harvester.harvesting.extractor.ExtractorOAI"> 
 *      <boolean name="enabled">true</boolean> 
 *      <newObject name="ExtractorOAI#decide-rules"
 *       class="org.archive.crawler.deciderules.DecideRuleSequence"> 
 *            <map name="rules"/> 
 *       </newObject> 
 *</newObject> 
 * <newObject name="ExtractorXML"
 *      class="org.archive.crawler.extractor.ExtractorXML"> 
 *      <boolean name="enabled">true</boolean> 
 *      <newObject name="ExtractorXML#decide-rules"
 *              class="org.archive.crawler.deciderules.DecideRuleSequence"> 
 *              <map name="rules"/> 
 *      </newObject> 
 * </newObject>
 */
public class ExtractorOAI extends ContentExtractor {

    /**
     * Regular expression matching the simple resumptionToken like this.
     * <resumptionToken>oai_dc/421315/56151148/100/0/292/x/x/x</resumptionToken>
     */
    public static final String SIMPLE_RESUMPTION_TOKEN_MATCH = "(?i)<resumptionToken>\\s*(.*)\\s*</resumptionToken>";

    /**
     * Regular expression matching the extended resumptionToken with attributes like this. <resumptionToken cursor="0"
     * completeListSize="421315">oai_dc/421315/56151148/100/0/292/x/x/x</resumptionToken> This is seen in OAI targets
     * used by PURE.
     */
    public static final String EXTENDED_RESUMPTION_TOKEN_MATCH = "(?i)<resumptionToken\\s*cursor=\"[0-9]+\"\\s*completeListSize=\"[0-9]+\">\\s*(.*)\\s*</resumptionToken>";
       
     /** The class logger. */
    final Log log = LogFactory.getLog(getClass());

    /**
     * The number of crawl-uris handled by this extractor.
     */
    private long numberOfCURIsHandled = 0;

    /**
     * The number of links extracted by this extractor.
     */
    private long numberOfLinksExtracted = 0;

    /**
     * Constructor for this extractor.
     */
    public ExtractorOAI() {
        super();
    }

    /**
     * Perform the link extraction on the current crawl uri. This method
     * does not set linkExtractorFinished() on the current crawlURI, so
     * subsequent extractors in the chain can find more links.
     * @param curi the CrawlUI from which to extract the link.
     */
    @Override
	protected boolean innerExtract(CrawlURI curi) {
        try {
            String query = curi.getUURI().getQuery();
            if (query == null) { // Test for null query - strange that we need to do that
                return false;
            }
            if (!query.contains("verb=ListRecords")) { //Not an OAI-PMH document
                return false;
            }
        } catch (URIException e) {
            log.error("Cannot get query part from '" + curi + "'", e);
        }
        this.numberOfCURIsHandled++;
        ReplayCharSequence cs = null;
        try {
        	cs = curi.getRecorder().getContentReplayCharSequence();            
        } catch (IOException e) {
            log.error("Failed getting ReplayCharSequence: " + e.getMessage());
        }
        if (cs == null) {
            log.error("Failed getting ReplayCharSequence: "
                    + curi.toString());
            return false;
        }
        try {
            boolean foundResumptionToken = processXml(curi, cs);
            if (foundResumptionToken) {
                numberOfLinksExtracted += 1;
            }
        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    log.warn(TextUtils.exceptionToString(
                            "Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
        return false;
    }

    /**
     * Searches for resumption token and adds link if it is found. Returns true
     * iff a link is added.
     * @param curi the CrawlURI.
     * @param cs the character sequence in which to search.
     * @return true iff a resumptionToken is found and a link added.
     */
    public boolean processXml(CrawlURI curi, CharSequence cs) {
        Matcher m = TextUtils.getMatcher(SIMPLE_RESUMPTION_TOKEN_MATCH, cs);
        Matcher mPure = TextUtils.getMatcher(EXTENDED_RESUMPTION_TOKEN_MATCH, cs);
        boolean matchesPure = mPure.find();
        boolean matches = m.find();
        String token = null;
        if (matches) {
            token = m.group(1);
        } else if (matchesPure) {
            token = mPure.group(1);
        }
        
        if (token != null) {
            UURI oldUri = curi.getUURI();
            try {
                final String newQueryPart = "verb=ListRecords&resumptionToken="
                                            + token;
                URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                                     oldUri.getPath(),
                                     newQueryPart, oldUri.getFragment());
                
                log.info("Found resumption link: " + newUri);
                add(curi, 10000, newUri.toString(), LinkContext.NAVLINK_MISC, Hop.NAVLINK);
            } catch (URISyntaxException e) {
                log.error(e);
            } catch (URIException e) {
                log.error(e);
            }
        } else {
        	log.info("No resumption tokens found for url " + curi.getCanonicalString());
        }
        TextUtils.recycleMatcher(m);
        return matches;
    }

    /**
     * Return a report from this processor.
     * @return the report.
     */
    @Override
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: dk.netarkivet.harvester.harvesting.extractor.ExtractorOAI\n");
        ret.append("  Function:          Link extraction on OAI XML documents\n");
        ret.append("  CrawlURIs handled: " + this.numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + this.numberOfLinksExtracted + "\n\n");
        return ret.toString();
    }

    @Override
    protected boolean shouldExtract(CrawlURI curi) {
        //curi.isHttpTransaction();
        String mimeType = curi.getContentType();
        if (mimeType == null) {
            return false;
        }
        if ((mimeType.toLowerCase().indexOf("xml") < 0)
            && (!curi.toString().toLowerCase().endsWith(".rss"))
            && (!curi.toString().toLowerCase().endsWith(".xml"))) {
            return false;
        }
        return true;

    }    
}
