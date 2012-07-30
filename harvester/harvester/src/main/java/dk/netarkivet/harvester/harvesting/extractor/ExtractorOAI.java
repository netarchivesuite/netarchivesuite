/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
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
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.extractor.Extractor;
import org.archive.crawler.extractor.Link;
import org.archive.io.ReplayCharSequence;
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
public class ExtractorOAI extends Extractor {

    /**
     * Regular expression matching the resumptionToken.
     */
    private static final String RESUMPTION_TOKEN_MATCH
        = "(?i)<resumptionToken>\\s*(.*)\\s*</resumptionToken>";

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
     * @param name the name of this extractor
     */
    public ExtractorOAI(String name) {
        super(name, "Extractor which finds the resumptionToken in an OAI "
                    + "listMetadata query and adds the next page of results "
                    + "to the crawl");
    }

    /**
     * Perform the link extraction on the current crawl uri. This method
     * does not set linkExtractorFinished() on the current crawlURI, so
     * subsequent extractors in the chain can find more links.
     * @param curi the CrawlUI from which to extract the link.
     */
    @Override
    protected void extract(CrawlURI curi) {
         if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }
        String mimeType = curi.getContentType();
        if (mimeType == null) {
            return;
        }
        if ((mimeType.toLowerCase().indexOf("xml") < 0)
            && (!curi.toString().toLowerCase().endsWith(".rss"))
            && (!curi.toString().toLowerCase().endsWith(".xml"))) {
            return;
        }
        try {
            String query = curi.getUURI().getQuery();
            if (!query.contains("verb=ListRecords")) { //Not an OAI-PMH document
                return;
            }
        } catch (URIException e) {
            log.error("Cannot get query part from '" + curi + "'", e);
        }
        this.numberOfCURIsHandled++;
        ReplayCharSequence cs = null;
        try {
            cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch (IOException e) {
            log.error("Failed getting ReplayCharSequence: " + e.getMessage());
        }
        if (cs == null) {
            log.error("Failed getting ReplayCharSequence: "
                    + curi.toString());
            return;
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
    }

    /**
     * Searches for resumption token and adds link if it is found. Returns true
     * iff a link is added.
     * @param curi the CrawlURI.
     * @param cs the character sequency in which to search.
     * @return true iff a resumptionToken is found and a link added.
     */
    public boolean processXml(CrawlURI curi, CharSequence cs) {
        Matcher m = TextUtils.getMatcher(RESUMPTION_TOKEN_MATCH, cs);
        boolean matches = m.find();
        if (matches) {
            String token = m.group(1);
            UURI oldUri = curi.getUURI();
            try {
                final String newQueryPart = "verb=ListRecords&resumptionToken="
                                            + token;
                URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                                     oldUri.getPath(),
                                     newQueryPart, oldUri.getFragment());
                curi.createAndAddLink(newUri.toString(), Link.NAVLINK_MISC,
                                      Link.NAVLINK_HOP);
            } catch (URISyntaxException e) {
                log.error(e);
            } catch (URIException e) {
                log.error(e);
            }
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
        ret.append("Processor: org.archive.crawler.extractor.ExtractorHTML\n");
        ret.append("  Function:          Link extraction on HTML documents\n");
        ret.append("  CrawlURIs handled: " + this.numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + this.numberOfLinksExtracted
                + "\n\n");
        return ret.toString();
    }

}
