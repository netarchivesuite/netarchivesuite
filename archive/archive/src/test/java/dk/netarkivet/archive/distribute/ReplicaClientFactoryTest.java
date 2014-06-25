package dk.netarkivet.archive.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.testutils.ReflectUtils;

public class ReplicaClientFactoryTest {
    
    @Before
    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();
    }

    @After
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
    }
    
    @Test
    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(ReplicaClientFactory.class);
    }
    
    @Test
    public void testList() {
        List<ReplicaClient> clients = ReplicaClientFactory.getReplicaClients();
        
        for(ReplicaClient client : clients) {
            if(client instanceof ChecksumClient) {
                assertEquals("ChecksumClients must be of type " + ReplicaType.CHECKSUM,
                        ReplicaType.CHECKSUM, client.getType());
            } else if(client instanceof BitarchiveClient) {
                assertEquals("BitarchiveClients must be of type " + ReplicaType.BITARCHIVE,
                        ReplicaType.BITARCHIVE, client.getType());
            } else {
                fail("Unknown replica type: " + client.getType());
            }
        }
    }
}
