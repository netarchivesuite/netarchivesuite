package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import junit.framework.TestCase;

public class ChecksumStatusTester extends TestCase {

    public void testFromOrdinal() {
        assertEquals(ChecksumStatus.UNKNOWN, ChecksumStatus.fromOrdinal(0));
        assertEquals(ChecksumStatus.CORRUPT, ChecksumStatus.fromOrdinal(1));
        assertEquals(ChecksumStatus.OK, ChecksumStatus.fromOrdinal(2));
        try {
            ChecksumStatus.fromOrdinal(3);
            fail("Should throw ArgumentNotValid with argument > 2");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
      }
}
