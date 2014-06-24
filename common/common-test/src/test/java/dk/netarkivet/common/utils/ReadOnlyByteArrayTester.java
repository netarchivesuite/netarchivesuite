package dk.netarkivet.common.utils;

import junit.framework.TestCase;

/**
 * 
 * Unit tests for the {@link ReadOnlyByteArray} class.   
 *
 */
public class ReadOnlyByteArrayTester extends TestCase{

    public void testClassFunctionality() {
        try {
            new ReadOnlyByteArray(null);
        } catch(Exception e) {
            fail("new ReadOnlyByteArray(null) should not thrown exception: " + e);
        }
        byte[] emptyArray = new byte[]{};
        ReadOnlyByteArray roba = new ReadOnlyByteArray(emptyArray);
        assertTrue(roba.length() == 0);
        try {
            roba.get(0);
            fail("roba.get(0) should not be accepted");
        } catch (Exception e) {
            // Expected
        }
        
        byte[] notEmptyArray = new byte[]{22,42};
        roba = new ReadOnlyByteArray(notEmptyArray);
        assertTrue(roba.length() == 2);
        assertTrue(22 == roba.get(0));
        assertTrue(42 == roba.get(1));
    }
    
}
