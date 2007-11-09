package dk.netarkivet.testutils.preconfigured;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.archive.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;

public class MockupIndexServer implements TestConfigurationIF, MessageListener {
    private File resultFile;
    private boolean responseOK = true;
    private List<IndexRequestMessage> msgs = new ArrayList<IndexRequestMessage>();
    private boolean multiFile = false;
    private String origDir;

    public MockupIndexServer(File resultFile) {
        this.resultFile = resultFile;
    }
    public void setUp() {
        setResponseSuccessfull(true);
        origDir = Settings.get(Settings.CACHE_DIR);
        Settings.set(Settings.CACHE_DIR, Settings.get(Settings.DIR_COMMONTEMPDIR));
        JMSConnectionFactory.getInstance().setListener(Channels.getTheIndexServer(), this);
    }
    public void tearDown() {
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheIndexServer(), this);
        Settings.set(Settings.CACHE_DIR, origDir);
    }
    public void setResponseSuccessfull(boolean isOk) {
        responseOK = isOk;
    }
    public void resetMsgList() {
        msgs.clear();
    }
    public List<IndexRequestMessage> getMsgList() {
        return msgs;
    }
    public void onMessage(Message message) {
        IndexRequestMessage irm = (IndexRequestMessage) JMSConnection.unpack(message);
        msgs.add(irm);
        irm.setFoundJobs(irm.getRequestedJobs());
        if (irm.getRequestType() == RequestType.CDX) {
            RemoteFile resultFile = RemoteFileFactory
                    .getInstance(this.resultFile, true, false, true);
            irm.setResultFile(resultFile);
        } else {
            List<RemoteFile> resultFiles = new ArrayList<RemoteFile>();
            File[] files = resultFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        resultFiles.add(RemoteFileFactory.getInstance(f, true,
                                                                      false,
                                                                      true));
                    }
                }
            }
            irm.setResultFiles(resultFiles);
        }
        if (!responseOK) {
            irm.setNotOk("Test setting");
        }
        JMSConnectionFactory.getInstance().reply(irm);
    }
}
