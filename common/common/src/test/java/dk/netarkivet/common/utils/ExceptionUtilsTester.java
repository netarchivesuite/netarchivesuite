package dk.netarkivet.common.utils;
/**
 * kfc forgot to comment this!
 */

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.StringAsserts;


public class ExceptionUtilsTester extends TestCase {
    public ExceptionUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** Test that this utility class cannot be instantiated. */
    public void testExceptionUtils() {
        ClassAsserts.assertPrivateConstructor(ExceptionUtils.class);
    }

    /** Test that exceptions are printed including all stacktraces,
     * and that nul exceptions are turned into 'null\n'. */
    public void testGetStackTrace() throws Exception {
        assertEquals(
                "Null exceptions should simply return 'null' and a linebreak",
                "null\n", ExceptionUtils.getStackTrace(null));
        String exceptionMessage = "Test";
        ArgumentNotValid cause = new ArgumentNotValid(exceptionMessage);
        ArgumentNotValid throwable = new ArgumentNotValid(exceptionMessage,
                                                          cause);
        String result = ExceptionUtils.getStackTrace(
                throwable);
        StringAsserts.assertStringContains("Should contain the exception",
                                           ArgumentNotValid.class.getName(),
                                           result);
        StringAsserts.assertStringContains("Should contain the exception msg",
                                           exceptionMessage,
                                           result);
        StringAsserts.assertStringContains("Should contain the stacktrace",
                                           "testGetStackTrace",
                                           result);
        StringAsserts.assertStringContains("Should contain the stacktrace",
                                           "ExceptionUtilsTester",
                                           result);
        StringAsserts.assertStringContains("Should contain the cause",
                                           "Caused by:",
                                           result);
    }
}