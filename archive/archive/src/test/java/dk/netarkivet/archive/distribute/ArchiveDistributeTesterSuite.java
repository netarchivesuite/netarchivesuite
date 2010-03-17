/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 *  Unit-tester suite for the tests for package dk.netarkivet.archive.distribute.
 *
 */
public class ArchiveDistributeTesterSuite {
    /**
     * Create a test suite just for these tests.
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(ArchiveDistributeTesterSuite.class.getName());
        ArchiveDistributeTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArchiveMessageHandlerTester.class);
        suite.addTestSuite(ReplicaClientFactoryTester.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", ArchiveDistributeTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
