package dk.netarkivet.harvester.datamodel;

import java.util.Date;
import junit.framework.TestCase;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * 
 * Unit tests for the DomainOwner class.
 *
 */
public class DomainOwnerInfoTester extends TestCase {
    public DomainOwnerInfoTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testCompareTo() throws Exception {
        DomainOwnerInfo i1 = new DomainOwnerInfo(new Date(1), "foo");
        DomainOwnerInfo i2 = new DomainOwnerInfo(new Date(2), "bar");
        DomainOwnerInfo i3 = new DomainOwnerInfo(new Date(0), "baz");

        try {
            i1.compareTo(null);
            fail("Failed to throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertTrue("Earlier domain owner info should compare less",
                i1.compareTo(i2) < 0);
        assertTrue("Later domain owner info should compare greater",
                i2.compareTo(i3) > 0);
        assertTrue("Same domain owner info should compare equals",
                i2.compareTo(i2) == 0);
    }
}