
package dk.netarkivet.harvester.harvesting;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A post processor that adds an annotation
 *   content-size:<bytes>
 * for each successfully harvested URI.
 *
 */
@SuppressWarnings({ "serial"})
public class ContentSizeAnnotationPostProcessor extends Processor {

    /** Prefix associated with annotations made by this processor.*/
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

    /** For each URI with a successful status code (status code > 0),
     *  add annotation with content size.
     * @param crawlURI URI to add annotation for if successful.
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
