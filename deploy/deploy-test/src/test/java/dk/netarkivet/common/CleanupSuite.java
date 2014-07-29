/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class CleanupSuite {

	    /**
	     * Create a test suite just for these tests.
	     * @return this testsuite
	     */
	    public static Test suite() {
	        TestSuite suite;
	        suite = new TestSuite(CleanupSuite.class.getName());
	        CleanupSuite.addToSuite(suite);
	        return suite;
	    }

	    /**
	     * Add the tests here.
	     * @param suite The testsuite to be added
	     */
	    public static void addToSuite(TestSuite suite) {
	        suite.addTestSuite(CleanupTester.class);
	       
	    }

	    public static void main(String args[]) {
	        String args2[] = {"-noloading", CleanupSuite.class.getName()};
	        TestRunner.main(args2);
	    }
	
}
