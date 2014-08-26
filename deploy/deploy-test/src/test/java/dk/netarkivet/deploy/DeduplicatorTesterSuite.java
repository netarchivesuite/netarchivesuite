/*
 * #%L
 * Netarchivesuite - deploy - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.deploy;

//import dk.netarkivet.archive.arcrepository.ARCLookupTester;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

//import dk.netarkivet.harvester.indexserver.CDXOriginCrawlLogIteratorTester;

/**
 * This class runs all tests of Deduplicator functionality. Must be run as part of deduplicator upgrades. FIXME This
 * doesn't test the Heritrix modules.
 *
 */

public class DeduplicatorTesterSuite {

    /**
     *
     * @param suite
     */
    public static void addToSuite(TestSuite suite) {
        // FIXME: deploy cannot see archiver or harvester test classes.
        // suite.addTestSuite(CDXOriginCrawlLogIteratorTester.class);
        // suite.addTestSuite(ARCLookupTester.class);
    }

    /**
     *
     * @return
     */
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite("DeduplicatorTesterSuite");

        addToSuite(suite);

        return suite;
    }

    /**
     *
     * @param args
     */
    public static void main(String args[]) {
        String args2[] = {"-noloading", "dk.netarkivet.DeduplicatorTesterSuite"};
        TestRunner.main(args2);
    }
}
