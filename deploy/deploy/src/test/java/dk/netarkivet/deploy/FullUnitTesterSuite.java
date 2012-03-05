/* File:    $Id: UnitTesterSuite.java 2273 2012-02-24 12:40:49Z mss $
 * Version: $Revision: 2273 $
 * Date:    $Date: 2012-02-24 13:40:49 +0100 (Fre, 24 Feb 2012) $
 * Author:  $Author: mss $
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

package dk.netarkivet.deploy;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.common.CleanupSuite;
import dk.netarkivet.wayback.WaybackTesterSuite;

/**
 * This class runs all the module unit tests.
 */
public class FullUnitTesterSuite {
    public static void addToSuite(TestSuite suite) {

        dk.netarkivet.common.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.harvester.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.archive.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.viewerproxy.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.viewerproxy.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.wayback.UnitTesterSuite.addToSuite(suite);
        dk.netarkivet.deploy.UnitTesterSuite.addToSuite(suite);
        /*
         * Dummy testersuite to cleanup after the tests.
         */
        CleanupSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(FullUnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", FullUnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
