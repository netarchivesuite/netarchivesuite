package dk.netarkivet.common.utils.arc;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: lc
 * Date: Nov 4, 2004
 * Time: 2:38:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ARCKeyTester extends TestCase {
    /** Test that the constructore figures out to use .arc.gz files
     * when given a .dat entry.
     */
    public void testConstructor() {
        ARCKey key1 = new ARCKey("foo.arc", 0);
        assertEquals(key1.getFile().getName(), "foo.arc");
        ARCKey key2 = new ARCKey("foo.dat", 0);
        assertEquals(key2.getFile().getName(), "foo.arc.gz");
    }
}
