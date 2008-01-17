/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.common.utils.cdx;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;



public class CdxUtilsTesterSuite
{
    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite("CdxUtilsTesterSuite");

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ARCFilenameCDXRecordFilterTester.class);
        suite.addTestSuite(CDXReaderTester.class);
        suite.addTestSuite(ExtractCDXJobTester.class);
        suite.addTestSuite(BinSearchTester.class);
        suite.addTestSuite(CDXRecordTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", "dk.netarkivet.common.utils.cdx.CdxUtilsTesterSuite"};
        TestRunner.main(args2);
    }
}
