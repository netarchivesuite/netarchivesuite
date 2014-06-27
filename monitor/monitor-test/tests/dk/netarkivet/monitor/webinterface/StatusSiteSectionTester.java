package dk.netarkivet.monitor.webinterface;

import dk.netarkivet.common.webinterface.SiteSection;
import junit.framework.TestCase;

/** Unittest for StatusSiteSection class. */
public class StatusSiteSectionTester extends TestCase {

    public void testMethods() {
        SiteSection s = new StatusSiteSection();
        try {
            s.initialize();
            s.close();
        } catch (Exception e) {
            fail("Should not throw exception: " + e);
        }
    }
    
}
