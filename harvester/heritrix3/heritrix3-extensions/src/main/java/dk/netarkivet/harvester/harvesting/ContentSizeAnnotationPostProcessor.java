/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.harvesting;

import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A post processor that adds an annotation
 *   content-size:<bytes>
 * for each successfully harvested URI.
 * 
 *  The bean for this processor <bean id="ContentSizeAnnotationPostProcessor" class="dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor"/>
 *  should be added to the list of dispositionProcessors.
 *
 */
public class ContentSizeAnnotationPostProcessor extends Processor {

    /** Prefix associated with annotations made by this processor.*/
    public static final String CONTENT_SIZE_ANNOTATION_PREFIX = "content-size:";

    /**
     * Constructor.
     * @see Processor
     */
    public ContentSizeAnnotationPostProcessor() {
        super();
    }

    /** For each URI with a successful status code (status code > 0),
     *  add annotation with content size.
     * @param crawlURI URI to add annotation for if successful.
     * @throws ArgumentNotValid if crawlURI is null.
     * @throws InterruptedException never.
     * @see Processor
     */
    protected void innerProcess(CrawlURI crawlURI) throws InterruptedException {
        ArgumentNotValid.checkNotNull(crawlURI, "CrawlURI crawlURI");
        if (crawlURI.getFetchStatus() > 0) {
            crawlURI.getAnnotations().add(CONTENT_SIZE_ANNOTATION_PREFIX + crawlURI.getContentSize());
        }
    }

    @Override
    protected boolean shouldProcess(CrawlURI arg0) {
        if (arg0.isSuccess()) { 
            // TODO or maybe just: arg0.is2XXSuccess()
            // or maybe just: ?
            return true;
        }
        return false;
    }
}
