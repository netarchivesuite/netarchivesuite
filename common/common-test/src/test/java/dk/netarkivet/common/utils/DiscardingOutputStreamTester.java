package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

/**
 * 
 * Unit tests for the DiscardingOutputStream class.   
 *
 */
public class DiscardingOutputStreamTester extends TestCase{

    public void testWriteInt() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        try {
            os.write(20);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }

    public void testWriteBytearray() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        byte[] b = null;
        try {
            os.write(b);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }
 
    public void testWriteBytearrayWithArgs() throws IOException {
        OutputStream os = new DiscardingOutputStream();
        byte[] b = null;
        try {
            os.write(b, 0, 20);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        os.close();
    }
}
