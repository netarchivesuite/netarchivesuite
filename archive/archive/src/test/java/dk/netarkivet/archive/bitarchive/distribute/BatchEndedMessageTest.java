package dk.netarkivet.archive.bitarchive.distribute;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.testutils.Serial;

public class BatchEndedMessageTest {
    private static BatchEndedMessage bem;

    @Before
    public void setUp(){
        bem = new BatchEndedMessage(Channels.getTheBamon(), "BAId", "MsgId", null);
        bem.setNoOfFilesProcessed(42);
        bem.setFilesFailed(Arrays.asList(new File[]{new File("failed")}));
    }

    @After
    public void tearDown(){
        bem = null;
    }

    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        BatchEndedMessage bem2 = (BatchEndedMessage) Serial.serial(bem);
        assertEquals("Serialization error", relevantState(bem), relevantState(bem2));
    }

    private String relevantState(BatchEndedMessage b) {
        return b.toString();
    }

}
