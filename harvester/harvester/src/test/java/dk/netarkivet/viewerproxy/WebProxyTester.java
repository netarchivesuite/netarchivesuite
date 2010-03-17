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
package dk.netarkivet.viewerproxy;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Enumeration;
import java.util.Locale;
import java.util.logging.LogManager;
import java.security.Principal;

import junit.framework.TestCase;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mortbay.io.ByteArrayEndPoint;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.LocalConnector;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.viewerproxy.distribute.HTTPControllerServerTester;

/**
 * Test the WebProxy class
 *
 */
public class WebProxyTester extends TestCase {
    private static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");

    private WebProxy proxy;
    private int httpPort;

    public WebProxyTester(String s) {
        super(s);
    }

    public void setUp() throws IOException {
        //Check port not in use (since this will fail all tests)
        
        httpPort = Settings.getInt(CommonSettings.HTTP_PORT_NUMBER);
        if (httpPort < 1025 || httpPort > 65535) {
            fail("Port must be in the range 1025-65535, not " + httpPort);
        } 
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port already in use before unit test");
        } catch (IOException e) {
            //expected
        }
        FileInputStream fis
                = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
    }

    public void tearDown() {
        if (proxy != null) {
            proxy.kill();
        }
        //Check port not in use (since this might break later tests)
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port still in use after killing server");
        } catch (IOException e) {
            //expected
        }
    }

    public void testUriEncode() {
        String test_string = "{abcd{fgåæka}";
        assertEquals("Should recover original string after decoding", 
                "%7Babcd%7Bfgåæka%7D",
                WebProxy.HttpRequest.uriEncode(test_string));
    }

    /** Tests constructor. After running the constructor the following should be
     * true:
     * - There is a server running and listening on the HTTP port
     * - If you request a URL from the HTTP port, it is forwarded to the given
     *   uri resolver
     */
    public void testWebProxy() throws Exception {
        //Start server
        TestURIResolver uriResolver = new TestURIResolver();
        proxy = new WebProxy(uriResolver);

        //Check port in use
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
        } catch (IOException e) {
            fail("Port not in use after starting server");
        }

        //Send GET request
        HttpClient client = new HttpClient();
        HostConfiguration hc = new HostConfiguration();
        String hostName = SystemUtils.getLocalHostName();
        hc.setProxy(hostName, httpPort);
        client.setHostConfiguration(hc);
        GetMethod get = new GetMethod("http://foo.bar/");
        client.executeMethod(get);
        String body = get.getResponseBodyAsString();
        int status = get.getStatusCode();
        get.releaseConnection();

        //Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.",
                     1, uriResolver.lookupCount);
        assertEquals("URI resolver lookup should be called with right URI.",
                     new URI("http://foo.bar/"), uriResolver.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver gives",
                     242, status);
        assertEquals("Body should contain what URI resolver wrote",
                     "Test", body);

        //Send POST request
        PostMethod post = new PostMethod("http://foo2.bar/");
        post.addParameter("a", "x");
        post.addParameter("a", "y");
        client.executeMethod(post);
        body = get.getResponseBodyAsString();
        status = get.getStatusCode();
        get.releaseConnection();

        //Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.",
                     2, uriResolver.lookupCount);
        assertEquals("URI resolver lookup should be called with right URI.",
                     new URI("http://foo2.bar/"), uriResolver.lookupRequestArgument);
        assertEquals("Posted parameter should be received.",
                     1, uriResolver.lookupRequestParameteres.size());
        assertNotNull("Posted parameter should be received.",
                      uriResolver.lookupRequestParameteres.get("a"));
        assertEquals("Posted parameter should be received.",
                     2, uriResolver.lookupRequestParameteres.get("a").length);
        assertEquals("Posted parameter should be received.",
                     "x", uriResolver.lookupRequestParameteres.get("a")[0]);
        assertEquals("Posted parameter should be received.",
                     "y", uriResolver.lookupRequestParameteres.get("a")[1]);
        assertEquals("Status code should be what URI resolver gives",
                     242, status);
        assertEquals("Body should contain what URI resolver wrote",
                     "Test", body);

        //Send request with parameter and portno
        get = new GetMethod("http://foo2.bar:8090/?baz=boo");
        client.executeMethod(get);
        body = get.getResponseBodyAsString();
        status = get.getStatusCode();
        get.releaseConnection();

        //Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.",
                     3, uriResolver.lookupCount);
        assertEquals("URI resolver 2 lookup should be called with right URI.",
                     new URI("http://foo2.bar:8090/?baz=boo"), uriResolver.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver gives",
                     242, status);
        assertEquals("Body should contain what URI resolver wrote",
                     "Test", body);
    }

    public void testSetURIResolver() throws Exception {
        //Start server
        TestURIResolver uriResolver = new TestURIResolver();
        proxy = new WebProxy(uriResolver);

        //Send request
        HttpClient client = new HttpClient();
        HostConfiguration hc = new HostConfiguration();
        String hostName = SystemUtils.getLocalHostName();
        hc.setProxy(hostName, httpPort);
        client.setHostConfiguration(hc);
        GetMethod get = new GetMethod("http://foo.bar/");
        client.executeMethod(get);
        String body = get.getResponseBodyAsString();
        int status = get.getStatusCode();
        get.releaseConnection();

        //Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.",
                     1, uriResolver.lookupCount);
        assertEquals("URI resolver lookup should be called with right URI.",
                     new URI("http://foo.bar/"), uriResolver.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver gives",
                     242, status);
        assertEquals("Body should contain what URI resolver wrote",
                     "Test", body);

        //Start server
        TestURIResolver uriResolver2 = new TestURIResolver();
        proxy.setURIResolver(uriResolver2);

        //Send request
        get = new GetMethod("http://foo2.bar/");
        client.executeMethod(get);
        body = get.getResponseBodyAsString();
        status = get.getStatusCode();
        get.releaseConnection();

        //Check request not received by URIResolver1
        assertEquals("URI resolver 1 lookup should NOT be called.",
                     1, uriResolver.lookupCount);
        //Check request received by URIResolver2
        assertEquals("URI resolver 2 lookup should be called.",
                     1, uriResolver2.lookupCount);
        assertEquals("URI resolver 2 lookup should be called with right URI.",
                     new URI("http://foo2.bar/"), uriResolver2.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver 2 gives",
                     242, status);
        assertEquals("Body should contain what URI resolver 2 wrote",
                     "Test", body);
    }

    public void testKill() throws Exception {
        //Start server
        TestURIResolver uriResolver = new TestURIResolver();
        proxy = new WebProxy(uriResolver);

        //Check port in use
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
        } catch (IOException e) {
            fail("Port not in use after starting server");
        }

        //Kill server
        proxy.kill();

        //Check port not in use
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port still in use after killing server");
        } catch (IOException e) {
            //expected
        }
    }

    public void testHandle() throws Exception {
        //Start server
        TestURIResolver uriResolver = new TestURIResolver();
        proxy = new WebProxy(uriResolver);

        //Make a request and response object
        HttpConnection fakeConnection = new HttpConnection(new LocalConnector(),
                                                           new ByteArrayEndPoint(),
                                                           proxy.getServer());
        org.mortbay.jetty.Request request = new org.mortbay.jetty.Request(
                fakeConnection);
        request.setUri(new HttpURI("http://foo.bar/"));
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        org.mortbay.jetty.Response response = new org.mortbay.jetty.Response(fakeConnection) {
            public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    public void write(int b) throws IOException {
                        os.write(b);
                        os.flush();
                    }
                };

            }
        };

        //Call handle
        proxy.handle(null, request, response, 1);

        //Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.",
                     1, uriResolver.lookupCount);
        assertEquals("URI resolver lookup should be called with right URI.",
                     new URI("http://foo.bar/"), uriResolver.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver gives",
                     242, response.getStatus());
        assertEquals("Body should contain what URI resolver wrote",
                     "Test", os.toString());

        //Make an unparsable request object
        request = new org.mortbay.jetty.Request(
                fakeConnection);
        request.setUri(new HttpURI("not parsable"));
    }

    /** Test the error response generation. */
    public void testCreateErrorResponse() throws Exception {
        proxy = new WebProxy(new TestURIResolver());
        HTTPControllerServerTester.TestResponse response
                = new HTTPControllerServerTester.TestResponse();
        URI uri = new URI("http://www.statsbiblioteket.dk/");
        String ExceptionMessage = "ExceptionTestMessage";
        Exception e = new ArgumentNotValid(ExceptionMessage);
        Method m = ReflectUtils.getPrivateMethod(WebProxy.class,
                                                 "createErrorResponse",
                                                 URI.class, Response.class,
                                                 Throwable.class);
        m.invoke(proxy, uri, response, e);
        LogUtils.flushLogs(WebProxy.class.getName());
        FileAsserts.assertFileMatches("Should have logged a warning",
                                       "WARNING:.*" + uri + ".*\n.*"
                                       + ExceptionMessage,
                                       LOG_FILE);
        String result = response.getOutputStream().toString();
        StringAsserts.assertStringNotContains("Should not contain null",
                                              "null",
                                              result);
        StringAsserts.assertStringContains("Should contain the URI",
                                           uri.toString(),
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception",
                                           ArgumentNotValid.class.getName(),
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception msg",
                                           ExceptionMessage,
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception trace",
                                           "testCreateErrorResponse",
                                           result);
        response.reset();
        m.invoke(proxy, null, response, e);
        LogUtils.flushLogs(WebProxy.class.getName());
        FileAsserts.assertFileMatches("Should have logged a warning",
                                       "WARNING:.*" + null + ".*\n.*"
                                       + ExceptionMessage,
                                       LOG_FILE);
        result = response.getOutputStream().toString();
        StringAsserts.assertStringContains("Should contain null for the URI",
                                           "null",
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception",
                                           ArgumentNotValid.class.getName(),
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception msg",
                                           ExceptionMessage,
                                           result);
        StringAsserts.assertStringContains("Should contain the Exception trace",
                                           "testCreateErrorResponse",
                                           result);
        response.reset();
        m.invoke(proxy, uri, response, null);
        LogUtils.flushLogs(WebProxy.class.getName());
        FileAsserts.assertFileMatches("Should have logged a warning",
                                       "(?m)WARNING:.*" + uri + "$",
                                       LOG_FILE);
        result = response.getOutputStream().toString();
        StringAsserts.assertStringContains("Should contain the URI",
                                           uri.toString(),
                                           result);
        StringAsserts.assertStringContains("Should contain null for exception",
                                           "null\n",
                                           result);
        m.invoke(proxy, uri, null, e);
        LogUtils.flushLogs(WebProxy.class.getName());
        FileAsserts.assertFileMatches("Should have logged a warning",
                                       "WARNING:.*Error writing error response"
                                       + ".*" + uri + ".*"
                                       + e.getClass().getName()
                                       + ".*" + ExceptionMessage,
                                       LOG_FILE);
    }

    /**
     * Constructs an instance of the inner class WebProxy.HttpRequest with a
     * url containing "{" and checks that the resulting uri escapes the
     * curly brackets correctly.
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void testGetUri() throws NoSuchMethodException,
                                    InvocationTargetException,
                                    IllegalAccessException,
                                    InstantiationException {
        Constructor<WebProxy.HttpRequest> ctor = WebProxy.HttpRequest.class.getDeclaredConstructor(HttpServletRequest.class);
        ctor.setAccessible(true);
        HttpServletRequest servlet_request = new URIServlet("http://somedomain.dk", "id={12345}");
        WebProxy.HttpRequest http_request = (WebProxy.HttpRequest) ctor.newInstance(servlet_request);
        URI uri = http_request.getURI();
        assertEquals("Expect uri to be escaped", "http://somedomain.dk?id=%7B12345%7D", uri.toString());
    }

    public static class URIServlet implements HttpServletRequest {

        private String base_url;
        private String query_string;

        public URIServlet(String base, String query) {
            base_url = base;
            query_string = query;
        }

        public StringBuffer getRequestURL() {
            return new StringBuffer(base_url);
        }

        public String getQueryString() {
            return query_string;
        }

        public String getAuthType() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Cookie[] getCookies() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public long getDateHeader(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getHeader(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Enumeration getHeaders(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Enumeration getHeaderNames() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getIntHeader(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getMethod() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getPathInfo() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getPathTranslated() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getContextPath() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }



        public String getRemoteUser() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isUserInRole(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Principal getUserPrincipal() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getRequestedSessionId() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getRequestURI() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }


        public String getServletPath() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public HttpSession getSession(boolean b) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public HttpSession getSession() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isRequestedSessionIdValid() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isRequestedSessionIdFromCookie() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isRequestedSessionIdFromURL() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isRequestedSessionIdFromUrl() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Object getAttribute(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Enumeration getAttributeNames() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getCharacterEncoding() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void setCharacterEncoding(String s)
                throws UnsupportedEncodingException {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getContentLength() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getContentType() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public ServletInputStream getInputStream() throws IOException {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getParameter(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Enumeration getParameterNames() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String[] getParameterValues(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Map getParameterMap() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getProtocol() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getScheme() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getServerName() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getServerPort() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public BufferedReader getReader() throws IOException {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getRemoteAddr() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getRemoteHost() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void setAttribute(String s, Object o) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void removeAttribute(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Locale getLocale() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public Enumeration getLocales() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isSecure() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public RequestDispatcher getRequestDispatcher(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getRealPath(String s) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getRemotePort() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getLocalName() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getLocalAddr() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getLocalPort() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }
    }

    public static class TestURIResolver implements URIResolver {
        int lookupCount = 0;
        int totalCount = 0;
        URI lookupRequestArgument;
        private Map<String, String[]> lookupRequestParameteres;

        public int lookup(Request request, Response response) {
            lookupCount++;
            totalCount++;
            lookupRequestArgument = request.getURI();
            lookupRequestParameteres = request.getParameterMap();
            try {
                response.getOutputStream().write("Test".getBytes());
            } catch (IOException e) {
                fail("URI resolver cannot write to stream");
            }
            response.setStatus(242);
            return 42;
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

        public Map<String,String[]> getParameterMap() {
            return Collections.emptyMap();
        }
    }


}