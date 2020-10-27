/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.viewerproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

import org.archive.io.arc   .ARCConstants;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.ARCKey;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for ARCArchiveAccess. This only tests that we connect the CDX lookup with the ARC files ok, because
 * everything else is just being forwarded and so is (supposed to be) tested elsewhere.
 */
@SuppressWarnings({"unused", "unchecked"})
public class ARCArchiveAccessTester {

    // Unused files:
    private static final File MAIN_PATH = new File("tests/dk/netarkivet/viewerproxy/data/");

    /**
     * An URL not indexed in CDX_FILE. Initiated in setUp because it can throw exception
     */
    private static URI WRONG_URL;
    /**
     * An URL not indexed in CDX_FILE. Initiated in setUp because it can throw exception
     */
    private static URI GIF_URL;
    /**
     * The key listed for GIF_URL.
     */
    private static final ARCKey GIF_URL_KEY = new ARCKey(
            "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc", 73269);

    // Set up directories for local archive and bitarchive
    private static final File BASE_DIR = new File("tests/dk/netarkivet/viewerproxy/data");
    private static final File ORIGINALS = new File(BASE_DIR, "input");
    private static final File WORKING = new File(BASE_DIR, "working");

    // A web archive controller that always returns our own test record:
    private ArcRepositoryClient fakeArcRepos;

    // A web archive controller that always returns null:
    private ArcRepositoryClient nullArcRepos;

    // Our main instance of ARCArchiveAccess:
    private ARCArchiveAccess aaa;

    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        WRONG_URL = new URI("http://www.test.dk/hest");
        GIF_URL = new URI("http://netarkivet.dk/netarchive_alm/billeder/spacer.gif");

        // We need a controller that doesn't do much more than return a test
        // record:
        fakeArcRepos = new TestArcRepositoryClient();
        nullArcRepos = new NullArcRepositoryClient();

        // Although we also need some real data
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        Settings.set(HarvesterSettings.VIEWERPROXY_DIR, new File(WORKING, "viewerproxy").getAbsolutePath());
        FileUtils.createDir(new File(WORKING, "viewerproxy"));

        aaa = new ARCArchiveAccess(fakeArcRepos);
        aaa.setIndex(TestInfo.ZIPPED_INDEX_DIR);
    }

    @After
    public void tearDown() {
        FileUtils.removeRecursively(WORKING);
        rs.tearDown();
    }

    /**
     * Verify that the constructor fails if and only if it is given null parameter.
     */
    @Test
    public void testConstructor() {
        // Verify construction with OK parameters does not fail:
        new ARCArchiveAccess(fakeArcRepos);
        // Verify that null parameters makes the constructor fail:
        try {
            new ARCArchiveAccess(null);
            fail("Expected argument not valid on null argument.");
        } catch (ArgumentNotValid e) {
            // Expected.
        }
    }

    @Test
    public void testReadPage() throws Exception {
        Method readPage = ReflectUtils.getPrivateMethod(ARCArchiveAccess.class, "readPage", InputStream.class,
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
        assertEquals("Should get same bytes regardless of bytes read per read()", "foo", baos.toString());
    }

    @Test
    public void testCreateErrorResponse() throws Exception {
        TestResponse response = new TestResponse();

        aaa.createNotFoundResponse(WRONG_URL, response);

        assertTrue("Url should be in output", response.bas.toString().contains(WRONG_URL.toString()));
        assertTrue("Should have content type header", response.names.contains("Content-type"));
        assertTrue("Should have test/html header value", response.values.contains("text/html"));
        assertEquals("Should be a 404 URL", 404, response.statusInt);
        assertEquals("Reason should be as expected", "Not found", response.reasonStr);

        response = new TestResponse();

        // Test it works twice.
        aaa.createNotFoundResponse(WRONG_URL, response);

        assertTrue("Url should be in output", response.bas.toString().contains(WRONG_URL.toString()));
        assertTrue("Should have content type header", response.names.contains("Content-type"));
        assertTrue("Should have test/html header value", response.values.contains("text/html"));
        assertEquals("Should be a 404 URL", 404, response.statusInt);
        assertEquals("Reason should be as expected", "Not found", response.reasonStr);
    }

    /**
     * Verify that looking up an object that does not exist returns some kind of HTTP response reflecting this
     * situation. For now, we just need it to return ResponseCode.NOT_FOUND and not throw an Exception.
     */
    @Test
    public void testLookupNonexistingObject() throws Exception {
        Response response = new TestResponse();
        URI uri = new URI("http://does.not.exist");
        int code = aaa.lookup(new TestRequest(uri), response);
        assertEquals("Should return a NOT_FOUND http code", URIResolver.NOT_FOUND, code);
    }

    /**
     * Test that looking up a known URI gives a positive response code.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testLookupExistingObject() throws IOException, InterruptedException {
        TestResponse response = new TestResponse();
        int code = aaa.lookup(new TestRequest(GIF_URL), response);
        assertEquals("Should return a FOUND http code", 200, code);
        assertEquals("Should have a location header", 1, response.names.size());
        assertEquals("Should have a location header", 1, response.values.size());
        assertEquals("Should have a location header", "Location", response.names.get(0));
        assertEquals("Should have a location header", GIF_URL_KEY.getFile().toString(), response.values.get(0));
    }

    /**
     * Test that AAA checks for null returns from the controller. This can happen if a CDX file is not backed by an arc
     * file, or arc repos times out.
     *
     * @throws Exception
     */
    @Test
    public void testNullControllerReturn() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        Response response = new TestResponse();
        ARCArchiveAccess nullaaa = new ARCArchiveAccess(nullArcRepos);
        nullaaa.setIndex(TestInfo.ZIPPED_INDEX_DIR);
        try {
            nullaaa.lookup(new TestRequest(GIF_URL), response);
            fail("Should throw exception if file is in index but gets null");
        } catch (IOFailure e) {
            // expected
        }
        lr.assertLogContains("Bitarchive problem must be reported in the log", "ARC file '" + GIF_URL_KEY.getFile()
                + "' mentioned in index file was not found");
        lr.stopRecorder();
    }

    /**
     * Fake arc repository client which on get returns a fake record which is ok.
     */
    private class TestArcRepositoryClient extends dk.netarkivet.common.arcrepository.TestArcRepositoryClient {
        public TestArcRepositoryClient() {
            super(TestInfo.WORKING_DIR);
        }

        /**
         * Returns an OK BitarchiveRecord. Content is simply arcfile name and index encoded in a stream.
         */
        public BitarchiveRecord get(String arcFile, long index) {
            final Map<String, Object> metadata = new HashMap<String, Object>();
            for (String header_field : (List<String>) ARCConstants.REQUIRED_VERSION_1_HEADER_FIELDS) {
                metadata.put(header_field, "");
            }
            byte[] data = ("HTTP/1.1 200 OK\nLocation: " + arcFile + "\n\n" + arcFile + " " + index).getBytes();
            // TODO remove or replace this by something else ? (ARCConstants.LENGTH_HEADER_FIELD_KEY)
            // does not exist in Heritrix 1.10+
            // metadata.put(ARCConstants.LENGTH_HEADER_FIELD_KEY,
            // Integer.toString(data.length));
            // Note: ARCRecordMetadata.getLength() now reads the contents of the LENGTH_FIELD_KEY
            // instead of LENGTH_HEADER_FIELD_KEY
            metadata.put(ARCConstants.LENGTH_FIELD_KEY, Integer.toString(data.length));
            Long dummyOffset = Long.valueOf(0L); // Note: offset is not stored as a String, but as a Long
            metadata.put(ARCConstants.ABSOLUTE_OFFSET_KEY, dummyOffset);

            try {
                ARCRecordMetaData meta = new ARCRecordMetaData(arcFile, metadata);
                return new BitarchiveRecord(new ARCRecord(new ByteArrayInputStream(data), meta), arcFile);
            } catch (IOException e) {
                fail("Cant't create metadata record");
                return null;
            }
        }
    }

    /**
     * Fake arc repository client which on get returns a fake record which is null.
     */
    private class NullArcRepositoryClient extends dk.netarkivet.common.arcrepository.TestArcRepositoryClient {
        public NullArcRepositoryClient() {
            super(TestInfo.WORKING_DIR);
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
