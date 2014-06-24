package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.testutils.Serial;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Apr 11, 2005
 * Time: 12:51:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class BatchEndedMessageTester extends TestCase{
    private static BatchEndedMessage bem;

    public void setUp(){
        bem = new BatchEndedMessage(Channels.getTheBamon(), "BAId", "MsgId", null);
        bem.setNoOfFilesProcessed(42);
        bem.setFilesFailed(Arrays.asList(new File[]{new File("failed")}));
    }

    public void tearDown(){
        bem = null;
    }

    public void testSerializability() throws IOException, ClassNotFoundException {
        BatchEndedMessage bem2 = (BatchEndedMessage) Serial.serial(bem);
        assertEquals("Serialization error", relevantState(bem), relevantState(bem2));
    }

    private String relevantState(BatchEndedMessage b) {
        return b.toString();
    }

}
