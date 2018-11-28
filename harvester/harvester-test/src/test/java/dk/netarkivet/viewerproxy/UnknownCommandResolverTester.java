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
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unit-tests of the UnknownCommandResolver class.
 */
@SuppressWarnings({"unused"})
public class UnknownCommandResolverTester {

    @Test
    public void testLookup() throws Exception {
        URIResolver end = new URIResolver() {
            public int lookup(Request request, Response response) {
                return 200;
            }
        };
        URIResolver ucr = new UnknownCommandResolver(end);
        Response response = new Response() {
            int status = 300;
            public String reason;

            public OutputStream getOutputStream() {
                // TODO: implement method
                throw new NotImplementedException("Not implemented");
            }

            public void setStatus(int statusCode) {
                status = statusCode;
            }

            public void setStatus(int statusCode, String reason) {
                status = statusCode;
                this.reason = reason;
            }

            public void addHeaderField(String name, String value) {
                // TODO: implement method
                throw new NotImplementedException("Not implemented");
            }

            public int getStatus() {
                return status;
            }
        };
        int reply = ucr.lookup(makeRequest("http://www.flarf.smirk"), response);
        assertEquals("Should get underlying resolver's response", 200, reply);
        reply = ucr.lookup(makeRequest("http://129.0.0.1/hest"), response);
        assertEquals("Should get underlying resolver's response", 200, reply);
        try {
            reply = ucr.lookup(makeRequest("http://" + "netarchivesuite.viewerproxy.invalid" + "/get"), response);
            fail("Should give IOFailure on a command");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should have command in msg", "get", e.getMessage());
        }
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
                // TODO: implement method
                throw new NotImplementedException("Not implemented");
            }
        };
    }
}
