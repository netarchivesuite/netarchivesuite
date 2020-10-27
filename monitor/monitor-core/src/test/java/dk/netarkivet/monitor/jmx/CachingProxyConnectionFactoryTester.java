/*
 * #%L
 * Netarchivesuite - monitor - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.Set;

import javax.management.ObjectName;

import org.junit.Test;
import dk.netarkivet.common.exceptions.NotImplementedException;

/** Unittest for testing the CachingProxyConnectionFactory class. */
public class CachingProxyConnectionFactoryTester {
    @Test
    public void testGetConnection() throws Exception {
        JMXProxyConnectionFactory factory = new JMXProxyConnectionFactory() {
            public JMXProxyConnection getConnection(String server, int port, int rmiPort, String userName,
                    String password) {
                return connectionDescription(server, port, rmiPort, userName, password);
            }
        };

        final String server1 = "foo.dk";
        final int port1 = 1984;
        final int rmiPort1 = 1066;
        final String userName1 = "george";
        final String password1 = "godwinsson";

        assertNotSame("Two calls to factory should give different results",
                factory.getConnection(server1, port1, rmiPort1, userName1, password1),
                factory.getConnection(server1, port1, rmiPort1, userName1, password1));

        CachingProxyConnectionFactory proxy = new CachingProxyConnectionFactory(factory);

        JMXProxyConnection jmxProxyConnection = proxy.getConnection(server1, port1, rmiPort1, userName1, password1);

        assertSame("Two equals calls to cache should give same results", jmxProxyConnection,
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1));

        assertNotSame("Two different servers should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection("bar.dk", port1, rmiPort1, userName1, password1));

        assertNotSame("Two different ports should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, 1969, rmiPort1, userName1, password1));

        assertNotSame("Two different rmiports should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, 783, userName1, password1));

        assertNotSame("Two different users should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, rmiPort1, "harald", password1));

        assertNotSame("Two different passwords should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, rmiPort1, userName1, "orwell"));

        assertSame("Other equals values should also give same results",
                proxy.getConnection("fnord.com", port1, rmiPort1, userName1, password1),
                proxy.getConnection("fnord.com", port1, rmiPort1, userName1, password1));

        assertSame("First cached item should still exist", jmxProxyConnection,
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1));

    }

    private JMXProxyConnection connectionDescription(String server, int port, int rmiPort, String userName,
            String password) {
        return new JMXProxyConnection() {
            public <T> T createProxy(ObjectName name, Class<T> intf) {
                throw new NotImplementedException("Not implemented");
            }

            public Set<ObjectName> query(String query) {
                throw new NotImplementedException("Not implemented");
            }

            public boolean isLive() {
                return true;
            }

        };
    }
}
