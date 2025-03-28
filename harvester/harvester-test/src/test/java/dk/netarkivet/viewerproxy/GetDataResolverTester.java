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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.arcrepository.TestArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;
import dk.netarkivet.viewerproxy.distribute.HTTPControllerServerTester;

/**
 * Unit-tests for the GetDataResolver class.
 */
@SuppressWarnings({"unused"})
public class GetDataResolverTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    File tempdir = new File(TestInfo.WORKING_DIR, "commontempdir");
    ArcRepositoryClient arcrep = null;
    MockupJMS mjms = new MockupJMS();
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    @Before
    public void setUp() {
        rs.setUp();
        mtf.setUp();
        mjms.setUp();
        utrf.setUp();
        tempdir.mkdir();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, tempdir.getAbsolutePath());

        arcrep = new TestArcRepositoryClient(TestInfo.WORKING_DIR);
    }

    @After
    public void tearDown() {
        arcrep.close();
        utrf.tearDown();
        mjms.tearDown();
        mtf.tearDown();
        rs.tearDown();
    }

    @Test
    public void testExecuteCommand() throws Exception {
        String urlPrefix = "http://" + "netarchivesuite.viewerproxy.invalid";
        URIResolver res = new GetDataResolver(
                new URIResolver() {
            public int lookup(Request request, Response response) {
                return 201;
            }

        }, arcrep);
        Response response = makeNewResponse();
        File testFile = new File(TestInfo.WORKING_DIR, "fyensdk.arc");
        int result = res.lookup(makeRequest(urlPrefix + "/getFile?arcFile=" + testFile.getName()), response);
        assertEquals("Should get 200 result code", 200, result);
        ByteArrayOutputStream outputStream = (ByteArrayOutputStream) response.getOutputStream();
        assertEquals("Should have same amount of data", testFile.length(), outputStream.size());
        assertEquals("Should get file contents", FileUtils.readFile(testFile), outputStream.toString());

        response = makeNewResponse();
        testFile = new File(TestInfo.WORKING_DIR, "dummyNotFound");
        try {
            result = res.lookup(makeRequest(urlPrefix + "/getFile?arcFile=" + testFile.getName()), response);
            fail("Should get exception on missing file");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should have file name in error", testFile.getName(), e.getMessage());
        }

        response = makeNewResponse();
        testFile = new File(TestInfo.WORKING_DIR, "fyensdk.arc");
        try {
            result = res.lookup(makeRequest(urlPrefix + "/getRecord?arcFile=" + testFile.getName() + "&arcOffset="
                    + 104), response);
            fail("Should get exception on missing record");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should have file name in error", testFile.getName(), e.getMessage());
        }

        response = makeNewResponse();

        result = res.lookup(makeRequest(urlPrefix + "/getRecord?arcFile=" + testFile.getName() + "&arcOffset=" + 136),
                response);
        assertEquals("Should have 200 response for second record", 200, result);
        String resultText = response.getOutputStream().toString();
        assertTrue("Should have start of record in response, not " + resultText,
                resultText.startsWith("HTTP/1.1 200 OK"));
        assertEquals("Should have right length of data in response", 6669,
                ((ByteArrayOutputStream) response.getOutputStream()).size());

        // Metadata
        response = makeNewResponse();
        result = res.lookup(makeRequest(urlPrefix + "/getMetadata?jobID=" + 2), response);
        assertEquals("Should have 200 response for metadata", 200, result);
        resultText = response.getOutputStream().toString();
        assertTrue("Should have start of record in response, not " + resultText,
                resultText.startsWith("filedesc://2-metadata-1.arc "));
        assertEquals("Should have right length of data in response", 17948,
                ((ByteArrayOutputStream) response.getOutputStream()).size());

        response = makeNewResponse();
        try {
            result = res.lookup(makeRequest(urlPrefix + "/getMetadata?jobID=" + 3), response);
            fail("Should have gotten IOFailure on missing job");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should mention the job", "for job 3 or error", e.getMessage());
        }

        ((TestArcRepositoryClient) arcrep).tmpDir = new File("/dev");
        response = makeNewResponse();
        try {
            result = res.lookup(makeRequest(urlPrefix + "/getMetadata?jobID=" + 2), response);
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should mention the job", "for job 2 or error", e.getMessage());
        }

    }

    private Response makeNewResponse() {
        return new Response() {
            @Override public int hashCode() {
                throw new RuntimeException("Not yet implemented");
            }

            int status = 500;
            private String reason = "Never set";
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            public OutputStream getOutputStream() {
                return out;
            }

            public void setStatus(int statusCode) {
                status = statusCode;
            }

            public void setStatus(int statusCode, String reason) {
                this.status = statusCode;
                this.reason = reason;
            }

            public void addHeaderField(String name, String value) {
                // TODO: implement method
                // throw new NotImplementedException("Not implemented");
            }

            public int getStatus() {
                return status;
            }
        };
    }

    private Request makeRequest(final String uri) throws URISyntaxException {
        return new HTTPControllerServerTester.TestRequest(new URI(uri));
    }
}
