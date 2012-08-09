/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 *
 */
package dk.netarkivet.harvester.tools;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.framework.Processor;
import org.archive.crawler.settings.SimpleType;
import org.archive.crawler.settings.StringList;
import org.archive.crawler.settings.Type;

public class TwitterHarvesterProcessor extends Processor {

    public static final String PROCESSOR_NAME = "Twitter Harvester Processor";

    public static final String PROCESSOR_FULL_NAME = TwitterHarvesterProcessor.class
            .getName();

    public static final String PROCESSOR_DESCRIPTION = "Harvests Twitter and embedded url's via Twitter API";

    static Logger logger = Logger.getLogger(PROCESSOR_FULL_NAME);

    /**
     * Here we define bean properties which specify the search parameters for Twitter
     *
     */
    public static final String ATTR_KEYWORDS= "keywords";
    public static final String ATTR_PAGES = "pages";




    public TwitterHarvesterProcessor(String name) {
        super(name, PROCESSOR_NAME);
        Type e = addElementToDefinition(new StringList(ATTR_KEYWORDS, "Keywords to search for"));
        e = addElementToDefinition(new SimpleType(ATTR_PAGES, "Number of pages of twitter results to use.", new Integer(0) ));
    }

    @Override
    protected void initialTasks() {
        super.initialTasks();
        logger.info("Initial tasks for " + PROCESSOR_FULL_NAME);
        System.out.println("Initial tasks for " + PROCESSOR_FULL_NAME);
        StringList keywords = (StringList) getAttributeUnchecked(ATTR_KEYWORDS);
        for (Object keyword: keywords) {
            logger.info("Twitter processor keyword: " + keyword);
            System.out.println("Twitter processor keyword: " + keyword);
        }
        int pages = ((Integer) getAttributeUnchecked(ATTR_PAGES)).intValue();
        logger.info("Twitter processor will queue " + pages + " page(s) of results.");
        System.out.println("Twitter processor will queue " + pages + " page(s) of results.");
    }

    /**
     * Version of getAttributes that catches and logs exceptions
     * and returns null if failure to fetch the attribute.
     * @param name Attribute name.
     * @return Attribute or null.
     */
    public Object getAttributeUnchecked(String name) {
        Object result = null;
        try {
            result = super.getAttribute(name);
        } catch (AttributeNotFoundException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (MBeanException e) {
            logger.warning(e.getLocalizedMessage());
        } catch (ReflectionException e) {
            logger.warning(e.getLocalizedMessage());
        }
        return result;
    }


    @Override
    protected void innerProcess(CrawlURI curi) throws InterruptedException {
        super.innerProcess(
                curi);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String report() {
        return "Goodbye Cruel World";
    }

}
