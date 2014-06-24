package dk.netarkivet.monitor.jmx;

import junit.framework.TestCase;

/**
 * Tests the CachingProxyFactoryConnectionFactory class.
 *
 */
public class CachingProxyFactoryConnectionTest extends TestCase {

    public CachingProxyFactoryConnectionTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'dk.netarkivet.monitor.jmx.CachingProxyFactoryConnectionFactory.CachingProxyFactoryConnection(JMXProxyFactoryConnectionFactory)'
     */
    public void testCachingProxyFactoryConnection() {
        JMXProxyConnectionFactory f = new CachingProxyConnectionFactory(new DummyJMXProxyConnectionFactory());
    }

    /*
     * Test method for 'dk.netarkivet.monitor.jmx.CachingProxyFactoryConnectionFactory.getConnection(String, int, int, String, String)'
     */
    public void testGetConnection() {
        JMXProxyConnectionFactory f = new CachingProxyConnectionFactory(new DummyJMXProxyConnectionFactory());
        f.getConnection("server", 8001, 8101, "monitorRole", "Deterbareløgn");
    }
    
    // dummy class that implements JMXProxyFactoryConnectionFactory 
    
    private class DummyJMXProxyConnectionFactory implements
                                                 JMXProxyConnectionFactory {
        
        public JMXProxyConnection getConnection(String server, int port, int rmiPort,
                String userName, String password) {
            return 
                null;
        }
    }
    
    private class DummyJmxProxyFactory {
        
    }
    
    
    

}
