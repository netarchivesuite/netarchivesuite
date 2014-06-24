package dk.netarkivet.viewerproxy;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unit-tests of the UnknownCommandResolver class.
 */
@SuppressWarnings({ "unused"})
public class UnknownCommandResolverTester extends TestCase {
    public UnknownCommandResolverTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

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
                //TODO: implement method
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
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }

            public int getStatus() {
                return status;
            }
        };
        int reply =
                ucr.lookup(makeRequest("http://www.flarf.smirk"),
                response);
        assertEquals("Should get underlying resolver's response",
                200, reply);
        reply = ucr.lookup(makeRequest("http://129.0.0.1/hest"), response);
        assertEquals("Should get underlying resolver's response",
                200, reply);
        try {
            reply = ucr.lookup(makeRequest("http://"
                                           + "netarchivesuite.viewerproxy.invalid"
                                           + "/get"), response);
            fail("Should give IOFailure on a command");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should have command in msg",
                    "get", e.getMessage());
        }
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