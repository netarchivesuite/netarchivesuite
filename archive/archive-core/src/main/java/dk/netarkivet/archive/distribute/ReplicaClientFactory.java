/*
 * #%L
 * Netarchivesuite - archive
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
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
public final class ReplicaClientFactory {
    /**
     * Private constructor. Prevents instantiation of this class.
     */
    private ReplicaClientFactory() {
    }

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
        for (int i = 0; i < allBas.length; i++) {
            // the theCR for a bitarchive is 'null', and the bitarchive channels
            // are 'null' for the checksumarchive.
            if (theCRs[i] == null) {
                // add a bitarchive client.
                res.add(BitarchiveClient.getInstance(allBas[i], anyBas[i], theBamons[i]));
            } else {
                // add a checksum client.
                res.add(ChecksumClient.getInstance(theCRs[i]));
            }
        }

        return res;
    }
}
