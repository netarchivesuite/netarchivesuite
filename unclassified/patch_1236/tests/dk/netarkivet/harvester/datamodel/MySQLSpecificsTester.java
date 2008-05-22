/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.datamodel;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;

public class MySQLSpecificsTester extends TestCase {
    public MySQLSpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        Settings.set(Settings.DB_SPECIFICS_CLASS,
        "dk.netarkivet.harvester.datamodel.MySQLSpecifics");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        Settings.reload(); // Reset Settings so Settings.DB_SPECIFICS_CLASS is restored to standard value
    }
    
    public void testLoadClass() {
        DBSpecifics instance = DBSpecifics.getInstance(
                Settings.DB_SPECIFICS_CLASS);
    }
    
   public void testGetDriverClassName() {
           DBSpecifics instance = DBSpecifics.getInstance(
                   Settings.DB_SPECIFICS_CLASS);
           assertEquals("Wrong driver", instance.getDriverClassName(),
            "com.mysql.jdbc.Driver");
    }
}
