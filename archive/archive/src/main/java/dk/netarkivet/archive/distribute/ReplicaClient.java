/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.archive.distribute;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.GetMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllChecksumsMessage;
import dk.netarkivet.archive.checksum.distribute.GetAllFilenamesMessage;
import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * Interface for the replica clients.
 * To be used by the BitarchiveClient and the ChecksumClient. 
 */
public interface ReplicaClient {

    /**
     * Method for sending a batch message and retrieving the results.
     * This is only used by the bitarchive replicas.
     * 
     * @param msg The batch message to sent to the replica.
     * @return The batch message which has been sent.
     */
    public BatchMessage batch(BatchMessage msg);
    
    /**
     * Method for sending batch message and retrieving the results.
     * This will only work for Bitarchive replicas.
     * 
     * @param replyChannel The channel where the reply should be sent.
     * @param job The batchjob to execute on the replica.
     * @return The message which has been sent.
     */
    public BatchMessage batch(ChannelID replyChannel, FileBatchJob job);
    
    /**
     * The message for retrieving a record from a arc-file in the replica.
     * This is only used by the bitarchive replicas.
     * 
     * @param msg The message for retrieving the record in a arc-file.
     */
    public void get(GetMessage msg);
    
    /**
     * The message for retrieving an entire file from the replica.
     * This is only used by the bitarchive replicas.
     * 
     * @param msg The message for retrieving the file. 
     */
    public void getFile(GetFileMessage msg);
    
    /**
     * Message for deleting and retrieving a file from a archive.
     *  
     * @param msg The message for retrieving the file. 
     */
    public void removeAndGetFile(RemoveAndGetFileMessage msg);
    
    /**
     * Uploads a file to the replica archive.
     * This should create the UploadMessage and send it.
     * 
     * @param rf The remote file
     */
    public void upload(RemoteFile rf);
   
    /**
     * Retrieves the checksum for a specific arc file.
     * The GetChecksumMessage is sent along to the archive.
     * 
     * @param msg The GetChecksumMessage to be send to the replica.
     */
    public void getChecksum(GetChecksumMessage msg);
    
    /**
     * Retrieves the checksum for a specific file.
     * The method creates and sends the GetChecksumMessage to the archive.
     * 
     * @param replyChannel The channel where the reply should be sent.
     * @param filename The name of the file to retrieve the checksum from.
     * @return The message, after it has been sent.
     */
    public GetChecksumMessage getChecksum(ChannelID replyChannel, 
            String filename);
    
    /**
     * Retrieves the names of all the arc file in the replica archive.
     * 
     * @param msg The GetAllFilenamesMessage to sent to the replica. 
     */
    public void getAllFilenames(GetAllFilenamesMessage msg);
    
    /**
     * Retrieves the checksum for all the arc files in the replica archive.
     * This method is the ChecksumReplica equivalent to running a ChecksumJob.
     * 
     * The message is sent from this method.
     * 
     * @param msg The message for retrieving all the checksums.
     */
    public void getAllChecksums(GetAllChecksumsMessage msg);
    
    /**
     * For retrieving the type of archive.
     * This will either be 'bitArchive' or 'checksumArchive'.
     * 
     * @return The type of archive.
     */
    public ReplicaType getType();
    
    /**
     * For correcting a erroneous file in the archive. This creates and sends 
     * the message for correcting the corrupted file entry.
     * 
     * @param arcfile The file which is to replace the wrong file within the 
     * archive.
     * @param incorrectChecksum The incorrect checksum, which has been recorded. 
     */
    public void correct(RemoteFile arcfile, String incorrectChecksum);
    
    /**
     * Close the replica client.
     */
    public void close();
}
