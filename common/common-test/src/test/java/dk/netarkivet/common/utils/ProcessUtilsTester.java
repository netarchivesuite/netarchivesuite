package dk.netarkivet.common.utils;

import junit.framework.TestCase;

/**
 * Unit-test of ProcessUtils class.
 * 
 */
public class ProcessUtilsTester extends TestCase {
    public ProcessUtilsTester(String s) {
        super(s);
    }

    /**
     * 
     * FIXME: This test seems to test a platform dependent functionality. 
     * 
     * It is a bad idea to have unit tests which only works an a specific 
     * platform.
     */
    public void testWaitFor() throws Exception {
        // Test that we can wait for a process that doesn't work
        // This test only works on Linux.
        Process p = Runtime.getRuntime().exec("sleep 1");
        long t1 = System.currentTimeMillis();
        final long maxMillisToWait =  500L;
        Integer exit = ProcessUtils.waitFor(p, maxMillisToWait);
        assertNull("Should have no exit code after a short wait", exit);
        assertFalse("Should not be in interrupted state", 
                Thread.interrupted());
        long t2 = System.currentTimeMillis();
        assertTrue("At least 500 ms should have passed, but it only took "+ 
        		(t2 - t1) + "ms", (t2 - t1) > maxMillisToWait);
        long extraLongWait = maxMillisToWait * 2;
        exit = ProcessUtils.waitFor(p, extraLongWait);
        assertEquals("Should have exit code 0 after a long wait", 0, 
                (int) exit);
        assertFalse("Should not be in interrupted state", 
                Thread.interrupted());

        p = Runtime.getRuntime().exec("dir slark");
        exit = ProcessUtils.waitFor(p, maxMillisToWait);
        assertFalse("Should have exit code non-0 after a long wait", 0 == exit);
        Thread.currentThread();
		assertFalse("Should not be in interrupted state", 
                Thread.interrupted());
    }
}
