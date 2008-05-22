package dk.netarkivet.harvester.datamodel;
/**
 * lc forgot to comment this!
 */

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.TestUtils;


public class TLDInfoTester extends TestCase {
    public TLDInfoTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    public void testAddSubdomain() {
        TLDInfo i = new TLDInfo("um");
        assertEquals("Should know no subdomains to start with",
                0, i.getCount());
        i.addSubdomain("foo.um");
        assertEquals("Should have one after adding one",
                1, i.getCount());
        i.addSubdomain("foo.bar.um");
        assertEquals("Should have two after adding a subdomain",
                2, i.getCount());
        i.addSubdomain("foo.um");
        assertEquals("Should have three after adding the same domain",
                3, i.getCount());

        try {
            i.addSubdomain("bar.dk");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain("foo.museum");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain("um");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain(null);
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertEquals("Should have three after failures",
                3, i.getCount());
    }
}
