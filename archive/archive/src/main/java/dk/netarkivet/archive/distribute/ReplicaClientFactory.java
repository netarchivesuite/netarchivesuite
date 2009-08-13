package dk.netarkivet.archive.distribute;

import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;

/**
 * This contains a method for retrieving all the replica clients at once.
 */
public class ReplicaClientFactory {

    /**
     * Method for retrieving the clients for the correct replicas.
     * 
     * @return The clients to the different replicas as a list.
     */
    public static List<ReplicaClient> getReplicaClients() {
	// get the channels
        ChannelID[] allBas = Channels.getAllArchives_ALL_BAs();
        ChannelID[] anyBas = Channels.getAllArchives_ANY_BAs();
        ChannelID[] theBamons = Channels.getAllArchives_BAMONs();
        ChannelID[] theCRs = Channels.getAllArchives_CRs();
	
        // initialise the resulting list according to the number of channels.
	List<ReplicaClient> res = new ArrayList<ReplicaClient>(allBas.length);
	
	// extract the replica types and 
        for(int i=0; i < allBas.length; i++) {
            // the theCR for a bitarchive is 'null', and the bitarchive channels
            // are 'null' for the checksumarchive.
            if(theCRs[i] == null) {
        	// add a bitarchive client.
        	res.add(BitarchiveClient.getInstance(allBas[i], 
        		anyBas[i], theBamons[i]));
            } else {
        	// add a checksum client.
        	res.add(ChecksumClient.getInstance(theCRs[i]));
            }
        }
        
	return res;
    }
}
