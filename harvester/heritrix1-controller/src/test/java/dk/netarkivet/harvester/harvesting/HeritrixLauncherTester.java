/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.URIException;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.event.CrawlStatusListener;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.framework.exceptions.InitializationException;
import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;
import org.archive.crawler.settings.SettingsHandler;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.harvesting.controller.DirectHeritrixController;
import dk.netarkivet.testutils.TestResourceUtils;
import dk.netarkivet.testutils.XmlAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Tests various aspects of launching Heritrix and Heritrix' capabilities. Note that some of these tests require much
 * heap space, so JVM parameter -Xmx512M may be required.
 */
@SuppressWarnings({"deprecation", "unused", "unchecked"})
public class HeritrixLauncherTester {
    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    private MoveTestFiles mtf;
    private File dummyLuceneIndex;

    public HeritrixLauncherTester() {
        mtf = new MoveTestFiles(Heritrix1ControllerTestInfo.CRAWLDIR_ORIGINALS_DIR, Heritrix1ControllerTestInfo.WORKING_DIR);
    }

    @Before
    public void initialize() {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        Settings.set(HarvesterSettings.METADATA_FORMAT, "arc");
    }

    @Before
    public void setUp() throws IOException {
        mtf = new MoveTestFiles(Heritrix1ControllerTestInfo.CRAWLDIR_ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
        dummyLuceneIndex = mtf.newTmpDir();
        // Uncommented to avoid reference to archive module from harvester module.
        //
        // FIXME This makes HeritrixLauncherTester#testSetupOrderFile fail, as it requires
        // this method to be run
        // dk.netarkivet.archive.indexserver.LuceneUtils.makeDummyIndex(dummyLuceneIndex);
    }

    @After
    public void tearDown() {
        mtf.tearDown();
    }

    /**
     * Centralized place for tests to construct a HeritrixLauncher. - Constructs the given crawlDir. - Copies the given
     * order.xml to the proper place in the given crawlDir. - Copies the standard seeds.txt to the proper place in the
     * given crawlDir. - Constructs a HeritrixLauncher and returns it
     *
     * @param origOrderXml original order.xml
     * @param indexDir
     * @return a HeritrixLauncher used by (most) tests.
     */
    private HeritrixLauncher getHeritrixLauncher(File origOrderXml, File indexDir) {
        File origSeeds = Heritrix1ControllerTestInfo.SEEDS_FILE;
        File crawlDir = Heritrix1ControllerTestInfo.HERITRIX_TEMP_DIR;
        crawlDir.mkdirs();
        File orderXml = new File(crawlDir, "order.xml");
        File seedsTxt = new File(crawlDir, "seeds.txt");
        FileUtils.copyFile(origOrderXml, orderXml);
        FileUtils.copyFile(origSeeds, seedsTxt);
        HeritrixFiles files = new HeritrixFiles(crawlDir, new JobInfoTestImpl(Long.parseLong(Heritrix1ControllerTestInfo.ARC_JOB_ID),
                Long.parseLong(Heritrix1ControllerTestInfo.ARC_HARVEST_ID)));
        // If deduplicationMode != NO_DEDUPLICATION
        // write the zipped index to the indexdir inside the crawldir
        if (orderXml.exists() && orderXml.length() > 0
                && HeritrixTemplate.isDeduplicationEnabledInTemplate(XmlUtils.getXmlDoc(orderXml))) {
            assertNotNull("Must have a non-null index when deduplication is enabled", indexDir);
            files.setIndexDir(indexDir);
            assertTrue("Indexdir should exist now ", files.getIndexDir().isDirectory());
            assertTrue("Indexdir should contain real contents now", files.getIndexDir().listFiles().length > 0);
        }

        return HeritrixLauncherFactory.getInstance(files);
    }

    /**
     * Check that all urls in the given array are listed in the crawl log. Calls fail() at the first url that is not
     * found or if the crawl log is not found.
     *
     * @param urls An array of url strings
     * @throws IOException
     */
    protected void assertAllUrlsInCrawlLog(String[] urls) throws IOException {
        String crawlLog = "";
        crawlLog = FileUtils.readFile(Heritrix1ControllerTestInfo.HERITRIX_CRAWL_LOG_FILE);

        for (String s : Arrays.asList(urls)) {
            if (crawlLog.indexOf(s) == -1) {
                System.out.println("Crawl log: ");
                System.out.println(crawlLog);
                fail("URL " + s + " not found in crawl log");
            }
        }
    }

    /**
     * Check that no urls in the given array are listed in the crawl log. Calls fail() at the first url that is found or
     * if the crawl log is not found.
     *
     * @param urls An array of url strings
     * @throws IOException
     */
    protected void assertNoUrlsInCrawlLog(String[] urls) throws IOException {
        String crawlLog = "";
        crawlLog = FileUtils.readFile(Heritrix1ControllerTestInfo.HERITRIX_CRAWL_LOG_FILE);

        for (String s : Arrays.asList(urls)) {
            if (crawlLog.indexOf(s) != -1) {
                System.out.println("Crawl log: ");
                System.out.println(crawlLog);
                fail("URL " + s + " found in crawl log at " + crawlLog.indexOf(s));
            }
        }
    }

    /**
     * Test that the launcher aborts given a non-existing order-file.
     */
    @Test
    public void testStartMissingOrderFile() {
        try {
            HeritrixLauncherFactory.getInstance(new HeritrixFiles(mtf.newTmpDir(), new JobInfoTestImpl(42L, 42L)));
            fail("Expected IOFailure");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /**
     * Test that the launcher aborts given a non-existing seeds-file.
     */
    @Test
    public void testStartMissingSeedsFile() {
        try {
            HeritrixFiles hf = new HeritrixFiles(Heritrix1ControllerTestInfo.WORKING_DIR, new JobInfoTestImpl(42L, 42L));
            hf.getSeedsTxtFile().delete();
            HeritrixLauncherFactory.getInstance(hf);
            fail("Expected FileNotFoundException");
        } catch (ArgumentNotValid e) {
            // This is correct
        }
    }

    /**
     * Test that the launcher handles heritrix dying on a bad order file correctly.
     */
    @Test
    public void testStartBadOrderFile() {
        myTesterOfBadOrderfiles(Heritrix1ControllerTestInfo.BAD_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the disk node correctly.
     */
    @Test
    public void testStartMissingDiskFieldOrderFile() {
        myTesterOfBadOrderfiles(Heritrix1ControllerTestInfo.MISSING_DISK_FIELD_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the arcs-path node correctly.
     */

    @Test
    @Ignore("was commented out")
    public void testStartMissingARCsPathOrderFile() {
        myTesterOfBadOrderfiles(Heritrix1ControllerTestInfo.MISSING_ARCS_PATH_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the seedsfile node correctly.
     */
    @Test
    public void testStartMissingSeedsfileOrderFile() {
        myTesterOfBadOrderfiles(Heritrix1ControllerTestInfo.MISSING_SEEDS_FILE_ORDER_FILE);
    }

    /**
     * Test that the launcher handles heritrix dying on a order file missing the seedsfile node correctly.
     */
    @Test
    @Ignore("was commented out")
    public void testStartMissingPrefixOrderFile() {
        myTesterOfBadOrderfiles(Heritrix1ControllerTestInfo.MISSING_PREFIX_FIELD_ORDER_FILE);
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
        } catch (IllegalState e) {
            // expected case since a searched node could not be found in the bad
            // XML-order-file!
        }
    }

    /**
     * Test that the launcher handles an empty order file correctly.
     */
    @Test
    public void testStartEmptyFile() {
        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.EMPTY_ORDER_FILE, null);

        try {
            hl.doCrawl();
            fail("An exception should have been caught when launching with an empty order.xml file !");
        } catch (IOFailure e) {
            // Expected case
        }
    }

    /**
     * Test that starting a job does not throw an Exception. Will fail if tests/dk/netarkivet/jmxremote.password has
     * other rights than -r------
     * <p>
     * FIXME Fails on Hudson
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    @Ignore("Heritrix jar file not found")
    public void failingTestStartJob() throws NoSuchFieldException, IllegalAccessException {
        // HeritrixLauncher hl = getHeritrixLauncher(TestInfo.ORDER_FILE, null);
        // HeritrixLauncher hl = new HeritrixLauncher();
        // HeritrixFiles files =
        // (HeritrixFiles) ReflectUtils.getPrivateField(hl.getClass(),
        // "files").get(hl);
        // ReflectUtils.getPrivateField(hl.getClass(),
        // "heritrixController").set(hl, new TestCrawlController(files));
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$TestCrawlController");
        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        hl.doCrawl();
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");

    }

    /**
     * Test that the HostnameQueueAssignmentPolicy returns correct queue-names for different URLs. The
     * HostnameQueueAssignmentPolicy is the default in heritrix - our own DomainnameQueueAssignmentPolicy extends this
     * one and expects that it returns the right values
     */
    @Test
    @Ignore ("Missing tests dir after module refactoring")
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
            assertEquals("Should get host name from normal URL", hqap.getClassKey(new CrawlController(), cauri),
                    "www.netarkivet.dk");

            /**
             * Second test tests that foo.www.netarkivet.dk goes into a queue called: foo.www.netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://foo.www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get host name from non-www URL", hqap.getClassKey(new CrawlController(), cauri),
                    "foo.www.netarkivet.dk");

            /**
             * Third test tests that a https-URL goes into a queuename called www.domainname#443 (default syntax)
             */
            uri = UURIFactory.getInstance("https://www.netarkivet.dk/foo/bar.php");
            cauri = new CandidateURI(uri);
            assertEquals("Should get port-extended host name from HTTPS URL",
                    hqap.getClassKey(new CrawlController(), cauri), "www.netarkivet.dk#443");
        } catch (URIException e) {
            fail("Should not throw exception on valid URI's");
        }
    }

    /**
     * Test that the DomainnameQueueAssignmentPolicy returns correct queue-names for different URL's
     */
    @Test
    @Ignore ("Missing tests dir after module refactoring")
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
            assertEquals("Should get base domain name from normal URL", dqap.getClassKey(new CrawlController(), cauri),
                    "netarkivet.dk");

            /**
             * Second test tests that foo.www.netarkivet.dk goes into a queue called: netarkivet.dk
             */
            uri = UURIFactory.getInstance("http://foo.www.netarkivet.dk/foo/bar.cgi");
            cauri = new CandidateURI(uri);
            assertEquals("Should get base domain name from non-www URL",
                    dqap.getClassKey(new CrawlController(), cauri), "netarkivet.dk");

            /**
             * Third test tests that a https-URL goes into a queuename called domainname (default syntax)
             */
            uri = UURIFactory.getInstance("https://www.netarkivet.dk/foo/bar.php");
            cauri = new CandidateURI(uri);
            assertEquals("HTTPS should go into domains queue as well", "netarkivet.dk",
                    dqap.getClassKey(new CrawlController(), cauri));
        } catch (URIException e) {
            fail("Should not throw exception on valid URI's");
        }
    }

    /**
     * Tests, that the Heritrix order files is setup correctly. FIXME: Changed from " testSetupOrderFile()" to
     * FailingtestSetupOrderFile(), as it fails without dummyIndex
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Test
    @Ignore("apparently fails without dummyIndex")
    public void FailingtestSetupOrderFile() throws NoSuchFieldException, IllegalAccessException {

        /**
         * Check the DeduplicationType.NO_DEDUPLICATION type of deduplication is setup correctly
         */

        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        hl.setupOrderfile(hl.getHeritrixFiles());

        File orderFile = new File(Heritrix1ControllerTestInfo.HERITRIX_TEMP_DIR, "order.xml");
        Document doc = XmlUtils.getXmlDoc(orderFile);
        /* check, that deduplicator is not enabled in the order */
        assertFalse("Should not have deduplication enabled", HeritrixTemplate.isDeduplicationEnabledInTemplate(doc));

        /**
         * Check the DeduplicationType.DEDUPLICATION_USING_THE_DEDUPLICATOR type of deduplication is setup correctly
         */

        hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.DEDUP_ORDER_FILE, dummyLuceneIndex);
        hl.setupOrderfile(hl.getHeritrixFiles());

        // check, that the deduplicator is present in the order
        doc = XmlUtils.getXmlDoc(orderFile);
        assertTrue("Should have deduplication enabled", HeritrixTemplate.isDeduplicationEnabledInTemplate(doc));
        XmlAsserts.assertNodeWithXpath(doc, HeritrixTemplate.DEDUPLICATOR_XPATH);
        XmlAsserts.assertNodeWithXpath(doc, HeritrixTemplate.DEDUPLICATOR_INDEX_LOCATION_XPATH);
        XmlAsserts.assertNodeTextInXpath("Should have set index to right directory", doc,
                HeritrixTemplate.DEDUPLICATOR_INDEX_LOCATION_XPATH, dummyLuceneIndex.getAbsolutePath());
    }

    /**
     * Tests that HeritricLauncher will fail on an error in HeritrixController.initialize().
     * <p>
     * FIXME Fails in Hudson
     */
    @Test
    @Ignore("fails in hudson")
    public void failingTestFailOnInitialize() throws NoSuchFieldException, IllegalAccessException {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$SucceedOnCleanupTestController");
        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        try {
            hl.doCrawl();
            fail("HeritrixLanucher should throw an exception when it fails to initialize");
        } catch (IOFailure e) {
            assertTrue("Error message should be from initialiser", e.getMessage().contains("initialize"));
            // expected
        }
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");
    }

    /**
     * When the an exception is thrown in cleanup, any exceptions thrown in the initialiser are lost.
     */
    @Test (expected = Exception.class)
    public void testFailOnCleanup() {
        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        hl.doCrawl();
    }

    /**
     * A failure to communicate with heritrix during the crawl should be logged but not be in any way fatal to the
     * crawl.
     * <p>
     */
    @Test
    @Ignore ("Missing tests dir after module refactoring")
    public void testFailDuringCrawl() {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.HeritrixLauncherTester$FailDuringCrawlTestController");
        HeritrixLauncher hl = getHeritrixLauncher(Heritrix1ControllerTestInfo.ORDER_FILE_WITH_DEDUPLICATION_DISABLED, null);
        hl.doCrawl();
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS,
                "dk.netarkivet.harvester.harvesting.JMXHeritrixController");

    }

    /**
     * A class that closely emulates CrawlController, except it never starts Heritrix.
     */
    public static class TestCrawlController extends DirectHeritrixController {
        private static final long serialVersionUID = 1L;
        /**
         * List of crawl status listeners.
         * <p>
         * All iterations need to synchronize on this object if they're to avoid concurrent modification exceptions. See
         * {@link java.util.Collections#synchronizedList(List)}.
         */
        private List<CrawlStatusListener> listeners = new ArrayList<CrawlStatusListener>();

        public TestCrawlController(HeritrixFiles files) {
            super(files);
        }

        /**
         * Register for CrawlStatus events.
         *
         * @param cl a class implementing the CrawlStatusListener interface
         * @see CrawlStatusListener
         */
        @Override
        public void addCrawlStatusListener(CrawlStatusListener cl) {
            synchronized (this.listeners) {
                this.listeners.add(cl);
            }
        }

        /**
         * Operator requested crawl begin
         */
        @Override
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
         * Starting from nothing, set up CrawlController and associated classes to be ready for a first crawl.
         */
        public void initialize(SettingsHandler sH) throws InitializationException {}
        @Override
        public void requestCrawlStop(String test) {}
    }
}
