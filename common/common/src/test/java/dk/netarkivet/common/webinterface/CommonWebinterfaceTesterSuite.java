/* File:    $Id: UnitTesterSuite.java 1338 2010-03-17 15:27:53Z svc $
 * Version: $Revision: 1338 $
 * Date:    $Date: 2010-03-17 16:27:53 +0100 (Wed, 17 Mar 2010) $
 * Author:  $Author: svc $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.common.webinterface;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class CommonWebinterfaceTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonWebinterfaceTesterSuite.class.getName());
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(HTMLUtilsTester.class);
        suite.addTestSuite(SiteSectionTester.class);
        suite.addTestSuite(GUIWebServerTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", CommonWebinterfaceTesterSuite.class.getName()};

        TestRunner.main(args2);
    }

}
