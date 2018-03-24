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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumMap;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestClient;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit-tests for the ViewerProxy class.
 */
public class ViewerProxyTester {
    /** Viewerproxy instance to clean up in teardown. */
    ViewerProxy proxy;

    /** HTTP client set with localhost as proxy. */
    private HttpClient httpClient;

    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        Channels.reset();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.ARCHIVE_DIR);

        Settings.set(CommonSettings.CACHE_DIR, new File(TestInfo.WORKING_DIR, "cachedir").getAbsolutePath());
        // Set up an HTTP client that can send commands to our proxy;
        int httpPort = Integer.parseInt(Settings.get(CommonSettings.HTTP_PORT_NUMBER));
        httpClient = new HttpClient();
        HostConfiguration hc = new HostConfiguration();
        String hostName = "localhost";
        hc.setProxy(hostName, httpPort);
        httpClient.setHostConfiguration(hc);
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        if (proxy != null) {
            proxy.cleanup();
        }
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestRemoteFile.removeRemainingFiles();
        Field f = ReflectUtils.getPrivateField(IndexRequestClient.class, "clients");
        f.set(null, new EnumMap<RequestType, IndexRequestClient>(RequestType.class));
        f = ReflectUtils.getPrivateField(IndexRequestClient.class, "synchronizer");
        f.set(null, null);
        rs.tearDown();
    }

    /**
     * Verifies that the proxyserver is started without errors.
     */
    @Test
    public void testStartViewerProxy() {
        proxy = ClassAsserts.assertSingleton(ViewerProxy.class);
    }

    /**
     * Test that the proxyServer is giving meaningful output when asked for non-existing content
     */
    @Test
    public void testGetWithNoIndex() throws Exception {
        proxy = ViewerProxy.getInstance();
        String content = getURLfromProxyServer("http://www.nonexistingdomain.test/nonexistingfile.html");
        StringAsserts.assertStringContains("Getting any URL without setting an index should give a message",
                "No index set", content);

        // Set an index
        content = getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid"
                + "/changeIndex?returnURL=http://foo/&label=hest&jobID=2&jobID=3");

        content = getURLfromProxyServer("http://www.nonexistingdomain.test/nonexistingfile.html");
        StringAsserts
                .assertStringContains("Getting NON-existing URL-object out of the archive should give a message !",
                        "Can't find URL", content);
    }

    /**
     * Verifies that the proxyServer is logging when asked for non-existing content.
     *
     * @throws Exception
     */
    @Test
    public void testLoggingGetNonExistingURL() throws Exception {
        proxy = ViewerProxy.getInstance();
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid"
                + "/changeIndex?returnURL=http://foo/&label=hest&jobID=2&jobID=3");
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid" + "/startRecordingURIs?returnURL=/");
        String missingUrl = "http://www.nonexistingdomain.test/nonexistingfile.html";
        getURLfromProxyServer(missingUrl);
        String list = getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid" + "/getRecordedURIs");
        StringAsserts.assertStringContains("Getting NON-existing URL-object out of the archive should be logged",
                missingUrl, list);
    }

    /**
     * Verifies that reception of an unknown instruction is logged.
     */
    @Test
    public void testUnknownInstruction() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        proxy = ViewerProxy.getInstance();
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid" + "/unknown");
        lr.assertLogContains("Unknown instruction should get Logged !", "Unknown command");
        lr.stopRecorder();
    }

    /**
     * Method used for getting URL-objects from the archive through the proxyServer.
     *
     * @param uri the URL to fetch
     * @return the content as a String
     */
    private String getURLfromProxyServer(String uri) throws IOException {
        String result = "";
        GetMethod get = new GetMethod(uri);
        httpClient.executeMethod(get);
        result = get.getResponseBodyAsString();
        get.releaseConnection();
        return result;
    }
}
