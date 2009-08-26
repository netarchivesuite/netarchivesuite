package dk.netarkivet.wayback;

import junit.framework.TestCase;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;

import dk.netarkivet.common.utils.Settings;

/**
 *
 */
public class UrlCanonicalizerFactoryTester extends TestCase {

    public void testGetDefaultUrlCanonicalizer() {
        UrlCanonicalizer uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals("Expect default to return and instance of "
                     + "NetarchiveSuiteAggressiveUrlCanonicalizer class",
                     dk.netarkivet.wayback.
                             NetarchiveSuiteAggressiveUrlCanonicalizer.class,
                     uc1.getClass());
        Settings.set(WaybackSettings.URL_CANONICALIZER_CLASSNAME, "org.archive.wayback.util.url.IdentityUrlCanonicalizer");
        uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals("Expect to get IdentityUrlCanonicalizer", IdentityUrlCanonicalizer.class, uc1.getClass());
    }


}
