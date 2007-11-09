/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumMap;

import junit.framework.TestCase;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import dk.netarkivet.archive.indexserver.distribute.IndexRequestClient;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/** Test of viewer proxy class */
public class ViewerProxyTester extends TestCase {
    /** Viewerproxy instance to clean up in teardown*/
    ViewerProxy proxy;

    /** HTTP client set with localhost as proxy */
    private HttpClient httpClient;

    protected void setUp() throws Exception {
        Settings.reload();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        ChannelsTester.resetChannels();
        Settings.set(Settings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.ARCHIVE_DIR);

        Settings.set(Settings.CACHE_DIR,
                new File(TestInfo.WORKING_DIR, "cachedir").getAbsolutePath());
        //Set up an HTTP client that can send commands to our proxy;
        int httpPort = Integer.parseInt(Settings.get(Settings.HTTP_PORT_NUMBER));
        httpClient = new HttpClient();
        HostConfiguration hc = new HostConfiguration();
        String hostName = "localhost";
        hc.setProxy(hostName, httpPort);
        httpClient.setHostConfiguration(hc);
    }

    protected void tearDown() throws NoSuchFieldException,
            IllegalAccessException {
        if (proxy != null) {
            proxy.cleanup();
        }
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        Settings.reload();
        TestRemoteFile.removeRemainingFiles();
        Field f = ReflectUtils.getPrivateField(IndexRequestClient.class, "clients");
        f.set(null, new EnumMap<RequestType, IndexRequestClient>(RequestType.class));
        f = ReflectUtils.getPrivateField(IndexRequestClient.class, "synchronizer");
        f.set(null, null);
    }

    /**
     * Verfies that the proxyserver is started without errors.
     */
    public void testStartViewerProxy() {
        proxy = ClassAsserts.assertSingleton(ViewerProxy.class);
    }

    /**
     * Test that the proxyServer is giving meaningful output when asked for nonexisting content
     */
    public void testGetWithNoIndex() throws Exception {
        proxy = ViewerProxy.getInstance();
        String content = getURLfromProxyServer("http://www.nonexistingdomain.test/nonexistingfile.html");
        StringAsserts.assertStringContains("Getting any URL without setting an index should give a message",
                "No index set", content);

        // Set an index
        content = getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid"
                + "/changeIndex?returnURL=http://foo/&label=hest&jobID=2&jobID=3");

        content = getURLfromProxyServer("http://www.nonexistingdomain.test/nonexistingfile.html");
        StringAsserts.assertStringContains("Getting NON-existing URL-object out of the archive should give a message !",
                "Can't find URL", content);
    }

    /**
     * Verifies that the proxyServer is logging when asked
     *  for nonexisting content
     * @throws Exception
     */
    public void testLoggingGetNonExistingURL() throws Exception {
        proxy = ViewerProxy.getInstance();
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid"
                + "/changeIndex?returnURL=http://foo/&label=hest&jobID=2&jobID=3");
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid" + "/startRecordingURIs?returnURL=/");
        String missingUrl = "http://www.nonexistingdomain.test/nonexistingfile.html";
        getURLfromProxyServer(missingUrl);
        String list = getURLfromProxyServer(
                "http://" + "netarchivesuite.viewerproxy.invalid" + "/getRecordedURIs");
        StringAsserts.assertStringContains(
                "Getting NON-existing URL-object out of the archive should be logged",
                missingUrl, list);
    }

    /**
     * Verifies that reception of an unknown instruction
     * is logged.
     */
    public void testUnknownInstruction() throws IOException {
        proxy = ViewerProxy.getInstance();
        getURLfromProxyServer("http://" + "netarchivesuite.viewerproxy.invalid" + "/unknown");
        FileAsserts.assertFileContains("Unknown instruction should get Logged !",
                "Unknown command", TestInfo.LOG_FILE);
    }

    /**
     * Method used for getting URL-objects from the archive through the proxyServer
     * @param uri the URL to fecth
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
