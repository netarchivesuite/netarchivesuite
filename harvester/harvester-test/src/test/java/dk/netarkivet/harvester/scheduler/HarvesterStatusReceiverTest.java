package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import junit.framework.TestCase;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvesterStatusReceiverTest extends TestCase  {
    private JobDispatcher jobDispatcher;
    private HarvesterStatusReceiver receiver;
    private JMSConnection jmsConnection;
    private HarvestChannelDAO harvestChannelDao;
    
    public void setUp() throws Exception {
        jobDispatcher = mock(JobDispatcher.class);
        jmsConnection = mock(JMSConnection.class);
        harvestChannelDao = mock(HarvestChannelDAO.class);
        receiver = new HarvesterStatusReceiver(jobDispatcher, jmsConnection, harvestChannelDao, new HarvestChannelRegistry());
    }
    
    public void testStatusReception() {
        HarvestChannel highChan = new HarvestChannel("FOCUSED", false, true, "");
        HarvesterReadyMessage readyMessage =
                new HarvesterReadyMessage("Test", highChan.getName());
        when(harvestChannelDao.getByName(highChan.getName())).thenReturn(highChan);
        receiver.onMessage(JMSConnectionMockupMQ.getObjectMessage(readyMessage));
        verify(jobDispatcher).submitNextNewJob(highChan);
    }
    
    public void testInvalidMessageType() {
        CrawlProgressMessage statusmessage =  new CrawlProgressMessage(1,1);
        receiver.onMessage( JMSConnectionMockupMQ.getObjectMessage(statusmessage));
    }
}
