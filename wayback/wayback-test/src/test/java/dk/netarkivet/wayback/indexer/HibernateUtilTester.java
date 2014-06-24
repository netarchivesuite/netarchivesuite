package dk.netarkivet.wayback.indexer;

import junit.framework.TestCase;
import org.hibernate.Session;

public class HibernateUtilTester extends TestCase {

    /**
     * Tests that we can create an open session.
     */
    public void testGetSession() {
        Session session = HibernateUtil.getSession();
        assertTrue("Session should be connected.", session.isConnected());
        assertTrue("Session should be open.", session.isOpen());
        assertFalse("Session should not be dirty.", session.isDirty());
    }

}
