
package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.testutils.ReflectUtils;
import junit.framework.TestCase;

public class UtilityTester extends TestCase {

    public void testConstants() {
        ReflectUtils.testUtilityConstructor(Constants.class);
    }
    
}
