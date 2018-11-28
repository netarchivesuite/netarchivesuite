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

import java.net.URI;
import java.util.Observable;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit-tests of the abstract URIObserver class. Uses a local class TestURIObserver that extends URIObserver.
 */
public class URIObserverTester {
    private TestURIObserver uriObserver;

    @Before
    public void setUp() {
        uriObserver = new TestURIObserver();
    }

    /**
     * Tests that the Observer update method calls URIObservers notify if and only if update is called with a non-null
     * Response object as argument.
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        uriObserver.update(null, null);
        assertEquals("URIObserver notify should not be called", 0, uriObserver.notifyCount);
        uriObserver.update(null, "Test");
        assertEquals("URIObserver notify should not be called", 0, uriObserver.notifyCount);
        uriObserver.update(new Observable(), "Test");
        assertEquals("URIObserver notify should not be called", 0, uriObserver.notifyCount);
        uriObserver.update(null, new URIObserver.URIResponseCodePair(null, 42));
        assertEquals("URIObserver notify should be called", 1, uriObserver.notifyCount);
        assertEquals("URIObserver notify should be called with argument", 42, uriObserver.notifyResponseCodeArgument);
        assertEquals("URIObserver notify should be called with argument", null, uriObserver.notifyURIArgument);
        uriObserver.update(new Observable(), new URIObserver.URIResponseCodePair(new URI("http://foo.bar"), 42));
        assertEquals("URIObserver notify should be called", 2, uriObserver.notifyCount);
        assertEquals("URIObserver notify should be called with argument", 42, uriObserver.notifyResponseCodeArgument);
        assertEquals("URIObserver notify should be called with argument", new URI("http://foo.bar"),
                uriObserver.notifyURIArgument);

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

}
