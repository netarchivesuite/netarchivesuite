/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.archive.modules.extractor;

import static org.archive.modules.extractor.Hop.SPECULATIVE;
import static org.archive.modules.extractor.LinkContext.JS_MISC;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.archive.io.ReplayCharSequence;
import org.archive.modules.CrawlURI;
import org.archive.net.UURI;
import org.archive.util.DevUtils;
import org.archive.util.TextUtils;
import org.archive.util.UriUtils;

/**
 * Processes Javascript files for strings that are likely to be
 * crawlable URIs.
 *
 * NOTE: This processor may open a ReplayCharSequence from the 
 * CrawlURI's Recorder, without closing that ReplayCharSequence, to allow
 * reuse by later processors in sequence. In the usual (Heritrix) case, a 
 * call after all processing to the Recorder's endReplays() method ensures
 * timely close of any reused ReplayCharSequences. Reuse of this processor
 * elsewhere should ensure a similar cleanup call to Recorder.endReplays()
 * occurs. 
 * 
 * TODO: Replace with a system for actually executing Javascript in a 
 * browser-workalike DOM, such as via HtmlUnit or remote-controlled 
 * browser engines. 
 * 
 * @contributor gojomo
 * @contributor nlevitt
 */
public class ExtractorJS extends ContentExtractor {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 3L;

    private static Logger LOGGER = 
            Logger.getLogger(ExtractorJS.class.getName());

    // finds whitespace- and quote-free strings in Javascript
    // (areas between paired ' or " characters, possibly backslash-quoted
    // on the ends, but not in the middle)
    protected static final String JAVASCRIPT_STRING_EXTRACTOR =
    		"(\\\\{0,8}+(?:['\"]|u002[27]))([^\\s'\"]{1,"+UURI.MAX_URL_LENGTH+"})(?:\\1)";
    
    // GROUPS:
    // (G1) ' or " with optional leading backslashes
    // (G2) whitespace-free string delimited on boths ends by G1

    protected long numberOfCURIsHandled = 0;

    protected boolean shouldExtract(CrawlURI uri) {
        String contentType = uri.getContentType();
        if (contentType == null) {
        	LOGGER.info("ExtractorJS.shouldExtract: Content-type is null. rejected for extraction");
            return false;
        }

        // If the content-type indicates js, we should process it.
        if (contentType.indexOf("javascript") >= 0) {
        	LOGGER.info("Content-type matches 'javascript'. accepted for extraction");
            return true;
        }
        if (contentType.indexOf("jscript") >= 0) {
        	LOGGER.info("Content-type matches 'jscript'. accepted for extraction");
            return true;
        }
        if (contentType.indexOf("ecmascript") >= 0) {
        	LOGGER.info("Content-type matches 'ec,ascript'. accepted for extraction");
            return true;
        }

        if (contentType.startsWith("application/json")) {
        	LOGGER.info("Content-type matches 'application/json'. accepted for extraction");
            return true;
        }
        
        // If the filename indicates js, we should process it.
        if (uri.toString().toLowerCase().endsWith(".js")) {
        	LOGGER.info("Url ends with '.js': accepted for extraction");
            return true;
        }
        
        // If the viaContext indicates a script, we should process it.
        LinkContext context = uri.getViaContext();
        if (context == null) {
        	LOGGER.info("No linkContext - rejected for extraction");
            return false;
        }
        String s = context.toString().toLowerCase();
        if (s.startsWith("script")) {
        	LOGGER.info("The linkContext starts with 'script'. Accepted for extraction. The full linkcontext is: " + s);
        	return true;
        } else {
        	LOGGER.info("The linkContext does not start with 'script'. rejected for extraction. The full linkcontext is: " + s);
        	return false;
        }
    }

    @Override
    protected boolean innerExtract(CrawlURI curi) {
        this.numberOfCURIsHandled++;
        ReplayCharSequence cs = null;
        try {
            cs = curi.getRecorder().getContentReplayCharSequence();
            try {
                numberOfLinksExtracted.addAndGet(considerStrings(curi, cs));
            } catch (StackOverflowError e) {
                DevUtils.warnHandle(e, "ExtractorJS StackOverflowError");
            }
            // Set flag to indicate that link extraction is completed.
            return true;
        } catch (IOException e) {
            curi.getNonFatalFailures().add(e);
        }
        return false;
    }

    protected long considerStrings(CrawlURI curi, CharSequence cs) {
        return considerStrings(this, curi, cs, true);
    }
    
    public long considerStrings(Extractor ext, 
            CrawlURI curi, CharSequence cs) {
        return considerStrings(ext, curi, cs, false);
    }
    
    public long considerStrings(Extractor ext, 
            CrawlURI curi, CharSequence cs, boolean handlingJSFile) {
        long foundLinks = 0;
        
        Matcher strings =
            TextUtils.getMatcher(JAVASCRIPT_STRING_EXTRACTOR, cs);
        
        int startIndex = 0;
        while (strings.find(startIndex)) {
            CharSequence subsequence =
            		cs.subSequence(strings.start(2), strings.end(2));
            
            if (UriUtils.isPossibleUri(subsequence)) {
            	LOGGER.info("Found possibleURIcandidate url '" +  subsequence 
            			+ "' (curi = " + curi + ")");
                if (considerString(ext, curi, handlingJSFile, subsequence.toString())) {
                    foundLinks++;
                }
            }

            startIndex = strings.end(1);
        }
        TextUtils.recycleMatcher(strings);
        return foundLinks;
    }


    protected boolean considerString(Extractor ext, CrawlURI curi,
            boolean handlingJSFile, String candidate) {
    	LOGGER.info("Considering candidate url '" +  candidate 
    			+ "' (curi = " + curi + ")");
    	String bean = null;
    	if (ext != null){
    		bean = ext.getBeanName();
    	}
        try {
            candidate = StringEscapeUtils.unescapeJavaScript(candidate);
            LOGGER.info("(on behalf of " + bean + ")Candidate url after unescapingJavaScript '" + candidate 
        			+ "' (curi = " + curi+ ")");
            
        } catch (NestableRuntimeException e) {
            LOGGER.log(Level.WARNING, "problem unescaping some javascript", e);
        }
        
        candidate = UriUtils.speculativeFixup(candidate, curi.getUURI());
        LOGGER.info("(on behalf of " + bean + ")candidate url after speculativeFixup '" + candidate 
    			+ "' (curi = " + curi+ ")");

        if (UriUtils.isVeryLikelyUri(candidate)) {
        	LOGGER.info("(on behalf of " + bean + ")candidate url  '" + candidate 
        			+ "' considered veryLikelyURI (curi = " + curi + ")");
            try {
                int max = ext.getExtractorParameters().getMaxOutlinks();
                if (handlingJSFile) {
                	LOGGER.info("(on behalf of " + bean + ")Calling addRelativeToVia that adds '" + candidate + "' as acceptable new link");
                	addRelativeToVia(curi, max, candidate, JS_MISC, 
                            SPECULATIVE);
                    return true;
                } else {
                	LOGGER.info("(on behalf of " + bean + ")Calling addRelativeToBase that adds '" + candidate + "' as acceptable new link");
                    addRelativeToBase(curi, max, candidate, JS_MISC, 
                            SPECULATIVE);
                    return true;
                }
            } catch (URIException e) {
                ext.logUriError(e, curi.getUURI(), candidate);
            }
        }
        
        return false;
    }
}
