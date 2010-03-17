/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.Frontier;
import org.archive.crawler.framework.FrontierMarker;
import org.archive.crawler.framework.exceptions.EndedException;
import org.archive.crawler.framework.exceptions.FatalConfigurationException;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.framework.exceptions.InvalidFrontierMarkerException;
import org.archive.crawler.frontier.FrontierJournal;
import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.dom4j.Document;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.LuceneUtils;
import dk.netarkivet.testutils.XmlAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;

/**
 * Tests various aspects of launching Heritrix and Heritrix' capabilities.
 * Note that some of these tests require much heap space, so JVM parameter
 * -Xmx512M may be required.
 * Created by IntelliJ IDEA.
 * User: larsrc
 * Date: Dec 2, 2004
 * Time: 11:55:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class HeritrixLauncherTester extends TestCase {

    private MoveTestFiles mtf;
    private File dummyLuceneIndex;

    public HeritrixLauncherTester() {
        mtf = new MoveTestFiles (TestInfo.CRAWLDIR_ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    public void setUp() throws IOException {
        mtf.setUp();
        dummyLuceneIndex = mtf.newTmpDir();
        LuceneUtils.makeDummyIndex(dummyLuceneIndex);
    }

    public void tearDown() {
        mtf.tearDown();
    }

    /**
     * Centralized place for tests to construct a HeritrixLauncher.
     *  - Constructs the given crawlDir.
     *  - Copies the given order.xml to the proper place in the given crawlDir.
     *  - Copies the standard seeds.txt to the proper place in the given crawlDir.
     *  - Constructs a HeritrixLauncher and returns it
     *  @param origOrderXml original order.xml
     *  @param indexDir
     *  @return a HeritrixLauncher used by (most) tests.
     */
    private HeritrixLauncher getHeritrixLauncher(File origOrderXml,
                                                 File indexDir) {
        File origSeeds = TestInfo.SEEDS_FILE;
        File crawlDir = TestInfo.HERITRIX_TEMP_DIR;
        crawlDir.mkdirs();
        File orderXml = new File(crawlDir, "order.xml");
        File seedsTxt = new File(crawlDir, "seeds.txt");
        FileUtils.copyFile(origOrderXml, orderXml);
        FileUtils.copyFile(origSeeds, seedsTxt);
        HeritrixFiles files = new HeritrixFiles(crawlDir,
                                                Long.parseLong(TestInfo.ARC_JOB_ID),
                                                Long.parseLong(TestInfo.ARC_HARVEST_ID));
        // If deduplicationMode != NO_DEDUPLICATION
        // write the zipped index to the indexdir inside the crawldir
        if (orderXml.exists() && orderXml.length() > 0 &&    
            HeritrixLauncher.isDeduplicationEnabledInTemplate(XmlUtils.getXmlDoc(orderXml))) {
            assertNotNull("Must have a non-null index when deduplication is enabled",
                          indexDir);
            files.setIndexDir(indexDir);
            assertTrue("Indexdir should exist now ", files.getIndexDir().isDirectory());
            assertTrue("Indexdir should contain real contents now", files.getIndexDir().listFiles().length > 0);
        }

        return HeritrixLauncher.getInstance(files);
    }
    /**
     * Check that all urls in the given array are listed in the crawl log.
     * Calls fail() at the first url that is not found or if the crawl log is not
     * found.
     *
     * @param urls An array of url strings
     * @throws IOException
     */
    protected void assertAllUrlsInCrawlLog(String[] urls) throws IOException {
        String crawlLog = "";
        crawlLog = FileUtils.readFile(TestInfo.HERITRIX_CRAWL_LOG_FILE);

        for (String s : Arrays.asList(urls)) {
            if (crawlLog.indexOf(s) == -1) {
                System.out.println("Crawl log: ");
                System.out.println(crawlLog);
                fail("URL " + s + " not found in crawl log");
            }
        }
    }

    /**
     * Check that no urls in the given array are listed in the crawl log.
     * Calls fail() at the first url that is found or if the crawl log is not
     * found.
     *
     * @param urls An array of url strings
     * @throws IOException
     */
    protected void assertNoUrlsInCrawlLog(String[] urls) throws IOException {
        String crawlLog = "";
        crawlLog = FileUtils.readFile(TestInfo.HERITRIX_CRAWL_LOG_FILE);

        for (String s : Arrays.asList(urls)) {
            if (crawlLog.indexOf(s) != -1) {
                System.out.println("Crawl log: ");
                System.out.println(crawlLog);
                fail("URL " + s + " found in crawl log at " + crawlLog.indexOf(
                        s));
            }
        }
    }

    /**
     * Test that the launcher aborts given a non-existing order-file.
     */
    public void testStartMissingOrderFile() {
        try {
            HeritrixLauncher.getInstance(
                    new HeritrixFiles(mtf.newTmpDir(), 42, 42));
            fail("Expected IOFailure");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /**
     * Test that the launcher aborts given a non-existing seeds-file.
     */
    public void testStartMissingSeedsFile() {
        try {
            HeritrixFiles hf = new HeritrixFiles(TestInfo.WORKING_DIR, 42, 42);
            hf.getSeedsTxtFile().delete();
            HeritrixLauncher.getInstance(hf);
            fail("Expected FileNotFoundException");
        } catch (ArgumentNotValid e) {
            // This is correct
        }
    }

    /**
     * Test that the launcher handles heritrix dying on a bad order file correctly.
     */
    public void testStartBadOrderFile() {
        myTesterOfBadOrderfiles(TestInfo.BAD_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the disk node correctly.
     */
    public void testStartMissingDiskFieldOrderFile() {
        myTesterOfBadOrderfiles(TestInfo.MISSING_DISK_FIELD_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the arcs-path node correctly.
     */
    public void testStartMissingARCsPathOrderFile() {
        myTesterOfBadOrderfiles(TestInfo.MISSING_ARCS_PATH_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the seedsfile node correctly.
     */
    public void testStartMissingSeedsfileOrderFile() {
        myTesterOfBadOrderfiles(TestInfo.MISSING_SEEDS_FILE_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the seedsfile node correctly.
     */
    public void testStartMissingPrefixOrderFile() {
        myTesterOfBadOrderfiles(TestInfo.MISSING_PREFIX_FIELD_ORDER_FILE);
    }

    /**
     * This method is used to test various scenarios with bad order-files.
     *
     * @param orderfile
     */

    private void myTesterOfBadOrderfiles(File orderfile) {
        HeritrixLauncher hl = getHeritrixLauncher(orderfile, null);

        try {
            hl.doCrawl();
            fail("An exception should have been caught when launching with a bad order.xml file !");
        } catch (IOFailure e) {
            // expected case since a searched node could not be found in the bad
            // XML-order-file!
        }
    }

    /**
     * Test that the launcher handles an empty order file correctly.
     */
    public void testStartEmptyFile() {
        HeritrixLauncher hl = getHeritrixLauncher(TestInfo.EMPTY_ORDER_FILE, null);

        try {
            hl.doCrawl();
            fail("An exception should have been caught when launching with an empty order.xml file !");
        } catch (IOFailure e) {
            // Expected case
        }
    }

    /** Test that starting a job does not throw an Exception.
     * Will fail if tests/dk/netarkivet/jmxremote.password has other rights 
     * than -r------ 
     * @throws NoSuchFieldException
     * @throws IllegalAccessException */
    public void testStartJob()
            throws NoSuchFieldException, IllegalAccessException {
        //HeritrixLauncher hl = getHeritrixLauncher(TestInfo.ORDER_FILE, null);
        //HeritrixLauncher hl = new HeritrixLauncher();
        //HeritrixFiles files =
        //        (HeritrixFiles) ReflectUtils.getPrivateField(hl.getClass(),
        //                                                     "files").get(hl);
        //ReflectUtils.getPrivateField(hl.getClass(),
        //		"heritrixController").set(hl, new TestCrawlController(files));
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$TestCrawlController");
        HeritrixLauncher hl = getHeritrixLauncher(
                TestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, 
                null);
        hl.doCrawl();
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");

    }

    /**
     * Test that the HostnameQueueAssignmentPolicy returns correct queue-names
     * for different URLs.
     * The HostnameQueueAssignmentPolicy is the default in heritrix
     * - our own DomainnameQueueAssignmentPolicy extends this one and expects that it returns the ritht values
     */
    public void testHostnameQueueAssignmentPolicy() {
        HostnameQueueAssignmentPolicy hqap = new HostnameQueueAssignmentPolicy();
        UURI uri;
        CandidateURI cauri;
        try {
            /**
             * First test tests that www.netarkivet.dk goes into a queue called: www.netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get host name from normal URL",
                         hqap.getClassKey(new CrawlController(),cauri),"www.netarkivet.dk");

            /**
             * Second test tests that foo.www.netarkivet.dk goes into a queue called: foo.www.netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://foo.www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get host name from non-www URL",
                         hqap.getClassKey(new CrawlController(),cauri),"foo.www.netarkivet.dk");

            /**
             * Third test tests that a https-URL goes into a queuename called www.domainname#443 (default syntax)
             */
            uri = UURIFactory.getInstance("https://www.netarkivet.dk/foo/bar.php");
            cauri = new CandidateURI(uri);
            assertEquals("Should get port-extended host name from HTTPS URL",
                         hqap.getClassKey(new CrawlController(),cauri),"www.netarkivet.dk#443");
        } catch (URIException e) {
            fail("Should not throw exception on valid URI's");
        }
    }

    /**
     * Test that the DomainnameQueueAssignmentPolicy returns correct queue-names for different URL's
     */
    public void testDomainnameQueueAssignmentPolicy() {
        DomainnameQueueAssignmentPolicy dqap = new DomainnameQueueAssignmentPolicy();
        UURI uri;
        CandidateURI cauri;
        try {
            /**
             * First test tests that www.netarkivet.dk goes into a queue called: netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get base domain name from normal URL",
                         dqap.getClassKey(new CrawlController(),cauri),"netarkivet.dk");

            /**
             * Second test tests that foo.www.netarkivet.dk goes into a queue called: netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://foo.www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get base domain name from non-www URL",
                         dqap.getClassKey(new CrawlController(),cauri),"netarkivet.dk");

            /**
             * Third test tests that a https-URL goes into a queuename called domainname (default syntax)
             */
            uri = UURIFactory.getInstance("https://www.netarkivet.dk/foo/bar.php");
            cauri = new CandidateURI(uri);
            assertEquals("HTTPS should go into domains queue as well",
                         "netarkivet.dk",
                         dqap.getClassKey(new CrawlController(),cauri));
        } catch (URIException e) {
            fail("Should not throw exception on valid URI's");
        }
    }

    /**
     * Tests, that the Heritrix order files is setup correctly.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void testSetupOrderFile()  throws NoSuchFieldException, IllegalAccessException {

        /**
         * Check the DeduplicationType.NO_DEDUPLICATION type of deduplication is setup correctly
         */

        HeritrixLauncher hl = getHeritrixLauncher(
                TestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, 
                null);
        hl.setupOrderfile();

        File orderFile = new File (TestInfo.HERITRIX_TEMP_DIR, "order.xml");
        Document doc = XmlUtils.getXmlDoc(orderFile);
        /* check, that deduplicator is not enabled in the order */
        assertFalse("Should not have deduplication enabled",
                    HeritrixLauncher.isDeduplicationEnabledInTemplate(doc));

        /**
         * Check the DeduplicationType.DEDUPLICATION_USING_THE_DEDUPLICATOR
         * type of deduplication is setup correctly
         */

        hl = getHeritrixLauncher(TestInfo.DEDUP_ORDER_FILE,
                                 dummyLuceneIndex);
        hl.setupOrderfile();

        // check, that the deduplicator is present in the order
        doc = XmlUtils.getXmlDoc(orderFile);
        assertTrue("Should have deduplication enabled",
                   HeritrixLauncher.isDeduplicationEnabledInTemplate(doc));
        XmlAsserts.assertNodeWithXpath(
                doc, HeritrixTemplate.DEDUPLICATOR_XPATH);
        XmlAsserts.assertNodeWithXpath(
                doc, HeritrixLauncher.DEDUPLICATOR_INDEX_LOCATION_XPATH);
        XmlAsserts.assertNodeTextInXpath(
                "Should have set index to right directory",
                doc, HeritrixLauncher.DEDUPLICATOR_INDEX_LOCATION_XPATH,
                dummyLuceneIndex.getAbsolutePath());
    }


    /**
     * Tests that HeritricLauncher will fail on an error in
     * HeritrixController.initialize().
     */
    public void testFailOnInitialize()
            throws NoSuchFieldException, IllegalAccessException {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$SucceedOnCleanupTestController");
        HeritrixLauncher hl = getHeritrixLauncher(
                TestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        try {
            hl.doCrawl();
            fail("HeritrixLanucher should throw an exception when it fails to initialize");
        } catch (IOFailure e) {
            assertTrue("Error message should be from initialiser", e.getMessage().contains("initialize"));
            //expected
        }
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");
    }

    /**
     * When the an exception is thrown in cleanup, any exceptions thrown in the
     * initialiser are lost.
     */
    public void testFailOnCleanup() {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$FailingTestController");
        HeritrixLauncher hl = getHeritrixLauncher(
                TestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        try {
            hl.doCrawl();
            fail("HeritrixLanucher should throw an exception when it fails to initialize");
        } catch (IOFailure e) {
            assertTrue("Error message should be from cleanup", 
                    e.getMessage().contains("cleanup"));
            //expected
        }
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");
    }

    /**
     * A failure to communicate with heritrix during the crawl should be logged
     * but not be in any way fatal to the crawl.
     */
    public void testFailDuringCrawl() {
          Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
          "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$FailDuringCrawlTestController");
        HeritrixLauncher hl = getHeritrixLauncher(
                TestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        hl.doCrawl();
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");

    }

    /**
     * A test heritrixController which starts and stops a crawl cleanly but fails
     * during the crawl itself.
     */
    public static class FailDuringCrawlTestController extends FailingTestController {

        private int isEndedCalls = 0;

        public FailDuringCrawlTestController(HeritrixFiles files) {
            super(files);
        }

        public void requestCrawlStop(String reason) {

        }

        public void initialize() {


        }

        public void requestCrawlStart() throws IOFailure {


        }

        public boolean atFinish() {
          return false;
        }

        public void beginCrawlStop() {
        }

        public void cleanup() {

        }

        public boolean crawlIsEnded() {
           if (isEndedCalls >= 3) {
               return true;
           } else {
               isEndedCalls++;
               throw new IOFailure("Failure in crawlIsEnded");
           }
        }
    }

    /**
     * A Heritrix Controller which fails on every call.
     */
    public static class FailingTestController implements HeritrixController {

        public FailingTestController(HeritrixFiles files) {};

        public void initialize() {
            //TODO: implement method
            throw new IOFailure("Failed to initialize");
        }

        public void requestCrawlStart() throws IOFailure {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public void beginCrawlStop() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public void requestCrawlStop(String reason) {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public boolean atFinish() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public boolean crawlIsEnded() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public int getActiveToeCount() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public long getQueuedUriCount() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public int getCurrentProcessedKBPerSec() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public String getProgressStats() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public boolean isPaused() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }

        public void cleanup() {
            throw new IOFailure("cleanup failure");
        }

        public String getHarvestInformation() {
            //TODO: implement method
            throw new IOFailure("Not implemented");
        }
    }

    /**
     * A heritrix controller which fails on everything except cleanup
     */
    public static class SucceedOnCleanupTestController extends FailingTestController {
        public SucceedOnCleanupTestController(HeritrixFiles files) {super(files);}
        public void cleanup(){return;}
    }


    /** A class that closely emulates CrawlController, except it never
    * starts Heritrix.
    */
    public static class TestCrawlController extends DirectHeritrixController {
       private static final long serialVersionUID = 1L;
       /**
        * List of crawl status listeners.
        *
        * All iterations need to synchronize on this object if they're to avoid
        * concurrent modification exceptions.
        * See {@link java.util.Collections#synchronizedList(List)}.
        */
       private List<CrawlStatusListener> listeners
       = new ArrayList<CrawlStatusListener>();

        public TestCrawlController(HeritrixFiles files) {
            super(files);
        }

        /**
        * Register for CrawlStatus events.
        *
        * @param cl a class implementing the CrawlStatusListener interface
        *
        * @see CrawlStatusListener
        */
       public void addCrawlStatusListener(CrawlStatusListener cl) {
           synchronized (this.listeners) {
               this.listeners.add(cl);
           }
       }

       /**
        * Operator requested crawl begin
        */
       public void requestCrawlStart() {
           new Thread() {
               public void run() {
                   for (CrawlStatusListener l : listeners) {
                       l.crawlEnding("Fake over");
                       l.crawlEnded("Fake all over");
                   }
               }
           }.start();
       }

       /**
        * Starting from nothing, set up CrawlController and associated
        * classes to be ready for a first crawl.
        *
        * @param sH
        * @throws InitializationException
        */
       public void initialize(SettingsHandler sH)
       throws InitializationException {

       }

       public void requestCrawlStop(String test){

       }

       public Frontier getFrontier(){
           return new TestFrontier();
       }

       /**
        * Dummy frontier used by TestCrawlController
        */
       class TestFrontier implements Frontier {

           public void initialize(CrawlController crawlController)
           throws FatalConfigurationException, IOException {}

           public CrawlURI next() throws InterruptedException, EndedException {return null;}

           public boolean isEmpty() {return false;}

           public void schedule(CandidateURI candidateURI) {}

           public void finished(CrawlURI crawlURI) {}

           public long discoveredUriCount() {return 0;}

           public long queuedUriCount() {return 0;}

           public long finishedUriCount() {return 0;}

           public long succeededFetchCount() {return 0;}

           public long failedFetchCount() {return 0;}

           public long disregardedUriCount() {return 0;}

           public long totalBytesWritten() {return 0;}

           public String oneLineReport() {return null;}

           public String report() {return null;}

           public void importRecoverLog(String s, boolean b) throws IOException {}

           public FrontierMarker getInitialMarker(String s, boolean b) {return null;}

           public ArrayList getURIsList(FrontierMarker frontierMarker, int i, boolean b)
           throws InvalidFrontierMarkerException {return null;}

           public long deleteURIs(String s) {return 0;}

           public void deleted(CrawlURI crawlURI) {}

           public void considerIncluded(UURI uuri) {}

           public void kickUpdate() {}

           public void pause() {}

           public void unpause() {}

           public void terminate() {}

           public FrontierJournal getFrontierJournal() {return null;}

           public String getClassKey(CandidateURI candidateURI) {return null;}

           public void loadSeeds() {}

           public String[] getReports() {return new String[0];}

           //public void reportTo(String s, PrintWriter printWriter) throws IOException {}
           public void reportTo(String s, PrintWriter printWriter) {}

           public void reportTo(PrintWriter printWriter) throws IOException {}

           public void singleLineReportTo(PrintWriter printWriter) throws IOException {}

           public String singleLineReport() { return null;}

           public String singleLineLegend(){ return null; }
           public void start(){}
           public Frontier.FrontierGroup getGroup(CrawlURI crawlURI) {
               return null;
           }
           public float congestionRatio() {
               return 0.0f;
           }
           public long averageDepth() {
               return 0L;
           }
           public long deepestUri() {
               return 0L;
           }

           public long deleteURIs(String arg0, String arg1) {
        	   return 0L;
           }


       }
   };
}
