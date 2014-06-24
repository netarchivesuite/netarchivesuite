package dk.netarkivet.wayback;

import junit.framework.TestCase;
/** TODO complete unittests. */
@SuppressWarnings({ "unused"})
public class LRUCacheTester extends TestCase {
    
    public void testConstructor() {
        LRUCache.getInstance();
        LRUCache cache = new LRUCache();
    }
    
}
