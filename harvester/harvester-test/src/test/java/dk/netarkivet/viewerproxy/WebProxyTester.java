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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.viewerproxy.distribute.HTTPControllerServerTester;

/**
 * Test the WebProxy class which wraps a handler in a Jetty server. The testJettyIntegration test verifies the handler
 * <-> Jetty integration, where the remaining tests directly tests the WebProxy methods and inner classes.
 */
public class WebProxyTester {

    private URIResolver uriResolverMock;
    //private org.eclipse.jetty.server.Request requestMock;
    //private org.eclipse.jetty.server.Response responseMock;
    private org.mortbay.jetty.Request requestMock;
    private org.mortbay.jetty.Response responseMock;
    
    private WebProxy proxy;
    private int httpPort;

    @Before
    public void setUp() throws IOException {
        // Check port not in use (since this will fail all tests)

        httpPort = Settings.getInt(CommonSettings.HTTP_PORT_NUMBER);
        if (httpPort < 1025 || httpPort > 65535) {
            fail("Port must be in the range 1025-65535, not " + httpPort);
        }
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port already in use before unit test");
        } catch (IOException e) {
            // Expected
        }

        uriResolverMock = mock(URIResolver.class);
/*
        requestMock = mock(org.eclipse.jetty.server.Request.class);
        responseMock = mock(org.eclipse.jetty.server.Response.class);
*/
        requestMock = mock(org.mortbay.jetty.Request.class);
        responseMock = mock(org.mortbay.jetty.Response.class);

    }

    @After
    public void tearDown() {
        if (proxy != null) {
            proxy.kill();
        }
        // Check port not in use (since this might break later tests)
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port still in use after killing server");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testUriEncode() {
        String test_string = "{abcd{fgåæka}";
        assertEquals("Should recover original string after decoding", "%7Babcd%7Bfgåæka%7D",
                WebProxy.HttpRequest.uriEncode(test_string));
    }

    /**
     * Test the general integration of the WebProxy access through the running Jetty true:
     */
    @Test
    public void testJettyIntegration() throws Exception {
        TestURIResolver uriResolver = new TestURIResolver();
        proxy = new WebProxy(uriResolver);

        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
        } catch (IOException e) {
            fail("Port not in use after starting server");
        }

        // GET request
        HttpClient client = new HttpClient();
        HostConfiguration hc = new HostConfiguration();
        String hostName = SystemUtils.getLocalHostName();
        hc.setProxy(hostName, httpPort);
        client.setHostConfiguration(hc);
        GetMethod get = new GetMethod("http://foo.bar/");
        client.executeMethod(get);

        assertEquals("Status code should be what URI resolver gives", 242, get.getStatusCode());
        assertEquals("Body should contain what URI resolver wrote", "Test", get.getResponseBodyAsString());
        get.releaseConnection();

        // POST request
        PostMethod post = new PostMethod("http://foo2.bar/");
        post.addParameter("a", "x");
        post.addParameter("a", "y");
        client.executeMethod(post);

        // Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.", 2, uriResolver.lookupCount);
        assertEquals("URI resolver lookup should be called with right URI.", new URI("http://foo2.bar/"),
                uriResolver.lookupRequestArgument);
        assertEquals("Posted parameter should be received.", 1, uriResolver.lookupRequestParameteres.size());
        assertNotNull("Posted parameter should be received.", uriResolver.lookupRequestParameteres.get("a"));
        assertEquals("Posted parameter should be received.", 2, uriResolver.lookupRequestParameteres.get("a").length);
        assertEquals("Posted parameter should be received.", "x", uriResolver.lookupRequestParameteres.get("a")[0]);
        assertEquals("Posted parameter should be received.", "y", uriResolver.lookupRequestParameteres.get("a")[1]);
        assertEquals("Status code should be what URI resolver gives", 242, post.getStatusCode());
        assertEquals("Body should contain what URI resolver wrote", "Test", post.getResponseBodyAsString());
        post.releaseConnection();

        // Request with parameter and portno
        get = new GetMethod("http://foo2.bar:8090/?baz=boo");
        client.executeMethod(get);

        // Check request received by URIResolver
        assertEquals("URI resolver lookup should be called.", 3, uriResolver.lookupCount);
        assertEquals("URI resolver 2 lookup should be called with right URI.",
                new URI("http://foo2.bar:8090/?baz=boo"), uriResolver.lookupRequestArgument);
        assertEquals("Status code should be what URI resolver gives", 242, get.getStatusCode());
        assertEquals("Body should contain what URI resolver wrote", "Test", get.getResponseBodyAsString());
        get.releaseConnection();
    }

    /** Verify that the setURIResolver method changes the uriresolver correctly */
    @Test
    @Ignore
    public void testSetURIResolver() throws Exception {
        URIResolver uriResolverMock2 = mock(URIResolver.class);
        proxy = new WebProxy(uriResolverMock);
        proxy.setURIResolver(uriResolverMock2);
        // FIXME Compile-error when backporting to jetty 6.1.26 (Therefore is test ignored)
        //proxy.handle(null, null, requestMock, responseMock);

        verify(uriResolverMock2).lookup(any(Request.class), any(Response.class));
        verifyNoMoreInteractions(uriResolverMock, uriResolverMock2);
    }

    @Test
    public void testKill() throws Exception {
        proxy = new WebProxy(uriResolverMock);
        // Check port in use
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
        } catch (IOException e) {
            fail("Port not in use after starting server");
        }

        // Kill server
        proxy.kill();

        // Check port not in use
        try {
            new Socket(InetAddress.getLocalHost(), httpPort);
            fail("Port still in use after killing server");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    @Ignore
    public void testHandle() throws Exception {
        proxy = new WebProxy(uriResolverMock);
        String url = "http://foo.bar/";
        when(requestMock.getRequestURL()).thenReturn(new StringBuffer(url));
        // FIXME Compile-error when backporting to jetty 6.1.26 (Therefore is test ignored)
        //proxy.handle(null, null, requestMock, responseMock);

        verify(requestMock).setHandled(true);
        ArgumentCaptor<Request> requestArgument = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Response> responseArgument = ArgumentCaptor.forClass(Response.class);

        verify(uriResolverMock).lookup(requestArgument.capture(), responseArgument.capture());

        assertEquals(new URI("http://foo.bar/"), requestArgument.getValue().getURI());
        // The request/response in the delegated handle call should delegate method calls to the original
        // request/response objects"

        responseArgument.getValue().getOutputStream();
        verify(responseMock).getOutputStream();

        verifyNoMoreInteractions(uriResolverMock);
    }

    /**
     * Test the error response generation.
     */
    @Test
    @Ignore
    public void testCreateErrorResponse() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        proxy = new WebProxy(uriResolverMock);
        HTTPControllerServerTester.TestResponse response = new HTTPControllerServerTester.TestResponse();
        URI uri = new URI("http://www.statsbiblioteket.dk/");
        String ExceptionMessage = "ExceptionTestMessage";
        Exception e = new ArgumentNotValid(ExceptionMessage);
        Method m = ReflectUtils.getPrivateMethod(WebProxy.class, "createErrorResponse", URI.class, Response.class,
                Throwable.class);
        m.invoke(proxy, uri, response, e);
        // LogUtils.flushLogs(WebProxy.class.getName());
        /*
         * FileAsserts.assertFileMatches("Should have logged a warning", "WARNING:.*" + uri + ".*\n.*" +
         * ExceptionMessage, LOG_FILE);
         */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(out);
        e.printStackTrace(pw);
        pw.close();
        String eStr = new String(out.toByteArray());
        // lr.assertLogMatches("Should have logged a warning", "Exception for : " + uri + "\n" + eStr);
        lr.assertLogContains("Should have logged a warning", "Exception for : " + uri + "\n" + eStr);
        lr.reset();

        String result = response.getOutputStream().toString();
        StringAsserts.assertStringNotContains("Should not contain null", "null", result);
        StringAsserts.assertStringContains("Should contain the URI", uri.toString(), result);
        StringAsserts.assertStringContains("Should contain the Exception", ArgumentNotValid.class.getName(), result);
        StringAsserts.assertStringContains("Should contain the Exception msg", ExceptionMessage, result);
        StringAsserts.assertStringContains("Should contain the Exception trace", "testCreateErrorResponse", result);
        response.reset();
        m.invoke(proxy, null, response, e);
        // LogUtils.flushLogs(WebProxy.class.getName());
        /*
         * FileAsserts.assertFileMatches("Should have logged a warning", "WARNING:.*" + null + ".*\n.*" +
         * ExceptionMessage, LOG_FILE);
         */
        lr.assertLogMatches("Should have logged a warning", "Exception for : .*" + null + ".*\n.*" + ExceptionMessage);
        lr.reset();
        result = response.getOutputStream().toString();
        StringAsserts.assertStringContains("Should contain null for the URI", "null", result);
        StringAsserts.assertStringContains("Should contain the Exception", ArgumentNotValid.class.getName(), result);
        StringAsserts.assertStringContains("Should contain the Exception msg", ExceptionMessage, result);
        StringAsserts.assertStringContains("Should contain the Exception trace", "testCreateErrorResponse", result);
        response.reset();
        m.invoke(proxy, uri, response, null);
        // LogUtils.flushLogs(WebProxy.class.getName());
        /*
         * FileAsserts.assertFileMatches("Should have logged a warning", "(?m)WARNING:.*" + uri + "$", LOG_FILE);
         */
        lr.assertLogMatches("Should have logged a warning", "Exception for : .*" + uri);
        lr.reset();
        result = response.getOutputStream().toString();
        StringAsserts.assertStringContains("Should contain the URI", uri.toString(), result);
        StringAsserts.assertStringContains("Should contain null for exception", "null\n", result);
        m.invoke(proxy, uri, null, e);
        // TODO Remove when @Ignore is fixed.
        /*
         * FileAsserts.assertFileMatches("Should have logged a warning", "WARNING:.*Error writing error response" + ".*"
         * + uri + ".*" + e.getClass().getName() + ".*" + ExceptionMessage, LOG_FILE);
         */
        lr.assertLogMatches("Should have logged a warning", ".*Error writing error response" + ".*" + uri + ".*"
                + e.getClass().getName() + ".*" + ExceptionMessage);
        lr.reset();
        lr.stopRecorder();
    }

    /**
     * Constructs an instance of the inner class WebProxy.HttpRequest with a url containing "{" and checks that the
     * resulting uri escapes the curly brackets correctly.
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Test
    public void testGetUri() throws Exception {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://somedomain.dk?id=%7B12345%7D"));
        WebProxy.HttpRequest http_request = new WebProxy.HttpRequest(servletRequest);
        URI uri = http_request.getURI();
        assertEquals("Expect uri to be escaped", "http://somedomain.dk?id=%7B12345%7D", uri.toString());
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

}
