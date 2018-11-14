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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Unit-tests for the CommandResolver class.
 */
public class CommandResolverTester {

    @Test
    public void testIsCommandHostRequest() throws Exception {
        assertFalse("Null request should have no host", CommandResolver.isCommandHostRequest(null));
        assertFalse("Request with null uri should have no host", CommandResolver.isCommandHostRequest(new Request() {
            public URI getURI() {
                return null;
            }

            public Map<String, String[]> getParameterMap() {
                throw new NotImplementedException("Not implemented");
            }
        }));
        assertFalse("Request with other uri should not be command host",
                CommandResolver.isCommandHostRequest(makeRequest("http://www.foo.bims")));
        assertTrue("Request with actual localhost name should be command host",
                CommandResolver.isCommandHostRequest(makeRequest("http://" + "netarchivesuite.viewerproxy.invalid"
                        + "/stop?foo=bar")));
    }

    private Request makeRequest(final String uri) {
        return new Request() {
            public URI getURI() {
                try {
                    return new URI(uri);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Illegal URI " + uri, e);
                }
            }

            public Map<String, String[]> getParameterMap() {
                throw new NotImplementedException("Not implemented");
            }
        };
    }
}
