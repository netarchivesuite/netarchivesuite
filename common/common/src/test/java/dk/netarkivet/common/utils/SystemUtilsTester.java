/*$Id$
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
package dk.netarkivet.common.utils;

import dk.netarkivet.common.Constants;

import junit.framework.TestCase;

public class SystemUtilsTester extends TestCase {
   
    public void testGetLocalIP(){
        String ip = SystemUtils.getLocalIP();
        String[] parts = ip.split("\\.");
        assertTrue("Expected at least four parts in the IP adress " + ip,
                   parts.length >= 4);
    }

    /** Tests getting hostname.
     * This is nearly impossible, but we _can_ check that an IP address is not
     * returned, and that it at least does not throw an exception.
     * @throws Exception
     */
    public void testGetLocalHostName() throws Exception {
        String result = SystemUtils.getLocalHostName();
        assertFalse("Should not be an IPv4 address",
                    Constants.IP_KEY_REGEXP.matcher(result).matches());
        assertFalse("Should not be an IPv6 address",
                    Constants.IPv6_KEY_REGEXP.matcher(result).matches());
    }
}
