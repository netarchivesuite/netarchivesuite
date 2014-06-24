package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.Serial;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 3, 2005
 * Time: 10:29:56 AM
 * To change this template use File | Settings | File Templates.
 *
 */
@SuppressWarnings({ "serial"})
public class BatchMessageTester extends TestCase {
    // Need a couple of queues for the constructors for the messages
    private ChannelID q1 = TestInfo.QUEUE_1;
    private static FileBatchJob job;

    /**
     *
     */
    public void setUp() throws Exception {
        job = new TestBatchJob();
        super.setUp();
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testBatchMessageSerializable() throws IOException, ClassNotFoundException {
        BatchMessage bm = new BatchMessage(q1, job, Settings.get(
                CommonSettings.USE_REPLICA_ID));
        BatchMessage bm2 = (BatchMessage) Serial.serial(bm);
        assertEquals("Serializability failure for BatchMessage", relevantState(bm), relevantState(bm2));
    }

    private String relevantState(BatchMessage bm){
        return bm.toString();
    }

    private static class TestBatchJob extends FileBatchJob{

        public void initialize(OutputStream os) {
        }
        public void finish(OutputStream os) {
        }
        public boolean processFile(File file, OutputStream os) {
            return true;
        }
        public String toString(){return "a string";}
    }



}
