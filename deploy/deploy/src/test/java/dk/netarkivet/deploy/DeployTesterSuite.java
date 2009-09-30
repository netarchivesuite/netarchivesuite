/*$Id: DeployTesterSuite.java 470 2008-08-20 16:08:30Z svc $
* $Revision: 470 $
* $Date: 2008-08-20 18:08:30 +0200 (Wed, 20 Aug 2008) $
* $Author: svc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

public class DeployTesterSuite {
    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite(DeployTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(DeployTester.class);
        suite.addTestSuite(CompleteSettingsTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", DeployTesterSuite.class.getName()};
        TestRunner.main(args2);
    }

}
