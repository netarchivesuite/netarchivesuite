package dk.netarkivet.archive.checksum.distribute;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Proxy for remote checksum archive.
 * Establishes the jms connection to the remote checksum archive.
 * 
 * Can be used in combination with any type of ChecksumServerAPI.
 */
public class ChecksumClient implements ReplicaClient {
    // Each message is assigned a message id
    protected static final Log log
            = LogFactory.getLog(ChecksumClient.class.getName());

    // The instance.
    private static ChecksumClient instance;
    
    // Connection to JMS provider
    private JMSConnection jmsCon;

    // connection information
    // The connection to contact all checksum archives.
    private ChannelID the_CR;
    // The client.
    private ChannelID clientId = Channels.getTheRepos();
    
    /**
     * The constructor.
     * Cannot be used directly, use getInstance instead.
     * 
     * @param all_cs_in The channel for contacting all checksum archives. 
     * @param any_cs_in The channel for contacting any checksum archive.
     * @throws IOFailure If there is a problem with the connection.
     */
    private ChecksumClient(ChannelID the_CR_in) 
            throws IOFailure {
        this.the_CR = the_CR_in;
        jmsCon = JMSConnectionFactory.getInstance();
    }
    
    /**
     * The method for retrieving the invoked the instance. 
     * If not invoked yet, then invoke.
     * 
     * @param all_cs_in The channel for contacting all checksum archives. 
     * @param any_cs_in The channel for contacting any checksum archive.
     * @return The instance.
     * @throws IOFailure If there is a problem with the connection.
     */
    public static ChecksumClient getInstance(ChannelID the_CR_in) 
            throws IOFailure {
	// validate arguments
	ArgumentNotValid.checkNotNull(the_CR_in, "ChannelID all_cs_in");

	// Create instance if not created already.
	if(instance == null) {
	    instance = new ChecksumClient(the_CR_in);
	}
	return instance;
    }

    /**
     * Method for correcting a entity in the archive.
     * This will remove the old entry and upload the arcfile as the new.
     * 
     * @param arcfile The RemoteFile which should correct the current one in 
     * the archive, which is wrong.
     */
    @Override
    public void correct(RemoteFile arcfile, String checksum) {
	// validate argument
	ArgumentNotValid.checkNotNull(arcfile, "RemoteFile arcfile");
	
	// create and send message.
	CorrectMessage cmsg = new CorrectMessage(the_CR, clientId, checksum, 
		arcfile);
	jmsCon.send(cmsg);
	
	// log that the message has been sent.
	log.debug("\nSending correct message: \n" + cmsg.toString());
    }

    /**
     * A method for retrieving the names of all the files in the archive.
     * 
     * @return The GetAllFilenamesMessage when it has been sent. Then the reply 
     * of the message will be handled by the invoker of this method.
     */
    @Override
    public GetAllFilenamesMessage getAllFilenames() {
	// create and send message
	GetAllFilenamesMessage gafmsg = new GetAllFilenamesMessage(the_CR, 
		clientId, Settings.get(CommonSettings.USE_REPLICA_ID));
	jmsCon.send(gafmsg);
	
	// log message.
	log.debug("\nSending GetAllFilenamesMessage:\n " + gafmsg.toString());

	// return the message, so the results can be extracted.
	return gafmsg;
    }

    /**
     * Method for retrieving the checksum of a specific arcfile within 
     * the archive.
     * 
     * @param The GetChecksumMessage when it has been sent. Then the reply of 
     * the message will be handled by the invoker of this method.
     */
    @Override
    public GetChecksumMessage getChecksum(String arcName) {
	// Validate arguments
	ArgumentNotValid.checkNotNullOrEmpty(arcName, "String arcName");
	
	// change from set to list
	String arcList = arcName;
	
	// create and send message.
	GetChecksumMessage gcmsg = new GetChecksumMessage(the_CR, clientId, 
		arcList);
	jmsCon.send(gcmsg);
	
	// log what we are doing.
	log.debug("\nSending GetChecksumMessage: \n" + gcmsg.toString());
	
	// return the message so the results can be extracted.
	return gcmsg;
    }

    /**
     * Method for retrieving the type of replica.
     * 
     * @return The type of this replica.
     */
    @Override
    public ReplicaType getType() {
	// Returns the current replica type in the settings.
	return ReplicaType.CHECKSUM;
    }

    /**
     * Method for uploading a file to the archive. This is only uploaded to one
     * of the archives, not all of them. Thus using the 'any' channel.
     * 
     * @param rf The file to upload to the archive.
     */
    @Override
    public void upload(RemoteFile rf) {
	// validate arguments.
        ArgumentNotValid.checkNotNull(rf, "rf");
        
        // create and send message.
        UploadMessage up = new UploadMessage(the_CR, clientId, rf);
        jmsCon.send(up);

        // log message
        log.debug("Sending upload message\n" + up.toString());
    }

    @Override
    public BatchMessage batch(ChannelID replyChannel, FileBatchJob job) {
	// TODO Auto-generated method stub
	throw new NotImplementedException("Checksum replicas cannot handle "
		+ "BatchJobs.");
    }

    @Override
    public BatchMessage batch(BatchMessage msg) {
	// TODO Auto-generated method stub
	throw new NotImplementedException("Checksum replicas cannot handle "
		+ "BatchJobs.");
    }

    @Override
    public void get(GetMessage msg) {
	// TODO Auto-generated method stub
	throw new NotImplementedException("Checksum replicas cannot handle "
		+ "GetMessage.");
    }

    @Override
    public void getFile(GetFileMessage gfm) {
	// TODO Auto-generated method stub
	throw new NotImplementedException("Cannot retrieve a file from a"
			+ "checksum replica.");
    }

    @Override
    public void removeAndGetFile(RemoveAndGetFileMessage msg) {
	// TODO Auto-generated method stub
	throw new NotImplementedException("Cannot retrieve a file from a"
		+ "checksum replica.");
    }

    @Override
    public void close() {
	// TODO Auto-generated method stub
	if(instance != null) {
	    instance = null;
	}
    }
}
