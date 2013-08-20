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
 * 
 * Copied from heritrix-1.15.5/src/java/org/archive/crawler/extractor/ExtractorJS.java
 * 
 */

/* Copyright (C) 2003 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Created on Nov 17, 2003
 *
 */
package dk.netarkivet.harvester.harvesting.extractor;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringEscapeUtils;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.extractor.Extractor;
import org.archive.crawler.extractor.Link;
import org.archive.crawler.framework.CrawlController;
import org.archive.io.ReplayCharSequence;
import org.archive.net.UURI;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;
import org.archive.util.UriUtils;

/**
 * Processes Javascript files for strings that are likely to be
 * crawlable URIs.
 *
 * contributor gojomo
 * contributor szznax
 * contributor svc
 */
public class ExtractorJS extends Extractor implements CoreAttributeConstants {

    private static final long serialVersionUID = -2231962381454717720L;

    private static Logger LOGGER =
        Logger.getLogger("dk.netarkivet.harvester.harvesting.extractor.ExtractorJS");

    // finds whitespace-free strings in Javascript
    // (areas between paired ' or " characters, possibly backslash-quoted
    // on the ends, but not in the middle)
    static final String JAVASCRIPT_STRING_EXTRACTOR =
        "(\\\\{0,8}+(?:\"|\'))(\\S{0,"+UURI.MAX_URL_LENGTH+"}?)(?:\\1)";
    // GROUPS:
    // (G1) ' or " with optional leading backslashes
    // (G2) whitespace-free string delimited on boths ends by G1


    protected long numberOfCURIsHandled = 0;
    protected static long numberOfLinksExtracted = 0;

    
    // URIs known to produce false-positives with the current JS extractor.
    // e.g. currently (2.0.3) the JS extractor produces 13 false-positive 
    // URIs from http://www.google-analytics.com/urchin.js and only 2 
    // good URIs, which are merely one pixel images.
    // TODO: remove this blacklist when JS extractor is improved 
    protected final static String[] EXTRACTOR_URI_EXCEPTIONS = {
        "http://www.google-analytics.com/urchin.js"
        };
    
    /**
     * @param name
     */
    public ExtractorJS(String name) {
        super(name, "JavaScript extractor. Link extraction on JavaScript" +
                " files (.js).");
    }

    /* (non-Javadoc)
     * @see org.archive.crawler.framework.Processor#process(org.archive.crawler.datamodel.CrawlURI)
     */
    public void extract(CrawlURI curi) {
        // special-cases, for when we know our current JS extractor does poorly.
        // TODO: remove this test when JS extractor is improved 
        for (String s: EXTRACTOR_URI_EXCEPTIONS) {
            if (curi.toString().equals(s))
                return;
        }
            
        if (!isHttpTransactionContentToProcess(curi)) {
            return;
        }
        String contentType = curi.getContentType();
        if ((contentType == null)) {
            return;
        }
        // If content type is not js and if the viaContext
        // does not begin with 'script', return.
        if((contentType.indexOf("javascript") < 0) &&
            (contentType.indexOf("jscript") < 0) &&
            (contentType.indexOf("ecmascript") < 0) &&
            (!curi.toString().toLowerCase().endsWith(".js")) &&
            (curi.getViaContext() == null || !curi.getViaContext().
                toString().toLowerCase().startsWith("script"))) {
            return;
        }

        this.numberOfCURIsHandled++;

        ReplayCharSequence cs = null;
        try {
            cs = curi.getHttpRecorder().getReplayCharSequence();
        } catch (IOException e) {
            curi.addLocalizedError(this.getName(), e,
            	"Failed get of replay char sequence.");
        }
        if (cs == null) {
            LOGGER.warning("Failed getting ReplayCharSequence: " +
                curi.toString());
            return;
        }

        try {
            try {
                numberOfLinksExtracted += considerStrings(curi, cs,
                        getController(), true);
            } catch (StackOverflowError e) {
                DevUtils.warnHandle(e, "ExtractorJS StackOverflowError");
            }
            // Set flag to indicate that link extraction is completed.
            curi.linkExtractorFinished();
        } finally {
            // Done w/ the ReplayCharSequence. Close it.
            if (cs != null) {
                try {
                    cs.close();
                } catch (IOException ioe) {
                    LOGGER.warning(TextUtils.exceptionToString(
                        "Failed close of ReplayCharSequence.", ioe));
                }
            }
        }
    }

    public static long considerStrings(CrawlURI curi, CharSequence cs,
            CrawlController controller, boolean handlingJSFile) {
        long foundLinks = 0;
        Matcher strings =
            TextUtils.getMatcher(JAVASCRIPT_STRING_EXTRACTOR, cs);
        while(strings.find()) {
            CharSequence subsequence =
                cs.subSequence(strings.start(2), strings.end(2));

            if(UriUtils.isLikelyUriJavascriptContextLegacy(subsequence)) {
                String string = subsequence.toString();
                string = StringEscapeUtils.unescapeJavaScript(string);
                string = UriUtils.speculativeFixup(string, curi.getUURI());
                foundLinks++;
                try {
                    if (handlingJSFile) {
                        curi.createAndAddLinkRelativeToVia(string,
                            Link.JS_MISC, Link.SPECULATIVE_HOP);
                    } else {
                        curi.createAndAddLinkRelativeToBase(string,
                            Link.JS_MISC, Link.SPECULATIVE_HOP);
                    }
                } catch (URIException e) {
                    // There may not be a controller (e.g. If we're being run
                    // by the extractor tool).
                    if (controller != null) {
                        controller.logUriError(e, curi.getUURI(), string);
                    } else {
                        LOGGER.info(curi + ", " + string + ": " +
                            e.getMessage());
                    }
                }
            } else {
               foundLinks += considerStrings(curi, subsequence,
                   controller, handlingJSFile);
            }
        }
        TextUtils.recycleMatcher(strings);
        return foundLinks;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.archive.crawler.framework.Processor#report()
     */
    public String report() {
        StringBuffer ret = new StringBuffer();
        ret.append("Processor: dk.netarkivet.harvester.harvesting.extractor.ExtractorJS\n");
        ret.append("  Function:          Link extraction on JavaScript code\n");
        ret.append("  CrawlURIs handled: " + numberOfCURIsHandled + "\n");
        ret.append("  Links extracted:   " + numberOfLinksExtracted + "\n\n");

        return ret.toString();
    }
}
