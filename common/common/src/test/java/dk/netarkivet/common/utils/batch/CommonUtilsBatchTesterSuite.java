/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.common.utils.batch;

import dk.netarkivet.common.utils.batch.BatchFilterTester;
import dk.netarkivet.common.utils.batch.BatchLocalFilesTester;
import dk.netarkivet.common.utils.batch.ByteClassLoaderTester;
import dk.netarkivet.common.utils.batch.FileBatchJobTester;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJobTester;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJobTester;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Suite of unittests for the classes in the 
 * dk.netarkivet.commons.utils.batch package.
 */
public class CommonUtilsBatchTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsBatchTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(BatchFilterTester.class);
        suite.addTestSuite(BatchLocalFilesTester.class);
        suite.addTestSuite(ByteClassLoaderTester.class);
        suite.addTestSuite(FileBatchJobTester.class);
        suite.addTestSuite(LoadableFileBatchJobTester.class);
        suite.addTestSuite(LoadableJarBatchJobTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                CommonUtilsBatchTesterSuite.class.getName() };
        TestRunner.main(args2);
    }
}
