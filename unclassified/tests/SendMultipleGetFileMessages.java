import java.io.File;
import java.util.Iterator;
import java.util.List;

import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * Program that sends a lot of getFile messages to bitarchives.
 * 
 */
public class SendMultipleGetFileMessages {

    /**
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println(
                    "arguments missing. Expected <delay in seconds> <numberOfmessages> <file w/ filenames>");
            System.exit(1);
        }
        int delay = Integer.parseInt(args[0]);
        long delayMillis = delay * 1000L;
        Thread.sleep(delayMillis);
        long messagesToSend = Long.parseLong(args[1]);
        File filenamesFile = new File(args[2]);
        if (!filenamesFile.exists()) {
            System.err.println("File does not exist");
            System.exit(1);
        }
        
        List<String>filenames = FileUtils.readListFromFile(filenamesFile);    
        if (filenames.isEmpty()) {
            System.err.println("No filenames in given file: " 
                    + filenamesFile.getAbsolutePath());
            System.exit(1);
        }   
        
        Iterator<String> iterator = filenames.iterator();
        String repId = Settings.get(CommonSettings.USE_REPLICA_ID);
        
        System.out.println("System will now commence sending " + messagesToSend + " GetFileMessages " 
                + " to replica " + repId + " w/ a delay of " + delay + " seconds between them");
        
        JMSConnection con = JMSConnectionFactory.getInstance();
        String filename = null;
        for (long current=0; current < messagesToSend; current++) {
            if (!iterator.hasNext()) {
                // Reiterate
                iterator = filenames.iterator();
            }
            filename = iterator.next();
            System.out.println("Sending GetFileMessage-message (" + current 
                    + ") for presumed existing file " + filename);
            sendGetFileMessage(filename, repId, con);
            Thread.sleep(delayMillis);
        }
        
        con.cleanup();
    }
    
    public static void sendGetFileMessage(String filename, String repId, JMSConnection con) {
        ChannelID error = Channels.getError();
        ChannelID bamon = Channels.getAllBa();
        ArchiveMessage msg = new GetFileMessage(bamon, error, filename, repId);
        //ArchiveMessage msg = new GetChecksumMessage(bamon, error, filename, repId); 
        con.send(msg);
    }
    

}
