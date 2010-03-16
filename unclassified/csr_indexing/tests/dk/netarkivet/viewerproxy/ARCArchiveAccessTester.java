/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.viewerproxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import junit.framework.TestCase;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCKey;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for ARCArchiveAccess.  This only tests that we connect the CDX
 * lookup with the ARC files ok, because everything else is just being forwarded
 * and so is (supposed to be) tested elsewhere.
 */
public class ARCArchiveAccessTester extends TestCase {

    //Used files:
    private static final File MAIN_PATH = new File(
            "tests/dk/netarkivet/viewerproxy/data/");
    private static final File LOG_PATH = new File(MAIN_PATH, "tmp");

    /**
     * An URL not indexed in CDX_FILE. Initiated in setUp because it can throw
     * exception
     */
    private static URI WRONG_URL;
    /**
     * An URL not indexed in CDX_FILE. Initiated in setUp because it can throw
     * exception
     */
    private static URI GIF_URL;
    /**
     * The key listed for GIF_URL.
     */
    private static final ARCKey GIF_URL_KEY =
            new ARCKey(
                    "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc",
                    73269);

    private static final File LOG_FILE = new File(
            "tests/testlogs/netarkivtest.log");

    // Set up directories for local archive and bitarchive
    private static final File BASE_DIR = new File(
            "tests/dk/netarkivet/viewerproxy/data");
    private static final File ORIGINALS = new File(BASE_DIR, "input");
    private static final File WORKING = new File(BASE_DIR, "working");


    //A web archive controller that always returns our own test record:
    private ArcRepositoryClient fakeArcRepos;

    //A web archive controller that always returns null:
    private ArcRepositoryClient nullArcRepos;

    //A real web archive controller
    private ViewerArcRepositoryClient realArcRepos;

    //Our main instance of ARCArchiveAccess:
    private ARCArchiveAccess aaa;

    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws Exception {
        rs.setUp();
        WRONG_URL = new URI("http://www.test.dk/hest");
        GIF_URL = new URI(
                "http://netarkivet.dk/netarchive_alm/billeder/spacer.gif");

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();

        //We need a controller that doesn't do much more than return a test
        // record:
        fakeArcRepos = new TestArcRepositoryClient();
        nullArcRepos = new NullArcRepositoryClient();

        //Although we also need some real data
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        Settings.set(ViewerProxySettings.VIEWERPROXY_DIR, new File(WORKING, "viewerproxy").getAbsolutePath());
        FileUtils.createDir(new File(WORKING, "viewerproxy"));

        aaa = new ARCArchiveAccess(fakeArcRepos);
        aaa.setIndex(TestInfo.ZIPPED_INDEX_DIR);

        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, LOG_PATH.getAbsolutePath());
        Settings.set(CommonSettings.USE_REPLICA_ID, "ONE");
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, new File(WORKING, "admin-data").getAbsolutePath());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, LOG_PATH.getAbsolutePath());

        realArcRepos = ArcRepositoryClientFactory.getViewerInstance();

        FileInputStream fis
                = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
    }

    public void tearDown() {
        if (realArcRepos != null) {
            realArcRepos.close();
        }
        FileUtils.removeRecursively(WORKING);
        JMSConnectionMockupMQ.clearTestQueues();
        rs.tearDown();
    }

    /**
     * Verify that the constructor fails if and only if it is given null
     * parameter.
     */
    public void testConstructor() {
        //Verify construction with OK parameters does not fail:
        new ARCArchiveAccess(fakeArcRepos);
        //Verify that null parameters makes the constructor fail:
        try {
            new ARCArchiveAccess(null);
            fail("Expected argument not valid on null argument.");
        } catch (ArgumentNotValid e) {
            //Expected.
        }
    }

    public void testReadPage() throws Exception {
        Method readPage = ReflectUtils.getPrivateMethod(ARCArchiveAccess.class,
                                                        "readPage",
                                                        InputStream.class,
                                                        OutputStream.class);
        // Make an inputstream where read(byte[]) reads two chars at a time
        InputStream in = new ByteArrayInputStream("foo".getBytes()) {
            public int read(byte[] buf) {
                return super.read(buf, 0, 2);
            }
        };
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readPage.invoke(aaa, in, baos);
        baos.close();
        assertEquals(
                "Should get same bytes regardless of bytes read per read()",
                "foo", baos.toString());
    }

    public void testCreateErrorResponse() throws Exception {
        TestResponse response = new TestResponse();

        aaa.createNotFoundResponse(WRONG_URL, response);

        assertTrue("Url should be in output",
                   response.bas.toString().contains(WRONG_URL.toString()));
        assertTrue("Should have content type header",
                   response.names.contains("Content-type"));
        assertTrue("Should have test/html header value",
                   response.values.contains("text/html"));
        assertEquals("Should be a 404 URL", 404, response.statusInt);
        assertEquals("Reason should be as expected", "Not found",
                     response.reasonStr);

        response = new TestResponse();

        //Test it works twice.
        aaa.createNotFoundResponse(WRONG_URL, response);

        assertTrue("Url should be in output",
                   response.bas.toString().contains(WRONG_URL.toString()));
        assertTrue("Should have content type header",
                   response.names.contains("Content-type"));
        assertTrue("Should have test/html header value",
                   response.values.contains("text/html"));
        assertEquals("Should be a 404 URL", 404, response.statusInt);
        assertEquals("Reason should be as expected", "Not found",
                     response.reasonStr);
    }

    /**
     * Verify that looking up an object that does not exist returns some kind of
     * HTTP response reflecting this situation. For now, we just need it to
     * return ResponseCode.NOT_FOUND and not throw an Exception.
     */
    public void testLookupNonexistingObject() throws Exception {
        Response response = new TestResponse();
        URI uri = new URI("http://does.not.exist");
        int code = aaa.lookup(new TestRequest(uri), response);
        assertEquals("Should return a NOT_FOUND http code",
                     URIResolver.NOT_FOUND,
                     code);
    }

    /**
     * Test that looking up a known URI gives a positive response code.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void testLookupExistingObject()
            throws IOException, InterruptedException {
        TestResponse response = new TestResponse();
        int code = aaa.lookup(new TestRequest(GIF_URL), response);
        assertEquals("Should return a FOUND http code",
                     200,
                     code);
        assertEquals("Should have a location header",
                     1, response.names.size());
        assertEquals("Should have a location header",
                     1, response.values.size());
        assertEquals("Should have a location header",
                     "Location", response.names.get(0));
        assertEquals("Should have a location header",
                     GIF_URL_KEY.getFile().toString(), response.values.get(0));
    }

    /**
     * Test that AAA checks for null returns from the controller. This can
     * happen if a CDX file is not backed by an arc file, or arc repos times
     * out.
     *
     * @throws Exception
     */
    public void testNullControllerReturn()
            throws Exception {
        Response response = new TestResponse();
        ARCArchiveAccess nullaaa = new ARCArchiveAccess(nullArcRepos);
        nullaaa.setIndex(TestInfo.ZIPPED_INDEX_DIR);
        try {
            nullaaa.lookup(new TestRequest(GIF_URL), response);
            fail("Should throw exception if file is in index but gets null");
        } catch (IOFailure e) {
            //expected
        }
        LogUtils.flushLogs(ARCArchiveAccess.class.getName());
        assertTrue("Log file should exist after flushing",
                   LOG_FILE.exists());
        FileAsserts.assertFileContains(
                "Bitarchive problem must be reported in the log",
                "ARC file '"
                + GIF_URL_KEY.getFile()
                + "' mentioned in index file was not found", LOG_FILE);
    }

    /**
     * Fake arc repository client which on get returns a fake record which is
     * ok.
     */
    private class TestArcRepositoryClient extends JMSArcRepositoryClient {
        public TestArcRepositoryClient() {
            super();
        }

        /**
         * Returns an OK BitarchiveRecord. Content is simply arcfile name and
         * index encoded in a stream.
         */
        public BitarchiveRecord get(String arcFile, long index) {
            final Map<String, Object> metadata = new HashMap<String, Object>();
            for (String header_field : (List<String>)
                    ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                metadata.put(header_field, "");
            }
            byte[] data = ("HTTP/1.1 200 OK\nLocation: " + arcFile + "\n\n"
                           + arcFile + " " + index).getBytes();
            // TODO remove or replace this by something else ? (ARCConstants.LENGTH_HEADER_FIELD_KEY)
            // does not exist in Heritrix 1.10+
            //metadata.put(ARCConstants.LENGTH_HEADER_FIELD_KEY,
            //             Integer.toString(data.length));
            // Note: ARCRecordMetadata.getLength() now reads the contents of the LENGTH_FIELD_KEY
            // instead of LENGTH_HEADER_FIELD_KEY
            metadata.put(ARCConstants.LENGTH_FIELD_KEY,
                         Integer.toString(data.length));
            Long dummyOffset = new Long(
                    0L); //Note: offset is not stored as a String, but as a Long
            metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, dummyOffset);

            try {
                ARCRecordMetaData meta
                        = new ARCRecordMetaData(arcFile, metadata);
                return new BitarchiveRecord(
                        new ARCRecord(new ByteArrayInputStream(data),
                                      meta));
            } catch (IOException e) {
                fail("Cant't create metadata record");
                return null;
            }
        }
    }

    /**
     * Fake arc repository client which on get returns a fake record which is
     * null.
     */
    private class NullArcRepositoryClient extends JMSArcRepositoryClient {
        public NullArcRepositoryClient() {
            super();
        }

        public BitarchiveRecord get(String arcFile, long index) {
            return null;
        }
    }

    private static class TestResponse implements Response {
        ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream();
        int statusInt = 0;
        String reasonStr = "";
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        public TestResponse() {
        }

        public OutputStream getOutputStream() {
            return bas;
        }

        public void setStatus(int statusCode) {
            statusInt = statusCode;
        }

        public void setStatus(int statusCode, String reason) {
            statusInt = statusCode;
            reasonStr = reason;
        }

        public void addHeaderField(String name, String value) {
            names.add(name);
            values.add(value);
        }

        public int getStatus() {
            return statusInt;
        }
    }

    public class TestRequest implements Request {
        private URI uri;

        public TestRequest(URI uri) {
            this.uri = uri;
        }

        public URI getURI() {
            return uri;
        }

        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }
    }
}
