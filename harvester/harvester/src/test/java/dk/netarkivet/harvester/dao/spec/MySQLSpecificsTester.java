/* File:        $Id: MySQLSpecificsTester.java 2251 2012-02-08 13:03:03Z mss $
 * Revision:    $Revision: 2251 $
 * Author:      $Author: mss $
 * Date:        $Date: 2012-02-08 14:03:03 +0100 (Wed, 08 Feb 2012) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.dao.spec;

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class MySQLSpecificsTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public MySQLSpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        rs.setUp();
        Settings.set(CommonSettings.DB_SPECIFICS_CLASS,
                     "dk.netarkivet.harvester.datamodel.MySQLSpecifics");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        rs.tearDown();
    }
    
    public void testLoadClass() {
        DBSpecifics instance = DBSpecifics.getInstance(
                CommonSettings.DB_SPECIFICS_CLASS);
        assertNotNull("instance should not be null", instance);
    }
    
   public void testGetDriverClassName() {
           DBSpecifics instance = DBSpecifics.getInstance(
                   CommonSettings.DB_SPECIFICS_CLASS);
           assertEquals("Wrong driver", instance.getDriverClassName(),
            "com.mysql.jdbc.Driver");
    }
}
