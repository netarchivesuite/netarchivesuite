package dk.netarkivet.harvester.tools;

import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import junit.framework.TestCase;
import org.archive.crawler.settings.CrawlerSettings;
import org.archive.crawler.settings.XMLSettingsHandler;

import javax.management.InvalidAttributeValueException;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: csr
 * Date: 8/9/12
 * Time: 10:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TwitterHarvesterProcessorTest extends TestCase {

    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
                TestInfo.WORKING_DIR);

    @Override
    protected void setUp() throws Exception {
        super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
        mtf.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();    //To change body of overridden methods use File | Settings | File Templates.
        mtf.tearDown();
    }

    public void testCTOR() throws InvalidAttributeValueException {
        final File orderXml = new File(TestInfo.WORKING_DIR, "twitter_orderxml.xml");
        assertTrue(orderXml.exists());
        XMLSettingsHandler handler = new XMLSettingsHandler(orderXml);
        handler.initialize();
        handler.getOrCreateSettingsObject("twitter.com");
        CrawlerSettings settings =  handler.getSettings("twitter.com");
        CrawlerSettings settings2 = new CrawlerSettings(handler, "twitter.com");
        TwitterHarvesterExtractor processor = new TwitterHarvesterExtractor("aname");
        processor.earlyInitialize(settings);
        processor.initialTasks();
    }

}
