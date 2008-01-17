/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.harvester.harvesting;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A post processor that adds an annotation
 *   content-size:<bytes>
 * for each succesfully harvested URI.
 *
 */

public class ContentSizeAnnotationPostProcessor extends Processor {

    /** Prefix asssociated with annotations made by this processor.*/
    public static final String CONTENT_SIZE_ANNOTATION_PREFIX = "content-size:";

    /**
     * Constructor.
     * @param name the name of the processor.
     * @see Processor
     */
    public ContentSizeAnnotationPostProcessor(String name) {
        super(name, "A post processor that adds an annotation"
                    + " content-size:<bytes> for each successfully harvested"
                    + " URI.");
    }

    /** For each URI with a succesful status code (status code > 0),
     *  add annotation with content size.
     * @param crawlURI URI to add annotation for if succesful.
     * @throws ArgumentNotValid if crawlURI is null.
     * @throws InterruptedException never.
     * @see Processor#innerProcess(org.archive.crawler.datamodel.CrawlURI)
     */
    protected void innerProcess(CrawlURI crawlURI) throws InterruptedException {
        ArgumentNotValid.checkNotNull(crawlURI, "CrawlURI crawlURI");
        if (crawlURI.getFetchStatus() > 0) {
            crawlURI.addAnnotation(CONTENT_SIZE_ANNOTATION_PREFIX
                                   + crawlURI.getContentSize());
        }
    }
}
