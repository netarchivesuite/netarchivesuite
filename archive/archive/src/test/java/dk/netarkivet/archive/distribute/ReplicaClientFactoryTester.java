package dk.netarkivet.archive.distribute;

import java.util.List;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.testutils.ReflectUtils;
import junit.framework.TestCase;

public class ReplicaClientFactoryTester extends TestCase {
    
    public void setUp() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();
    }

    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
    }
    
    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(ReplicaClientFactory.class);
    }
    
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
