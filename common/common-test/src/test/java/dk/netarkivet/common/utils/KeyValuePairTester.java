package dk.netarkivet.common.utils;

import junit.framework.TestCase;

/**
 * 
 * Unit tests for the KeyValuePair class.   
 *
 */
public class KeyValuePairTester extends TestCase{

    public void testGetValue() {
        KeyValuePair<String, String> pair = new KeyValuePair<String, String>("key", "value");
        assertTrue(pair.getKey().equals("key"));
        assertTrue(pair.getValue().equals("value"));
    }
    
    public void testSetValue() {
        KeyValuePair<String, String> pair = new KeyValuePair<String, String>("key", "value");
        try {
            pair.setValue("newValue");
            fail("Should not be able to set value");
        } catch (UnsupportedOperationException e) {
            // Expected behaviour
        }
    }
}
