/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.jmx;

/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

import junit.framework.TestCase;


public class JMXUtilsTester extends TestCase {
    public JMXUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
/*
    public void testGetAttribute() throws Exception {
        MBeanServerConnection connection = new TestMBeanServerConnection(0);
        Object ret = connection.getAttribute("aBean", "anAttribute");
        assertEquals("Should have composed bean/attribute name",
                     "aBean:anAttribute", ret);

        fail("Test not implemented yet");
    }

    class TestMBeanServerConnection extends MBeanServerConnection {
        private int failCount;

        /** Create a test MBeanServerConnection that fails a number of times.
         *
         * @param failCount Number of times the getAttribute/executeCommand
         * methods should be called before they succeed.
         */
    /*
        TestMBeanServerConnection(int failCount) {
            this.failCount = failCount;
        }
        public Object getAttribute(ObjectName beanName, String attribute) {
            if (failCount-- > 0) {
                throw new InstanceNotFoundException();
            }
            return beanName + ":" + attribute;
        }
    }
*/

}
