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
package dk.netarkivet.viewerproxy.distribute;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.viewerproxy.ARCArchiveAccess;
import dk.netarkivet.viewerproxy.DelegatingController;
import dk.netarkivet.viewerproxy.LocalCDXCache;
import dk.netarkivet.viewerproxy.MissingURIRecorder;
import dk.netarkivet.viewerproxy.Request;
import dk.netarkivet.viewerproxy.Response;
import dk.netarkivet.viewerproxy.URIResolver;

/**
 * Unit tests for the HTTPControllerServer class. 
 *
 */
public class HTTPControllerServerTester extends TestCase {
    private TestDelegatingController c;
    private TestURIResolver ur;
    private TestResponse response;
    private TestRequest startRequest;
    private TestRequest stopRequest;
    private TestRequest clearRequest;
    private TestRequest getRecordedURIsRequest;
    private TestRequest changeIndexRequest;
    private TestRequest changeIndexRequest2;

    private TestRequest[] wrongRequests;

    public HTTPControllerServerTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ur = new TestURIResolver();
        c = new TestDelegatingController();
        response = new TestResponse();
        String hostname = "netarchivesuite.viewerproxy.invalid";
        String returnUrl = URLEncoder.encode("http://foo.bar", "UTF-8");
        startRequest = new TestRequest(new URI("http://" + hostname
                + "/startRecordingURIs?returnURL=" + returnUrl));
        stopRequest = new TestRequest(new URI("http://" + hostname
                + "/stopRecordingURIs?returnURL=" + returnUrl));
        ArrayList<String> values = new ArrayList<String>();
        values.add("http://foo.bar");
        clearRequest = new TestRequest(new URI("http://" + hostname
                + "/clearRecordedURIs"));
        clearRequest.postParameters.put("returnURL", values);
        getRecordedURIsRequest = new TestRequest(new URI("http://" + hostname
                + "/getRecordedURIs"));
        changeIndexRequest = new TestRequest(new URI("http://" + hostname
                + "/changeIndex?jobID=1&label=myTestLabel&returnURL="
                                                     + returnUrl));
        changeIndexRequest2 = new TestRequest(new URI("http://" + hostname
                + "/changeIndex?jobID=1&label=myTestLabel&returnURL="
                                                      + returnUrl));
        values = new ArrayList<String>();
        values.add("8");
        changeIndexRequest2.postParameters.put("jobID", values);
        changeIndexRequest2.postParameters.put("label",
                                               Arrays.asList("myTestLabel"));
        wrongRequests = new TestRequest[] {
                new TestRequest(new URI("http://" + hostname
                        + "/startRecordingURIs?fnidder=fnadder")),
                new TestRequest(new URI("http://" + hostname
                        + "/stopRecordingURIs?fnidder=fnadder")),
                new TestRequest(new URI("http://" + hostname
                        + "/clearRecordedURIs?fnidder=fnadder")),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?returnURL=" + returnUrl)),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?jobID=" + 1)),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?label=foobar")),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?label=foobar&returnURL=" + returnUrl)),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?label=foobar&jobID=" + 1)),
                new TestRequest(new URI("http://" + hostname
                        + "/changeIndex?returnURL=" + returnUrl
                        + "&jobID=" + 1))
        };
    }

    public void tearDown() {
        if (TestDelegatingController.arcRep != null) {
            TestDelegatingController.arcRep.close();
            TestDelegatingController.arcRep = null;
        }
    }

    /**
     * Tests constructor. The only thing really testable is that it throws
     * argument not valid on null arguments.
     *
     * @throws Exception
     */
    public void testControllerServer() throws Exception {
        try {
            new HTTPControllerServer(c, null);
            fail("Should have thrown ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }
        try {
            new HTTPControllerServer(null, ur);
            fail("Should have thrown ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }
        new HTTPControllerServer(c, ur);
    }

    /**
     * Test setURIResolver. Tests null arguments, and that lookup calls are
     * delegated to this resolver after setting it.
     */
    public void testSetURIResolver() throws Exception {
        HTTPControllerServer cs = new HTTPControllerServer(this.c, ur);
        try {
            cs.setURIResolver(null);
            fail("Should throw ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        cs.lookup(startRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called start on controller",
                     1, c.startCounter);
        cs.lookup(new TestRequest(new URI("http://foo.bar")), response);
        assertEquals("Should have called lookup method on uri resolver",
                     1, ur.lookupCount);
        TestURIResolver ur2 = new TestURIResolver();
        cs.setURIResolver(ur2);
        cs.lookup(startRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur2.lookupCount);
        assertEquals("Should have called start on controller",
                     2, c.startCounter);
        cs.lookup(new TestRequest(new URI("http://foo.bar")), response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     1, ur.lookupCount);
        assertEquals("Should have called lookup method on uri resolver 2",
                     1, ur2.lookupCount);
    }

    /**
     * Tests that command urls go to controller, and other urls go to wrapped
     * uri resolver.
     *
     * @throws Exception
     */
    public void testLookup() throws Exception {
        HTTPControllerServer cs = new HTTPControllerServer(this.c, ur);

        //Check start command
        int result = cs.lookup(startRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called start on controller",
                     1, c.startCounter);
        assertEquals("Should have called nothing else on controller",
                     1, c.totalCounter);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response needs redirect status code",
                     303, response.status);
        assertEquals("Nothing should be written to body",
                     "", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include location header",
                     "Location", response.headerFields.get(0).getKey());
        assertEquals("Response must now include location header to foo.bar",
                     "http://foo.bar", response.headerFields.get(0).getValue());

        //Check stop command
        response.reset();
        result = cs.lookup(stopRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called start on controller",
                     1, c.stopCounter);
        assertEquals("Should have called nothing else on controller",
                     2, c.totalCounter);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response needs redirect status code",
                     303, response.status);
        assertEquals("Nothing should be written to body",
                     "", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include location header",
                     "Location", response.headerFields.get(0).getKey());
        assertEquals("Response must now include location header to foo.bar",
                     "http://foo.bar", response.headerFields.get(0).getValue());

        //Check clear command
        response.reset();
        result = cs.lookup(clearRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called start on controller",
                     1, c.clearCounter);
        assertEquals("Should have called nothing else on controller",
                     3, c.totalCounter);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response needs redirect status code",
                     303, response.status);
        assertEquals("Nothing should be written to body",
                     "", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include location header",
                     "Location", response.headerFields.get(0).getKey());
        assertEquals("Response must now include location header to foo.bar",
                     "http://foo.bar", response.headerFields.get(0).getValue());

        //Check get command
        response.reset();
        result = cs.lookup(getRecordedURIsRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called start on controller",
                     1, c.getRecordedURICounter);
        assertEquals("Should have called nothing else on controller",
                     4, c.totalCounter);
        assertEquals("Response should be OK",
                     200, result);
        assertEquals("Response needs OK status code",
                     200, response.status);
        assertEquals("URL list should be written to body",
                     "http://bar.foo\n", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include content type header",
                     "Content-Type", response.headerFields.get(0).getKey());
        assertEquals("Response must now include content type header text/plain",
                     "text/plain; charset=UTF-8", response.headerFields.get(0).getValue());

        //Check changeIndex command with one argument
        response.reset();
        result = cs.lookup(changeIndexRequest, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called changeIndex on controller",
                     1, c.changeIndexCounter);
        assertEquals("Should have called nothing else on controller",
                     5, c.totalCounter);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response needs redirect status code",
                     303, response.status);
        assertEquals("Nothing should be written to body",
                     "", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include location header",
                     "Location", response.headerFields.get(0).getKey());
        assertEquals("Response must now include location header to foo.bar",
                     "http://foo.bar", response.headerFields.get(0).getValue());
        assertEquals("Should have received one job id",
                     1, c.changeIndexJobListArgument.size());
        assertEquals("Should have received job id '1'",
                     new Long(1L), c.changeIndexJobListArgument.toArray()[0]);
        assertEquals("Should have received label", "myTestLabel",
                     c.changeIndexLabelParameter);

        //Check changeIndex command with two arguments
        response.reset();
        result = cs.lookup(changeIndexRequest2, response);
        assertEquals("Should NOT have called lookup method on uri resolver",
                     0, ur.lookupCount);
        assertEquals("Should have called changeIndex on controller",
                     2, c.changeIndexCounter);
        assertEquals("Should have called nothing else on controller",
                     6, c.totalCounter);
        assertEquals("Response should be redirect",
                     303, result);
        assertEquals("Response needs redirect status code",
                     303, response.status);
        assertEquals("Nothing should be written to body",
                     "", response.os.toString());
        assertEquals("Response must now include one extra header",
                     1, response.headerFields.size());
        assertEquals("Response must now include location header",
                     "Location", response.headerFields.get(0).getKey());
        assertEquals("Response must now include location header to foo.bar",
                     "http://foo.bar", response.headerFields.get(0).getValue());
        assertEquals("Should have received two job ids",
                     2, c.changeIndexJobListArgument.size());
 
        // Check, that the set of expected Job ids is {1L,8L}
        Set<Long> setOfExpectedJobIds = new HashSet<Long>();
        setOfExpectedJobIds.add(new Long(1L));
        setOfExpectedJobIds.add(new Long(8L));
        assertTrue("Should have received job ids '1' and '8'",
                setOfExpectedJobIds.containsAll(c.changeIndexJobListArgument));

        assertEquals("Should have received label", "myTestLabel",
                     c.changeIndexLabelParameter);

        //Check wrong arguments
        for (TestRequest wrongRequest : wrongRequests) {
            response.reset();
            try {
                result = cs.lookup(wrongRequest, response);
                fail("Should throw exception on wrong arguments");
            } catch (IOFailure e) {
                //expected
            }
            assertEquals("Should NOT have called lookup method on uri resolver",
                    0, ur.lookupCount);
            assertEquals("Should NOT have called anything on controller",
                    6, c.totalCounter);
        }

        //Check non-command url
        result = cs.lookup(new TestRequest(new URI("http://non.command.url")), response);
        assertEquals("Should have called lookup method on uri resolver",
                     1, ur.lookupCount);
        assertEquals("Should have called nothing on controller",
                     6, c.totalCounter);
        assertEquals("Result should be set by uri resolver",
                     42, result);
        assertEquals("Response should have been received by uri resolver",
                     response, ur.lookupResponseArgument);
        assertEquals("URI should have been received by uri resolver",
                     new URI("http://non.command.url"), ur.lookupRequestArgument.getURI());

        //It's not up to the controller to handle null in lookup
        result = cs.lookup(new TestRequest(new URI("http://non.command.url")), null);
        assertEquals("Should have called lookup method on uri resolver",
                     2, ur.lookupCount);
        assertEquals("Should have called nothing on controller",
                     6, c.totalCounter);
        assertEquals("Result should be set by uri resolver",
                     42, result);
        assertEquals("Null should have been received by uri resolver",
                     null, ur.lookupResponseArgument);
        assertEquals("URI should have been received by uri resolver",
                     new URI("http://non.command.url"), ur.lookupRequestArgument.getURI());

        //It's not up to the controller to handle null in lookup
        result = cs.lookup(null, response);
        assertEquals("Should have called lookup method on uri resolver",
                     3, ur.lookupCount);
        assertEquals("Should have called nothing on controller",
                     6, c.totalCounter);
        assertEquals("Result should be set by uri resolver",
                     42, result);
        assertEquals("Response should have been received by uri resolver",
                     response, ur.lookupResponseArgument);
        assertEquals("null should have been received by uri resolver",
                     null, ur.lookupRequestArgument);

    }

    public static class TestDelegatingController extends DelegatingController {
        int totalCounter = 0;
        int startCounter = 0;
        int stopCounter = 0;
        int clearCounter = 0;
        int getRecordedURICounter = 0;
        int changeIndexCounter = 0;
        Set<Long> changeIndexJobListArgument;
        public static ViewerArcRepositoryClient arcRep;
        String changeIndexLabelParameter;

        public TestDelegatingController() {
            super(new MissingURIRecorder(),
                  new LocalCDXCache(ArcRepositoryClientFactory.getViewerInstance()),
                  new ARCArchiveAccess(
                        arcRep =  ArcRepositoryClientFactory.getViewerInstance()));
        }

        public void changeIndex(Set<Long> jobList, String label) {
            totalCounter++;
            changeIndexCounter++;
            changeIndexJobListArgument = jobList;
            changeIndexLabelParameter = label;
        }

        public String getStatus(Locale locale) {
            return "Hello world.";
        }

        public void startRecordingURIs() {
            totalCounter++;
            startCounter++;
        }

        public void stopRecordingURIs() {
            totalCounter++;
            stopCounter++;
        }

        public void clearRecordedURIs() {
            totalCounter++;
            clearCounter++;
        }

        public Set<URI> getRecordedURIs() {
            totalCounter++;
            getRecordedURICounter++;
            HashSet<URI> uris = new HashSet<URI>();
            try {
                uris.add(new URI("http://bar.foo"));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Illegal URI not possible");
            }
            return uris;
        }
    }

    public static class TestURIResolver implements URIResolver {
        int lookupCount = 0;
        int totalCount = 0;
        Response lookupResponseArgument;
        Request lookupRequestArgument;

        public int lookup(Request request, Response response) {
            lookupCount++;
            totalCount++;
            lookupRequestArgument = request;
            lookupResponseArgument = response;
            return 42;
        }
    }

    public static class TestResponse implements Response {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int status = 0;
        String reason = "";
        List<KeyValuePair<String, String>> headerFields
                = new ArrayList<KeyValuePair<String, String>>();

        public OutputStream getOutputStream() {
            return os;
        }

        public void setStatus(int statusCode) {
            status = statusCode;
        }

        public void setStatus(int statusCode, String reason) {
            status = statusCode;
            this.reason = reason;
        }

        public void addHeaderField(String name, String value) {
            headerFields.add(new KeyValuePair<String, String>(name, value));
        }

        public int getStatus() {
            return status;
        }

        public void reset() {
            os = new ByteArrayOutputStream();
            status = 0;
            reason = "";
            headerFields = new ArrayList<KeyValuePair<String, String>>();
        }
    }

    public static class TestRequest implements Request {
        private URI uri;
        public Map<String, List<String>> postParameters
                = new HashMap<String, List<String>>();

        public TestRequest(URI uri) {
            this.uri = uri;
        }

        public URI getURI() {
            return uri;
        }

        public Map<String,String[]> getParameterMap() {
           //First parse get parameters
            String query = uri.getRawQuery();
            Map<String, String[]> queryMap;
            String[] queryParts;
            queryMap = new HashMap<String, String[]>();
            if (query != null) {
                queryParts = query.split("&");
                for (String parameter : queryParts) {
                    String[] parameterParts = parameter.split("=");
                    if (parameterParts.length == 2) {
                        String name = parameterParts[0];
                        String value = parameterParts[1];
                        List<String> list;
                        if (!queryMap.containsKey(name)) {
                            list = new ArrayList<String>();
                        } else {
                            list = Arrays.asList(queryMap.get(name));
                        }
                        try {
                            list.add(URLDecoder.decode(value, "UTF-8"));
                            queryMap.put(name, list.toArray(new String[]{}));
                        } catch (UnsupportedEncodingException e) {
                            throw new ArgumentNotValid("This should never happen: "
                                                       + "UTF-8 is an"
                                                       + " unsupported charset.");
                        }
                    }
                }
            }
            //Then parse post parameters
            for (Map.Entry<String, List<String>> entry
                    : postParameters.entrySet()) {
                List<String> values = new ArrayList<String>();
                if (queryMap.get(entry.getKey()) != null) {
                    values.addAll(Arrays.asList(queryMap.get(entry.getKey())));
                }
                values.addAll(entry.getValue());
                queryMap.put(entry.getKey(), values.toArray(new String[]{}));
            }
            return queryMap;
        }
    }
}
