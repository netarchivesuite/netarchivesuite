
/**
 * Class testing the NullRemoteFile class.
 */

package dk.netarkivet.common.distribute;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import dk.netarkivet.common.exceptions.NotImplementedException;
import junit.framework.TestCase;

public class NullRemoteFileTester extends TestCase {

    public void testNewInstance() {
        RemoteFile nrf1 = NullRemoteFile.getInstance(null, false, false, false);
        assertTrue(nrf1 instanceof NullRemoteFile);
        assertEquals(nrf1.getSize(), 0);
        assertEquals(nrf1.getInputStream(), null);
        assertEquals(nrf1.getName(), null);
        try {
        	nrf1.getChecksum();
        	fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e){
        	// Expected
        }
        OutputStream os = new ByteArrayOutputStream();
        nrf1.appendTo(os);
        try {
            nrf1.appendTo(null);
        } catch (Exception e) {
            fail("Exception not expected with appendTo and null arg ");
        }
    }
}
