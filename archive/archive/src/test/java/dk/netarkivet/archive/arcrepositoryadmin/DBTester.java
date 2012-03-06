/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.archive.arcrepositoryadmin;

import junit.framework.TestCase;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class DBTester extends TestCase {

    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.TEST_DIR);
    
    public void setUp() {
        mtf.setUp();
    }
    
    public void tearDown() {
        mtf.tearDown();
    }
    
    public void testDBConnect() {
        ReflectUtils.testUtilityConstructor(ArchiveDBConnection.class);
    }
    
    public void testDerbyServerSpecifics() {
        DerbySpecifics ds = new DerbyServerSpecifics();
        
        ds.shutdownDatabase();
        
        try {
            ds.backupDatabase(null, TestInfo.TEST_DIR);
            fail("Should fail");
        } catch (Throwable e) {
            // expected.
        }
    }
    
    public void testDerbyEmbeddedSpecifics() {
        DerbySpecifics ds = new DerbyEmbeddedSpecifics();
        
        assertEquals("Wrong driver class name.", "org.apache.derby.jdbc.EmbeddedDriver", 
                ds.getDriverClassName());

        ds.shutdownDatabase();

        try {
            ds.backupDatabase(null, TestInfo.TEST_DIR);
            fail("Should fail");
        } catch (Throwable e) {
            // expected.
        }
        
        // Cannot test the others!
//        try {
//            ds.backupDatabase(DriverManager.getConnection("jdbc:derby:;shutdown"), TestInfo.TEST_DIR);
//            fail("This should not happen!");
//        } catch (Throwable e) {
//            System.out.println(e);
//            e.printStackTrace();
//        }
    }
}
