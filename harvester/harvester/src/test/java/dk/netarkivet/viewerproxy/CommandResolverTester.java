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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Unit-tests for the CommandResolver class.
 */
public class CommandResolverTester extends TestCase {
    public CommandResolverTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testIsCommandHostRequest() throws Exception {
        assertFalse("Null request should have no host",
                CommandResolver.isCommandHostRequest(null));
        assertFalse("Request with null uri should have no host",
                CommandResolver.isCommandHostRequest(new Request() {
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
                CommandResolver.isCommandHostRequest(makeRequest("http://"
                        + "netarchivesuite.viewerproxy.invalid"
                        + "/stop?foo=bar")));
    }

    private Request makeRequest(final String uri) {
        return new Request(){
            public URI getURI() {
                try {
                    return new URI(uri);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Illegal URI " + uri, e);
                }
            }

            public Map<String, String[]> getParameterMap() {
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }
        };
    }
}