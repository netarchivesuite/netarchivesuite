package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Observable;

import junit.framework.TestCase;

/**
 * Unit-tests of the abstract URIObserver class.
 * Uses a local class TestURIObserver that extends 
 * URIObserver.   
 */
public class URIObserverTester extends TestCase {
    private TestURIObserver uriObserver;

    public URIObserverTester(String s) {
        super(s);
    }

    public void setUp() {
        uriObserver = new TestURIObserver();
    }

    public void tearDown() {
    }

    /** Tests that the Observer update method calls URIObservers notify if and
     * only if update is called with a non-null Response object as argument.
     * @throws Exception
     */
    public void testUpdate() throws Exception {
        uriObserver.update(null, null);
        assertEquals("URIObserver notify should not be called",
                     0, uriObserver.notifyCount);
        uriObserver.update(null, "Test");
        assertEquals("URIObserver notify should not be called",
                     0, uriObserver.notifyCount);
        uriObserver.update(new Observable(), "Test");
        assertEquals("URIObserver notify should not be called",
                     0, uriObserver.notifyCount);
        uriObserver.update(null, new URIObserver.URIResponseCodePair(null, 42));
        assertEquals("URIObserver notify should be called",
                     1, uriObserver.notifyCount);
        assertEquals("URIObserver notify should be called with argument",
                     42, uriObserver.notifyResponseCodeArgument);
        assertEquals("URIObserver notify should be called with argument",
                     null, uriObserver.notifyURIArgument);
        uriObserver.update(new Observable(),
                           new URIObserver.URIResponseCodePair(new URI("http://foo.bar"),
                                                               42));
        assertEquals("URIObserver notify should be called",
                     2, uriObserver.notifyCount);
        assertEquals("URIObserver notify should be called with argument",
                     42, uriObserver.notifyResponseCodeArgument);
        assertEquals("URIObserver notify should be called with argument",
                     new URI("http://foo.bar"), uriObserver.notifyURIArgument);

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