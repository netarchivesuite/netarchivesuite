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
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Test of the NotifyingURIResolver class.
 */
public class NotifyingURIResolverTester {
    private TestURIResolver uriResolver;
    private TestURIObserver uriObserver;
    private TestResponse response;

    @Before
    public void setUp() {
        uriResolver = new TestURIResolver();
        uriObserver = new TestURIObserver();
        response = new TestResponse();
    }

    /**
     * Test constructor. Only thing really testable is that ArgumentNotValid is thrown on null arguments.
     */
    @Test
    public void testNotifyingURIResolver() throws Exception {
        try {
            new NotifyingURIResolver(null, uriObserver);
            fail("Should throw ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        try {
            new NotifyingURIResolver(uriResolver, null);
            fail("Should throw ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        new NotifyingURIResolver(uriResolver, uriObserver);
    }

    /**
     * Test setURIResolver. Tests null arguments, and that lookup calls are delegated to this resolver after setting it.
     */
    @Test
    public void testSetURIResolver() throws Exception {
        NotifyingURIResolver notifying = new NotifyingURIResolver(uriResolver, uriObserver);
        try {
            notifying.setURIResolver(null);
            fail("Should throw ArgumentNotValid on null argument");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        notifying.lookup(null, response);
        assertEquals("Should have called lookup method on uri resolver", 1, uriResolver.lookupCount);
        TestURIResolver uriResolver2 = new TestURIResolver();
        notifying.setURIResolver(uriResolver2);
        notifying.lookup(null, response);
        assertEquals("Should NOT have called lookup method on uri resolver", 1, uriResolver.lookupCount);
        assertEquals("Should have called lookup method on uri resolver 2", 1, uriResolver2.lookupCount);
    }

    /**
     * Tests lookup. Tests that argument is not checked here - that should be done by the wrapped class. Tests that
     * lookup is delegated to wrapped class with argument. Also tests that result of this method is given to the
     * observing class.
     *
     * @throws Exception
     */
    @Test
    public void testLookup() throws Exception {
        NotifyingURIResolver notifying = new NotifyingURIResolver(uriResolver, uriObserver);
        notifying.lookup(new TestRequest(new URI("http://foo.bar")), response);
        assertEquals("Should have called lookup method on uri resolver", 1, uriResolver.lookupCount);
        assertEquals("Argument should be given unchanged", new URI("http://foo.bar"),
                uriResolver.lookupRequestArgument.getURI());
        assertEquals("Argument should be given unchanged", response, uriResolver.lookupResponseArgument);
        assertEquals("Observers notify method should be called.", 1, uriObserver.notifyCount);
        assertEquals("URI argument should be given unchanged.", new URI("http://foo.bar"),
                uriObserver.notifyURIArgument);
        assertEquals("URI argument should be given uri resolver argument.", 42, uriObserver.notifyResponseCodeArgument);
        try {
            notifying.lookup(null, response);
            assertEquals("Should have called lookup method on uri resolver", 2, uriResolver.lookupCount);
            assertEquals("Argument should be given unchanged", null, uriResolver.lookupRequestArgument);
            assertEquals("Argument should be given unchanged", response, uriResolver.lookupResponseArgument);
            assertEquals("URI argument should be given unchanged.", null, uriObserver.notifyURIArgument);
            assertEquals("URI argument should be given uri resolver argument.", 42,
                    uriObserver.notifyResponseCodeArgument);
        } catch (ArgumentNotValid e) {
            fail("Should NOT throw ArgumentNotValid on null argument."
                    + "This wuold be the job of the wrapped class - this is just" + "a wrapper");
        }
        try {
            notifying.lookup(new TestRequest(new URI("http://foo.bar")), null);
            assertEquals("Should have called lookup method on uri resolver", 3, uriResolver.lookupCount);
            assertEquals("Argument should be given unchanged", new URI("http://foo.bar"),
                    uriResolver.lookupRequestArgument.getURI());
            assertEquals("Argument should be given unchanged", null, uriResolver.lookupResponseArgument);
            assertEquals("URI argument should be given unchanged.", new URI("http://foo.bar"),
                    uriObserver.notifyURIArgument);
            assertEquals("URI argument should be given uri resolver argument.", 42,
                    uriObserver.notifyResponseCodeArgument);
        } catch (ArgumentNotValid e) {
            fail("Should NOT throw ArgumentNotValid on null argument."
                    + "This wuold be the job of the wrapped class - this is just" + "a wrapper");
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

    public static class TestURIObserver extends URIObserver {
        int notifyCount = 0;
        int totalCount = 0;
        URI notifyURIArgument;
        int notifyResponseCodeArgument;

        public void notify(URI uri, int responseCode) {
            notifyCount++;
            totalCount++;
            notifyURIArgument = uri;
            notifyResponseCodeArgument = responseCode;
        }
    }

    public static class TestResponse implements Response {
        public OutputStream getOutputStream() {
            return null;
        }

        public void setStatus(int statusCode) {
        }

        public void setStatus(int statusCode, String reason) {
        }

        public void addHeaderField(String name, String value) {
        }

        public int getStatus() {
            return 0;
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
