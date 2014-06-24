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
                throw new NotImplementedException("Not implemented");
            }
        };
    }
}