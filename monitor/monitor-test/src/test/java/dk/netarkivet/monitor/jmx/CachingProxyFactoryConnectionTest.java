/*
 * #%L
 * Netarchivesuite - monitor - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
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
     * Test method for
     * 'dk.netarkivet.monitor.jmx.CachingProxyFactoryConnectionFactory.CachingProxyFactoryConnection(JMXProxyFactoryConnectionFactory)'
     */
    public void testCachingProxyFactoryConnection() {
        JMXProxyConnectionFactory f = new CachingProxyConnectionFactory(new DummyJMXProxyConnectionFactory());
    }

    /*
     * Test method for 'dk.netarkivet.monitor.jmx.CachingProxyFactoryConnectionFactory.getConnection(String, int, int,
     * String, String)'
     */
    public void testGetConnection() {
        JMXProxyConnectionFactory f = new CachingProxyConnectionFactory(new DummyJMXProxyConnectionFactory());
        f.getConnection("server", 8001, 8101, "monitorRole", "Deterbareløgn");
    }

    // dummy class that implements JMXProxyFactoryConnectionFactory

    private class DummyJMXProxyConnectionFactory implements JMXProxyConnectionFactory {

        public JMXProxyConnection getConnection(String server, int port, int rmiPort, String userName, String password) {
            return null;
        }
    }

    private class DummyJmxProxyFactory {

    }

}
