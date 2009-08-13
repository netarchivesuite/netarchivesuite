package dk.netarkivet.archive.checksum.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.ChecksumArchiveAPI;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;

/**
 * Any subclass must be invoked through a method called 'getInstance'.
 */
public abstract class ChecksumServerAPI extends ArchiveMessageHandler 
        implements CleanupIF {
    
    /**
     * The archive which contain the actual data.
     */
    protected ChecksumArchiveAPI cs;
    
    /**
     * The JMS connection.
     */
    protected JMSConnection jmsCon;
    
    /**
     * The unique id for this instance.
     */
    protected String checksumAppId;

    protected ChannelID theCR;

    abstract void close();
    abstract public void cleanup();
    
    abstract public String getAppId();
    abstract protected String createAppId();

    abstract public void visit(UploadMessage msg);
    abstract public void visit(CorrectMessage msg);
    abstract public void visit(GetChecksumMessage msg);
    
//    abstract public void visit(RemoveAndGetFileMessage msg);
//    abstract public void visit(final BatchMessage msg);
//    abstract public void visit(GetFileMessage msg);
    
    
}
