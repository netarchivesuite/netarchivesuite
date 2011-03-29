/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback;

import dk.netarkivet.wayback.aggregator.AggregationWorkerTest;
import dk.netarkivet.wayback.aggregator.IndexAggregatorTest;
import dk.netarkivet.wayback.indexer.ArchiveFileDAOTester;
import dk.netarkivet.wayback.indexer.ArchiveFileTester;
import dk.netarkivet.wayback.indexer.FileNameHarvesterTester;
import dk.netarkivet.wayback.indexer.HibernateUtilTester;
import dk.netarkivet.wayback.indexer.IndexerQueueTester;
import dk.netarkivet.wayback.indexer.WaybackIndexerTester;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:32:46 AM To
 * change this template use File | Settings | File Templates.
 */
public class WaybackTesterSuite {
            /**
     * Create a test suite just for these tests.
     * @return this testsuite
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(WaybackTesterSuite.class.getName());
        WaybackTesterSuite.addToSuite(suite);
        return suite;
    }

    /**
     * Add the tests here.
     * @param suite The testsuite to be added
     */
    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(ArchiveFileTester.class);
        suite.addTestSuite(ArchiveFileDAOTester.class);
        suite.addTestSuite(ExtractWaybackCDXBatchJobTester.class);
        suite.addTestSuite(FileNameHarvesterTester.class);
        suite.addTestSuite(IndexerQueueTester.class);
        suite.addTestSuite(NetarchiveResourceStoreTester.class);
        suite.addTestSuite(UrlCanonicalizerFactoryTester.class);
        suite.addTestSuite(DeduplicateToCDXAdapterTester.class);
        suite.addTestSuite(ExtractDeduplicateCDXBatchJobTester.class);
        suite.addTestSuite(DeduplicateToCDXApplicationTester.class);
        suite.addTestSuite(HibernateUtilTester.class);
        suite.addTestSuite(WaybackIndexerTester.class);
        suite.addTestSuite(IndexAggregatorTest.class);
        suite.addTestSuite(AggregationWorkerTest.class);
    }

    public static void main(String args[]) {
        String args2[] = {"-noloading", WaybackTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
