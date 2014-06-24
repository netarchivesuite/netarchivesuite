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
